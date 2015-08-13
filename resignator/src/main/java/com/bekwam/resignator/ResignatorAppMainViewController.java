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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bekwam.jfxbop.guice.GuiceBaseView;
import com.bekwam.jfxbop.view.Viewable;
import com.bekwam.resignator.commands.SignCommand;
import com.bekwam.resignator.commands.UnsignCommand;
import com.bekwam.resignator.model.ConfigurationDataSource;
import com.bekwam.resignator.model.Profile;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

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

    @FXML
    SplitPane sp;

    @FXML
    VBox console;

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

    @Inject
    ConfigurationDataSource configurationDS;

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

    private String jarDir = System.getProperty("user.home");

    @FXML
    public void initialize() {

        try {
            configurationDS.loadConfiguration();

            activeConfiguration.activeProfileProperty().bindBidirectional(activeProfile.profileNameProperty());
            tfSourceFile.textProperty().bindBidirectional(activeProfile.sourceFileFileNameProperty());
            tfTargetFile.textProperty().bindBidirectional(activeProfile.targetFileFileNameProperty() );
            ckReplace.selectedProperty().bindBidirectional(activeProfile.replaceSignaturesProperty());

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

        configurationDS.loadProfile( result.get() );

        Stage s = (Stage) sp.getScene().getWindow();
        s.setTitle("ResignatorApp - " + result.get());

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
            	activeConfiguration.activeProfileProperty().set(result.get());

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
                logger.error( "error saving profile '" + activeConfiguration.activeProfileProperty().get() + "'", exc );

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

        Dialog<String> dialog = new TextInputDialog();
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

            activeConfiguration.activeProfileProperty().set(result.get());  // activeProfile object tweaked w. new name

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
            tfSourceFile.setText( f.getAbsolutePath() );

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
    	
    	if( StringUtils.isBlank(activeProfile.getSourceFileFileName() ) ) {

    		if( !tfSourceFile.getStyleClass().contains("tf-validation-error") ) {
    			tfSourceFile.getStyleClass().add("tf-validation-error");
    		}    		
    		isValid = false;
    		
    	} else {
    		
    		if( !new File(activeProfile.getSourceFileFileName()).exists() ) {
    			
                Alert alert = new Alert(
                		Alert.AlertType.ERROR,
                		"Specified Source JAR does not exist"
                		);
                
                alert.showAndWait();

        		if( !tfSourceFile.getStyleClass().contains("tf-validation-error") ) {
        			tfSourceFile.getStyleClass().add("tf-validation-error");
        		}    		

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
    	if( StringUtils.isBlank(activeProfile.getJarsignerConfigKeystore() ) ) {
    		jarsignerConfigField = "Keystore";
    		jarsignerConfigMessage = "A keystore must be specified";        		
    	} else if( StringUtils.isBlank(activeProfile.getJarsignerConfigStorepass() ) ) {
    		jarsignerConfigField = "Storepass";
        	jarsignerConfigMessage = "A password for the keystore must be specified";
    	} else if( StringUtils.isBlank(activeProfile.getJarsignerConfigAlias() ) ) {
        	jarsignerConfigField = "Alias";
        	jarsignerConfigMessage = "An alias for the key must be specified";
        } else if( StringUtils.isBlank(activeProfile.getJarsignerConfigKeypass() ) ) {
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

                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "JAR signing cancelle");
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
    
}
