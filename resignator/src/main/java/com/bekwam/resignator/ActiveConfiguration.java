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
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import javax.inject.Singleton;
import java.util.List;

/**
 * @author carlwalker
 *
 */
@Singleton
public class ActiveConfiguration implements ActiveRecord<Configuration> {

	private final StringProperty jdkHome = new SimpleStringProperty("");
	private final StringProperty activeProfile = new SimpleStringProperty("");
	private final ListProperty<String> recentProfiles = new SimpleListProperty<String>();
	
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
		recentProfiles.clear();
		recentProfiles.addAll( rps );
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
}
