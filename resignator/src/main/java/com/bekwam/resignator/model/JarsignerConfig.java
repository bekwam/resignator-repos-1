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

/**
 * @author carl_000
 */
public class JarsignerConfig {

    private final String alias;
    private final String storepass;
    private final String keypass;
    private final String keystore;
    private final Boolean verbose;

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

    public String getKeypass() {
        return keypass;
    }

    public String getKeystore() {
        return keystore;
    }

    public Boolean getVerbose() {
        return verbose;
    }

    @Override
    public String toString() {
        return "JarsignerConfig{" +
                "alias='" + alias + '\'' +
                ", storepass='" + storepass + '\'' +
                ", keypass='" + keypass + '\'' +
                ", keystore='" + keystore + '\'' +
                ", verbose=" + verbose +
                '}';
    }
}
