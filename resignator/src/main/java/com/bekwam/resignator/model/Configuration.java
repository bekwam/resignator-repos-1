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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author carl_000
 */
public class Configuration {

    private Optional<String> jdkHome = Optional.empty();
    private Optional<String> activeProfile = Optional.empty();
    private final List<String> recentProfiles = new ArrayList<>();
    private final List<Profile> profiles = new ArrayList<>();
    private Optional<String> hashedPassword = Optional.empty();
    private Optional<LocalDateTime> lastUpdatedDateTime = Optional.empty();

    public Optional<String> getJDKHome() { return jdkHome; }

    public void setJDKHome(Optional<String> jdkHome) {
        this.jdkHome = jdkHome;
    }

    public Optional<String> getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(Optional<String> activeProfile) {
        this.activeProfile = activeProfile;
    }

    public List<String> getRecentProfiles() {
        return recentProfiles;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public Optional<String> getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(Optional<String> hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public Optional<LocalDateTime> getLastUpdatedDateTime() { return lastUpdatedDateTime; }
    public void setLastUpdatedDateTime(Optional<LocalDateTime> lastUpdatedDateTime) {
        this.lastUpdatedDateTime = lastUpdatedDateTime;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "jdkHome=" + jdkHome +
                ", activeProfile=" + activeProfile +
                ", recentProfiles=" + recentProfiles +
                ", profiles=" + profiles +
                ", password is empty?=" + hashedPassword.isPresent() +
                ", lastUpdatedDateTime=" + lastUpdatedDateTime +
                "}";
    }
}
