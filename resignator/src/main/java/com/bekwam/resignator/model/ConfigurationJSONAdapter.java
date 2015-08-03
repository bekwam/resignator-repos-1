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
package com.bekwam.resignator.model;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converts JSON to and from the Configuration domain object
 *
 * @author carl_000
 * @since 1.0.0
 */
public class ConfigurationJSONAdapter implements JsonDeserializer<Configuration>, JsonSerializer<Configuration> {

    private final static Logger logger = LoggerFactory.getLogger(ConfigurationJSONAdapter.class);

    @Override
    public Configuration deserialize(JsonElement jsonElement, Type type,
                                     JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {

        if( logger.isDebugEnabled() ) {
            logger.debug("[DESERIALIZE]");
        }

        JsonObject obj = (JsonObject)jsonElement;

        JsonElement apElement = obj.get("activeProfile");
        String ap = "";
        if( apElement != null ) {
            ap = apElement.getAsString();
        }

        JsonElement jdkElem = obj.get("jdkHome");
        String jdkHome = "";
        if( jdkElem != null ) {
            jdkHome = jdkElem.getAsString();
        }

        JsonArray recentProfiles = obj.getAsJsonArray("recentProfiles");
        JsonArray profiles = obj.getAsJsonArray("profiles");

        if( logger.isDebugEnabled() ) {
            logger.debug("[DESERIALIZE] rp={}, ap={}, jdkHome={}, keytool={}, profiles={}",
                    recentProfiles.toString(), ap, jdkHome,  profiles.toString());
        }

        Configuration conf = new Configuration();
        conf.setActiveProfile(Optional.of(ap));
        conf.setJDKHome(Optional.of(jdkHome));
        conf.getRecentProfiles().addAll( deserializeRecentProfiles(recentProfiles) );
        conf.getProfiles().addAll( deserializeProfiles(profiles) );

        return conf;
    }

    private List<String> deserializeRecentProfiles( JsonArray recentProfiles ) {
        List<String> rps = new ArrayList<>();
        for( JsonElement e : recentProfiles ) {
            rps.add( e.getAsString() );
        }
        return rps;
    }

    private List<Profile> deserializeProfiles( JsonArray profiles ) {

        List<Profile> ps = new ArrayList<>();

        for( JsonElement e : profiles ) {

            JsonObject profileObj = (JsonObject)e;
            String profileName = profileObj.get("profileName").getAsString();

            Profile p = new Profile(profileName);

            //
            // SourceFile part
            //
            JsonObject sourceObj = (JsonObject)profileObj.getAsJsonObject("sourceFile");
            if( sourceObj != null ) {
                JsonElement sfe = sourceObj.get("fileName");
                if( sfe != null ) {
                    SourceFile sf = new SourceFile(sfe.getAsString());
                    p.setSourceFile(Optional.of(sf));
                }
            }

            //
            // TargetFile part
            //
            JsonObject targetObj = (JsonObject)profileObj.getAsJsonObject("targetFile");
            if( sourceObj != null ) {
                JsonElement tfe = targetObj.get("fileName");
                if( tfe != null ) {
                    TargetFile tf = new TargetFile(tfe.getAsString());
                    p.setTargetFile(Optional.of(tf));
                }
            }

            //
            // JarsignerConfig part
            //
            JsonObject jcObj = (JsonObject)profileObj.getAsJsonObject("jarsignerConfig");
            if( jcObj != null ) {

                String alias = "";
                String storepass = "";
                String keypass = "";
                String keystore = "";
                Boolean verbose = false;

                JsonElement ae = jcObj.get("alias");
                if( ae != null ) {
                    alias = ae.getAsString();
                }

                JsonElement spe = jcObj.get("storepass");
                if( spe != null ) {
                    storepass = spe.getAsString();
                }

                JsonElement kpe = jcObj.get("keypass");
                if( kpe != null ) {
                    keypass = kpe.getAsString();
                }

                JsonElement kse = jcObj.get("keystore");
                if( kse != null ) {
                    keystore = kse.getAsString();
                }

                JsonElement ve = jcObj.get("verbose");
                if( ve != null ) {
                    verbose = ve.getAsBoolean();
                }

                JarsignerConfig jc = new JarsignerConfig(alias, storepass, keypass, keystore, verbose );
                p.setJarsignerConfig( Optional.of(jc) );
            }

            ps.add( p );
        }

        return ps;
    }

    @Override
    public JsonElement serialize(Configuration configuration, Type type, JsonSerializationContext jsonSerializationContext) {

        if( logger.isDebugEnabled() ) {
            logger.debug("[SERIALIZE]");
        }

        String ap = configuration.getActiveProfile().orElse("");
        String jdkHome = configuration.getJDKHome().orElse("");
        JsonArray profiles = serializeProfiles( configuration.getProfiles() );

        JsonObject root = new JsonObject();
        root.addProperty("activeProfile", ap );
        root.addProperty("jdkHome", jdkHome);

        if( logger.isDebugEnabled() ) {
            logger.debug("[SERIALIZE] ap={}, jdkHome={}", ap, jdkHome);
        }

        root.add( "recentProfiles", serializeRecentProfiles(configuration.getRecentProfiles()) );
        root.add( "profiles", profiles );

        return root;
    }

    private JsonArray serializeRecentProfiles( List<String> recentProfiles ) {
        JsonArray rps = new JsonArray();
        for( String rp : recentProfiles ) {
            rps.add( new JsonPrimitive(rp) );
        }
        return rps;
    }

    private JsonArray serializeProfiles( List<Profile> profiles ) {
        JsonArray ps = new JsonArray();

        for( Profile p : profiles ) {

            JsonObject profileObj = new JsonObject();

            profileObj.addProperty("profileName", p.getProfileName());

            if( p.getSourceFile().isPresent() ) {
                SourceFile sf = p.getSourceFile().get();
                JsonObject sfObj = new JsonObject();
                sfObj.addProperty("fileName", sf.getFileName());
                profileObj.add( "sourceFile", sfObj );
            }

            if( p.getTargetFile().isPresent() ) {
                TargetFile tf = p.getTargetFile().get();
                JsonObject tfObj = new JsonObject();
                tfObj.addProperty("fileName", tf.getFileName());
                profileObj.add( "targetFile", tfObj );
            }

            if( p.getJarsignerConfig().isPresent() ) {
                JarsignerConfig jc = p.getJarsignerConfig().get();
                JsonObject jcObj = new JsonObject();
                jcObj.addProperty("alias", jc.getAlias());
                jcObj.addProperty("storepass", jc.getStorepass());
                jcObj.addProperty("keypass", jc.getKeypass());
                jcObj.addProperty("keystore", jc.getKeystore());
                jcObj.addProperty("verbose", jc.getVerbose());
                profileObj.add( "jarsignerConfig", jcObj );
            }

            ps.add(profileObj);
        }

        return ps;
    }

}
