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
package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.QuizSetting

interface QuizSettingsRepo extends CrudRepository<QuizSetting, Long> {

    @Nullable
    QuizSetting findBySettingAndQuizRefId(String setting, Integer quizRefId)

    @Nullable
    @Query('''select qS from QuizSetting qS, QuizDef quiz where quiz.id = qS.quizRefId and qS.setting = ?1 and lower(quiz.quizId) = lower(?2)''')
    QuizSetting findBySettingAndQuizId(String setting, String quizId)

    @Nullable
    List<QuizSetting> findAllByQuizRefIdAndSettingIn(Integer quizRefId, List<String> settings)

    @Nullable
    List<QuizSetting> findAllByQuizRefId(Integer quizRefId)

    @Nullable
    List<QuizSetting> findAllByQuizRefIdAndUserRefId(Integer quizRefId,  Integer userRefId)

    @Nullable
    QuizSetting findBySettingAndQuizRefIdAndUserRefId(String setting, Integer quizRefId, Integer userRefId)

}
