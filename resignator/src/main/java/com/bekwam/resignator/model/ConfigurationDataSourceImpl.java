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

import com.bekwam.jfxbop.data.BaseManagedDataSource;
import com.bekwam.resignator.ActiveProfile;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ConfigurationDataSource
 *
 * @author carl_000
 * @since 1.0.0
 */
public class ConfigurationDataSourceImpl extends BaseManagedDataSource implements ConfigurationDataSource {

    private final static Logger logger = LoggerFactory.getLogger(ConfigurationDataSourceImpl.class);

    @Inject @Named("ConfigFile")
    String jsonConfigFile;

    @Inject @Named("ConfigDir")
    String configDir;

    private final ActiveProfile activeProfile = new ActiveProfile();

    private Optional<Configuration> configuration = Optional.empty();
    private Optional<File> configFile = Optional.empty();

    @Override
    public void init() throws Exception {

        if( logger.isDebugEnabled() ) {
            logger.debug("[INIT]");
        }

        configuration = Optional.of( new Configuration() );

        initFileSystem();
        loadConfiguration();
    }

    @Override
    public boolean isInitialized() {
        return configFile.isPresent();
    }

    @Override
    public void loadProfile(String profileName) {

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE]");
        }

        Optional<Profile> profile = configuration.
                    get().
                    getProfiles().
                    stream().
                    filter( p -> StringUtils.equalsIgnoreCase(p.getProfileName(), profileName) ).
                    findFirst();

        activeProfile.loadFromProfile( profile.get() );
    }

    @Override
    public void saveProfile() throws IOException {

        //
        // Find Profile in Configuration and remove if exists
        //
        Iterator<Profile> iterator = configuration.get().getProfiles().iterator();
        while( iterator.hasNext() ) {
            Profile p = iterator.next();
            if( Objects.equal(p.getProfileName(), activeProfile.getProfileName()) ) {
                iterator.remove();
            }
        }

        //
        // Add a new or replacing profile
        //
        configuration.get().getProfiles().add( activeProfile.toProfile() );

        //
        // Mark this Profile as activeProfile
        //
        configuration.get().setActiveProfile(Optional.of( activeProfile.getProfileName() ));

        //
        // Verify that Profile is in recentProfiles
        //
        if( !configuration.get().getRecentProfiles().contains( activeProfile.getProfileName() ) ) {
            configuration.get().getRecentProfiles().add(  activeProfile.getProfileName() );
        }

        saveConfiguration();
    }

    @Override
    public List<String> getRecentProfileNames() {
        return null;
    }

    @Override
    public List<Profile> getProfiles() {

        return configuration.get().getProfiles();
    }

    @Override
    public void loadConfiguration() throws IOException {

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD CONF]");
        }

        File cf = configFile.get();

        Gson gson = new GsonBuilder().
                            registerTypeAdapter(Configuration.class, new ConfigurationJSONAdapter()).
                            create();
        Configuration cfg = gson.fromJson(new FileReader(cf), Configuration.class);

        configuration = Optional.of(cfg);
    }

    @Override
    public void saveConfiguration() throws IOException {

        Gson gson = new GsonBuilder().
                registerTypeAdapter(Configuration.class, new ConfigurationJSONAdapter()).
                setPrettyPrinting().
                create();

        JsonWriter jw = new JsonWriter( new FileWriter( configFile.get() ) );
        gson.toJson( configuration.get(), Configuration.class, jw );
        jw.close();
    }

    private void initFileSystem() throws IOException {

        if( logger.isDebugEnabled() ) {
            logger.debug("[INIT FS]");
        }

        String userHome = System.getProperty("user.home");

        Preconditions.checkNotNull( userHome );
        Preconditions.checkNotNull( configDir );
        Preconditions.checkNotNull( jsonConfigFile );

        //
        // Create the .resignator / resignator.properties if not present in ${user.home}
        //

        File fullConfigDir = new File(userHome, configDir);

        if( fullConfigDir.exists() && !fullConfigDir.isDirectory() ) {
            String msg = fullConfigDir.getAbsolutePath() + " is a file and must be a directory; delete the file and restart the app";
            logger.error( msg );
            throw new IllegalStateException(msg);
        }

        if( !fullConfigDir.exists() ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[INIT FS] configDir {} does not exist; creating", fullConfigDir.getAbsolutePath());
            }
            fullConfigDir.mkdir();
        }

        File cf = new File(fullConfigDir, jsonConfigFile );

        boolean retval = cf.createNewFile();  // verifies that user can create a file here

        configFile = Optional.of( cf );

        if( logger.isDebugEnabled() ) {
            logger.debug("[INIT FS] configFile already existed?={}", retval);
        }

        if( retval ) { // write out empty json
            if( logger.isDebugEnabled() ) {
                logger.debug("[INIT FS] serializing empty config object to file for first time");
            }
            saveConfiguration();  // empty config
        }
    }

    public ActiveProfile getActiveProfile() { return activeProfile; }
}
