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

import com.bekwam.resignator.model.JarsignerConfig;
import com.bekwam.resignator.model.Profile;
import com.bekwam.resignator.model.SourceFile;
import com.bekwam.resignator.model.TargetFile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Optional;

/**
 * @author carl_000
 */
public class ActiveProfile {

    private StringProperty profileName = new SimpleStringProperty("");
    private StringProperty sourceFileFileName = new SimpleStringProperty("");
    private StringProperty targetFileFileName = new SimpleStringProperty("");
    private StringProperty jarsignerConfigAlias = new SimpleStringProperty("");
    private StringProperty jarsignerConfigStorepass = new SimpleStringProperty("");
    private StringProperty jarsignerConfigKeypass = new SimpleStringProperty("");
    private StringProperty jarsignerConfigKeystore = new SimpleStringProperty("");
    private BooleanProperty jarsignerConfigVerbose = new SimpleBooleanProperty(Boolean.FALSE);

    public String getProfileName() { return profileName.get(); }
    public void setProfileName(String profileName_s) { profileName.set(profileName_s); }

    public String getSourceFileFileName() { return sourceFileFileName.get(); }
    public void setSourceFileFileName(String sourceFileFileName_s) { sourceFileFileName.set(sourceFileFileName_s); }

    public String getTargetFileFileName() { return targetFileFileName.get(); }
    public void setTargetFileFileName(String targetFileFileName_s) { targetFileFileName.set(targetFileFileName_s); }

    public String getJarsignerConfigAlias() { return jarsignerConfigAlias.get(); }
    public void setJarsignerConfigAlias(String jarsignerConfigAlias_s) { jarsignerConfigAlias.set(jarsignerConfigAlias_s); }

    public String getJarsignerConfigStorepass() { return jarsignerConfigStorepass.get(); }
    public void setJarsignerConfigStorepass(String jarsignerConfigStorepass_s) { jarsignerConfigStorepass.set(jarsignerConfigStorepass_s); }

    public String getJarsignerConfigKeypass() { return jarsignerConfigKeypass.get(); }
    public void setJarsignerConfigKeypass(String jarsignerConfigKeypass_s) { jarsignerConfigKeypass.set(jarsignerConfigKeypass_s); }

    public String getJarsignerConfigKeystore() { return jarsignerConfigKeystore.get(); }
    public void setJarsignerConfigKeystore(String jarsignerConfigKeystore_s) { jarsignerConfigKeystore.set(jarsignerConfigKeystore_s); }

    public Boolean getJarsignerConfigVerbose() { return jarsignerConfigVerbose.get(); }
    public void setJarsignerConfigVerbose(Boolean jarsignerConfigVerbose_b) { jarsignerConfigVerbose.set(jarsignerConfigVerbose_b); }

    public StringProperty profileNameProperty() { return profileName; }
    public StringProperty sourceFileFileNameProperty() { return sourceFileFileName; }
    public StringProperty targetFileFileNameProperty() { return targetFileFileName; }
    public StringProperty jarsignerConfigAliasProperty() { return jarsignerConfigAlias; }
    public StringProperty jarsignerConfigStorepassProperty() { return jarsignerConfigStorepass; }
    public StringProperty jarsignerConfigKeypassProperty() { return jarsignerConfigKeypass; }
    public StringProperty jarsignerConfigKeystoreProperty() { return jarsignerConfigKeystore; }
    public BooleanProperty jarsignerConfigVerboseProperty() { return jarsignerConfigVerbose; }

    public void reset() {
        profileName.setValue("");
        sourceFileFileName.setValue("");
        targetFileFileName.setValue("");
        jarsignerConfigAlias.setValue("");
        jarsignerConfigStorepass.setValue("");
        jarsignerConfigKeypass.setValue("");
        jarsignerConfigKeystore.setValue("");
        jarsignerConfigVerbose.setValue(Boolean.FALSE);
    }

    public Profile toProfile() {

        Profile p = new Profile( profileName.get() );

        SourceFile sf = new SourceFile( sourceFileFileName.get() );
        p.setSourceFile(Optional.of(sf));

        TargetFile tf = new TargetFile( targetFileFileName.get() );
        p.setTargetFile(Optional.of(tf));

        JarsignerConfig jc = new JarsignerConfig(
                jarsignerConfigAlias.getValue(),
                jarsignerConfigStorepass.getValue(),
                jarsignerConfigKeypass.getValue(),
                jarsignerConfigKeystore.getValue(),
                jarsignerConfigVerbose.getValue() );
        p.setJarsignerConfig(Optional.of(jc));

        return p;
    }

    public void loadFromProfile(Profile p) {

        profileName.set( p.getProfileName() );

        sourceFileFileName.set(p.getSourceFile().orElse(new SourceFile("")).getFileName());
        targetFileFileName.set(p.getTargetFile().orElse(new TargetFile("")).getFileName());

        if( p.getJarsignerConfig().isPresent() ) {
            JarsignerConfig jc = p.getJarsignerConfig().get();
            jarsignerConfigAlias.setValue(jc.getAlias());
            jarsignerConfigStorepass.setValue(jc.getStorepass());
            jarsignerConfigKeypass.setValue( jc.getKeypass() );
            jarsignerConfigKeystore.setValue( jc.getKeystore() );
            jarsignerConfigVerbose.setValue( jc.getVerbose() );
        } else {
            jarsignerConfigAlias.setValue("");
            jarsignerConfigStorepass.setValue("");
            jarsignerConfigKeypass.setValue("");
            jarsignerConfigKeystore.setValue("");
            jarsignerConfigVerbose.setValue(Boolean.FALSE);
        }
    }
}
