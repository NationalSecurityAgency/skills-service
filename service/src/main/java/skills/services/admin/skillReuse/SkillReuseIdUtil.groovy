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
package skills.services.admin.skillReuse

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

class SkillReuseIdUtil {
    static String REUSE_TAG = "STREUSESKILLST"
    private static String EXPECTED_FORMAT = "<skill id>${REUSE_TAG}<number>"

    static Integer extractReuseCounter(String val) {
        if (StringUtils.isBlank(val)) {
            throw new IllegalArgumentException("Provided bad value [${val}]. Expected format is [${EXPECTED_FORMAT}]")
        }
        String[] split = val.split(REUSE_TAG)
        if (split.length != 2) {
            throw new IllegalArgumentException("Provided bad value [${val}]. Expected format is [${EXPECTED_FORMAT}]")
        }
        String numStr = split[1]
        if (!NumberUtils.isCreatable(numStr)) {
            throw new IllegalArgumentException("Provided bad value [${val}]. Expected format is [${EXPECTED_FORMAT}]")
        }
        return NumberUtils.createInteger(numStr)
    }

    static String addTag(String val, Integer reuseCounter) {
        assert reuseCounter != null
        return "${val}${REUSE_TAG}${reuseCounter}"
    }

    static Pattern REUSED_ID_PATTERN = ~/.*STREUSESKILLST[\d]++/

    static boolean isTagged(String val) {
        if (StringUtils.isBlank(val)) {
            return false
        }
        Matcher match = REUSED_ID_PATTERN.matcher(val)
        return match.matches()
        return val?.endsWith(REUSE_TAG)
    }

    static String removeTag(String val) {
        if (isTagged(val)) {
            return val.substring(0, val.indexOf(REUSE_TAG))
        }
        return val
    }
}
