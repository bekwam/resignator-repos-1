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

import javax.inject.Singleton;

import com.bekwam.jfxbop.view.Viewable;

/**
 * JavaFX Controller and view component for the About screen
 * 
 * @author carl_000
 */
@Viewable(
        fxml="/fxml/About.fxml",
        stylesheet = "/css/resignator.css",
        title = "About"
)
@Singleton
public class AboutController extends ResignatorBaseView {

}
