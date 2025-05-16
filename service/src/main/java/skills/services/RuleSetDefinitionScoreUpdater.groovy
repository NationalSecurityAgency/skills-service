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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
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

    private static List<String> SUBJ_TO_SKILL_REL_TYPES = [SkillRelDef.RelationshipType.GroupSkillToSubject, SkillRelDef.RelationshipType.RuleSetDefinition].collect { it.toString() }
    private static List<String> GROUP_TO_SKILL_REL_TYPES = [SkillRelDef.RelationshipType.SkillsGroupRequirement].collect { it.toString() }

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService


    @Profile
    void updateFromLeaf(SkillDef skillDef) {
        if (skillDef.type == SkillDef.ContainerType.SkillsGroup) {
            updateGroupDef(skillDef)
        } else if (skillDef.type == SkillDef.ContainerType.Subject){
            updateSubjectSkillDef(skillDef)
        }

        List<SkillDef> parents = skillRelDefRepo.findParentByChildIdAndTypes(skillDef.id, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        parents?.each {
            updateFromLeaf(it)
        }

        if (skillDef.type == SkillDef.ContainerType.Subject) {
            updateProjDef(skillDef.projectId)
        }
    }

    @Profile
    void updateGroupDef(SkillDef skillDef) {
        List<SkillDef> children = skillRelDefRepo.findChildrenByParent(skillDef.id, [SkillRelDef.RelationshipType.SkillsGroupRequirement])
        int total = skillsGroupAdminService.getGroupTotalPoints(children)
        skillDef.totalPoints = total
        skillDefRepo.save(skillDef)
    }

    @Profile
    void updateProjDef(String projectId) {
        // it's important to update ProjDef object so Hibernate cache is updated as well
        // as ProjDef object is retrieved later in the execution path
        Integer projTotalPoints = skillDefRepo.getProjectsTotalPoints(projectId, true)
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        projDef.totalPoints = projTotalPoints
        projDefRepo.save(projDef)
    }

    @Profile
    void updateSubjectSkillDef(SkillDef subjectDef, boolean enabledSkillsOnly = true) {
        assert subjectDef.type == SkillDef.ContainerType.Subject
        // it's important to update SkillDef object so Hibernate cache is updated as well
        // as SkillDef object is retrieved later in the execution path
        Integer totalPoints = skillDefRepo.calculateSkillDefTotalPointsBySummingChildPoints(subjectDef.id, SUBJ_TO_SKILL_REL_TYPES, enabledSkillsOnly)
        subjectDef.totalPoints = totalPoints
        skillDefRepo.save(subjectDef)
    }

    @Profile
    void updateSubjectTotalPoints(String projectId, String subjectId, boolean enabledSkillsOnly = true) {
        SkillDef subjectDef = skillDefRepo.findByProjectIdAndSkillId(projectId, subjectId)
        this.updateSubjectSkillDef(subjectDef, enabledSkillsOnly)
    }


    @Profile
    void updateGroupTotalPoints(String projectId, String groupId, boolean enabledSkillsOnly = true) {
        SkillDef groupDef = skillDefRepo.findByProjectIdAndSkillId(projectId, groupId)
        assert groupDef.type == SkillDef.ContainerType.SkillsGroup
        Integer totalPoints = skillDefRepo.calculateSkillDefTotalPointsBySummingChildPoints(groupDef.id, GROUP_TO_SKILL_REL_TYPES, enabledSkillsOnly)
        groupDef.totalPoints = totalPoints
        skillDefRepo.save(groupDef)
    }

}
