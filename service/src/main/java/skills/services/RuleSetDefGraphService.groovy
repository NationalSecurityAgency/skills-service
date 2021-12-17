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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.SkillRelDef.RelationshipType
import skills.storage.accessors.SkillDefAccessor
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo

@Service
@Slf4j
class RuleSetDefGraphService {

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Transactional
    SkillDef getParentSkill(SkillDef skillDef) {
        List<SkillRelDef> parents = skillRelDefRepo.findAllByChildAndTypeIn(skillDef, [RelationshipType.RuleSetDefinition, RelationshipType.SkillsGroupRequirement])
        // assume that I only have one parent
        SkillDef parent = parents.first().parent
        return parent
    }

    @Transactional
    List<SkillDef> getChildrenSkills(SkillDef skillDef, List<RelationshipType> relationshipTypes) {
        return skillRelDefRepo.getChildren(skillDef.projectId, skillDef.skillId, relationshipTypes)
    }

    @Transactional
    List<SkillDef> getChildrenSkills(String projectId, String skillId, List<RelationshipType> relationshipTypes) {
        return skillRelDefRepo.getChildren(projectId, skillId, relationshipTypes)
    }

    @Transactional
    void deleteSkillWithItsDescendants(SkillDef skillDef) {
        List<SkillDef> toDelete = []

        List<SkillDef> currentChildren = getChildrenSkills(skillDef, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
        while (currentChildren) {
            toDelete.addAll(currentChildren)
            currentChildren = currentChildren?.collect {
                getChildrenSkills(it, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
            }?.flatten()
        }
        toDelete.add(skillDef)
        log.debug("Deleting [{}] skill definitions (descendants + me) under [{}]", toDelete.size(), skillDef.skillId)
        skillDefRepo.deleteAll(toDelete)
    }

    @Transactional
    void assignGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipSkillId, RelationshipType relationshipType) {
        assignGraphRelationship(projectId, skillId, skillType, projectId, relationshipSkillId, relationshipType)
    }
    @Transactional
    void assignGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipProjectId, String relationshipSkillId, RelationshipType relationshipType) {
        SkillDef skill1 = skillDefAccessor.getSkillDef(projectId, skillId, [skillType])
        SkillDef skill2 = skillDefAccessor.getSkillDef(relationshipProjectId, relationshipSkillId)
        skillRelDefRepo.save(new SkillRelDef(parent: skill1, child: skill2, type: relationshipType))
    }

    @Transactional
    void removeGraphRelationship(String projectId, String skillId, SkillDef.ContainerType skillType,
                                 String relationshipProjectId, String relationshipSkillId, RelationshipType relationshipType){
        SkillDef skill1 = skillDefAccessor.getSkillDef(projectId, skillId, [skillType])
        SkillDef skill2 = skillDefAccessor.getSkillDef(relationshipProjectId, relationshipSkillId)
        SkillRelDef relDef = skillRelDefRepo.findByChildAndParentAndType(skill2, skill1, relationshipType)
        if (!relDef) {
            throw new SkillException("Failed to find relationship [$relationshipType] between [$skillId] and [$relationshipSkillId] for [$projectId]", projectId, skillId)
        }
        skillRelDefRepo.delete(relDef)
    }
}
