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
package skills.utils

import java.util.regex.Pattern

class PatternsUtil {
    /**
     * email validation regex as defined by RFC 5322
     */
    public static final Pattern VALID_EMAIL = ~/^[a-zA-Z0-9_!#$%&'*+\/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$/

    public static boolean isValidEmail(String maybeEmail) {
        if (!maybeEmail) {
            return false
        }
        return VALID_EMAIL.matcher(maybeEmail).matches()
    }

}
