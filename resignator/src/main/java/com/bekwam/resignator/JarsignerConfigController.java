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
import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bekwam.jfxbop.view.Viewable;
import com.bekwam.resignator.commands.CommandExecutionException;
import com.bekwam.resignator.commands.KeytoolCommand;
import com.bekwam.resignator.model.ConfigurationDataSource;
import com.google.common.base.Preconditions;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
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
public class JarsignerConfigController extends ResignatorBaseView {

    private final static Logger logger = LoggerFactory.getLogger(JarsignerConfigController.class);

	private WeakReference<ResignatorAppMainViewController> parentRef;
	private final InvalidationListener needsSaveListener = (evt) -> {
		if( parentRef != null ) {
			parentRef.get().needsSave.set(true);
		}
	};

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
	private ChoiceBox<String> cbAlias;
	
	@FXML
	private ChoiceBox<Boolean> cbVerbose;

	@FXML
	private Label lblConfKeypass;

	@FXML
	private Label lblConfStorepass;

	@FXML
	private Label lblKeystoreNotFound;

	@FXML
	private ProgressBar pbAlias;

	@FXML
	private Label lblAliasProgress;

	@FXML
	private HBox hboxAliasProgress;

	@FXML
	private VBox vbox;

	@Inject
	private ConfigurationDataSource configurationDS;

	@Inject
	private ActiveProfile activeProfile;

	@Inject
	private ActiveConfiguration activeConfiguration;

	@Inject
	private KeytoolCommand keytoolCommand;

	private String keystoreDir = System.getProperty("user.home");

	@FXML
	public void initialize() {
		
		if( logger.isDebugEnabled() ) {
			logger.debug("[INIT] instance={}, configurationDS={}", this.hashCode(), configurationDS.hashCode());
		}
		
		cbVerbose.getItems().addAll(Boolean.TRUE, Boolean.FALSE);

		pfStorepass.textProperty().bindBidirectional(activeProfile.jarsignerConfigStorepassProperty());
		tfKeystore.textProperty().bindBidirectional(activeProfile.jarsignerConfigKeystoreProperty());
		pfKeypass.textProperty().bindBidirectional(activeProfile.jarsignerConfigKeypassProperty());
		cbVerbose.valueProperty().bindBidirectional(activeProfile.jarsignerConfigVerboseProperty());

		lblConfKeypass.setVisible(false);
		lblConfStorepass.setVisible( false );
		lblKeystoreNotFound.setVisible(false);

		//
		// Enables ChoiceBox controls to use the arrow keys without losing focus
		//
		vbox.addEventFilter(KeyEvent.KEY_PRESSED, (evt) -> {
			if (evt.getCode() == KeyCode.UP || evt.getCode() == KeyCode.DOWN) {
				evt.consume();
			}
		});
		
		//
		// #7 fire action event when tfs lose focus
		//
		InvalidationListener pfConfStorepassListener = (evt) -> {
			if( !pfConfStorepass.isFocused() ) {
				verifyStorepass();
			};
		};
			
		pfConfStorepass.focusedProperty().addListener(
				new WeakInvalidationListener(pfConfStorepassListener)
				);
		
		InvalidationListener pfConfKeypassListener = (evt) -> {
			if( !pfConfKeypass.isFocused() ) {
				verifyKeypass();
			};
		};

		pfConfKeypass.focusedProperty().addListener( 
				new WeakInvalidationListener(pfConfKeypassListener)
				);

		tfKeystore.textProperty().addListener(new WeakInvalidationListener(needsSaveListener));
		pfStorepass.textProperty().addListener(new WeakInvalidationListener(needsSaveListener));
		pfKeypass.textProperty().addListener(new WeakInvalidationListener(needsSaveListener));
		cbAlias.valueProperty().addListener(new WeakInvalidationListener(needsSaveListener));
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
		loadAliases();
	}

	private void loadAliases() {

		if( StringUtils.isNotEmpty(tfKeystore.getText()) && StringUtils.isNotEmpty(pfStorepass.getText()) ) {
			if( logger.isDebugEnabled() ) {
				logger.debug("[LOAD ALIASES] there are values for the keytool command");
			}

			hboxAliasProgress.setVisible( true );
			cbAlias.valueProperty().unbindBidirectional(activeProfile.jarsignerConfigAliasProperty());
			cbAlias.getItems().clear();

			final String ks = tfKeystore.getText();
			final String sp = pfStorepass.getText();

			Task<Void> t = new Task<Void>() {
				public Void call() {

					try {

						updateMessage("Loading...");
						updateProgress( 0.1d, 1.0d );

						final List<String> aliases = keytoolCommand.findAliases(
								activeConfiguration.getKeytoolCommand().toString(),
							ks,
							sp
						);

						updateMessage("Updating...");
						updateProgress(0.8d, 1.0d);

						Platform.runLater( () -> {
							if (CollectionUtils.isNotEmpty(aliases)) {

								cbAlias.getItems().addAll(aliases);
								cbAlias.valueProperty().bindBidirectional(activeProfile.jarsignerConfigAliasProperty());

							}

							cbAlias.setDisable(false);  // might be an empty list for empty keystore
							hboxAliasProgress.setVisible( false );
						});

					} catch(CommandExecutionException exc) {
						if( logger.isWarnEnabled() ) {
							logger.warn("error getting aliases", exc);
						}
						Platform.runLater(() -> {
							cbAlias.setDisable( true );
							hboxAliasProgress.setVisible( false );
						});
					} finally {
						updateMessage("");
						updateProgress( 0.0d, 1.0d );
					}

					return null;
				}
			};

			lblAliasProgress.textProperty().bind( t.messageProperty() );
			pbAlias.progressProperty().bind( t.progressProperty() );

			new Thread(t).start();

		} else {

			cbAlias.getItems().clear();
			cbAlias.setDisable( true );
		}
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

		if( StringUtils.isBlank(pfStorepass.textProperty().getValue() ) ) {
			
    		if( !pfKeypass.getStyleClass().contains("tf-validation-error") ) {
    			pfKeypass.getStyleClass().add("tf-validation-error");
    		}    		

    		cbAlias.getItems().clear();
			cbAlias.setDisable( true );

			Tooltip tt = pfKeypass.getTooltip();
			tt.show(pfKeypass.getParent().getScene().getWindow());

			return;
		} 

		if( StringUtils.equals(pfKeypass.textProperty().getValue(), pfConfKeypass.textProperty().getValue()) ) {
			lblConfKeypass.setText("Ok");
			lblConfKeypass.setTextFill(Color.GREEN);
		} else {
			lblConfKeypass.setText("Bad");
			lblConfKeypass.setTextFill(Color.RED);
		}

		lblConfKeypass.setVisible(true);
		
		clearValidationErrors();
	}

	@FXML
	public void resetConfStorepass() {
		pfConfStorepass.setText("");

		lblConfStorepass.setText("Ok");
		lblConfStorepass.setTextFill(Color.GREEN);
		lblConfStorepass.setVisible(false);
	}

    @FXML
    public void clearValidationErrors() {
    	if( pfStorepass.getStyleClass().contains("tf-validation-error") ) {
    		pfStorepass.getStyleClass().remove("tf-validation-error");
    	}
    	if( pfKeypass.getStyleClass().contains("tf-validation-error") ) {
    		pfKeypass.getStyleClass().remove("tf-validation-error");
    	}
    }

    @FXML
	public void verifyStorepass() {

		Preconditions.checkNotNull(pfStorepass.textProperty());
		Preconditions.checkNotNull(pfConfStorepass.textProperty());

		if( StringUtils.isBlank(pfStorepass.textProperty().getValue()) ) {
			
    		if( !pfStorepass.getStyleClass().contains("tf-validation-error") ) {
    			pfStorepass.getStyleClass().add("tf-validation-error");
    		}    		

    		cbAlias.getItems().clear();
			cbAlias.setDisable( true );

			Tooltip tt = pfStorepass.getTooltip();
			tt.show(pfStorepass.getParent().getScene().getWindow());
			
			return;
		} 
			
		if( StringUtils.equals(pfStorepass.textProperty().getValue(), pfConfStorepass.textProperty().getValue()) ) {
			lblConfStorepass.setText("Ok");
			lblConfStorepass.setTextFill(Color.GREEN);

			loadAliases();

		} else {
			lblConfStorepass.setText("Bad");
			lblConfStorepass.setTextFill(Color.RED);

			cbAlias.getItems().clear();
			cbAlias.setDisable(true);
		}
		lblConfStorepass.setVisible(true);
		
		clearValidationErrors();

	}

	@FXML
	public void browse() {
		if( logger.isDebugEnabled() ){
			logger.debug("[BROWSE]");
		}

		if( lblKeystoreNotFound.isVisible() ) {
			lblKeystoreNotFound.setVisible(true);
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select keystore");
		fileChooser.setInitialDirectory(new File(keystoreDir));

		File f = fileChooser.showOpenDialog(stage);
		if( f != null ) {
			if( logger.isDebugEnabled() ) {
				logger.debug("[BROWSE] selected file={}", f.getAbsolutePath());
			}
			tfKeystore.setText(f.getAbsolutePath());

			keystoreDir = FilenameUtils.getFullPath(f.getAbsolutePath());

            validateKeystore();
		}
	}

    @FXML
    public void validateKeystore() {
		if( logger.isDebugEnabled() ) {
			logger.debug("[VALIDATE KS] validating text field");
		}
		if( new File(tfKeystore.getText()).exists() == false ) {
			lblKeystoreNotFound.setVisible( true );
		} else {
			lblKeystoreNotFound.setVisible(false);
		}
    }

	public void setParent(ResignatorAppMainViewController parent) {

		//
		// Only setting parentRef if needed
		//

		if( this.parentRef == null || this.parentRef.get() != parent ) {
			this.parentRef = new WeakReference<>(parent);
		}
	}
}
