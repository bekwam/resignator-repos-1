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
import com.bekwam.resignator.commands.SignCommand;
import com.bekwam.resignator.commands.UnsignCommand;
import com.bekwam.resignator.model.ConfigurationDataSource;
import com.bekwam.resignator.model.Profile;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
@Singleton
public class ResignatorAppMainViewController extends GuiceBaseView {

    private final static Logger logger = LoggerFactory.getLogger(ResignatorAppMainViewController.class);

    public final BooleanProperty needsSave = new SimpleBooleanProperty(false);

    private final InvalidationListener needsSaveListener = (evt) ->  needsSave.set(true);
    private final MenuItem MI_NO_PROFILES = new MenuItem("< None >");
    @FXML
    SplitPane sp;
    @FXML
    SplitPane outerSp;
    @FXML
    VBox console;
    @FXML
    VBox profileBrowser;
    @FXML
    TextField tfSourceFile;
    @FXML
    TextField tfTargetFile;
    @FXML
    Label lblStatus;
    @FXML
    ProgressIndicator piSignProgress;
    @FXML
    TextArea txtConsole;
    @FXML
    CheckBox ckReplace;
    @FXML
    MenuItem miSave;
    @FXML
    ListView<String> lvProfiles;
    @FXML
    Menu mRecentProfiles;
    @Inject
    ConfigurationDataSource configurationDS;
    private final EventHandler<ActionEvent> recentProfileLoadHandler = (evt) -> doLoadProfile(((MenuItem) evt.getSource()).getText());
    @Inject @Named("ConfigDir")
    String configDir;

    @Inject @Named("ConfigFile")
    String configFile;

    @Inject
    Provider<SettingsController> settingsControllerProvider;

    @Inject
    Provider<JarsignerConfigController> jarsignerConfigControllerProvider;

    @Inject
    ActiveConfiguration activeConfiguration;

    @Inject
    ActiveProfile activeProfile;

    @Inject
    Provider<SignCommand> signCommandProvider;

    @Inject
    Provider<UnsignCommand> unsignCommandProvider;

    @Inject
    @Named("NumRecentProfiles")
    Integer numRecentProfiles = 4;

    private String jarDir = System.getProperty("user.home");

    @FXML
    public void initialize() {

        try {
            activeConfiguration.activeProfileProperty().bindBidirectional(activeProfile.profileNameProperty());
            tfSourceFile.textProperty().bindBidirectional(activeProfile.sourceFileFileNameProperty());
            tfTargetFile.textProperty().bindBidirectional(activeProfile.targetFileFileNameProperty());
            ckReplace.selectedProperty().bindBidirectional(activeProfile.replaceSignaturesProperty());

            miSave.disableProperty().bind(needsSave.not());

            tfSourceFile.textProperty().addListener(new WeakInvalidationListener(needsSaveListener));
            tfTargetFile.textProperty().addListener(new WeakInvalidationListener(needsSaveListener));

            lvProfiles.getSelectionModel().selectedItemProperty().addListener((ov, old_v, new_v) -> {

                if (needsSave.getValue()) {

                    Alert alert = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "Overwrite existing profile?");
                    alert.setHeaderText("Unsaved profile");
                    Optional<ButtonType> response = alert.showAndWait();
                    if (!response.isPresent() || response.get() != ButtonType.OK) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("[SELECT] overwrite canceled");
                        }
                        return;
                    }
                }

                doLoadProfile(new_v);
            });

            Task<Void> t = new Task<Void>() {

                @Override
                protected Void call() throws Exception {

                    updateMessage("Loading configuration");
                    configurationDS.loadConfiguration();

                    //
                    // init profileBrowser
                    //
                    final List<String> profileNames = configurationDS.getProfiles().
                            stream().
                            map(Profile::getProfileName).
                            sorted((o1, o2) -> o1.compareToIgnoreCase(o2)).
                            collect(Collectors.toList());

                    final List<String> recentProfiles = configurationDS.getRecentProfileNames();

                    Platform.runLater(() -> {
                        lvProfiles.setItems(FXCollections.observableArrayList(profileNames));

                        if (CollectionUtils.isNotEmpty(recentProfiles)) {
                            mRecentProfiles.getItems().clear();
                            mRecentProfiles.getItems().addAll(FXCollections.observableArrayList(
                                    recentProfiles.stream().
                                            map((s) -> new MenuItem(s)).
                                            collect(Collectors.toList())
                            ));
                        }
                    });

                    return null;
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    updateMessage("");
                }

                @Override
                protected void cancelled() {
                    super.cancelled();
                    updateMessage("");
                }

                @Override
                protected void failed() {
                    super.failed();
                    updateMessage("");
                }
            };

            lblStatus.textProperty().bind(t.messageProperty());

            new Thread(t).start();

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
        }
    }

    @FXML
    public void showProfileBrowser(ActionEvent evt) {

        CheckMenuItem mi = (CheckMenuItem)evt.getSource();

        if( logger.isDebugEnabled() ) {
            logger.debug("[SHOW] show={}", mi.isSelected());
        }

        if( mi.isSelected() && !outerSp.getItems().contains(profileBrowser) ) {
            if( logger.isDebugEnabled()) {
                logger.debug("[SHOW] adding profileBrowser region");
            }

            profileBrowser.setOpacity(0.0d);

            outerSp.getItems().add(0, profileBrowser);
            outerSp.setDividerPositions(0.3);

            FadeTransition ft = new FadeTransition(Duration.millis(400), profileBrowser);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.setCycleCount(1);
            ft.setAutoReverse(false);
            ft.play();

            return;
        }

        if( !mi.isSelected() && outerSp.getItems().contains(profileBrowser)) {

            if( logger.isDebugEnabled()) {
                logger.debug("[SHOW] removing profileBrowser region");
            }

            FadeTransition ft = new FadeTransition(Duration.millis(300), profileBrowser);
            ft.setFromValue(1.0);
            ft.setToValue(0.1);
            ft.setCycleCount(1);
            ft.setAutoReverse(false);
            ft.play();

            ft.setOnFinished( (e) -> outerSp.getItems().remove(profileBrowser) );
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

        activeProfile.reset();

        Stage s = (Stage) sp.getScene().getWindow();
        s.setTitle("ResignatorApp");
    }

    @FXML
    public void loadProfile() {

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE]");
        }

        //
        // Clear output from last operation
        //
        lblStatus.setText("");
        txtConsole.setText("");
        piSignProgress.setProgress(0.0d);
        piSignProgress.setVisible(false);
        clearValidationErrors();
        
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
        if( StringUtils.isNotEmpty(activeConfiguration.activeProfileProperty().getValue()) ) {
            defaultProfileName = activeConfiguration.activeProfileProperty().getValue();
        }

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE] default profileName={}", defaultProfileName);
        }

        Dialog<String> dialog = new ChoiceDialog<>(defaultProfileName, profileNames);
        dialog.setTitle("Profile");
        dialog.setHeaderText("Select profile ");
        Optional<String> result = dialog.showAndWait();

        if( !result.isPresent() ) {
            return; // cancel
        }

        if( logger.isDebugEnabled() ) {
            logger.debug("[LOAD PROFILE] selected={}", result.get());
        }

        doLoadProfile( result.get() );
    }

    private void doLoadProfile(String profileName) {
        configurationDS.loadProfile( profileName );

        Stage s = (Stage) sp.getScene().getWindow();
        s.setTitle("ResignatorApp - " + profileName);

        needsSave.set(false);  // this was just loaded
    }

    @FXML
    public void saveProfile() {

        if( logger.isDebugEnabled() ){
            logger.debug("[SAVE PROFILE]");
        }

        if( activeConfiguration.activeProfileProperty().isEmpty().get() ) {

            if( logger.isDebugEnabled() ){
                logger.debug("[SAVE PROFILE] activeProfileName is empty");
            }

            Dialog<String> dialog = new TextInputDialog();
            dialog.setTitle("Profile name");
            dialog.setHeaderText("Enter profile name");
            Optional<String> result = dialog.showAndWait();

            if( result.isPresent() ) {
                String newProfileName = result.get();
                activeConfiguration.activeProfileProperty().set(newProfileName);

                try {
                    recordRecentProfile(newProfileName);  // #18
                    configurationDS.saveProfile();  // saves active profile

                    Stage s = (Stage) sp.getScene().getWindow();
                    s.setTitle("ResignatorApp - " + newProfileName);

                    needsSave.set(false);

                    addToProfileBrowser(newProfileName);

                } catch(IOException exc) {
                    logger.error( "error saving profile '" + newProfileName + "'", exc );

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
                recordRecentProfile(activeProfile.getProfileName());  // #18
                configurationDS.saveProfile();  // saves active profile
                needsSave.set(false);

            } catch(IOException exc) {
                logger.error( "error saving profile '" + activeConfiguration.activeProfileProperty().get() + "'", exc );

                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        exc.getMessage());
                alert.setHeaderText("Can't save profile");
                alert.showAndWait();
            }

        }
    }

    private void addToProfileBrowser(String newProfileName) {
        int pos = 0;
        for (; pos < CollectionUtils.size(lvProfiles.getItems()); pos++) {
            String pn = lvProfiles.getItems().get(pos);
            if (pn.compareToIgnoreCase(newProfileName) > 0) {
                break;
            }
        }

        lvProfiles.getItems().add(pos, newProfileName);
    }

    void recordRecentProfile(String newProfileName) {

        //
        // #18 record recent history on save
        //
        List<MenuItem> rpItems = mRecentProfiles.getItems();
        MenuItem rpItem = new MenuItem(newProfileName);
        rpItem.setOnAction(recentProfileLoadHandler);

        if (CollectionUtils.isNotEmpty(rpItems)) {
            if (CollectionUtils.size(rpItems) == 1) {
                if (StringUtils.equalsIgnoreCase(rpItems.get(0).getText(), MI_NO_PROFILES.getText())) {
                    rpItems.set(0, rpItem);
                } else {
                    rpItems.add(0, rpItem);
                }
            } else {
                rpItems.add(0, rpItem);
                if (CollectionUtils.size(rpItems) > numRecentProfiles) {
                    for (int i = (CollectionUtils.size(rpItems) - 1); i >= numRecentProfiles; i--) {
                        rpItems.remove(i);
                    }
                }
            }
        } else {
            // should never have no items (at least one < None >)
            rpItems.add(rpItem);
        }

        // reconcile with active record
        activeConfiguration.getRecentProfiles().clear();
        if (!(CollectionUtils.size(rpItems) == 1 &&
                StringUtils.equalsIgnoreCase(rpItems.get(0).getText(), MI_NO_PROFILES.getText()))
                ) {
            // there's more than just a < None > element
            activeConfiguration.setRecentProfiles(
                    rpItems.
                            stream().
                            map((mi) -> mi.getText()).
                            collect(Collectors.toList())
            );
        }
        // end #18
    }

    @FXML
    public void saveAsProfile() {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Profile name");
        dialog.setHeaderText("Enter profile name");
        Optional<String> result = dialog.showAndWait();

        if( result.isPresent() ) {

            //
            // Check for uniqueness; prompt for overwrite
            //
            final String profileName = result.get();
            if (profileNameInUse(profileName)) {
                if( logger.isDebugEnabled() ) {
                    logger.debug("[SAVE AS] profile name in use; prompt for overwrite");
                }
                Alert alert = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Overwrite existing profile '" + profileName + "'?");
                alert.setHeaderText("Profile name in use");
                Optional<ButtonType> response = alert.showAndWait();
                if( !response.isPresent() || response.get() != ButtonType.OK ) {
                    if( logger.isDebugEnabled() ) {
                        logger.debug("[SAVE AS] overwrite canceled");
                    }
                    return;
                }
            }

            activeConfiguration.activeProfileProperty().set(profileName);  // activeProfile object tweaked w. new name

            try {
                recordRecentProfile(activeProfile.getProfileName());  // #18
                configurationDS.saveProfile();

                Stage s = (Stage) sp.getScene().getWindow();
                s.setTitle("ResignatorApp - " + profileName);

                needsSave.set(false);

                addToProfileBrowser(profileName);

            } catch(IOException exc) {
                logger.error("error saving profile '" + profileName + "'", exc);

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

        clearValidationErrors();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Source JAR");
        fileChooser.setInitialDirectory(new File(jarDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JAR", "*.jar")
        );

        File f = fileChooser.showOpenDialog(stage);
        if( f != null ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[BROWSE SOURCE] selected file={}", f.getAbsolutePath());
            }
            tfSourceFile.setText(f.getAbsolutePath());

            jarDir = FilenameUtils.getFullPath(f.getAbsolutePath());
        }
    }

    @FXML
    public void browseTarget() {
        if( logger.isDebugEnabled() ){
            logger.debug("[BROWSE TARGET]");
        }

        clearValidationErrors();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Target JAR");
        fileChooser.setInitialDirectory(new File(jarDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JAR", "*.jar")
        );

        File f = fileChooser.showOpenDialog(stage);
        if( f != null ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[BROWSE TARGET] selected file={}", f.getAbsolutePath());
            }
            tfTargetFile.setText( f.getAbsolutePath() );

            jarDir = FilenameUtils.getFullPath(f.getAbsolutePath());
        }
    }

    @FXML
    public void copySourceToTarget() {
        if( logger.isDebugEnabled() ){
            logger.debug("[COPY SOURCE TO TARGET]");
        }
        clearValidationErrors();
        tfTargetFile.setText(tfSourceFile.getText());
    }

    @FXML
    public void clearValidationErrors() {
    	if( tfSourceFile.getStyleClass().contains("tf-validation-error") ) {
    		tfSourceFile.getStyleClass().remove("tf-validation-error");
    	}
    	if( tfTargetFile.getStyleClass().contains("tf-validation-error") ) {
    		tfTargetFile.getStyleClass().remove("tf-validation-error");
    	}
    }
    
    private boolean validateSign() {
    
    	if( logger.isDebugEnabled() ) {
    		logger.debug("[VALIDATE]");
    	}
    	
    	boolean isValid = true;

    	//
    	// Validate the Source JAR field
    	//
    	
    	if( StringUtils.isBlank(activeProfile.getSourceFileFileName()) ) {

    		if( !tfSourceFile.getStyleClass().contains("tf-validation-error") ) {
    			tfSourceFile.getStyleClass().add("tf-validation-error");
    		}    		
    		isValid = false;

    	} else {

    		if( !new File(activeProfile.getSourceFileFileName()).exists() ) {

                if( !tfSourceFile.getStyleClass().contains("tf-validation-error") ) {
                    tfSourceFile.getStyleClass().add("tf-validation-error");
                }

                Alert alert = new Alert(
                		Alert.AlertType.ERROR,
                		"Specified Source JAR does not exist"
                		);
                
                alert.showAndWait();

        		isValid = false;
    		}
    	}

    	//
    	// Validate the TargetJAR field
    	//

    	if( StringUtils.isBlank(activeProfile.getTargetFileFileName() ) ) {
    		if( !tfTargetFile.getStyleClass().contains("tf-validation-error") ) {
    			tfTargetFile.getStyleClass().add("tf-validation-error");
    		}
    		isValid = false;
    	}

    	//
    	// #13 Validate the Jarsigner Config form
    	//

    	String jarsignerConfigField = "";
    	String jarsignerConfigMessage = "";
    	if( isValid && StringUtils.isBlank(activeProfile.getJarsignerConfigKeystore() ) ) {
    		jarsignerConfigField = "Keystore";
    		jarsignerConfigMessage = "A keystore must be specified";
    	} else if( isValid && StringUtils.isBlank(activeProfile.getJarsignerConfigStorepass() ) ) {
    		jarsignerConfigField = "Storepass";
        	jarsignerConfigMessage = "A password for the keystore must be specified";
    	} else if( isValid && StringUtils.isBlank(activeProfile.getJarsignerConfigAlias() ) ) {
        	jarsignerConfigField = "Alias";
        	jarsignerConfigMessage = "An alias for the key must be specified";
        } else if( isValid && StringUtils.isBlank(activeProfile.getJarsignerConfigKeypass() ) ) {
        	jarsignerConfigField = "Keypass";
    		jarsignerConfigMessage = "A password for the key must be specified";
        }

    	if( StringUtils.isNotEmpty(jarsignerConfigMessage) ) {

    		if( logger.isDebugEnabled() ) {
    			logger.debug("[VALIDATE] jarsigner config not valid {}", jarsignerConfigMessage);
    		}

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Set " + jarsignerConfigField + " in Configure");
            alert.setHeaderText(jarsignerConfigMessage);

            FlowPane fp = new FlowPane();
            Label lbl = new Label("Set " + jarsignerConfigField + " in ");
            Hyperlink link = new Hyperlink("Configure");
            fp.getChildren().addAll( lbl, link);

            link.setOnAction( (evt) -> {
                alert.close();
                openJarsignerConfig();
            } );

            alert.getDialogPane().contentProperty().set( fp );
            alert.showAndWait();

            isValid = false;
    	}

    	return isValid;
    }
    
    @FXML
    public void sign() {

        if( logger.isDebugEnabled() ) {
            logger.debug("[SIGN] activeProfile sourceFile={}, targetFile={}",
                    activeProfile.getSourceFileFileName(),
                    activeProfile.getTargetFileFileName() );
        }

        boolean isValid = validateSign();
        
        if( !isValid ) {
        	if( logger.isDebugEnabled() ) {
        		logger.debug("[SIGN] form not valid; returning");
        	}
        	return;
        }
        
        final Boolean doUnsign = ckReplace.isSelected();

        //
        // #2 confirm an overwrite (if needed)
        //
        if (doUnsign) {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Overwrite any existing signatures in JAR?");
            alert.setHeaderText("Overwrite signatures");
            Optional<ButtonType> response = alert.showAndWait();
            if (!response.isPresent() || response.get() != ButtonType.OK) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[SIGN] overwrite cancelled");
                }
                return;
            }
        } else {

        	//
        	// #6 sign-only to a different target filename needs a copy and
        	// possible overwrite
        	//
        	
        	File tf = new File(activeProfile.getTargetFileFileName());
        	if( tf.exists() ) {
                Alert alert = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Overwrite existing file '" + tf.getName() + "'?");
                alert.setHeaderText("Overwrite existing file");
                Optional<ButtonType> response = alert.showAndWait();
                if (!response.isPresent() || response.get() != ButtonType.OK) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[SIGN] overwrite file cancelled");
                    }
                    return;
                }        		
        	}
        }
        
        UnsignCommand unsignCommand = unsignCommandProvider.get();
        SignCommand signCommand = signCommandProvider.get();

        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {

                updateMessage("");
                Platform.runLater(() -> piSignProgress.setVisible(true));
                updateProgress(0.1d, 1.0d);

                if (doUnsign) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[SIGN] doing unsign operation");
                    }
                    updateTitle("Unsigning JAR");
                    unsignCommand.unsignJAR(
                            Paths.get(activeProfile.getSourceFileFileName()),
                            Paths.get(activeProfile.getTargetFileFileName()),
                            s ->
                                    Platform.runLater(() ->
                                                    txtConsole.appendText(s + System.getProperty("line.separator"))
                                    )
                    );

                    if (isCancelled()) {
                        return null;
                    }
                } else {
                	
                	//
                	// #6 needs a copy to the target if target file doesn't
                	// exist
                	//
                	if( logger.isDebugEnabled() ) {
                		logger.debug("[SIGN] copying for sign operation");
                	}
                	updateTitle("Copying JAR");
                    Platform.runLater(
                            () -> txtConsole.appendText("Copying JAR" + System.getProperty("line.separator"))
                    );
                    unsignCommand.copyJAR(activeProfile.getSourceFileFileName(), activeProfile.getTargetFileFileName());
                }

                updateProgress(0.5d, 1.0d);
                updateTitle("Signing JAR");

                signCommand.signJAR(
                        Paths.get(activeProfile.getTargetFileFileName()),
                        Paths.get(activeProfile.getJarsignerConfigKeystore()),
                        activeProfile.getJarsignerConfigStorepass(),
                        activeProfile.getJarsignerConfigAlias(),
                        activeProfile.getJarsignerConfigKeypass(),
                        s ->
                                Platform.runLater(() ->
                                                txtConsole.appendText(s + System.getProperty("line.separator"))
                                )
                );

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                updateProgress(1.0d, 1.0d);
                updateMessage("JAR signed successfully");

                Platform.runLater( () -> {
                    piSignProgress.progressProperty().unbind();
                    lblStatus.textProperty().unbind();
                });
            }

            @Override
            protected void failed() {
                super.failed();

                logger.error("error unsigning and signing jar", exceptionProperty().getValue());

                updateProgress(1.0d, 1.0d);
                updateMessage("Error signing JAR");

                Platform.runLater(() -> {
                    piSignProgress.progressProperty().unbind();
                    lblStatus.textProperty().unbind();

                    piSignProgress.setVisible(false);

                    Alert alert = new Alert(Alert.AlertType.ERROR, exceptionProperty().getValue().getMessage());
                    alert.showAndWait();
                });
            }

            @Override
            protected void cancelled() {
                super.cancelled();

                if( logger.isWarnEnabled() ) {
                    logger.warn("signing jar operation cancelled");
                }

                updateProgress(1.0d, 1.0d);
                updateMessage("JAR signing cancelled");

                Platform.runLater(() -> {
                    piSignProgress.progressProperty().unbind();
                    lblStatus.textProperty().unbind();

                    piSignProgress.setVisible(false);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "JAR signing cancelled");
                    alert.showAndWait();
                });

            }
        };

        piSignProgress.progressProperty().bind(task.progressProperty());
        lblStatus.textProperty().bind(task.messageProperty());

        new Thread(task).start();
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
    
    @FXML
    public void openJarsignerConfig() {

    	clearValidationErrors();
    	
        if( StringUtils.isNotEmpty(activeConfiguration.getJDKHome()) ) {

            JarsignerConfigController jarsignerConfigView = jarsignerConfigControllerProvider.get();
            jarsignerConfigView.setParent( this );
            try {
                jarsignerConfigView.show();
            } catch (Exception exc) {
                String msg = "Error launching jarsigner config";
                logger.error(msg, exc);
                Alert alert = new Alert(Alert.AlertType.ERROR, msg);
                alert.showAndWait();
            }
        } else {
            if( logger.isDebugEnabled() ) {
                logger.debug("[OPEN JARSIGNER CONFIG] JDK_HOME not set");
            }

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Set JDK_HOME in File > Settings");
            alert.setHeaderText("JDK_HOME not defined");

            FlowPane fp = new FlowPane();
            Label lbl = new Label("Set JDK_HOME in ");
            Hyperlink link = new Hyperlink("File > Settings");
            fp.getChildren().addAll( lbl, link);

            link.setOnAction( (evt) -> {
                alert.close();
                openSettings();
            } );

            alert.getDialogPane().contentProperty().set( fp );

            alert.showAndWait();

        }
    }

    @FXML
    public void deleteProfile(ActionEvent evt) {

        final String profileNameToDelete = lvProfiles.getSelectionModel().getSelectedItem();

        if( logger.isDebugEnabled() ) {
            logger.debug("[DELETE PROFILE] delete {}", profileNameToDelete);
        }

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete profile '" + profileNameToDelete + "'?");
        alert.setHeaderText("Delete profile");
        Optional<ButtonType> response = alert.showAndWait();
        if (!response.isPresent() || response.get() != ButtonType.OK) {
            if (logger.isDebugEnabled()) {
                logger.debug("[DELETE PROFILE] delete profile cancelled");
            }
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                configurationDS.deleteProfile( profileNameToDelete );
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                Platform.runLater(() -> {

                    // #18 recent profiles
                    Iterator<MenuItem> iterator = mRecentProfiles.getItems().iterator();
                    while (iterator.hasNext()) {
                        MenuItem mi = iterator.next();
                        if (StringUtils.equalsIgnoreCase(mi.getText(), profileNameToDelete)) {
                            iterator.remove();
                        }
                    }
                    if (CollectionUtils.isEmpty(mRecentProfiles.getItems())) {
                        mRecentProfiles.getItems().add(MI_NO_PROFILES);
                    }

                    lvProfiles.getItems().remove( profileNameToDelete );

                    if( StringUtils.equalsIgnoreCase( profileNameToDelete, activeProfile.getProfileName() ) ) {
                        newProfile();
                    }

                    needsSave.set(false);
                });
            }

            @Override
            protected void failed() {
                super.failed();
                Platform.runLater( () -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, getException().getMessage());
                    alert.setHeaderText("Error deleting profile");
                    alert.showAndWait();
                });
            }

        };

        new Thread(task).start();
    }
}
