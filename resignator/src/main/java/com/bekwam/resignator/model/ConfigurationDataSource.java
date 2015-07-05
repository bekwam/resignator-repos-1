package com.bekwam.resignator.model;

import java.io.IOException;
import java.util.List;

import com.bekwam.resignator.ActiveProfile;

/**
 * Main DAO for access all settings / preferences info in the app
 *
 * @author carl_000
 * @since 1.0.0
 */
public interface ConfigurationDataSource {

    /**
     * Loads named Profile into the activeProfile field of Configuration
     *
     * @param profileName name of Profile to load
     * @since 1.0.0
     */
    void loadProfile(String profileName);

    /**
     * Saves named Profile id'd in the activeProfile field
     *
     * @since 1.0.0
     */
    void saveProfile() throws IOException;

    /**
     * Returns a list of recently-used Profiles
     *
     * Will remove deleted Profiles from list and re-save
     *
     * @return List of valid Profile objects
     * @since 1.0.0
     */
    List<String> getRecentProfileNames();

    /**
     * Returns a list of Profiles
     *
     * @return List of valid Profile objects
     * @since 1.0.0
     */
    List<Profile> getProfiles();

    /**
     * Loads the Configuration including all Profiles
     *
     * @throws IOException error reading JSON into Configuration object from config file
     * @since 1.0.0
     */
    void loadConfiguration() throws IOException;

    /**
     * Saves the Configuration including all Profiles
     *
     * @throws IOException error writing JSON based on Configuration object to config file
     * @since 1.0.0
     */
    void saveConfiguration() throws IOException;

    /**
     * Returns the FX object ActiveProfile
     *
     * @return
     */
    ActiveProfile getActiveProfile();

    /**
     * Sets the jarsignerExec property
     *
     */
    void setJarsignerExec(String jarsignerExec);

}
