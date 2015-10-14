package com.bekwam.resignator.model;

import java.io.IOException;
import java.util.List;

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
     * Returns the backing datastore Configuration object
     * 
     */
    Configuration getConfiguration();

    /**
     * Removes a profile
     *
     * @param profileName the profile to delete
     * @throws IOException
     */
    void deleteProfile(String profileName) throws IOException;

    /**
     * Checks if a profile exists already
     *
     * @param profileName the profile to check
     * @throws IOException
     */
    boolean profileExists(String profileName);

    /**
     * Renames a profile
     *
     * @param oldProfileName the profile to rename
     * @param newProfileName the new profile name
     * @throws IOException
     */
    void renameProfile(String oldProfileName, String newProfileName) throws IOException;

    /**
     * Suggest unique profile name
     *
     * @param profileName
     * @return unique profile name
     * @throws IOException
     */
    String suggestUniqueProfileName(String profileName);

    /**
     * If there is a password (hashed) stored with json doc
     *
     * @return true if password exists
     */
    boolean isSecured();

    /**
     * Separates decrypt operation for init problem with verifying password after a loadConfiguration()
     *
     * @param passPhrase
     */
    void decrypt(String passPhrase);

    /**
     * Deletes the resignator.json file; used if password forgotten
     */
    void deleteDataFile();
}
