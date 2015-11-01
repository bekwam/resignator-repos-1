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

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

/**
 * App that re-signs JAR files by stripping the prior signature and applying a new signature
 *
 * @author carl_000
 * @since 1.0.0
 */
public class ResignatorApp extends Application {

    private final static Logger logger = LoggerFactory.getLogger(ResignatorApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        if( logger.isDebugEnabled() ) {
            logger.debug("[START] starting app");
        }

        Platform.setImplicitExit(false);
        
        primaryStage.setTitle("Resignator");
        
        primaryStage.getIcons().addAll( 
        		new Image("/images/Logo16.png", true), 
        		new Image("/images/Logo32.png", true) 
        		);

        Security.addProvider(new BouncyCastleProvider());

        //
        // Initiaize Google Guice
        //
        Injector injector = Guice.createInjector(new ResignatorModule());

        ResignatorAppMainViewController mv = injector.getInstance(ResignatorAppMainViewController.class);

        try {
            mv.show();  // ignoring the primaryStage
        } catch(Exception exc) {
            String msg = "Error launching ResignatorApp";
            logger.error( msg, exc );
            Alert alert = new Alert(Alert.AlertType.ERROR, msg);
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
