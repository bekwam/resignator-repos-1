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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import com.bekwam.resignator.model.Configuration;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

/**
 * @author carlwalker
 *
 */
@Singleton
public class ActiveConfiguration implements ActiveRecord<Configuration> {

	private final StringProperty jdkHome = new SimpleStringProperty("");
	private final StringProperty activeProfile = new SimpleStringProperty("");
	private final ListProperty<String> recentProfiles = new SimpleListProperty<String>();

	// computed properties
	private Optional<Path> keytoolCommand = Optional.empty();
	private Optional<Path> jarsignerCommand = Optional.empty();
	private Optional<Path> jarCommand = Optional.empty();
	// end computed properties

	public ActiveConfiguration() {

		//
		// register a change listener for computed properties
		//

		jdkHome.addListener(observable -> formJDKCommands());	
		
		setRecentProfiles(new ArrayList<String>());
	}

	public String getJDKHome() { return jdkHome.get(); }
	public void setJDKHome(String jh) {
		jdkHome.set(jh);
	}

	public String getActiveProfile() { return activeProfile.get(); }
	public void setActiveProfile(String ap) {
		activeProfile.set(ap);
	}

	public List<String> getRecentProfiles() { return recentProfiles.get(); }
	public void setRecentProfiles(List<String> rps) {
		recentProfiles.setValue( FXCollections.observableArrayList(rps) );
	}

	public Path getKeytoolCommand() {
		return keytoolCommand.orElse(null);
	}

	public Path getJarsignerCommand() {
		return jarsignerCommand.orElse(null);
	}

	public Path getJarCommand() {
		return jarCommand.orElse(null);
	}

	public StringProperty jdkHomeProperty() { return jdkHome; }
	public StringProperty activeProfileProperty() { return activeProfile; }
	public ListProperty<String> recentProfilesProperty() { return recentProfiles; }
	
	@Override
	public String toString() {
		return "ActiveConfiguration [jdkHome=" + jdkHome + ", activeProfile=" + activeProfile
				+ ", recentProfiles=" + recentProfiles + "]";
	}	
	
	@Override
	public void reset() {
		jdkHome.set("");
		activeProfile.set("");
		recentProfiles.clear();
	}

	@Override
	public Configuration toDomain() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fromDomain(Configuration domain) {
		jdkHome.setValue( domain.getJDKHome().orElse(""));
		activeProfile.setValue( domain.getActiveProfile().orElse("") );
		recentProfiles.setValue( FXCollections.observableList(domain.getRecentProfiles()) );
	}

	private void formJDKCommands() {
		if( jdkHome.isEmpty().getValue() ) {
			keytoolCommand = Optional.empty();
			jarsignerCommand = Optional.empty();
			jarCommand = Optional.empty();
		} else {
			keytoolCommand = Optional.of( Paths.get( getJDKHome(), "bin", "keytool") );
			jarsignerCommand = Optional.of( Paths.get( getJDKHome(), "bin", "jarsigner") );
			jarCommand = Optional.of( Paths.get( getJDKHome(), "bin", "jar") );
		}
	}
}
