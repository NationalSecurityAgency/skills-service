/**
 * Copyright 2025 SkillTree
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
package skills.services.slides

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.services.attributes.QuizAttrs
import skills.services.attributes.SlidesAttrs
import skills.storage.repos.QuizDefRepo

@Service
@Slf4j
class QuizAttrsStore {

    static final ObjectMapper mapper = new ObjectMapper()

    @Autowired
    QuizDefRepo quizDefRepo

    QuizAttrs getQuizAttrs(String quizId) {
        String quizAttributes = quizDefRepo.getQuizAttributes(quizId)
        if (quizAttributes) {
            return getQuizAttrsFromString(quizAttributes)
        }
        return null
    }

    SlidesAttrs getSlidesAttrs(String quizId) {
        String slidesAttributes = quizDefRepo.getSlidesAttributes(quizId)
        if (slidesAttributes) {
            return getSlideAttrsFromString(slidesAttributes)
        }
        return null
    }

    static QuizAttrs getQuizAttrsFromString(String quizAttributes) {
        QuizAttrs res = mapper.readValue(quizAttributes, QuizAttrs.class)
        return res
    }

    static SlidesAttrs getSlideAttrsFromString(String slidesAttributes) {
        SlidesAttrs res = mapper.readValue(slidesAttributes, SlidesAttrs.class)
        return res
    }

    static String convertQuizAttrsToString(QuizAttrs quizAttrs) {
        String res = mapper.writeValueAsString(quizAttrs)
        return res
    }

    void saveQuizAttrs(String quizId, QuizAttrs quizAttrs) {
        quizDefRepo.saveAttributes(quizId, convertQuizAttrsToString(quizAttrs))
    }
}
