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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

/**
 * Wraps up an external call to keytool.exe
 *
 * keytool.exe doesn't return anything on stderr
 *
 * @author carl_000
 */
public class KeytoolCommand {

    private final static Logger logger = LoggerFactory.getLogger(KeytoolCommand.class);

    private enum KeystoreListParseStateType { START, HEADER, ENTRY, FP, END, ERROR_END };

    private final static String OUTPUTFILE_PREFIX = "keytoolCommand-";
    private final static String OUTPUTFILE_SUFFIX = ".txt";

    private final int TIMEOUT_SECS = 5;

    private final static String newLine = System.getProperty("line.separator");

    public List<String> findAliases(String keytoolExec, String keystore, String storepass) throws CommandExecutionException {

        List<String> aliases =
                findKeystoreEntries(keytoolExec, keystore, storepass).
                        stream().
                        map(KeystoreEntry::getAlias).
                        collect(toList());

        return aliases;
    }

    public List<KeystoreEntry> findKeystoreEntries(String keytoolExec, String keystore, String storepass) throws CommandExecutionException {

        List<KeystoreEntry> entries = new ArrayList<>();

        Preconditions.checkNotNull(keytoolExec);
        Preconditions.checkNotNull(keystore);
        Preconditions.checkNotNull(storepass);

        File outputFile = null;

        try {

            String[] cmdAndArgs = {
                    keytoolExec,
                    "-keystore", keystore,
                    "-storepass", storepass,
                    "-list"
            };

            File resignatorDir = new File(System.getProperty("user.home"), ".resignator");

            String outputFileName = OUTPUTFILE_PREFIX +
                    StringUtils.lowerCase(RandomStringUtils.randomAlphabetic(12)) +
                    OUTPUTFILE_SUFFIX;

            outputFile = new File(resignatorDir, outputFileName);

            ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
            pb.redirectErrorStream(false);
            pb.redirectOutput(outputFile);

            Process p = pb.start();

            boolean exitted = p.waitFor(TIMEOUT_SECS, TimeUnit.SECONDS);

            if( exitted ) {

                if( p.exitValue() == 0 ) {

                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile)));
                    entries.addAll(parseKeystoreEntries(br));
                    br.close();

                } else {

                    String firstLine = "";
                    if( outputFile != null && outputFile.exists() ) {
                        BufferedReader br = new BufferedReader(new FileReader(outputFile));
                        firstLine = br.readLine();
                        br.close();
                    }

                    if( logger.isErrorEnabled() ) {
                        logger.error("error running exec={}; firstLine={}",
                                keytoolExec,
                                firstLine);
                    }

                    throw new CommandExecutionException( "Command '" + keytoolExec + "' failed to run" + newLine + firstLine );
                }

            } else {

                if( logger.isErrorEnabled() ) {
                    logger.error("command '" + keytoolExec + "' timed out");
                }

                throw new CommandExecutionException("Command '" + keytoolExec + "' timed out");
            }

        } catch(Exception exc) {  // includes interrupted exception

            if( logger.isErrorEnabled() ) {
                logger.error("error running keytool", exc);
            }

            throw new CommandExecutionException("Error running keytool command" + newLine + exc.getMessage() );

        } finally {
            if( outputFile != null ) {
                outputFile.delete();
            }
        }

        return entries;
    }

    public List<KeystoreEntry> parseKeystoreEntries(BufferedReader br) throws IOException {

        List<KeystoreEntry> entries = new ArrayList<>();

        String line = "";
        KeystoreListParseStateType parseState = KeystoreListParseStateType.HEADER;
        KeystoreEntry currEntry = null;

        while( parseState != KeystoreListParseStateType.END && parseState != KeystoreListParseStateType.ERROR_END ) {

            switch( parseState ) {
                case START:
                    parseState = KeystoreListParseStateType.HEADER;
                    break;
                case HEADER:

                    line = br.readLine();

                    if( StringUtils.startsWith(line, "Your keystore") ) {
                        parseState = KeystoreListParseStateType.ENTRY;
                    } else if( line == null ) {
                        parseState = KeystoreListParseStateType.ERROR_END;
                    }

                    break;
                case ENTRY :

                    line = br.readLine();

                    if( StringUtils.startsWith(line, "Certificate fingerprint") ) {
                        parseState = KeystoreListParseStateType.FP;
                    } else {

                        if( line == null ) {

                            parseState = KeystoreListParseStateType.ERROR_END;

                        } else if( StringUtils.isNotEmpty(line) ) {
                            String[] toks = StringUtils.split( line, "," );
                            currEntry = new KeystoreEntry( StringUtils.trim(toks[0]),
                                                           StringUtils.trim(toks[1] + toks[2]),
                                                           StringUtils.trim(toks[3]) );
                        }
                    }

                    break;
                case FP:

                    if( StringUtils.isNotEmpty(line) ) {
                        StrTokenizer st = new StrTokenizer( line, ": " );
                        if( st != null ) {
                            String[] toks = st.getTokenArray();
                            currEntry = new KeystoreEntry(currEntry, toks[1]);
                            entries.add(currEntry);
                        } else {
                            System.err.println("parsing error on line=" + line);
                        }
                    }

                    parseState = KeystoreListParseStateType.ENTRY;

                case END:
                    break;
            }
        }

        return entries;
    }

    public static void main(String[] args) throws Exception {

        KeytoolCommand cmd = new KeytoolCommand();

        System.out.println("****** cacerts *******");
        List<KeystoreEntry> entries = cmd.findKeystoreEntries(
                "C:\\Program Files\\Java\\jdk1.8.0_40\\bin\\keytool",
                "C:\\Program Files\\Java\\jdk1.8.0_40\\jre\\lib\\security\\cacerts",
                "changeit"
        );

        entries.stream().forEach( (e) -> System.out.println(e.getAlias() + "/" + e.getFingerprint()));

        System.out.println("****** just cacerts aliases *******");
        List<String> aliases = cmd.findAliases(
                "C:\\Program Files\\Java\\jdk1.8.0_40\\bin\\keytool",
                "C:\\Program Files\\Java\\jdk1.8.0_40\\jre\\lib\\security\\cacerts",
                "changeit"
        );

        aliases.stream().forEach((a) -> System.out.println(a));


        System.out.println("****** test *******");
        entries = cmd.findKeystoreEntries(
                "C:\\Program Files\\Java\\jdk1.8.0_40\\bin\\keytool",
                "C:\\Users\\carl_000\\git\\resignator-repos-1\\resignator\\mykeystore",
                "ab987c"
        );

        entries.stream().forEach((e) -> System.out.println(e.getAlias() + "/" + e.getFingerprint()));

        System.out.println("****** just test aliases *******");
        aliases = cmd.findAliases(
                "C:\\Program Files\\Java\\jdk1.8.0_40\\bin\\keytool",
                "C:\\Users\\carl_000\\git\\resignator-repos-1\\resignator\\mykeystore",
                "ab987c"
        );

        aliases.stream().forEach((a) -> System.out.println(a));

        System.out.println("****** empty *******");
        entries = cmd.findKeystoreEntries(
                "C:\\Program Files\\Java\\jdk1.8.0_40\\bin\\keytool",
                "C:\\Users\\carl_000\\git\\resignator-repos-1\\resignator\\emptykeystore",
                "ab987c"
        );

        entries.stream().forEach((e) -> System.out.println(e.getAlias() + "/" + e.getFingerprint()));

        System.out.println("****** just empty aliases *******");
        aliases = cmd.findAliases(
                "C:\\Program Files\\Java\\jdk1.8.0_40\\bin\\keytool",
                "C:\\Users\\carl_000\\git\\resignator-repos-1\\resignator\\emptykeystore",
                "ab987c"
        );

        aliases.stream().forEach((a) -> System.out.println(a));

        System.out.println("***** bad password *****");

        entries = cmd.findKeystoreEntries(
                "C:\\Program Files\\Java\\jdk1.8.0_40\\bin\\keytool",
                "C:\\Users\\carl_000\\git\\resignator-repos-1\\resignator\\emptykeystore",
                "xxx"
        );

        System.out.println("# entries=" + CollectionUtils.size(entries));
    }
}
