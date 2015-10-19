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

import com.google.common.base.Preconditions;

import java.util.Optional;

/**
 * @author carl_000
 */
public class Profile {

    private final String profileName;
    private final Boolean replaceSignatures;
    private final SigningArgumentsType argsType;
    private Optional<SourceFile> sourceFile = Optional.empty();
    private Optional<TargetFile> targetFile = Optional.empty();
    private Optional<JarsignerConfig> jarsignerConfig = Optional.empty();
    
    public Profile(String profileName, Boolean replaceSignatures, SigningArgumentsType argsType) {
        Preconditions.checkNotNull( profileName );
        this.profileName = profileName;
        this.replaceSignatures = replaceSignatures;
        this.argsType = argsType;
    }

    public String getProfileName() {
        return profileName;
    }

    public Optional<SourceFile> getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(Optional<SourceFile> sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Optional<TargetFile> getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(Optional<TargetFile> targetFile) {
        this.targetFile = targetFile;
    }

    public Optional<JarsignerConfig> getJarsignerConfig() {
        return jarsignerConfig;
    }

    public void setJarsignerConfig(Optional<JarsignerConfig> jarsignerConfig) {
        this.jarsignerConfig = jarsignerConfig;
    }

    public Boolean getReplaceSignatures() {
        return replaceSignatures;
    }

    public SigningArgumentsType getArgsType() { return argsType; }
    
    @Override
    public String toString() {
        return "Profile{" +
                "profileName='" + profileName + '\'' +
                ", sourceFile=" + sourceFile +
                ", targetFile=" + targetFile +
                ", jarsignerConfig=" + jarsignerConfig +
                ", replaceSignatures=" + replaceSignatures +
                ", argsType=" + argsType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Profile profile = (Profile) o;

        return profileName.equals(profile.profileName);

    }

    @Override
    public int hashCode() {
        return profileName.hashCode();
    }
}
