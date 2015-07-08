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

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bekwam.jfxbop.guice.GuiceBaseView;
import com.bekwam.jfxbop.view.Viewable;
import com.bekwam.resignator.model.Configuration;
import com.bekwam.resignator.model.ConfigurationDataSource;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

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

    @Inject
    ActiveConfiguration activeConfiguration;
    
    private String jarsignerDir = System.getProperty("java.home");
    private boolean dirtyFlag = false;
    
    @FXML
    public void initialize() {
    	
        if( logger.isDebugEnabled() ){
            logger.debug("[INIT]");
        }
        tfJarsignerExec.textProperty().bindBidirectional(activeConfiguration.jarsignerExecutableProperty());
        
        tfJarsignerExec.textProperty().addListener(evt -> {
        	dirtyFlag = true;
        });
    }

    @FXML
    public void browse() {
        if( logger.isDebugEnabled() ){
            logger.debug("[BROWSE]");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select jarsigner.exe");
        fileChooser.setInitialDirectory(new File(jarsignerDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("EXE", "*.exe")
        );

        File f = fileChooser.showOpenDialog(stage);
        if( f != null ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[BROWSE] selected file={}", f.getAbsolutePath());
            }
            tfJarsignerExec.setText(f.getAbsolutePath());
            
            jarsignerDir = FilenameUtils.getFullPath(f.getAbsolutePath());
        }
    }

    @FXML
    public void save(ActionEvent evt) {

        if( logger.isDebugEnabled() ) {
            logger.debug("[SAVE] saving configuration; jarsignerExec={}", activeConfiguration.jarsignerExecutableProperty());
        }

        try {
            configurationDS.saveConfiguration();
            
            Scene scene = ((Button)evt.getSource()).getScene();
            if( scene != null ) {
                Window w = scene.getWindow();
                if (w != null) {
                    w.hide();
                }
            }

            dirtyFlag = false;
            
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

    	if( dirtyFlag ) {
            
    		Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Form has been modified");
            alert.setHeaderText("Discard edits?");
            
            Optional<ButtonType> response = alert.showAndWait();

            if( response.isPresent() && response.get() == ButtonType.OK ) {
            	
            	Configuration savedConf = configurationDS.getConfiguration();

            	if( logger.isDebugEnabled() ) {
            		logger.debug("[CANCEL] reverting configuration ac jse={} to sc jse={}", 
            				activeConfiguration.getJarsignerExecutable(),
            				savedConf.getJarsignerExecutable().get());
            	}
            	
            	activeConfiguration.setJarsignerExecutable( savedConf.getJarsignerExecutable().get() );
            	
            	dirtyFlag = false;
            	
            } else {
            	return;  // dirtyFlag continues to be true
            }
    	}

   		Scene scene = ((Button)evt.getSource()).getScene();
   		if( scene != null ) {
   			Window w = scene.getWindow();
   			if (w != null) {
   				w.hide();
   			}
   		}
    }
}
