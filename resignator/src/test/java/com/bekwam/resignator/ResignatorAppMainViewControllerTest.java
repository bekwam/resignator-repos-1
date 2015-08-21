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

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for ResignatorAppMainViewController
 *
 * @author carl_000
 */
public class ResignatorAppMainViewControllerTest {

    private final String noneMenuItemText = "< None >";
    private ResignatorAppMainViewController resignatorAppMainViewController;

    @Before
    public void init() {

        resignatorAppMainViewController = new ResignatorAppMainViewController();

        Menu mRecentProfiles = new Menu();

        List<MenuItem> rpItems = new ArrayList<>();

        rpItems.add(new MenuItem(noneMenuItemText));

        mRecentProfiles.getItems().clear();
        mRecentProfiles.getItems().addAll(rpItems);

        ActiveConfiguration activeConfiguration = new ActiveConfiguration();

        resignatorAppMainViewController.mRecentProfiles = mRecentProfiles;
        resignatorAppMainViewController.activeConfiguration = activeConfiguration;
    }

    @Test
    public void addOne() {

        resignatorAppMainViewController.recordRecentProfile("profile1");

        assertEquals(1, CollectionUtils.size(resignatorAppMainViewController.mRecentProfiles.getItems()));
        assertEquals("profile1", resignatorAppMainViewController.mRecentProfiles.getItems().get(0).getText());
        assertEquals(1, CollectionUtils.size(resignatorAppMainViewController.activeConfiguration.getRecentProfiles()));
        assertEquals("profile1", resignatorAppMainViewController.activeConfiguration.getRecentProfiles().get(0));
    }

    @Test
    public void addTwo() {

        resignatorAppMainViewController.recordRecentProfile("profile1");
        resignatorAppMainViewController.recordRecentProfile("profile2");

        assertEquals(2, CollectionUtils.size(resignatorAppMainViewController.mRecentProfiles.getItems()));
        assertEquals("profile2", resignatorAppMainViewController.mRecentProfiles.getItems().get(0).getText());
        assertEquals("profile1", resignatorAppMainViewController.mRecentProfiles.getItems().get(1).getText());

        assertEquals(2, CollectionUtils.size(resignatorAppMainViewController.activeConfiguration.getRecentProfiles()));
        assertEquals("profile2", resignatorAppMainViewController.activeConfiguration.getRecentProfiles().get(0));
        assertEquals("profile1", resignatorAppMainViewController.activeConfiguration.getRecentProfiles().get(1));
    }

    @Test
    public void addMax() {

        resignatorAppMainViewController.recordRecentProfile("profile1");  // will be removed
        resignatorAppMainViewController.recordRecentProfile("profile2");
        resignatorAppMainViewController.recordRecentProfile("profile3");
        resignatorAppMainViewController.recordRecentProfile("profile4");
        resignatorAppMainViewController.recordRecentProfile("profile5");

        assertEquals(4, CollectionUtils.size(resignatorAppMainViewController.mRecentProfiles.getItems()));
        assertEquals("profile5", resignatorAppMainViewController.mRecentProfiles.getItems().get(0).getText());
        assertEquals("profile4", resignatorAppMainViewController.mRecentProfiles.getItems().get(1).getText());
        assertEquals("profile3", resignatorAppMainViewController.mRecentProfiles.getItems().get(2).getText());
        assertEquals("profile2", resignatorAppMainViewController.mRecentProfiles.getItems().get(3).getText());

        assertEquals(4, CollectionUtils.size(resignatorAppMainViewController.activeConfiguration.getRecentProfiles()));
        assertEquals("profile5", resignatorAppMainViewController.activeConfiguration.getRecentProfiles().get(0));
        assertEquals("profile4", resignatorAppMainViewController.activeConfiguration.getRecentProfiles().get(1));
        assertEquals("profile3", resignatorAppMainViewController.activeConfiguration.getRecentProfiles().get(2));
        assertEquals("profile2", resignatorAppMainViewController.activeConfiguration.getRecentProfiles().get(3));
    }
}
