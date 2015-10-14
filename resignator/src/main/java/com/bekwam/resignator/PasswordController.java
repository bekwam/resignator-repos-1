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

import com.bekwam.jfxbop.view.Viewable;
import com.bekwam.resignator.model.ConfigurationDataSource;
import com.bekwam.resignator.util.HashUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

/**
 * @author carl_000
 */
@Viewable(
        fxml="/fxml/Password.fxml",
        stylesheet = "/css/resignator.css",
        title = "Password"
)
public class PasswordController extends ResignatorBaseView {

    private final static int MAX_NUM_RETRIES = 3;
    @FXML
    VBox vboxContents;
    @FXML
    VBox vboxErr;
    @FXML
    PasswordField pfPassword;
    @Inject
    ActiveConfiguration activeConfiguration;
    @Inject
    HashUtils hashUtils;
    @Inject
    ConfigurationDataSource configurationDataSource;
    private Logger logger = LoggerFactory.getLogger(PasswordController.class);
    private int numRetries = 1;
    private BooleanProperty passwordMatches = new SimpleBooleanProperty(true);
    private ExitCodeType exitCode = ExitCodeType.OK;

    public String getPassword() { return pfPassword.getText(); }

    @FXML
    public void initialize() {
        if (vboxContents.getChildren().contains(vboxErr)) {
            vboxContents.getChildren().remove(vboxErr);
        }
    }

    @FXML
    public void ok(ActionEvent evt) {

        if (logger.isDebugEnabled()) {
            logger.debug("[OK]");
        }

        String tmpHash = hashUtils.hash(pfPassword.getText());

        if (StringUtils.equalsIgnoreCase(tmpHash, activeConfiguration.getHashedPassword())) {

            if (logger.isDebugEnabled()) {
                logger.debug("[OK] password matches");
            }

            exitCode = ExitCodeType.OK;
            passwordMatches.setValue(true);

            synchronized (this) {
                this.notify();
            }

            ((Button) evt.getSource()).getScene().getWindow().hide();

        } else {

            if (logger.isDebugEnabled()) {
                logger.debug("[OK] password does not match; numretries={}", numRetries);
            }

            if (numRetries >= MAX_NUM_RETRIES) {

                numRetries = 1;  // reset the counter
                exitCode = ExitCodeType.MAX_RETRIES;
                passwordMatches.setValue(false);

                synchronized (this) {
                    this.notify();
                }

                ((Button) evt.getSource()).getScene().getWindow().hide();

            } else {  // allow for another attempt

                numRetries++;
                passwordMatches.setValue(false);

                if (!vboxContents.getChildren().contains(vboxErr)) {
                    vboxContents.getChildren().add(vboxErr);
                }

                ((Button) evt.getSource()).getScene().getWindow().sizeToScene();
            }
        }

    }

    @FXML
    public void cancel(ActionEvent evt) {
        if (logger.isDebugEnabled()) {
            logger.debug("[CANCEL]");
        }

        exitCode = ExitCodeType.CANCELLED;
        passwordMatches.setValue(false);

        ((Button) evt.getSource()).getScene().getWindow().hide();

        synchronized (this) {
            this.notify();
        }
    }

    public boolean doesPasswordMatch() {
        return passwordMatches.get();
    }

    public boolean wasCancelled() {
        return exitCode.equals(ExitCodeType.CANCELLED);
    }

    public boolean wasReset() {
        return exitCode.equals(ExitCodeType.RESET);
    }

    @Override
    protected void postInit() throws Exception {
        super.postInit();
        getStage().setOnCloseRequest( (evt) -> {
            exitCode = ExitCodeType.CANCELLED;
            passwordMatches.setValue(false);
            ((Window)evt.getSource()).hide();
            synchronized (PasswordController.this) {
                PasswordController.this.notify();
            }
        } );
    }

    @FXML
    public void resetDataFile(ActionEvent evt) {

        if (logger.isDebugEnabled()) {
            logger.debug("[RESET DATA FILE]");
        }

        ((Hyperlink) evt.getSource()).getScene().getWindow().hide();

        ButtonType myCancel = new ButtonType("Just Exit", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete all data and exit the app?",
                ButtonType.OK,
                myCancel);

        alert.setHeaderText("Delete all data");
        //alert.setOnCloseRequest((w_evt) -> Platform.exit());

        Optional<ButtonType> response = alert.showAndWait();

        if (!response.isPresent() || response.get() != ButtonType.OK) {
            if (logger.isDebugEnabled()) {
                logger.debug("[RESET DATA FILE] reset cancelled");
            }

            exitCode = ExitCodeType.CANCELLED;
            passwordMatches.setValue(false);

            synchronized (this) {
                this.notify();
            }

        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("[RESET DATA FILE] reset");
            }

            configurationDataSource.deleteDataFile();

            exitCode = ExitCodeType.RESET;
            passwordMatches.setValue(false);

            synchronized (this) {
                this.notify();
            }
        }
    }

    enum ExitCodeType {OK, CANCELLED, MAX_RETRIES, RESET}
}