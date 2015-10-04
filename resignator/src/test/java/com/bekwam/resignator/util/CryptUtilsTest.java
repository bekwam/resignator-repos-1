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
package com.bekwam.resignator.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.Security;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for crypt utils
 *
 * @author carl_000
 */
public class CryptUtilsTest {

    private CryptUtils cryptUtils = new CryptUtils();

    @BeforeClass
    public static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test(expected=IllegalArgumentException.class)
    public void encryptNoData() throws PGPException, IOException, NoSuchProviderException {
        cryptUtils.encrypt( null, "password" );
    }

    @Test(expected=IllegalArgumentException.class)
    public void encryptNoPassword() throws PGPException, IOException, NoSuchProviderException {
        cryptUtils.encrypt( "ABCDEF101", null );
    }

    @Test(expected=IllegalArgumentException.class)
    public void decryptNoData() throws PGPException, IOException, NoSuchProviderException {
        cryptUtils.decrypt(null, "password");

    }

    @Test(expected=IllegalArgumentException.class)
    public void decryptNoPassword() throws PGPException, IOException, NoSuchProviderException {
        cryptUtils.decrypt("ABCDEF101", null);
    }

    @Test
    public void encryptAndDecrypt() throws PGPException, IOException, NoSuchProviderException {

        String source = "the quick brown fox jumped over the lazy dog";
        String password = "abc123";

        String ciphertext = cryptUtils.encrypt( source, password );

        assertFalse( ciphertext.equals(source) );

        String cleartext = cryptUtils.decrypt( ciphertext, password );

        assertTrue( cleartext.equals(source) );
    }
}
