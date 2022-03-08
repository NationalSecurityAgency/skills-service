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
import skills.services.admin.SkillsGroupAdminService
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo


@Service
@Slf4j
class RuleSetDefinitionScoreUpdater {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService


    void updateFromLeaf(SkillDef skillDef) {
        if (skillDef.type == SkillDef.ContainerType.SkillsGroup) {
            List<SkillDef> children = skillRelDefRepo.findChildrenByParent(skillDef.id, [SkillRelDef.RelationshipType.SkillsGroupRequirement], Boolean.TRUE.toString())
            int total = skillsGroupAdminService.getGroupTotalPoints(children, skillDef.numSkillsRequired)
            skillDef.totalPoints = total
            skillDefRepo.save(skillDef)
        } else {
            skillDefRepo.updateSubjectTotalPoints(skillDef.projectId, skillDef.skillId)
        }

        List<SkillDef> parents = skillRelDefRepo.findParentByChildIdAndTypes(skillDef.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        parents?.each {
            updateFromLeaf(it)
        }

        if (skillDef.type == SkillDef.ContainerType.Subject) {
            skillDefRepo.updateProjectsTotalPoints(skillDef.projectId)
        }
    }

    void skillToBeRemoved(SkillDef skillDef) {
        List<SkillDef> parents = skillRelDefRepo.findParentByChildIdAndTypes(skillDef.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        if (isChildOfDisabledSkillsGroup(skillDef, parents)) {
            // disabled groups do not contribute to parent total points, just update the group itself
            SkillDef skillsGroupDef = parents.first()
            skillsGroupDef.totalPoints = skillsGroupDef.totalPoints - skillDef.totalPoints
            skillDefRepo.save(skillDef)
        } else {
            parents?.each {
                walkUpAndSubtractFromTotal(it, skillDef.totalPoints)
            }
            if(SkillDef.ContainerType.Skill == skillDef.type){
                ProjDef projDef = projDefRepo.findByProjectId(skillDef.projectId)
                projDef.totalPoints -= skillDef.totalPoints
            }
        }
    }

    private void walkUpAndSubtractFromTotal(SkillDef skillDef, int pointsToSubtract) {
        skillDef.totalPoints = skillDef.totalPoints - pointsToSubtract
        skillDefRepo.save(skillDef)

        List<SkillDef> parents = skillRelDefRepo.findParentByChildIdAndTypes(skillDef.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        parents?.each {
            walkUpAndSubtractFromTotal(it, pointsToSubtract)
        }
    }

    private boolean isChildOfDisabledSkillsGroup(SkillDef skillDef, List<SkillDef> parents) {
        boolean isSkillsGroupChild = StringUtils.isNotBlank(skillDef.groupId)
        if (isSkillsGroupChild) {
            assert parents && parents.size() == 1 && parents.first().type == SkillDef.ContainerType.SkillsGroup && parents.first().skillId == skillDef.groupId
            SkillDef skillsGroupDef = parents.first()
            return !Boolean.valueOf(skillsGroupDef.enabled)
        }
        return false
    }
}
