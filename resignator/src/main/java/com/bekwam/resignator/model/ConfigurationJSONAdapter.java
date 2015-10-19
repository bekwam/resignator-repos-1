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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        JsonElement hpElem = obj.get("hashedPassword");
        String hp = "";
        if( hpElem != null ) {
            hp = hpElem.getAsString();
        }

        JsonElement ludElem = obj.get("lastUpdatedDate");
        String lud = "";
        LocalDateTime lastUpdatedDate = null;
        if( ludElem != null ) {
            lud = ludElem.getAsString();
            if( StringUtils.isNotEmpty(lud) ) {
                lastUpdatedDate = LocalDateTime.parse(lud, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
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
        conf.getRecentProfiles().addAll(deserializeRecentProfiles(recentProfiles));
        conf.getProfiles().addAll(deserializeProfiles(profiles));
        conf.setHashedPassword(Optional.of(hp));
        conf.setLastUpdatedDateTime(Optional.ofNullable(lastUpdatedDate));
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

            Boolean rs = Boolean.FALSE;
            if (profileObj.get("replaceSignatures") != null) {
                rs = profileObj.get("replaceSignatures").getAsBoolean();
            }

            SigningArgumentsType argsType = SigningArgumentsType.JAR;
            if( profileObj.get("argsType") != null ) {
            	String at = profileObj.get("argsType").getAsString();
            	if( StringUtils.equalsIgnoreCase(at, String.valueOf(SigningArgumentsType.FOLDER)) ) {
            		argsType = SigningArgumentsType.FOLDER;
            	}
            }
            
            Profile p = new Profile(profileName, rs, argsType);

            //
            // SourceFile part
            //
            JsonObject sourceObj = profileObj.getAsJsonObject("sourceFile");
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
            JsonObject targetObj = profileObj.getAsJsonObject("targetFile");
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
            JsonObject jcObj = profileObj.getAsJsonObject("jarsignerConfig");
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

                JarsignerConfig jc = new JarsignerConfig(alias, "", "", keystore, verbose );
                jc.setEncryptedKeypass(keypass);
                jc.setEncryptedStorepass(storepass);

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
        JsonArray profiles = serializeProfiles(configuration.getProfiles());
        String hp = configuration.getHashedPassword().orElse("");
        String lud = "";
        if( configuration.getLastUpdatedDateTime().isPresent() ) {

            lud = configuration.getLastUpdatedDateTime()
                    .get()
                    .format(
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    );
        }

        JsonObject root = new JsonObject();
        root.addProperty("activeProfile", ap );
        root.addProperty("jdkHome", jdkHome);
        root.addProperty("hashedPassword", hp);
        root.addProperty("lastUpdatedDate", lud);

        if( logger.isDebugEnabled() ) {
            logger.debug("[SERIALIZE] ap={}, jdkHome={}, hp empty?={}, lastUpdatedDate={}", ap, jdkHome, StringUtils.isEmpty(hp), lud);
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
            profileObj.addProperty("replaceSignatures", p.getReplaceSignatures());
            profileObj.addProperty("argsType", String.valueOf(p.getArgsType()));
            
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

                //
                // #1 storepass and keypass become temporary fields while the encrypted fields
                // are persisted
                //

                jcObj.addProperty("storepass", jc.getEncryptedStorepass());
                jcObj.addProperty("keypass", jc.getEncryptedKeypass());

                jcObj.addProperty("keystore", jc.getKeystore());
                jcObj.addProperty("verbose", jc.getVerbose());
                profileObj.add( "jarsignerConfig", jcObj );
            }

            ps.add(profileObj);
        }

        return ps;
    }

}
