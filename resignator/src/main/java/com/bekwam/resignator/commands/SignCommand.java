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
package com.bekwam.resignator.commands;

import com.bekwam.resignator.ActiveConfiguration;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Signs a JAR file using the jarsigner command
 *
 * @author carl_000
 */
public class SignCommand {

    @Inject
    ActiveConfiguration activeConfiguration;

    public void signJAR(Path jarFilePath,
                        Path keystore,
                        String storepass,
                        String alias,
                        String keypass,
                        Consumer<String> observer) throws CommandExecutionException {

        if( StringUtils.isEmpty(storepass) ) {
            throw new CommandExecutionException("storepass is required");
        }

        if( StringUtils.isEmpty(keypass) ) {
            throw new CommandExecutionException("keypass is required");
        }

        if( StringUtils.isEmpty(alias) ) {
            throw new CommandExecutionException("alias is required");
        }

        if( keystore == null ) {
            throw new CommandExecutionException("keystore is required");
        }

        if( jarFilePath == null  ) {
            throw new CommandExecutionException("jarFilePath is required");
        }

        if( !jarFilePath.toFile().exists()  ) {
            throw new CommandExecutionException(String.format("jar file %s not found", jarFilePath.toString()));
        }

        Preconditions.checkNotNull(activeConfiguration.getJarsignerCommand() );

        observer.accept("Running jarsigner command");

        String[] cmdAndArgs = {
                activeConfiguration.getJarsignerCommand().toString(),
                "-keystore", keystore.toString(),
                "-storepass", storepass,
                "-keypass", keypass,
                "-tsa", "http://timestamp.digicert.com",
                jarFilePath.toString(),
                alias
        };

        CommandExecutor cmd = new CommandExecutor();
        cmd.exec(cmdAndArgs);

        observer.accept( "Finished" );
    }

    public static void main(String[] args) throws Exception {
        SignCommand cmd = new SignCommand();
        cmd.signJAR(Paths.get("C:\\Users\\carl_000\\.resignator\\myoutputjar.jar"),
                Paths.get("C:\\Users\\carl_000\\git\\resignator-repos-1\\resignator\\mykeystore3"),
                "ab987c",
                "business3",
                "ab987c",
                s -> System.out.println(s)
                );
    }
}