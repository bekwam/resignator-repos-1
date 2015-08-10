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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests ConfigurationJSONAdapter
 *
 * @author carl_000
 */
public class ConfigurationJSONAdapterTest {

    private ConfigurationJSONAdapter jsonAdapter;

    @Before
    public void init() {
        jsonAdapter = new ConfigurationJSONAdapter();
    }

    @Test
    public void serializeEmptyConfig() {

        Configuration conf = new Configuration();

        JsonObject root = (JsonObject)jsonAdapter.serialize(conf, Configuration.class, null);

        assertNotNull( root );

        JsonArray recentProfiles = root.getAsJsonArray("recentProfiles");
        JsonArray profiles = root.getAsJsonArray("profiles");
        String activeProfile = root.get("activeProfile").getAsString();

        assertEquals( "", activeProfile );
        assertEquals( 0, recentProfiles.size() );
        assertEquals( 0, profiles.size() );
    }

    @Test
    public void serializeRecentProfiles() {

        Configuration conf = new Configuration();
        conf.getRecentProfiles().add("a");
        conf.getRecentProfiles().add("b");
        conf.getRecentProfiles().add("c");

        JsonObject root = (JsonObject)jsonAdapter.serialize(conf, Configuration.class, null);

        assertNotNull(root);

        JsonArray recentProfiles = root.getAsJsonArray("recentProfiles");
        JsonArray profiles = root.getAsJsonArray("profiles");
        String activeProfile = root.get("activeProfile").getAsString();

        assertEquals("", activeProfile);
        assertEquals( 0, profiles.size() );
        assertEquals( 3, recentProfiles.size() );
    }

    @Test
    public void serializeProfiles() {

        Configuration conf = new Configuration();
        conf.setActiveProfile(Optional.of("a"));
        conf.getRecentProfiles().add("a");

        Profile p = new Profile("a", Boolean.FALSE);

        SourceFile sf = new SourceFile("C:\\jars\\mycode.jar");
        p.setSourceFile(Optional.of(sf));

        TargetFile tf = new TargetFile("C:\\jars\\mycode-signed.jar");
        p.setTargetFile(Optional.of(tf));

        JarsignerConfig jc = new JarsignerConfig("mykey", "storepass", "keypass", "keystore", Boolean.TRUE);
        p.setJarsignerConfig(Optional.of(jc));

        conf.getProfiles().add(p);

        JsonObject root = (JsonObject)jsonAdapter.serialize(conf, Configuration.class, null);

        assertNotNull(root);

        JsonArray recentProfiles = root.getAsJsonArray("recentProfiles");
        JsonArray profiles = root.getAsJsonArray("profiles");
        String activeProfile = root.get("activeProfile").getAsString();

        JsonObject profile = (JsonObject)profiles.get(0);
        String pn = profile.get("profileName").getAsString();

        JsonObject sfObj = profile.getAsJsonObject("sourceFile");
        String sourceFileFileName = sfObj.get("fileName").getAsString();

        JsonObject tfObj = profile.getAsJsonObject("targetFile");
        String targetFileFileName = tfObj.get("fileName").getAsString();

        JsonObject jcObj = profile.getAsJsonObject("jarsignerConfig");
        String alias = jcObj.get("alias").getAsString();
        String storepass = jcObj.get("storepass").getAsString();
        String keypass = jcObj.get("keypass").getAsString();
        String keystore = jcObj.get("keystore").getAsString();
        Boolean verbose = jcObj.get("verbose").getAsBoolean();

        assertEquals("a", activeProfile);
        assertEquals( 1, recentProfiles.size() );
        assertEquals( 1, profiles.size() );
        assertEquals( "a", pn );
        assertEquals("C:\\jars\\mycode.jar", sourceFileFileName );
        assertEquals("C:\\jars\\mycode-signed.jar", targetFileFileName );
        assertEquals("mykey", alias);
        assertEquals( "storepass", storepass );
        assertEquals( "keypass", keypass );
        assertEquals( "keystore", keystore );
        assertEquals( Boolean.TRUE, verbose );
    }

    @Test
    public void deserializeEmptyConfig() {

        JsonObject root = new JsonObject();

        root.addProperty("activeProfile", "");
        root.add("recentProfiles", new JsonArray());
        root.add("profiles", new JsonArray());

        Configuration conf = jsonAdapter.deserialize(root, Configuration.class, null);

        assertNotNull(conf);
        assertEquals( 0, conf.getProfiles().size() );
        assertEquals( 0, conf.getRecentProfiles().size() );
    }

    @Test
    public void deserializeRecentProfiles() {

        JsonObject root = new JsonObject();

        JsonArray rps = new JsonArray();
        rps.add(new JsonPrimitive( "a"));
        rps.add(new JsonPrimitive("b"));
        rps.add(new JsonPrimitive("c"));

        root.addProperty("activeProfile", "");
        root.add("recentProfiles", rps);
        root.add("profiles", new JsonArray());

        Configuration conf = jsonAdapter.deserialize( root , Configuration.class, null );

        assertNotNull(conf);
        assertEquals(0, conf.getProfiles().size());
        assertEquals(3, conf.getRecentProfiles().size());
        assertEquals("a", conf.getRecentProfiles().get(0));
        assertEquals( "b", conf.getRecentProfiles().get(1) );
        assertEquals( "c", conf.getRecentProfiles().get(2) );
    }

    @Test
    public void deserializeProfiles() {

        JsonObject root = new JsonObject();

        JsonArray rps = new JsonArray();
        rps.add(new JsonPrimitive( "a"));

        JsonArray ps = new JsonArray();

        JsonObject p = new JsonObject();
        p.add( "profileName", new JsonPrimitive("a") );

        JsonObject sf = new JsonObject();
        sf.add( "fileName", new JsonPrimitive("C:\\jars\\mycode.jar") );
        p.add("sourceFile", sf);

        JsonObject tf = new JsonObject();
        tf.add("fileName", new JsonPrimitive("C:\\jars\\mycode-signed.jar"));
        p.add("targetFile", tf);

        JsonObject jc = new JsonObject();
        jc.addProperty( "alias", "mykey" );
        jc.addProperty( "storepass", "abc123" );
        jc.addProperty( "keypass", "password" );
        jc.addProperty( "keystore", "C:\\keystores\\keystore.jks" );
        jc.addProperty("verbose", Boolean.TRUE);
        p.add("jarsignerConfig", jc );

        ps.add(p);

        root.addProperty("activeProfile", "a");
        root.add("recentProfiles", rps);
        root.add("profiles", ps);

        Configuration conf = jsonAdapter.deserialize( root , Configuration.class, null );

        assertNotNull(conf);
        assertEquals("a", conf.getActiveProfile().get());
        assertEquals(1, conf.getRecentProfiles().size());
        assertEquals(1, conf.getProfiles().size());
        assertEquals( "a", conf.getProfiles().get(0).getProfileName() );
        assertEquals( "C:\\jars\\mycode.jar", conf.getProfiles().get(0).getSourceFile().get().getFileName());
        assertEquals( "C:\\jars\\mycode-signed.jar", conf.getProfiles().get(0).getTargetFile().get().getFileName());

        assertEquals( "mykey", conf.getProfiles().get(0).getJarsignerConfig().get().getAlias() );
        assertEquals( "abc123", conf.getProfiles().get(0).getJarsignerConfig().get().getStorepass());
        assertEquals( "password", conf.getProfiles().get(0).getJarsignerConfig().get().getKeypass());
        assertEquals( "C:\\keystores\\keystore.jks", conf.getProfiles().get(0).getJarsignerConfig().get().getKeystore());
        assertEquals( true, conf.getProfiles().get(0).getJarsignerConfig().get().getVerbose());
    }

}
