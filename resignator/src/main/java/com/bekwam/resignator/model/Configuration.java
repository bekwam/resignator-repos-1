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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author carl_000
 */
public class Configuration {

    private Optional<String> jarsignerExecutable = Optional.empty();
    private Optional<String> activeProfile = Optional.empty();
    private final List<String> recentProfiles = new ArrayList<>();
    private final List<Profile> profiles = new ArrayList<>();

    public Optional<String> getJarsignerExecutable() { return jarsignerExecutable; }

    public void setJarsignerExecutable(Optional<String> jarsignerExecutable) {
        this.jarsignerExecutable = jarsignerExecutable;
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

    @Override
    public String toString() {
        return "Configuration{" +
                "jarsignerExecutable=" + jarsignerExecutable +
                ", activeProfile=" + activeProfile +
                ", recentProfiles=" + recentProfiles +
                ", profiles=" + profiles +
                '}';
    }
}
