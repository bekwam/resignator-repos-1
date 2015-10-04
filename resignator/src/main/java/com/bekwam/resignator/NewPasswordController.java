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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Controller for retrieving a new confirmation password from the user
 *
 * @author carl_000
 */
@Viewable(
        fxml="/fxml/NewPassword.fxml",
        stylesheet = "/css/resignator.css",
        title = "New Password"
)
public class NewPasswordController extends ResignatorBaseView {

    private final static Logger logger = LoggerFactory.getLogger(NewPasswordController.class);

    private final static int MIN_PASSWORD_LENGTH = 8;

    @FXML
    PasswordField pfPassword;

    @FXML
    PasswordField pfConfirmPassword;

    @FXML
    Label lblError;

    @Inject
    HashUtils hashUtils;

    private Optional<String> confirmedPassword = Optional.empty();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
    }

    public String getHashedPassword() {
        if( confirmedPassword.isPresent() ) {
            return hashUtils.hash(confirmedPassword.get());
        }
        return "";
    }

    public String getUnhashedPassword() {
        if( confirmedPassword.isPresent() ) {
            return confirmedPassword.get();
        }
        return "";
    }

    public void reset() {
        pfPassword.setText("");
        pfConfirmPassword.setText("");
    }

    @FXML
    public void ok(ActionEvent evt) {
        if( logger.isDebugEnabled() ) {
            logger.debug("[OK]");
        }

        String errMsg = validate();

        if( StringUtils.isEmpty(errMsg) ) {
            confirmedPassword = Optional.of( pfPassword.getText() );

            if( logger.isDebugEnabled() ) {
                logger.debug("[OK] password is present?=" + (confirmedPassword.isPresent() &&
                        !StringUtils.isEmpty(confirmedPassword.get())));
            }

            if( logger.isDebugEnabled() ) {
                logger.debug("[OK] npc id={}", this.hashCode());
            }

            synchronized(this) {
                notify();
            }

            ((Button)evt.getSource()).getScene().getWindow().hide();

        } else {
            lblError.setVisible( true );
            lblError.setText(errMsg);
        }
    }

    @FXML
    public void cancel(ActionEvent evt) {

        if( logger.isDebugEnabled() ) {
            logger.debug("[CANCEL]");
        }

        reset();

        synchronized(this) {
            notify();
        }

        ((Button)evt.getSource()).getScene().getWindow().hide();
    }

    private String validate() {

        if( StringUtils.isBlank(pfPassword.getText()) ) {
            return "Password cannot be blank.";
        }

        if( StringUtils.length(pfPassword.getText()) < MIN_PASSWORD_LENGTH ) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }

        if( StringUtils.isBlank(pfConfirmPassword.getText()) ) {
            return "Confirmation cannot be blank.";
        }

        if( !StringUtils.equals(pfPassword.getText(), pfConfirmPassword.getText()) ) {
            return "Passwords do not match";
        }

        return "";
    }


}
