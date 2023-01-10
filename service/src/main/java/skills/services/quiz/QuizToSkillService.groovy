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
package skills.services.quiz

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.storage.model.QuizDef
import skills.storage.model.QuizToSkillDef
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo

@Service
@Slf4j
class QuizToSkillService {

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Transactional
    void saveQuizToSkillAssignment(Integer skillRef, String quizId) {
        QuizDef quizDef = getQuizDef(quizId)

        QuizToSkillDef quizToSkillDef = quizToSkillDefRepo.findByQuizRefIdAndSkillRefId(quizDef.id, skillRef)
        if (!quizToSkillDef) {
            quizToSkillDef = new QuizToSkillDef(quizRefId: quizDef.id, skillRefId: skillRef)
            quizToSkillDefRepo.save(quizToSkillDef)
        }
    }

    QuizToSkillDefRepo.QuizNameAndId getQuizIdForSkillRefId(Integer skillRefId) {
        return quizToSkillDefRepo.getQuizIdBySkillIdRef(skillRefId)
    }

    private QuizDef getQuizDef(String quizId) {
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return quizDef
    }
}
