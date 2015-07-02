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
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    }

    @Override
    public void saveProfile() {

    }

    @Override
    public void saveAsProfile(String newProfileName) {

    }

    @Override
    public List<Profile> getRecentProfiles() {
        return null;
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
}
