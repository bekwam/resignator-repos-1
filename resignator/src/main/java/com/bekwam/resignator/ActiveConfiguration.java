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

import com.bekwam.resignator.model.Configuration;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author carlwalker
 *
 */
@Singleton
public class ActiveConfiguration implements ActiveRecord<Configuration> {

	private final StringProperty jdkHome = new SimpleStringProperty("");
	private final StringProperty activeProfile = new SimpleStringProperty("");
	private final ListProperty<String> recentProfiles = new SimpleListProperty<String>();
	private final StringProperty hashedPassword = new SimpleStringProperty("");
	private final ObjectProperty<LocalDateTime> lastUpdatedDateTime = new SimpleObjectProperty<>();
	private final StringProperty unhashedPassword = new SimpleStringProperty("");  // not saved

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
		recentProfiles.setValue(FXCollections.observableArrayList(rps));
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

	public String getHashedPassword() { return hashedPassword.get(); }
	public void setHashedPassword(String hp) {
		hashedPassword.set(hp);
	}

	public String getUnhashedPassword() { return unhashedPassword.get(); }
	public void setUnhashedPassword(String uhp) {
		unhashedPassword.set(uhp);
	}

	public LocalDateTime getLastUpdatedDateTime() { return lastUpdatedDateTime.get(); }
	public void setLastUpdatedDateTime(LocalDateTime lud) {
		lastUpdatedDateTime.set(lud);
	}

	public StringProperty jdkHomeProperty() { return jdkHome; }
	public StringProperty activeProfileProperty() { return activeProfile; }
	public ListProperty<String> recentProfilesProperty() { return recentProfiles; }
	public StringProperty hashedPasswordProperty() { return hashedPassword; }
	public StringProperty unhashedPasswordProperty() { return unhashedPassword; }

	public ObjectProperty<LocalDateTime> lastUpdatedDateTimeProperty() { return lastUpdatedDateTime; }

	@Override
	public String toString() {
		return "ActiveConfiguration [jdkHome=" + jdkHome + ", activeProfile=" + activeProfile
				+ ", recentProfiles=" + recentProfiles
				+ ", hashedPassword is empty?=" + StringUtils.isEmpty(hashedPassword.get())
				+ ", unhashedPassword is empty?=" + StringUtils.isEmpty(unhashedPassword.get())
				+ ", lastUpdatedDateTime=" + lastUpdatedDateTime.get()
				+ "]";
	}	
	
	@Override
	public void reset() {
		jdkHome.set("");
		activeProfile.set("");
		recentProfiles.clear();
		hashedPassword.set("");
		unhashedPassword.set("");
		lastUpdatedDateTime.set(null);
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
		hashedPassword.setValue( domain.getHashedPassword().orElse("") );
		//unhashedPassword.set("");
		if( domain.getLastUpdatedDateTime().isPresent() ) {
			lastUpdatedDateTime.setValue(domain.getLastUpdatedDateTime().get());
		}
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
