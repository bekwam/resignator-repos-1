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

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.util.io.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Wraps up encryption and decryption
 *
 * The String encrypt and decrypt functions use Base64 to produce a format usable by JSON (no line breaks).
 *
 * This is intended for small amounts of data (there is no compression applied).
 *
 * @author carl_000
 */
public class CryptUtils {

    private final static Logger logger = LoggerFactory.getLogger(CryptUtils.class);

    public String decrypt(
            String encrypted,
            String passPhrase)
            throws IOException, PGPException, NoSuchProviderException {

        if( StringUtils.isBlank(encrypted) ) {
            throw new IllegalArgumentException("encrypted text is blank");
        }

        if( StringUtils.isBlank(passPhrase) ) {
            throw new IllegalArgumentException("passPhrase is required");
        }

        byte[] ciphertext;
        try {
            ciphertext = Base64.getDecoder().decode(encrypted.getBytes(StandardCharsets.ISO_8859_1));
        } catch(IllegalArgumentException exc) {
            if( logger.isWarnEnabled() ) {
                if( logger.isWarnEnabled() ) {
                    logger.warn("Field could not be decoded. (Config file modified outside of app?)  Returning input bytes as encrypted bytes.");
                }
            }
            return encrypted;
        }

        byte[] cleartext = decrypt(ciphertext, passPhrase.toCharArray());
        return new String(cleartext, StandardCharsets.ISO_8859_1);
    }

    private byte[] decrypt(
            byte[] encrypted,
            char[] passPhrase)
            throws IOException, PGPException, NoSuchProviderException
    {
        InputStream in = new ByteArrayInputStream(encrypted);

        in = PGPUtil.getDecoderStream(in);

        PGPObjectFactory pgpF = new PGPObjectFactory(in, new BcKeyFingerprintCalculator());
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        if( o == null ) { // decryption failed; there is no next object

            //
            // This could arise if there is a problem with the underlying file.
            //

            if( logger.isWarnEnabled() ) {
                logger.warn("Field could not be decrypted. (Config file modified outside of app?)  Returning input bytes as encrypted bytes.");
            }

            return encrypted;
        }

        //
        // the first object might be a PGP marker packet.
        //

        if (o instanceof PGPEncryptedDataList)
        {
            enc = (PGPEncryptedDataList)o;
        }
        else
        {
            enc = (PGPEncryptedDataList)pgpF.nextObject(); // i don't think this will be used
        }

        PGPPBEEncryptedData pbe = (PGPPBEEncryptedData)enc.get(0);

        InputStream clear = pbe.getDataStream(
                new JcePBEDataDecryptorFactoryBuilder(
                        new JcaPGPDigestCalculatorProviderBuilder()
                                .setProvider("BC")
                                .build())
                        .setProvider("BC")
                        .build(passPhrase));

        return Streams.readAll(clear);
    }

    public String encrypt(
            String encrypted,
            String passPhrase)
            throws IOException, PGPException, NoSuchProviderException {

        if( StringUtils.isBlank(encrypted) ) {
            throw new IllegalArgumentException("encrypted text is blank");
        }

        if( StringUtils.isBlank(passPhrase) ) {
            throw new IllegalArgumentException("passPhrase is required");
        }

        byte[] ciphertext = encrypt( encrypted.getBytes(StandardCharsets.ISO_8859_1), passPhrase.toCharArray() );
        String ciphertext64 = Base64.getEncoder().encodeToString( ciphertext );  // uses ISO_8859_1
        return ciphertext64;
    }

    private byte[] encrypt(
            byte[]  clearData,
            char[]  passPhrase)
            throws IOException, PGPException, NoSuchProviderException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        //
        // armor makes the encrypted output more readable (includes header, footer, printable chars)
        //

        OutputStream out = bOut;
        out = new ArmoredOutputStream(out);

        //
        // The standard jre installation limits keysize to 128.  Use the unlimited jars to go higher.
        //
        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_128)
                        .setSecureRandom(
                                new SecureRandom()
                        ).setProvider("BC")
        );

        encGen.addMethod(new JcePBEKeyEncryptionMethodGenerator(passPhrase).setProvider("BC"));

        OutputStream encOut = encGen.open(out, clearData.length);

        encOut.write(clearData);
        encOut.close();

        out.close();

        return bOut.toByteArray();
    }
}
