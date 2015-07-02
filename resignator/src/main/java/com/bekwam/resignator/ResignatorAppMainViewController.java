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
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Optional;


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

    private StringProperty activeProfileName = new SimpleStringProperty("");  // a "hidden" field

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
    public void loadProfile() {


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
        }
    }

    @FXML
    public void saveAsProfile() {

    }

}
