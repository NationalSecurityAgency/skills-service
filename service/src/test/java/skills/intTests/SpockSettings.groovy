/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.intTests

class SpockSettings {

    static final boolean DB_IS_POSTGRESQL = checkPostgresProp();
    static final boolean DB_NOT_POSTGRESQL = !DB_IS_POSTGRESQL;

    static boolean checkPostgresProp() {
        String prop = System.getProperty("spring.datasource.url")
        if (prop) {
            return prop.contains("postgresql")
        }

        return false;
    }
}
