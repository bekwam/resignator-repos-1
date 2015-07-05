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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

/**
 * Captures application-wide settings
 *
 * @author carl_000
 */
@Viewable(
        fxml="/fxml/Settings.fxml",
        stylesheet = "/css/resignator.css",
        title = "Settings"
)
@Singleton
public class SettingsController extends GuiceBaseView {

    private final static Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML
    TextField tfJarsignerExec;

    @Inject
    ConfigurationDataSource configurationDS;

    private StringProperty jarsignerExec = new SimpleStringProperty("");  // a "hidden" field

    @FXML
    public void initialize() {
        if( logger.isDebugEnabled() ){
            logger.debug("[INIT]");
        }
        tfJarsignerExec.textProperty().bindBidirectional(jarsignerExec, new DefaultStringConverter());
    }

    @FXML
    public void browse() {
        if( logger.isDebugEnabled() ){
            logger.debug("[BROWSE]");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select jarsigner.exe");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("EXE", "*.exe")
        );

        File f = fileChooser.showOpenDialog(stage);
        if( f != null ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[BROWSE] selected file={}", f.getAbsolutePath());
            }
            tfJarsignerExec.setText(f.getAbsolutePath());
        }
    }

    @FXML
    public void save() {

        if( logger.isDebugEnabled() ) {
            logger.debug("[SAVE] saving configuration; jarsignerExec={}", jarsignerExec.getValue());
        }

        try {
            configurationDS.setJarsignerExec(jarsignerExec.get());
            configurationDS.saveConfiguration();
        } catch(IOException exc) {

            logger.error("error saving or setting jarsignerexec, exc");

            String msg = exc.getMessage();
            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    msg);
            alert.setHeaderText("Can't save configuration");
            alert.showAndWait();

        }
    }

    @FXML
    public void cancel(ActionEvent evt) {

        //
        // For some reason, this.getScene() which is on the fx:root returns null
        //

        Scene scene = ((Button)evt.getSource()).getScene();
        if( scene != null ) {
            Window w = scene.getWindow();
            if (w != null) {
                w.hide();
            }
        }
    }

}
