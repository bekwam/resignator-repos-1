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

import com.google.common.base.Preconditions;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bekwam.jfxbop.guice.GuiceBaseView;
import com.bekwam.jfxbop.view.Viewable;
import com.bekwam.resignator.model.ConfigurationDataSource;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
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
	private PasswordField pfKeypass;

	@FXML
	private PasswordField pfConfKeypass;

	@FXML
	private PasswordField pfStorepass;

	@FXML
	private PasswordField pfConfStorepass;

	@FXML
	private TextField tfAlias;
	
	@FXML
	private ChoiceBox<Boolean> cbVerbose;

	@FXML
	private Label lblConfKeypass;

	@FXML
	private Label lblConfStorepass;

	@Inject
	private ConfigurationDataSource configurationDS;

	public void JarsignerConfigController() {
		if( logger.isDebugEnabled() ) {
			logger.debug("[CONSTRUCTOR]");
		}
	}

	@FXML
	public void initialize() {
		
		if( logger.isDebugEnabled() ) {
			logger.debug("[INIT] instance={}, configurationDS={}", this.hashCode(), configurationDS.hashCode());
		}
		
		cbVerbose.getItems().addAll(Boolean.TRUE, Boolean.FALSE);

		tfAlias.textProperty().bindBidirectional(configurationDS.getActiveProfile().jarsignerConfigAliasProperty());
		pfStorepass.textProperty().bindBidirectional(configurationDS.getActiveProfile().jarsignerConfigStorepassProperty());
		tfKeystore.textProperty().bindBidirectional(configurationDS.getActiveProfile().jarsignerConfigKeystoreProperty());
		pfKeypass.textProperty().bindBidirectional(configurationDS.getActiveProfile().jarsignerConfigKeypassProperty());
		cbVerbose.valueProperty().bindBidirectional(configurationDS.getActiveProfile().jarsignerConfigVerboseProperty());

		lblConfKeypass.setVisible(false);
		lblConfStorepass.setVisible( false );
	}
	
	@FXML
	public void close(ActionEvent evt) {
		if( logger.isDebugEnabled() ) {
			logger.debug("[CLOSE]");
		}
        Scene scene = ((Button)evt.getSource()).getScene();
        if( scene != null ) {
            Window w = scene.getWindow();
            if (w != null) {
                w.hide();
            }
        }

	}

	@Override
	public void show() throws Exception {
		super.show();
		if( logger.isDebugEnabled() ) {
			logger.debug("[SHOW] instance={}, configurationDS={}", this.hashCode(), configurationDS.hashCode());
		}

		resetConfKeypass();
		resetConfStorepass();
	}

	@FXML
	public void resetConfKeypass() {
		pfConfKeypass.setText("");

		lblConfKeypass.setText("Ok");
		lblConfKeypass.setTextFill(Color.GREEN);
		lblConfKeypass.setVisible(false);
	}

	@FXML
	public void verifyKeypass() {

		Preconditions.checkNotNull(pfKeypass.textProperty());
		Preconditions.checkNotNull(pfConfKeypass.textProperty());

		if( StringUtils.equals(pfKeypass.textProperty().getValue(), pfConfKeypass.textProperty().getValue()) ) {
			lblConfKeypass.setText("Ok");
			lblConfKeypass.setTextFill(Color.GREEN);
		} else {
			lblConfKeypass.setText("Bad");
			lblConfKeypass.setTextFill(Color.RED);
		}

		lblConfKeypass.setVisible(true);
	}

	@FXML
	public void resetConfStorepass() {
		pfConfStorepass.setText("");

		lblConfStorepass.setText("Ok");
		lblConfStorepass.setTextFill(Color.GREEN);
		lblConfStorepass.setVisible(false);
	}

	@FXML
	public void verifyStorepass() {

		Preconditions.checkNotNull(pfStorepass.textProperty());
		Preconditions.checkNotNull(pfConfStorepass.textProperty());

		if( StringUtils.equals(pfStorepass.textProperty().getValue(), pfConfStorepass.textProperty().getValue()) ) {
			lblConfStorepass.setText("Ok");
			lblConfStorepass.setTextFill(Color.GREEN);
		} else {
			lblConfStorepass.setText("Bad");
			lblConfStorepass.setTextFill(Color.RED);
		}
		lblConfStorepass.setVisible(true);
	}
}
