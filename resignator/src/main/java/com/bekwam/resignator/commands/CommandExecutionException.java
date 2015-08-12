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
package com.bekwam.resignator.commands;

/**
 * A checked exception thrown when there is a problem executing the underlying command
 *
 * @author carl_000
 */
public class CommandExecutionException extends Exception {

	private static final long serialVersionUID = 8454824353765192272L;

	public CommandExecutionException(String message) {
        super(message);
    }
}
