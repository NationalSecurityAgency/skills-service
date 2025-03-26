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
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.SkillAttributesDefRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class VideoCaptionsService {

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    SkillAttributesDefRepo skillAttributesDefRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionRepo

    String getVideoCaptions(String projectId, String skillId) {
        String res = skillAttributesDefRepo.getVideoCaptionsByProjectAndSkillId(projectId, skillId)
        if (StringUtils.isNotBlank(res)) {
            return InputSanitizer.unSanitizeCaption(res)
        }
        return StringUtils.isNotBlank(res) ? res : ""
    }

    String getVideoTranscript(String projectId, String skillId) {
        String res = skillAttributesDefRepo.getVideoTranscriptsByProjectAndSkillId(projectId, skillId)
        return StringUtils.isNotBlank(res) ? res : ""
    }

    String getVideoCaptionsForQuiz(String quizId, Integer questionId) {
        String res = quizQuestionRepo.getVideoCaptions(quizId, questionId)
        if (StringUtils.isNotBlank(res)) {
            return InputSanitizer.unSanitizeCaption(res)
        }
        return StringUtils.isNotBlank(res) ? res : ""
    }

    String getVideoTranscriptForQuiz(String quizId, Integer questionId) {
        String res = quizQuestionRepo.getVideoTranscripts(quizId, questionId)
        return StringUtils.isNotBlank(res) ? res : ""
    }
}
