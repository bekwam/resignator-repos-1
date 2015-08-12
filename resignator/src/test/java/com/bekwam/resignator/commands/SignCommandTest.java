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

import com.bekwam.resignator.commands.CommandExecutionException;
import com.bekwam.resignator.commands.SignCommand;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

/**
 * Unit test for SignCommand
 *
 * @author carl_000
 */
public class SignCommandTest {

    private SignCommand cmd;

    @Before
    public void init() {
        cmd = new SignCommand();
    }

    @Test(expected=CommandExecutionException.class)
    public void noStorepass() throws CommandExecutionException {
        cmd = new SignCommand();
        cmd.signJAR(null, Paths.get("keystore"), "alias", null, "keypass", s -> System.out.println(s) );
    }

    @Test(expected=CommandExecutionException.class)
    public void noKeypass() throws CommandExecutionException {
        cmd = new SignCommand();
        cmd.signJAR(null, Paths.get("keystore"), "alias", "storepass", null,  s -> System.out.println(s) );
    }

    @Test(expected=CommandExecutionException.class)
    public void noAlias() throws CommandExecutionException {
        cmd = new SignCommand();
        cmd.signJAR(null, Paths.get("keystore"), null, "storepass", "keypass",  s -> System.out.println(s) );
    }

    @Test(expected=CommandExecutionException.class)
    public void noKeystore() throws CommandExecutionException {
        cmd = new SignCommand();
        cmd.signJAR(null, null, "alias", "storepass", "keypass",  s -> System.out.println(s) );
    }

}
