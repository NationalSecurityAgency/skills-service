/**
 * Copyright 2022 SkillTree
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
package skills.storage.model

interface SkillDefPartial extends SkillDefSkinny {
    Integer getPointIncrement()
    Integer getPointIncrementInterval()
    Integer getNumMaxOccurrencesIncrementInterval()
    String getIconClass()
    SkillDef.ContainerType getSkillType()
    Date getUpdated()
    SkillDef.SelfReportingType getSelfReportingType()
    String getEnabled()
    Integer getNumSkillsRequired()
    Integer getCopiedFrom()
    String getCopiedFromProjectId()
    Boolean getReadOnly()
    String getCopiedFromProjectName()
    Boolean getSharedToCatalog()
    String getQuizId()
    String getQuizName()
    QuizDefParent.QuizType getQuizType()
    Boolean getHasBadges()
}
