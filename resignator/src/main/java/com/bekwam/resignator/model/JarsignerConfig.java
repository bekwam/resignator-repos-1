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
package com.bekwam.resignator.model;

import org.apache.commons.lang3.StringUtils;

/**
 * @author carl_000
 */
public class JarsignerConfig {

    private final String alias;
    private final Boolean verbose;
    private final String keystore;

    private String keypass;
    private String storepass;
    private String encryptedStorepass;
    private String encryptedKeypass;

    public JarsignerConfig(String alias, String storepass, String keypass, String keystore, Boolean verbose) {
        this.alias = alias;
        this.storepass = storepass;
        this.keypass = keypass;
        this.keystore = keystore;
        this.verbose = verbose;
    }

    public String getAlias() {
        return alias;
    }

    public String getStorepass() {
        return storepass;
    }

    public void setStorepass(String storepass) { this.storepass = storepass; }

    public String getKeypass() {
        return keypass;
    }

    public void setKeypass(String keypass) { this.keypass = keypass; }

    public String getKeystore() {
        return keystore;
    }

    public Boolean getVerbose() {
        return verbose;
    }

    public String getEncryptedStorepass() {
        return encryptedStorepass;
    }

    public void setEncryptedStorepass(String encryptedStorepass) {
        this.encryptedStorepass = encryptedStorepass;
    }

    public String getEncryptedKeypass() {
        return encryptedKeypass;
    }

    public void setEncryptedKeypass(String encryptedKeypass) {
        this.encryptedKeypass = encryptedKeypass;
    }

    @Override
    public String toString() {
        return "JarsignerConfig{" +
                "alias='" + alias + '\'' +
                ", storepass not empty?='" + StringUtils.isNotEmpty(storepass) + '\'' +
                ", keypass not empty?='" + StringUtils.isNotEmpty(keypass) + '\'' +
                ", keystore='" + keystore + '\'' +
                ", verbose=" + verbose + '\'' +
                ", encStorepass not empty?='" + StringUtils.isNotEmpty(encryptedStorepass) + '\'' +
                ", encKeypass not empty?='" + StringUtils.isNotEmpty(encryptedKeypass) +
                '}';
    }
}
