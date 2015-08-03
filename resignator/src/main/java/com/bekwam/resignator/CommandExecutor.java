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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Executes an OS command as a Process
 *
 * @author carl_000
 */
public class CommandExecutor {

    private Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

    private final static int DEFAULT_TIMEOUT_IN_SECONDS = 10;

    private final Path workingDir;
    private final int timeoutInSeconds;
    private final Optional<Path> outputDir = Optional.empty();

    public CommandExecutor() {
        this(DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public CommandExecutor(int timeoutInSeconds) {
        this.timeoutInSeconds = timeoutInSeconds;
        this.workingDir = Paths.get(System.getProperty("user.dir"));
    }

    public CommandExecutor(int timeoutInSeconds, Path workingDir) {
        Preconditions.checkNotNull(workingDir);
        this.timeoutInSeconds = timeoutInSeconds;
        this.workingDir = workingDir;
    }

    public CommandExecutor(int timeoutInSeconds, Path workingDir, Path outputDir) {
        this(timeoutInSeconds, workingDir);
        Preconditions.checkNotNull(outputDir);
        this.outputDir.of(outputDir);
    }

    public void exec(String[] cmdAndArgs) throws CommandExecutionException {

        try {

            if( cmdAndArgs == null || cmdAndArgs.length <1 ) {
                logger.error( "cmdAndArgs must be specified" );
                throw new CommandExecutionException( "cmdAndArgs cannot be null and must contain at least one item" );
            }

            if( logger.isDebugEnabled() ) {
                for( int i=0; i<cmdAndArgs.length; i++ ) {
                    logger.debug("[EXEC] [{}] : {}", i, cmdAndArgs[i]);
                }
            }

            ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(false);

            Path d = outputDir.orElse(workingDir);
            Path execOutput = Files.createTempFile(d, "", ".txt");
            execOutput.toFile().deleteOnExit();
            pb.redirectOutput(execOutput.toFile());

            Process p = pb.start();

            boolean exitted = p.waitFor(timeoutInSeconds, TimeUnit.SECONDS);

            if( exitted ) {

                if( p.exitValue() == 0 ) {

                    dumpOutputFile(execOutput);

                    if(logger.isDebugEnabled() ) {
                        logger.debug("[EXEC] command {} executed successfully", cmdAndArgs[0]);
                    }

                } else {

                    dumpOutputFile(execOutput);

                    String msg = String.format("error invoking command %s", cmdAndArgs[0]);
                    logger.error( msg );
                    throw new CommandExecutionException(msg);
                }

            } else {

                dumpOutputFile(execOutput);

                String msg = String.format("command %s timed out after %d seconds", cmdAndArgs[0], timeoutInSeconds);
                logger.error( msg );
                throw new CommandExecutionException(msg);
            }

        } catch(Exception exc) {
            String msg = String.format("error invoking command %s", cmdAndArgs[0]);
            logger.error( msg, exc );
            throw new CommandExecutionException(msg);
        }
    }

    private void dumpOutputFile(Path outputFile) {
        Preconditions.checkNotNull(outputFile);
        if (logger.isDebugEnabled()) {
            File of = outputFile.toFile();
            if (of.exists()) {
                logger.debug("[EXEC] output from command");
                try (
                        LineNumberReader lnr = new LineNumberReader(
                                new BufferedReader(
                                        new FileReader(of)
                                )
                        )
                        ) {
                    String line;
                    while ((line = lnr.readLine()) != null) {
                        logger.debug("{} {}", lnr.getLineNumber(), line);
                    }
                } catch(IOException exc) {
                    logger.warn("[EXEC] error reading file=" + outputFile.toString(), exc);
                }
            }

        } else {
            logger.debug("[EXEC] no outputFile present");
        }
    }
}