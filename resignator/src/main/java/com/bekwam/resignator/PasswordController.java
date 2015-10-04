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
import com.bekwam.resignator.util.HashUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author carl_000
 */
@Viewable(
        fxml="/fxml/Password.fxml",
        stylesheet = "/css/resignator.css",
        title = "Password"
)
public class PasswordController extends ResignatorBaseView {

    private Logger logger = LoggerFactory.getLogger(PasswordController.class);

    private final static int MAX_NUM_RETRIES = 3;

    @FXML
    Label lblError;

    @FXML
    PasswordField pfPassword;

    @Inject
    ActiveConfiguration activeConfiguration;

    @Inject
    HashUtils hashUtils;

    private int numRetries = 1;
    BooleanProperty passwordMatches = new SimpleBooleanProperty(true);
    boolean cancelled = false;

    public String getPassword() { return pfPassword.getText(); }

    @FXML
    public void initialize() {

        lblError.visibleProperty().bind(passwordMatches.not());
    }

    @FXML
    public void ok(ActionEvent evt) {

        if (logger.isDebugEnabled()) {
            logger.debug("[OK]");
        }

        String tmpHash = hashUtils.hash( pfPassword.getText() );

        if (StringUtils.equalsIgnoreCase(tmpHash, activeConfiguration.getHashedPassword())) {

            if (logger.isDebugEnabled()) {
                logger.debug("[OK] password matches");
            }

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
                cancelled = false;
                passwordMatches.setValue(false);

                synchronized (this) {
                    this.notify();
                }

                ((Button) evt.getSource()).getScene().getWindow().hide();

            } else {  // allow for another attempt

                numRetries++;
                passwordMatches.setValue(false);
            }
        }

    }

    @FXML
    public void cancel(ActionEvent evt) {
        if (logger.isDebugEnabled()) {
            logger.debug("[CANCEL]");
        }

        cancelled = true;
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
        return cancelled;
    }

    @Override
    protected void postInit() throws Exception {
        super.postInit();
        getStage().setOnCloseRequest( (evt) -> {
            cancelled = true;
            passwordMatches.setValue(false);
            ((Window)evt.getSource()).hide();
            synchronized (PasswordController.this) {
                PasswordController.this.notify();
            }
        } );
    }
}