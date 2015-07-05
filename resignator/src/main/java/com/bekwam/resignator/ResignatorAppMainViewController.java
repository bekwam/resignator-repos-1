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
package com.bekwam.resignator;

import com.bekwam.jfxbop.guice.GuiceBaseView;
import com.bekwam.jfxbop.view.Viewable;
import com.bekwam.resignator.model.ConfigurationDataSource;
import com.bekwam.resignator.model.Profile;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * JavaFX Controller and JFXBop View for the Resignator App
 *
 * @author carl_000
 * @since 1.0.0
 */
@Viewable(
        fxml="/fxml/ResignatorAppMainView.fxml",
        stylesheet = "/css/resignator.css",
        title = "ResignatorApp"
)
public class ResignatorAppMainViewController extends GuiceBaseView {

    private final static Logger logger = LoggerFactory.getLogger(ResignatorAppMainViewController.class);

    @FXML
    SplitPane sp;

    @FXML
    VBox console;

    @FXML
    TextField tfSourceFile;

    @FXML
    TextField tfTargetFile;

    @Inject
    ConfigurationDataSource configurationDS;

    @Inject @Named("ConfigDir")
    String configDir;

    @Inject @Named("ConfigFile")
    String configFile;

    @Inject
    Provider<SettingsController> settingsControllerProvider;

    private StringProperty activeProfileName = new SimpleStringProperty("");  // a "hidden" field
    private String defaultDir = System.getProperty("user.home");

    @FXML
    public void initialize() {

        try {
            configurationDS.loadConfiguration();

            activeProfileName.bindBidirectional(configurationDS.getActiveProfile().profileNameProperty());
            tfSourceFile.textProperty().bindBidirectional(configurationDS.getActiveProfile().sourceFileFileNameProperty());
            tfTargetFile.textProperty().bindBidirectional( configurationDS.getActiveProfile().targetFileFileNameProperty() );

        } catch(Exception exc) {

            logger.error("can't load configuration", exc);

            String msg = "Verify that the user has access to the directory '" + configFile +
                    "' under " + System.getProperty("user.home") + ".";

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    msg);
            alert.setHeaderText("Can't load config file");
            alert.showAndWait();

            Platform.exit();
        }
    }

    @FXML
    public void showConsole(ActionEvent evt) {

        CheckMenuItem mi = (CheckMenuItem)evt.getSource();

        if( logger.isDebugEnabled() ) {
            logger.debug("[SHOW] show={}", mi.isSelected());
        }

        if( mi.isSelected() && !sp.getItems().contains(console) ) {
            if( logger.isDebugEnabled()) {
                logger.debug("[SHOW] adding console region");
            }

            console.setOpacity(0.0d);

            sp.getItems().add(console);

            FadeTransition ft = new FadeTransition(Duration.millis(400), console);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.setCycleCount(1);
            ft.setAutoReverse(false);
            ft.play();

            return;
        }

        if( !mi.isSelected() && sp.getItems().contains(console)) {

            if( logger.isDebugEnabled()) {
                logger.debug("[SHOW] removing console region");
            }

            FadeTransition ft = new FadeTransition(Duration.millis(300), console);
            ft.setFromValue(1.0);
            ft.setToValue(0.1);
            ft.setCycleCount(1);
            ft.setAutoReverse(false);
            ft.play();

            ft.setOnFinished( (e) -> sp.getItems().remove(console) );

            return;
        }
    }

    @FXML
    public void close() {

        //
        // save config prior to exit()
        //

        Platform.exit();
    }

    @FXML
    public void newProfile() {

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE]");
        }

        configurationDS.getActiveProfile().reset();

        Stage s = (Stage) sp.getScene().getWindow();
        s.setTitle("ResignatorApp");
    }

    @FXML
    public void loadProfile() {

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE]");
        }

        //
        // Get profiles from loaded Configuration object
        //
        List<Profile> profiles = configurationDS.getProfiles();

        if( CollectionUtils.isEmpty(profiles) ) {

            if(logger.isDebugEnabled() ) {
                logger.debug("[LOAD PROFILE] no profiles");
            }

            String msg = "Select File > Save Profile to save the active profile.";
            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION,
                    msg);
            alert.setHeaderText("No profiles saved");
            alert.showAndWait();
            return;
        }

        //
        // Distill list of profile names from List of Profile objects
        //
        List<String> profileNames = profiles.
                stream().
                sorted(comparing(Profile::getProfileName)).
                map(Profile::getProfileName).
                collect(toList());

        //
        // Select default item which is active item if available otherwise first item
        //
        String defaultProfileName = profileNames.get(0);

        //
        // Prompt user for selection - default is first item
        //
        if( StringUtils.isNotEmpty(activeProfileName.getValue()) ) {
            defaultProfileName = activeProfileName.getValue();
        }

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE] default profileName={}", defaultProfileName);
        }

        Dialog dialog = new ChoiceDialog<>(defaultProfileName, profileNames);
        dialog.setTitle("Profile");
        dialog.setHeaderText("Select profile ");
        Optional<String> result = dialog.showAndWait();

        if( !result.isPresent() ) {
            return; // cancel
        }

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE] selected={}", result.get());
        }

        configurationDS.loadProfile( result.get() );

        Stage s = (Stage) sp.getScene().getWindow();
        s.setTitle("ResignatorApp - " + result.get());

    }

    @FXML
    public void saveProfile() {

        if( logger.isDebugEnabled() ){
            logger.debug("[SAVE PROFILE]");
        }

        if( activeProfileName.isEmpty().get() ) {

            if( logger.isDebugEnabled() ){
                logger.debug("[SAVE PROFILE] activeProfileName is empty");
            }

            Dialog dialog = new TextInputDialog();
            dialog.setTitle("Profile name");
            dialog.setHeaderText("Enter profile name");
            Optional<String> result = dialog.showAndWait();

            if( result.isPresent() ) {
                activeProfileName.set(result.get());

                try {
                    configurationDS.saveProfile();  // saves active profile

                    Stage s = (Stage) sp.getScene().getWindow();
                    s.setTitle("ResignatorApp - " + result.get());
                } catch(IOException exc) {
                    logger.error( "error saving profile '" + result.get() + "'", exc );

                    Alert alert = new Alert(
                            Alert.AlertType.ERROR,
                            exc.getMessage());
                    alert.setHeaderText("Can't save profile");
                    alert.showAndWait();
                }

            } else {
                String msg = "A profile name is required";
                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        msg);
                alert.setHeaderText("Can't save profile");
                alert.showAndWait();
            }
        } else { // just save

            if( logger.isDebugEnabled() ){
                logger.debug("[SAVE PROFILE] there is an active profile");
            }

            try {
                configurationDS.saveProfile();  // saves active profile
            } catch(IOException exc) {
                logger.error( "error saving profile '" + activeProfileName.get() + "'", exc );

                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        exc.getMessage());
                alert.setHeaderText("Can't save profile");
                alert.showAndWait();
            }

        }
    }

    @FXML
    public void saveAsProfile() {

        Dialog dialog = new TextInputDialog();
        dialog.setTitle("Profile name");
        dialog.setHeaderText("Enter profile name");
        Optional<String> result = dialog.showAndWait();

        if( result.isPresent() ) {

            //
            // Check for uniqueness; prompt for overwrite
            //
            if( profileNameInUse(result.get()) ) {
                if( logger.isDebugEnabled() ) {
                    logger.debug("[SAVE AS] profile name in use; prompt for overwrite");
                }
                Alert alert = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Overwrite existing profile '" + result.get() + "'?");
                alert.setHeaderText("Profile name in use");
                Optional<ButtonType> response = alert.showAndWait();
                if( !response.isPresent() || response.get() != ButtonType.OK ) {
                    if( logger.isDebugEnabled() ) {
                        logger.debug("[SAVE AS] overwrite canceled");
                    }
                    return;
                }
            }

            activeProfileName.set(result.get());  // activeProfile object tweaked w. new name

            try {
                configurationDS.saveProfile();

                Stage s = (Stage) sp.getScene().getWindow();
                s.setTitle("ResignatorApp - " + result.get());

            } catch(IOException exc) {
                logger.error( "error saving profile '" + result.get() + "'", exc );

                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        exc.getMessage());
                alert.setHeaderText("Can't save profile");
                alert.showAndWait();
            }

        } else {
            String msg = "A profile name is required";
            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    msg);
            alert.setHeaderText("Can't save profile");
            alert.showAndWait();
        }
    }

    boolean profileNameInUse(String profileName) {
        return configurationDS.
                getProfiles().
                stream().
                filter(p -> StringUtils.equalsIgnoreCase(p.getProfileName(), profileName)).
                count() > 0;
    }

    @FXML
    public void browseSource() {
        if( logger.isDebugEnabled() ){
            logger.debug("[BROWSE SOURCE]");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Source JAR");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JAR", "*.jar")
        );

        File f = fileChooser.showOpenDialog(stage);
        if( f != null ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[BROWSE SOURCE] selected file={}", f.getAbsolutePath());
            }
            tfSourceFile.setText( f.getAbsolutePath() );
        }
    }

    @FXML
    public void browseTarget() {
        if( logger.isDebugEnabled() ){
            logger.debug("[BROWSE TARGET]");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Target JAR");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JAR", "*.jar")
        );

        File f = fileChooser.showOpenDialog(stage);
        if( f != null ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[BROWSE TARGET] selected file={}", f.getAbsolutePath());
            }
            tfTargetFile.setText( f.getAbsolutePath() );
        }
    }

    @FXML
    public void copySourceToTarget() {
        if( logger.isDebugEnabled() ){
            logger.debug("[COPY SOURCE TO TARGET]");
        }
        tfTargetFile.setText( tfSourceFile.getText() );
    }

    @FXML
    public void sign() {
        if( logger.isDebugEnabled() ) {
            logger.debug("[SIGN] activeProfile sourceFile={}, targetFile={}",
                    configurationDS.getActiveProfile().getSourceFileFileName(),
                    configurationDS.getActiveProfile().getTargetFileFileName() );
        }
    }

    @FXML
    public void openSettings() {
        SettingsController settingsView = settingsControllerProvider.get();
        try {
            settingsView.show();  // ignoring the primaryStage
        } catch(Exception exc) {
            String msg = "Error launching Settings";
            logger.error( msg, exc );
            Alert alert = new Alert(Alert.AlertType.ERROR, msg);
            alert.showAndWait();
        }
    }
}
