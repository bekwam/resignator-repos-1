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
import com.bekwam.resignator.ActiveConfiguration;
import com.bekwam.resignator.ActiveProfile;
import com.bekwam.resignator.util.CryptUtils;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ConfigurationDataSource
 *
 * Watch out for bound properties being used in calls wrapped by a Task.
 *
 * @author carl_000
 * @since 1.0.0
 */
@Singleton
public class ConfigurationDataSourceImpl extends BaseManagedDataSource implements ConfigurationDataSource {

    private final static Logger logger = LoggerFactory.getLogger(ConfigurationDataSourceImpl.class);

    @Inject @Named("ConfigFile")
    String jsonConfigFile;

    @Inject @Named("ConfigDir")
    String configDir;

    @Inject
    ActiveConfiguration activeConf;
    
    @Inject
    ActiveProfile activeProfile;

    @Inject
    CryptUtils cryptUtils;

    private Optional<Configuration> configuration = Optional.empty();
    private Optional<File> configFile = Optional.empty();

    @Override
    public void init() throws Exception {

        if( logger.isDebugEnabled() ) {
            logger.debug("[INIT]");
        }

        configuration = Optional.of( new Configuration() );

        initFileSystem();
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

        activeProfile.fromDomain(profile.get());
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
        configuration.get().getProfiles().add( activeProfile.toDomain() );

        activeConf.setActiveProfile( activeProfile.getProfileName() );

        //
        // Verify that Profile is in recentProfiles
        //
        if( !activeConf.getRecentProfiles().contains(activeProfile.getProfileName()) ) {
        	activeConf.getRecentProfiles().add( activeProfile.getProfileName() );
        }

        saveConfiguration();
    }

    @Override
    public List<String> getRecentProfileNames() {
        return configuration.get().getRecentProfiles();
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

        try (
                FileReader fr = new FileReader(cf)
        ) {
            Gson gson = new GsonBuilder().
                    registerTypeAdapter(Configuration.class, new ConfigurationJSONAdapter()).
                    create();
            Configuration cfg = gson.fromJson(fr, Configuration.class);

            configuration = Optional.of(cfg);
            activeConf.fromDomain(configuration.get());
        }
    }

    @Override
    public void decrypt(String passPhrase) {

        //
        // #1 decrypt the derived fields in jarsignerconfig.  There may not be a password at this point,
        // so just skip the decryption until next call when one has been given.
        //
        if( StringUtils.isNotBlank(activeConf.getUnhashedPassword()) ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[DECRYPT] there is a password; decrypting");
            }
            try {
                for (Profile p : configuration.get().getProfiles()) {
                    if (p.getJarsignerConfig().isPresent()) {
                        JarsignerConfig jc = p.getJarsignerConfig().get();
                        if( StringUtils.isNotBlank(jc.getEncryptedKeypass() ) ) {
                            jc.setKeypass(cryptUtils.decrypt(jc.getEncryptedKeypass(), passPhrase));
                        }
                        if( StringUtils.isNotBlank(jc.getEncryptedStorepass()) ) {
                            jc.setStorepass(cryptUtils.decrypt(jc.getEncryptedStorepass(), passPhrase));
                        }
                    }
                }
            } catch (Exception exc) {
                logger.error("encryption error", exc);
            }
        } else {
            if( logger.isDebugEnabled() ) {
                logger.debug("[DECRYPT] no password available; deferring decryption");
            }
        }
    }

    @Override
    public void saveConfiguration() throws IOException {

    	Preconditions.checkArgument( configuration.isPresent() );
    	
    	Configuration c = configuration.get();
    	
    	//
    	// Merges ActiveConfiguration which doesn't include any Profiles with the latest
    	// Configuration object
    	//
    	
    	c.setActiveProfile(Optional.of(activeConf.getActiveProfile()));
    	c.setJDKHome(Optional.of(activeConf.getJDKHome()));
    	c.getRecentProfiles().clear();
    	c.getRecentProfiles().addAll(activeConf.getRecentProfiles());
    	c.setHashedPassword(Optional.of(activeConf.getHashedPassword()));
        c.setLastUpdatedDateTime(Optional.of(activeConf.getLastUpdatedDateTime()));

        //
        // #1 set the derived field in jarsignerconfig to include encrypted values
        //
        try {
            for (Profile p : c.getProfiles()) {
                if (p.getJarsignerConfig().isPresent()) {
                    JarsignerConfig jc = p.getJarsignerConfig().get();
                    if( StringUtils.isNotBlank(jc.getKeypass()) ) {
                        jc.setEncryptedKeypass(cryptUtils.encrypt(jc.getKeypass(), activeConf.getUnhashedPassword()));
                    }
                    if( StringUtils.isNotBlank(jc.getStorepass() ) ) {
                        jc.setEncryptedStorepass(cryptUtils.encrypt(jc.getStorepass(), activeConf.getUnhashedPassword()));
                    }
                }
            }
        } catch(Exception exc) {
            logger.error( "encryption error", exc );
        }

        Gson gson = new GsonBuilder().
                registerTypeAdapter(Configuration.class, new ConfigurationJSONAdapter()).
                setPrettyPrinting().
                create();

        try (
                FileWriter fw = new FileWriter(configFile.get())
        ) {
            try (
                    JsonWriter jw = new JsonWriter(fw)
            ) {
                gson.toJson(configuration.get(), Configuration.class, jw);
            }
        }
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
            logger.debug("[INIT FS] configFile newly created?={}", retval);
        }

        if( retval ) { // write out empty json
            if( logger.isDebugEnabled() ) {
                logger.debug("[INIT FS] serializing empty config object to file for first time");
            }
            activeConf.setLastUpdatedDateTime(LocalDateTime.now());
            saveConfiguration();  // empty config
        }
    }

    @Override
    public Configuration getConfiguration() { return configuration.get(); }

    /**
     * Not exposed outside of package or impl.
     * <p/>
     * Intended for unit tests
     *
     * @param configuration
     */
    void setConfiguration(Optional<Configuration> configuration) {
        this.configuration = configuration;
    }

    @Override
    public void deleteProfile(String profileName) throws IOException {

        if( logger.isDebugEnabled() ) {
            logger.debug("[DELETE] deleting profileName={}", profileName);
        }

        if( configuration.isPresent() ) {

            if( logger.isDebugEnabled() ) {
                logger.debug("[DELETE] before deletion # recent={}, # profiles={}",
                        CollectionUtils.size(configuration.get().getRecentProfiles()),
                        CollectionUtils.size(configuration.get().getProfiles())
                );
            }

            Profile key = new Profile(profileName, false, SigningArgumentsType.JAR);  // argsType not important
            configuration.get().getProfiles().remove(key);
            configuration.get().getRecentProfiles().remove( profileName );

            if( logger.isDebugEnabled() ) {
                logger.debug("[DELETE] before save # recent={}, # profiles={}",
                        CollectionUtils.size(configuration.get().getRecentProfiles()),
                        CollectionUtils.size(configuration.get().getProfiles())
                );
            }

            saveConfiguration();
        }
    }

    @Override
    public boolean profileExists(String profileName) {
        boolean exists = false;
        if (configuration.isPresent()) {
            for (Profile p : configuration.get().getProfiles()) {
                if (StringUtils.equalsIgnoreCase(p.getProfileName(), profileName)) {
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }

    @Override
    public void renameProfile(String oldProfileName, String newProfileName) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("[RENAME] old={}, new={}", oldProfileName, newProfileName);
        }

        //
        // if needed, rename the item in recent profiles
        //
        for (int i = 0; i < activeConf.getRecentProfiles().size(); i++) {
            if (StringUtils.equalsIgnoreCase(activeConf.getRecentProfiles().get(i), oldProfileName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[RENAME] renaming item in recent profiles");
                }
                activeConf.getRecentProfiles().set(i, newProfileName);
            }
        }

        if (StringUtils.equalsIgnoreCase(activeProfile.getProfileName(), oldProfileName)) {

            if (logger.isDebugEnabled()) {
                logger.debug("[RENAME] renaming the active profile");
            }
            //
            // rename the active profilename property
            //
            activeProfile.setProfileName(newProfileName);

            //
            // save everything to disk
            //
            saveProfile();

        } else {

            if (logger.isDebugEnabled()) {
                logger.debug("[RENAME] renaming profile that is NOT the active profile");
            }

            //
            // rename target is not the active record; save directly to dao
            //

            if (configuration.isPresent()) {
                for (int j = 0; j < configuration.get().getProfiles().size(); j++) {
                    Profile p = configuration.get().getProfiles().get(j);
                    if (StringUtils.equalsIgnoreCase(p.getProfileName(), oldProfileName)) {
                        Profile np = new Profile(newProfileName, p.getReplaceSignatures(), p.getArgsType());
                        np.setSourceFile(p.getSourceFile());
                        np.setTargetFile(p.getTargetFile());
                        np.setJarsignerConfig(p.getJarsignerConfig());
                        configuration.get().getProfiles().set(j, np);
                        saveConfiguration();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public String suggestUniqueProfileName(String profileName) {

        int counter = 2;

        do {
            final String pn = profileName + "-" + counter;

            boolean found = configuration.get().getProfiles().
                    stream().
                    anyMatch(p -> StringUtils.equalsIgnoreCase(p.getProfileName(), pn));

            if (!found) {
                return pn;
            }

            counter++;
        } while (true);
    }

    @Override
    public boolean isSecured() {
        if( configuration.isPresent() ) {
            return configuration.get().getHashedPassword().filter( (p) -> StringUtils.isNotBlank(p) ).isPresent();
        }
        return false;
    }

    @Override
    public void deleteDataFile() {
        if (configFile.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("[DELETE DATA FILE] deleting {} on exit", configFile.get().getAbsolutePath());
            }
            configFile.get().deleteOnExit();
        }
    }
}
