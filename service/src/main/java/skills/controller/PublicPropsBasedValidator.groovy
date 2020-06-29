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
package skills.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.PublicProps
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator

@Component
class PublicPropsBasedValidator {

    @Autowired
    PublicProps publicProps

    void validateMaxStrLength(PublicProps.UiProp prop, String fieldName, String value) {
        if (value) {
            int maxLength = publicProps.getInt(prop)
            if (value.length() > maxLength) {
                throw new SkillException("[${fieldName}] must not exceed [${maxLength}] chars.")
            }
        }
    }

    void validateMinStrLength(PublicProps.UiProp prop, String fieldName, String value) {
        SkillsValidator.isNotBlank(value, fieldName)
        int minLen = publicProps.getInt(prop)
        if(value.length() < minLen){
            throw new SkillException("[${fieldName}] must not be less than [${minLen}] chars.")
        }
    }

    void validateMaxIntValue(PublicProps.UiProp prop, String fieldName, int value) {
        int maxVal = publicProps.getInt(prop)
        if (value > maxVal) {
            throw new SkillException("[${fieldName}] must be <= [${maxVal}]")
        }
    }
}
