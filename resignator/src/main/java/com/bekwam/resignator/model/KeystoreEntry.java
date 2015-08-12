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

/**
 * Maps to the output of the keytool -list command
 *
 * @author carl_000
 */
public class KeystoreEntry {

    private final String alias;
    private final String creationDate;
    private final String entryType;
    private final String fingerprint;

    public KeystoreEntry(String alias, String creationDate, String entryType) {
        this(alias, creationDate, entryType, "");
    }

    public KeystoreEntry(String alias, String creationDate, String entryType, String fingerprint) {

        Preconditions.checkNotNull(alias);

        this.alias = alias;
        this.creationDate = creationDate;
        this.entryType = entryType;
        this.fingerprint = fingerprint;
    }

    public KeystoreEntry(KeystoreEntry otherEntry, String fingerprint) {
        this( otherEntry.getAlias(), otherEntry.getCreationDate(), otherEntry.getEntryType(), fingerprint );
    }

    public String getAlias() {
        return alias;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getEntryType() {
        return entryType;
    }

    public String getFingerprint() { return fingerprint; }

    @Override
    public String toString() {
        return "KeystoreEntry{" +
                "alias='" + alias + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", entryType='" + entryType + '\'' +
                ", fingerprint='" + fingerprint + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeystoreEntry that = (KeystoreEntry) o;

        return alias.equals(that.alias);

    }

    @Override
    public int hashCode() {
        return alias.hashCode();
    }
}
