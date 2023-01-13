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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.controller.request.model.QuizSettingsRequest
import skills.controller.result.model.QuizSettingsRes
import skills.storage.model.QuizSetting
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizSettingsRepo

@Service
@Slf4j
class QuizSettingsService {

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Transactional
    void saveSettings(String quizId, List<QuizSettingsRequest> settingsRequests) {
        Integer quizRefId = getQuizDefRefId(quizId)
        settingsRequests.each {
            QuizValidator.isNotBlank(it.setting, "settings.setting", quizId)
            QuizValidator.isNotBlank(it.value, "settings.value", quizId)

            QuizSetting existing = quizSettingsRepo.findBySettingAndQuizRefId(it.setting, quizRefId)
            if (existing) {
                existing.value = it.value
                quizSettingsRepo.save(existing)
            } else {
                quizSettingsRepo.save(new QuizSetting(setting: it.setting, value: it.value, quizRefId: quizRefId))
            }
        }
    }

    @Transactional(readOnly = true)
    List<QuizSettingsRes> getSettings(String quizId) {
        Integer quizRefId = getQuizDefRefId(quizId)
        List<QuizSetting> quizSettings = quizSettingsRepo.findAllByQuizRefId(quizRefId)
        List<QuizSettingsRes> res = quizSettings.collect {
            new QuizSettingsRes(setting: it.setting, value: it.value, created: it.created, updated: it.updated)
        } ?: []

        return res.sort({ it.setting })
    }

    @Profile
    private Integer getQuizDefRefId(String quizId) {
        Integer id = quizDefRepo.getQuizRefIdByQuizIdIgnoreCase(quizId)
        if (id == null) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return id
    }
}
