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
package skills.controller.exceptions

import org.apache.commons.lang3.StringUtils

class SkillsValidator {

    static void isNotBlank(String value, String attrName, String projectId = null, String skillId = null) {
        if (StringUtils.isBlank(value) || value?.trim().equalsIgnoreCase("null")) {
            throw new SkillException("${attrName} was not provided.".toString(), projectId, skillId, ErrorCode.BadParam)
        }
    }

    static void isNotNull(Object value, String attrName, String projectId = null, String skillId = null) {
        if (value == null) {
            throw new SkillException("${attrName} was not provided.".toString(), projectId, skillId, ErrorCode.BadParam)
        }
    }

    static void isFirstOrMustEqualToSecond(String first, String second, String attrName) {
        if (first && second != first) {
            throw new SkillException("${attrName} in the request doesn't equal to ${attrName} in the URL. [${first}]<>[${second}]", null, null, ErrorCode.BadParam)
        }
    }

    static void isTrue(boolean condition, String msg, String projectId = null, String skillId = null) {
        if (!condition) {
            throw new SkillException(msg, projectId, skillId, ErrorCode.BadParam)
        }
    }
}
