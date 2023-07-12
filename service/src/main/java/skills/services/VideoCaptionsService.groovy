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
package skills.services

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.services.attributes.SkillAttributeService
import skills.services.attributes.SkillVideoAttrs

@Service
@Slf4j
class VideoCaptionsService {

    @Autowired
    SkillAttributeService skillAttributeService

    String getVideoCaptions(String projectId, String skillId) {
        SkillVideoAttrs skillVideoAttrs = skillAttributeService.getVideoAttrs(projectId, skillId)
        return StringUtils.isNotBlank(skillVideoAttrs?.captions) ? skillVideoAttrs.captions : ""
    }
}
