/*
 * Copyright 2015 Bekwam, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bekwam.resignator;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

/**
 * Unsigns a JAR by unpacking it, stripping signature related items from the contents including META-INF/MANIFEST.MF,
 * and repacks
 *
 * @author carl_000
 */
public class UnsignCommand {

    private Logger logger = LoggerFactory.getLogger(UnsignCommand.class);

    @Inject
    ActiveConfiguration activeConfiguration;

    private final int TIMEOUT_SECS = 10;

    private Path tempDir = null;

    public void unsignJAR(Path sourceJARFile, Path targetJARFile, Consumer<String> observer) throws CommandExecutionException {

        if( logger.isDebugEnabled() ) {
            logger.debug("[UNSIGN] sourceJARFilePath={}, targetJARFilePath={}", sourceJARFile, targetJARFile);
        }

        //
        // Verify source jar
        //
        observer.accept("Verifying source JAR");
        File sourceJarFile = verifySource(sourceJARFile);

        if(logger.isDebugEnabled() ) {
            logger.debug("[UNSIGN] source jar file name={}", sourceJarFile.getName());
        }

        //
        // Create a temporary and unique folder
        //
        observer.accept("Creating temp dir");
        Path appDir = createTempDir();

        Preconditions.checkNotNull( tempDir );

        //
        // Register cleanup handler
        //
        observer.accept("Registering cleanup handler");
        registerCleanup();

        //
        // Copy jarFile to temp folder
        //
        observer.accept("Copying JAR");
        Path workingJarFile = copyJAR(sourceJarFile);

        //
        // Unpack JAR
        //
        observer.accept("Unpacking JAR");
        unJAR(workingJarFile.toString(), tempDir);
        observer.accept("Deleting working JAR file");
        workingJarFile.toFile().delete();  // don't include for later re-jar operation

        //
        // Locate .SF files. Remove these and corresponding signature blocks like .RSA.
        //
        observer.accept("Removing old signature blocks");
        File metaInfDir = new File(tempDir.toFile(), "META-INF");
        removeSigs(metaInfDir);

        //
        // Strip the SHA.*\: entries from the MANIFEST.MF (ok to leave Name: lines?)
        //
        observer.accept("Editing MANIFEST.MF");
        editManifest(metaInfDir);

        //
        // Repack JAR
        //
        observer.accept("Repacking JAR");
        repackJAR(targetJARFile, appDir);
    }

    private File verifySource(Path sourceJARFilePath) throws CommandExecutionException {
        File sourceJarFile = sourceJARFilePath.toFile();
        if( !sourceJarFile.exists() ) {
            String msg = String.format("source jar file %s does not exist", sourceJARFilePath);
            logger.error( msg );
            throw new CommandExecutionException( msg );
        }
        return sourceJarFile;
    }

    private Path createTempDir() throws CommandExecutionException {
        Path appDir = Paths.get(
                System.getProperty("user.home"),
                ".resignator"
        );

        try {
            tempDir = Files.createTempDirectory(appDir, "");
        } catch(IOException exc) {
            String msg = String.format("can't create tempDir %s",  tempDir.toString());
            logger.error( msg, exc );
            throw new CommandExecutionException( msg );
        }
        return appDir;
    }

    private void registerCleanup() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (logger.isDebugEnabled()) {
                    logger.debug("[UNSIGN] shutting down unsign jar command");
                }
                if (tempDir != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[UNSIGN] tempDir is not null");
                    }
                    try {
                        Path d = Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("[UNSIGN] deleting file={}", file.getFileName());
                                }
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                if (exc == null) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("[UNSIGN] deleting dir={}", dir.getFileName());
                                    }
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                } else {
                                    // directory iteration failed
                                    throw exc;
                                }
                            }
                        });
                    } catch (Exception exc) {
                        logger.error("error removing tempDir=" + tempDir, exc);
                    }
                }
            }
        });
    }

    private Path copyJAR(File sourceJarFile) throws CommandExecutionException {
        Path workingJarFile = Paths.get(tempDir.toString(), sourceJarFile.getName());

        try {
            Files.copy(Paths.get(sourceJarFile.getAbsolutePath()), workingJarFile);
        } catch(IOException exc) {
            String msg = String.format("can't copy %s to %s", sourceJarFile.getAbsolutePath(), workingJarFile.toString());
            logger.error( msg, exc );
            throw new CommandExecutionException( msg );
        }
        return workingJarFile;
    }

    private void removeSigs(File metaInfDir) {
        File[] sfFiles = metaInfDir.listFiles(
                pathname -> StringUtils.endsWith(pathname.getName(), ".SF")
        );
        for( File sf : sfFiles ) {
            File[] dsfFiles = metaInfDir.listFiles(
                    pathname -> StringUtils.startsWith(pathname.getName(), FilenameUtils.getBaseName(sf.getName())) &&
                            !StringUtils.endsWith(pathname.getName(), ".SF")
            );
            for( File dsf : dsfFiles ) {
                if(logger.isDebugEnabled() ) {
                    logger.debug("[UNSIGN] deleting dsf={}", dsf.getName());
                }
                dsf.delete();
            }
            if(logger.isDebugEnabled() ) {
                logger.debug("[UNSIGN] deleting sf={}", sf.getName());
            }
            sf.delete();
        }
    }

    private void editManifest(File metaInfDir) {
        File manifestMF = new File(metaInfDir, "MANIFEST.MF");
        File tmpManifestMF = new File(metaInfDir, "MANIFEST.MF-" + RandomStringUtils.randomAlphanumeric(10) );

        try (
            BufferedReader br = new BufferedReader(new FileReader(manifestMF));
            BufferedWriter bw = new BufferedWriter(new FileWriter(tmpManifestMF))
        ) {
            String line;
            while( (line = br.readLine()) != null ) {
                if( line.contains("Digest:") ||
                        line.contains("Digest-Manifest:") ||
                            line.contains("Digest-Manifest-Main-Attributes:")) {
                    continue;
                }
                bw.write( line );
                bw.newLine();
            }

        } catch(IOException exc) {
            logger.error("error processing MANIFEST.MF", exc);
        }

        String absPath = manifestMF.getAbsolutePath();
        boolean wasDeleted = manifestMF.delete();
        if( logger.isDebugEnabled() ) {
            logger.debug("[UNSIGN] wasDeleted={}", wasDeleted);
        }
        boolean wasRenamed = tmpManifestMF.renameTo( new File(absPath) );  // swap
        if( logger.isDebugEnabled() ) {
            logger.debug("[UNSIGN] wasRenamed={}", wasRenamed);
        }
    }

    private void repackJAR(Path targetJARFilePath, Path appDir) throws CommandExecutionException {
        Preconditions.checkNotNull( activeConfiguration.getJarCommand() );
        String[] cmdAndArgs = {
                activeConfiguration.getJarCommand().toString(),
                "cMf", targetJARFilePath.toString(),
                "-C", tempDir.toAbsolutePath().toString(),
                "."
        };

        CommandExecutor cmd = new CommandExecutor();
        cmd.exec(cmdAndArgs);
    }

    private void unJAR(String zipFile, Path tempDir) throws CommandExecutionException {

        Preconditions.checkNotNull( activeConfiguration.getJarCommand() );

        String[] cmdAndArgs = {
            activeConfiguration.getJarCommand().toString(),
            "xf", zipFile
        };

        Path defaultWorkingDir = Paths.get(System.getProperty("user.dir"));

        CommandExecutor cmd = new CommandExecutor(TIMEOUT_SECS, tempDir, defaultWorkingDir);
        cmd.exec(cmdAndArgs);
    }

    public static void main(String[] args) throws Exception {
        UnsignCommand cmd = new UnsignCommand();
        cmd.unsignJAR(
                Paths.get("C:\\Users\\carl_000\\git\\resignator-repos-1\\resignator\\mavenpomupdater-1.3.1.jar"),
                Paths.get("C:\\Users\\carl_000\\.resignator\\myoutputjar.jar"),
                s -> System.out.println(s));
    }
}
