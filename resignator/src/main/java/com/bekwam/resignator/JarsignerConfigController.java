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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bekwam.jfxbop.guice.GuiceBaseView;
import com.bekwam.jfxbop.view.Viewable;
import com.bekwam.resignator.model.ConfigurationDataSource;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

/**
 * Screen dedicated to gathering jarsigner.exe command info
 * 
 * @author carlwalker
 * @since 1.0.0
 */
@Viewable(
        fxml="/fxml/JarsignerConfig.fxml",
        stylesheet = "/css/resignator.css",
        title = "Jarsigner Config"
)
@Singleton
public class JarsignerConfigController extends GuiceBaseView {

    private final static Logger logger = LoggerFactory.getLogger(JarsignerConfigController.class);

	@FXML
	private TextField tfKeystore;
	
	@FXML
	private TextField tfKeypass;
	
	@FXML
	private TextField tfStorepass;
	
	@FXML
	private TextField tfAlias;
	
	@FXML
	private ChoiceBox<Boolean> cbVerbose;
	
	@Inject
	private ConfigurationDataSource configurationDS;
	
	@FXML
	public void initialize() {
		
		if( logger.isDebugEnabled() ) {
			logger.debug("[INIT]");
		}
		
		cbVerbose.getItems().addAll(Boolean.TRUE, Boolean.FALSE);
		
		configurationDS.getActiveProfile().jarsignerConfigAliasProperty().bindBidirectional(tfAlias.textProperty());
		configurationDS.getActiveProfile().jarsignerConfigStorepassProperty().bindBidirectional(tfStorepass.textProperty());
		configurationDS.getActiveProfile().jarsignerConfigKeystoreProperty().bindBidirectional(tfKeystore.textProperty());
		configurationDS.getActiveProfile().jarsignerConfigKeypassProperty().bindBidirectional(tfKeypass.textProperty());
		configurationDS.getActiveProfile().jarsignerConfigVerboseProperty().bindBidirectional(cbVerbose.valueProperty());
	}
	
	@FXML
	public void save() {
		if( logger.isDebugEnabled() ) {
			logger.debug("[SAVE]");
		}
	}
	
	@FXML
	public void cancel(ActionEvent evt) {
		if( logger.isDebugEnabled() ) {
			logger.debug("[CANCEL]");
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
