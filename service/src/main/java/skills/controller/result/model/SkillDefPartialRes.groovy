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
package skills.controller.result.model

import jakarta.persistence.Convert
import skills.storage.converters.BooleanConverter
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

class SkillDefPartialRes extends SkillDefSkinnyRes{
    Integer pointIncrement

    // Time Window - in minute; 0 means that the action can be performed right away
    Integer pointIncrementInterval
    // Max Occurrences Within Window;
    Integer numMaxOccurrencesIncrementInterval

    Integer numPerformToCompletion
    Integer totalPoints

    SkillDef.ContainerType type

    Date updated

    int numUsers

    SkillDef.SelfReportingType selfReportingType

    List<SkillTagRes> tags = []

    // SkillsGroup related props
    Integer numSkillsInGroup
    Integer numSelfReportSkills
    Integer numSkillsRequired
    boolean enabled
    boolean justificationRequired
    String groupId
    String groupName
    @Convert(converter=BooleanConverter)
    Boolean readOnly
    String copiedFromProjectId
    String copiedFromProjectName
    @Convert(converter=BooleanConverter)
    Boolean sharedToCatalog
    @Convert(converter=BooleanConverter)
    Boolean reusedSkill

    // utilized when selfReportingType=quiz
    String quizId
    String quizName
    QuizDefParent.QuizType quizType

    // expiration props
    String expirationType
    Integer every
    String monthlyDay
    Date nextExpirationDate

    String iconClass
}
