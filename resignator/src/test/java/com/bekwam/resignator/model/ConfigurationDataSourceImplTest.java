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

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for ConfigurationDataSourceImpl
 *
 * @author carl_000
 */
public class ConfigurationDataSourceImplTest {

    private ConfigurationDataSourceImpl ds;

    @Before
    public void init() {
        ds = new ConfigurationDataSourceImpl();
    }

    @Test
    public void secured() {

        Configuration configuration = new Configuration();
        configuration.setHashedPassword(Optional.of("ABCDEF1234"));

        ds.setConfiguration(Optional.of(configuration));

        assertTrue( ds.isSecured() );
    }

    @Test
    public void notSecured() {
        assertFalse(ds.isSecured());
    }
}
