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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bekwam.jfxbop.guice.GuiceBaseView;
import com.google.common.base.Preconditions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author carl_000
 */
public class ResignatorBaseView extends GuiceBaseView {

    private Logger logger = LoggerFactory.getLogger(ResignatorBaseView.class);

    @Inject
    HelpDelegate helpDelegate;
    
    @Override
    protected void postInit() throws Exception {
        super.postInit();
        stage.initModality(Modality.APPLICATION_MODAL);
        
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, (evt) -> {
        	if( evt.getCode() == KeyCode.F1 ) {
        		helpDelegate.showHelp();
        	}
        });
    }

    @Override
    protected void init() throws Exception {

        if( logger.isDebugEnabled() ) {
            logger.debug("[SHOW] creating stage");
        }

        readAnnotation();

        Preconditions.checkNotNull(fxml);
        Preconditions.checkNotNull( builderFactory );
        Preconditions.checkNotNull( guiceControllerFactory );

        Parent p = FXMLLoader.load(getClass().getResource(fxml), null, builderFactory, param -> this);

        stage = new Stage();

        scene = new Scene(p);

        if( stylesheet != null ) {
            scene.getStylesheets().add(stylesheet);
        }

        if( title != null ) {
            stage.setTitle(title);
        }

        stage.setScene(scene);

        postInit();
    }

    public void showAndWait() throws Exception {

        if (stage == null) {
            init();
        }

        if (!stage.isShowing()) {
            if (logger.isDebugEnabled()) {
                logger.debug("[SHOW] stage is not showing");
            }
            stage.showAndWait();
        }
    }

    public Stage getStage() { 
    	Preconditions.checkNotNull(stage);
    	return stage; 
    }
}
