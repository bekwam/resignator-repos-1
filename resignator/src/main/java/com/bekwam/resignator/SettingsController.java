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
import com.bekwam.resignator.model.Configuration;
import com.bekwam.resignator.model.ConfigurationDataSource;
import com.google.common.base.Preconditions;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
    TextField tfJDKHome;

    @FXML
    ProgressIndicator piSettings;
    
    @FXML
    Label lblErrJDKHome;

    @Inject
    ConfigurationDataSource configurationDS;

    @Inject
    ActiveConfiguration activeConfiguration;
    
    private String jdkDir = System.getProperty("java.home");
    private boolean dirtyFlag = false;
    
    @FXML
    public void initialize() {
    	
        if( logger.isDebugEnabled() ){
            logger.debug("[INIT]");
        }
        tfJDKHome.textProperty().bindBidirectional(activeConfiguration.jdkHomeProperty());

        tfJDKHome.textProperty().addListener(evt -> {
            dirtyFlag = true;
            lblErrJDKHome.setVisible(false);
        });
        
        piSettings.setVisible(false);
        lblErrJDKHome.setVisible(false);
    }

    @FXML
    public void browseForJDK() {
        if( logger.isDebugEnabled() ){
            logger.debug("[BROWSE FOR JDK]");
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select a JDK");
        dirChooser.setInitialDirectory(new File(jdkDir));
        
        File d = dirChooser.showDialog(stage);
        if( d != null ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("[BROWSE FOR JDK] selected dir={}", d.getAbsolutePath());
            }
            tfJDKHome.setText(d.getAbsolutePath());
            jdkDir = FilenameUtils.getFullPath(d.getAbsolutePath());
        }
    }

    @FXML
    public void save(ActionEvent evt) {

        if( logger.isDebugEnabled() ) {
            logger.debug("[SAVE] saving configuration; jarsignerExec={}", activeConfiguration.jdkHomeProperty());
        }

        try {
        	piSettings.setProgress(0.0d);
        	piSettings.setVisible(true);
        	
        	piSettings.setProgress(0.3d);

        	if( validateJDKHome(activeConfiguration.getJDKHome()) ) {
            	piSettings.setProgress(0.4d);
        		configurationDS.saveConfiguration();
            	piSettings.setProgress(0.6d);

                ((Button)evt.getSource()).getScene().getWindow().hide();

        	} else {
        		// report error
        		lblErrJDKHome.setVisible(true);

                // still dirty
        	}

            piSettings.setVisible(false);            

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
            		logger.debug("[CANCEL] reverting configuration ac jdkhome={} to sc jjdkhome={}",
            				activeConfiguration.getJDKHome(),
            				savedConf.getJDKHome().get());
                }

                activeConfiguration.setJDKHome(savedConf.getJDKHome().get());

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
    
    private boolean validateJDKHome(String jdkHome) {

        Preconditions.checkNotNull( jdkHome );

        Map<Path, Integer> cmdsAndResults = new LinkedHashMap<>();
        cmdsAndResults.put(Paths.get(jdkHome, "bin", "keytool"),0);
        cmdsAndResults.put(Paths.get(jdkHome, "bin", "jarsigner"),0);
        cmdsAndResults.put(Paths.get(jdkHome, "bin", "jar"),1);

        try {
            piSettings.setVisible(true);
            piSettings.setProgress(0.0d);

            for (Path cmd : cmdsAndResults.keySet()) {

                if (logger.isDebugEnabled()) {
                    logger.debug("[VAL JDKHOME] cmd={}", cmd.toString());
                }

                Process p = Runtime.getRuntime().exec(new String[]{cmd.toString()});

                int exitValue = p.waitFor();

                if (logger.isDebugEnabled()) {
                    logger.debug("[VAL JDKHOME] retval={}", exitValue);
                }

                if( exitValue != cmdsAndResults.get(cmd) ) {  // lookup expected return code
                    return false;
                }

                piSettings.setProgress(
                        piSettings.getProgress() + 1/cmdsAndResults.size()
                );
            }
        } catch(IOException | InterruptedException exc){
            logger.error("error running '{" + jdkHome + "}'", exc);
            return false;
        } finally {
            piSettings.setVisible( false );
            piSettings.setProgress(0.0d);
        }

        return true;
    }
}
