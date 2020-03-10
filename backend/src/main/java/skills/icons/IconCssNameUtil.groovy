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
package skills.icons

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class IconCssNameUtil {
    static final Pattern CLEAN = Pattern.compile("(?i)[^a-z0-9]")

    public static String getCssClass(String projectId, String filename) {
        return "${projectId ?: 'GLOBAL'}-"+CLEAN.matcher(filename).replaceAll("")
    }
}
