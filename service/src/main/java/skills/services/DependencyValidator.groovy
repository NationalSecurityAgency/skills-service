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

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefParent
import skills.storage.model.SkillShareDef
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.SkillShareDefRepo

@Component
@CompileStatic
class DependencyValidator {

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    void validateDependencyEligibility(String projectId, SkillDefParent skill) {
        if ( projectId?.equalsIgnoreCase(skill.projectId)){
            // can dependent on skills from your project without any restrictions
            return
        }

        SkillShareDef skillShareDef = skillShareDefRepo.findBySharedToProjectIdAndSkillId(projectId, skill.id)
        if (!skillShareDef) {
            // check if the dependency is shared with ALL projects (null shared_to_project_id)
            skillShareDef = skillShareDefRepo.findBySkillIdAndSharedToProjectIsNull(skill.id)
        }

        if (!skillShareDef){
            SkillDef globalBadge = skillRelDefRepo.findGlobalBadgeByChildSkillId(skill.skillId)
            if(globalBadge) {
                //if the skillId is a child of a global badge, then we expect there not to be an explicit
                //SkillShareDef
                return
            }
        }

        if (!skillShareDef) {
            throw new SkillException("Skill [${skill.projectId}:${skill.skillId}] is not shared (or does not exist) to [$projectId] project", projectId)
        }
    }
}
