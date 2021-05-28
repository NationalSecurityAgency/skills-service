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
package skills

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.UIConfigProperties

@Component
class PublicProps {

    enum UiProp {
        descriptionMaxLength,
        maxTimeWindowInMinutes,
        docsHost,
        maxProjectsPerAdmin,
        maxSubjectsPerProject,
        maxBadgesPerProject,
        maxSkillsPerSubject,
        numProjectsForTableView,
        paragraphValidationRegex,
        paragraphValidationMessage,
        nameValidationRegex,
        nameValidationMessage,
        maxFirstNameLength,
        maxLastNameLength,
        maxNicknameLength,
        minPasswordLength,
        maxPasswordLength,
        minNameLength,
        maxBadgeNameLength,
        maxProjectNameLength,
        maxSkillNameLength,
        maxSubjectNameLength,
        maxLevelNameLength,
        minIdLength,
        maxIdLength,
        maxSkillVersion,
        maxPointIncrement,
        maxNumPerformToCompletion,
        maxNumPointIncrementMaxOccurrences,
    }

    @Autowired
    UIConfigProperties uiConfigProperties

    def get(UiProp prop) {
        def res = uiConfigProperties.ui."${prop.name()}"
        if (res == null) {
            throw new IllegalArgumentException("Failed to find property ${prop}")
        }
        return res
    }

    int getInt(UiProp prop) {
        Object res = get(prop)
        return res instanceof String ? res.toInteger() : (Integer)res
    }
}
