/**
 * Copyright 2026 SkillTree
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
package skills.skillLoading

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.SkillDefGraphRes
import skills.controller.result.model.SkillsGraphRes
import skills.services.GlobalBadgesService
import skills.services.admin.SkillsDepsService
import skills.skillLoading.model.SkillDependencyInfo
import skills.skillLoading.model.SkillDependencyInfo.SkillRelationship
import skills.skillLoading.model.SkillSummary
import skills.storage.repos.SkillRelDefRepo

@Component
class SkillLearningPathLoader {

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    SkillsDepsService skillsDepsService

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Transactional(readOnly = true)
    SkillSummary loadSharedSkillSummary(String projectId, String userId, String crossProjectId, String skillId, String subjectId) {
        skillsDepsService.validateProjectAccess(crossProjectId)
        boolean directPrerequisite = skillsDepsService.checkIfSkillInAnotherProjectPartOfLearningPath(projectId, crossProjectId, skillId)
        boolean inheritedPrerequisite = !directPrerequisite && isVisibleIndirectPrerequisite(projectId, userId, crossProjectId, skillId)
        // Preserve normal explicit-sharing checks; only a verified inherited prerequisite uses the read-only exception.
        return skillsLoader.loadSkillSummary(projectId, userId, crossProjectId, skillId, subjectId, inheritedPrerequisite)
    }

    @Transactional(readOnly = true)
    SkillDependencyInfo load(String projectId, String userId, String skillId, Integer version, String crossProjectId) {
        boolean sharedSkill = StringUtils.isNotBlank(crossProjectId) && projectId != crossProjectId
        if (sharedSkill) {
            // Check access before membership so status codes do not disclose hidden source relationships.
            skillsDepsService.validateProjectAccess(crossProjectId)
            boolean associated = skillsDepsService.checkIfSkillInAnotherProjectPartOfLearningPath(projectId, crossProjectId, skillId) ||
                    globalBadgesService.checkIfSkillBelongsToBadgeThatThisProjectIsPartOf(projectId, crossProjectId, skillId) ||
                    isVisibleIndirectPrerequisite(projectId, userId, crossProjectId, skillId)
            SkillsValidator.isTrue(associated,
                    'The shared skill must be associated with this project through a Learning Path or Global Badge', projectId, skillId)
        }

        // Shared pages must not return the source project's prerequisite graph.
        SkillDependencyInfo result = new SkillDependencyInfo(dependencies: sharedSkill ? [] : loadVisibleDependencies(projectId, userId, skillId))
        SkillsGraphRes graph = skillsDepsService.getUserDependentSkillsGraph(projectId, userId, version,
                sharedSkill ? crossProjectId : projectId, skillId)
        Set<Integer> successorIds = graph.edges.collect { SkillsGraphRes.Edge edge -> edge.fromId }.toSet()
        result.nextItems = graph.nodes.findAll { SkillDefGraphRes node -> successorIds.contains(node.id) }
        return result
    }

    private boolean isVisibleIndirectPrerequisite(String projectId, String userId, String sourceProjectId, String skillId) {
        // Reuse badge expansion from the prerequisite loader instead of authorizing arbitrary source skills.
        return skillRelDefRepo.findCrossProjectDependencyParentSkillIds(projectId).any { String rootSkillId ->
            loadVisibleDependencies(projectId, userId, rootSkillId).any { SkillRelationship relationship ->
                relationship.dependsOn.projectId == sourceProjectId && relationship.dependsOn.skillId.equalsIgnoreCase(skillId)
            }
        }
    }

    private List<SkillRelationship> loadVisibleDependencies(String projectId, String userId, String skillId) {
        SkillDependencyInfo info = skillsLoader.loadSkillDependencyInfo(projectId, userId, skillId)
        return skillsDepsService.filterAccessibleDependencies(projectId, skillId, info.dependencies)
    }
}
