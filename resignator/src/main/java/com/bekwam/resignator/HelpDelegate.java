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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;

/**
 * Help command accessible throughout app
 * 
 * @author carlwalker
 * @since 1.0.0
 */
public class HelpDelegate {

    private final static Logger logger = LoggerFactory.getLogger(HelpDelegate.class);

    @Inject @Named("HelpLink")
    String helpLink;
    
    public void showHelp() {
    	
    	try {
    		if( logger.isDebugEnabled() ) {
    			logger.debug("[SHOW HELP] helpLink={}", helpLink);
    		}
    		
			java.awt.Desktop.getDesktop().browse(new URI(helpLink));
		} catch (IOException | URISyntaxException exc) {
			logger.error( "error opening helpLink=" + helpLink, exc);
			
			Alert alert = new Alert(
					Alert.AlertType.ERROR,
					"Cannot open help web page " + helpLink + "; check network connectivity");
			alert.showAndWait();
		}
    }
}
