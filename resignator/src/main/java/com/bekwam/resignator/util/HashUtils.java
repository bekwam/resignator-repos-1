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

import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * Wraps up hashing algorithm and combines padding
 *
 * @author carl_000
 */
public class HashUtils {

    private final static Logger logger = LoggerFactory.getLogger(HashUtils.class);

    public String hash(String unhashed) {

        String digestString = "";

        try {
            MessageDigest md = new SHA512.Digest();
            byte[] passwordBytes = unhashed.getBytes();
            byte[] digest = md.digest(passwordBytes);

            digestString = new String(Base64.getEncoder().encode(digest));

            if( logger.isDebugEnabled() ) {
                logger.debug("[HASH] digestString={}", digestString);
            }

        } catch(Exception exc) {
            logger.error("digest exc", exc);
        }

        return digestString;
    }
}
