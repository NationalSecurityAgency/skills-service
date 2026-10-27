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
package skills.services.admin

import groovy.util.logging.Slf4j
import org.jsoup.helper.Validate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.result.model.SharedSkillResult
import skills.controller.result.model.TableResult
import skills.controller.result.model.UserQuizAnswer
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillShareDef
import skills.storage.accessors.ProjDefAccessor
import skills.storage.accessors.SkillDefAccessor
import skills.storage.repos.SkillShareDefRepo

@Service
@Slf4j
class ShareSkillsService {

    static final ALL_SKILLS_PROJECTS = 'ALL_SKILLS_PROJECTS'

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    SkillDefAccessor skillDefAccessor

    @Autowired
    SkillsDepsService skillsDepsService

    @Transactional(readOnly = true)
    TableResult getSharedSkillsFromOtherProjects(String projectId, PageRequest pageRequest, String filter = '') {
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        Page<SkillShareDefRepo.SkillSharedMeta> sharedMetaPage = skillShareDefRepo.getSkillDefsSharedFromOtherProjectsByProjectId(projDef, filter, pageRequest)
        List<SkillShareDefRepo.SkillSharedMeta> sharedMetas = sharedMetaPage.getContent()
        List<SharedSkillResult> results = sharedMetas.collect { SkillShareDefRepo.SkillSharedMeta meta ->
            new SharedSkillResult(
                    skillName: meta.skillName, skillId: meta.skillId,
                    projectName: meta.projectName, projectId: meta.projectId
            )
        }

        return new TableResult(totalCount: sharedMetaPage.getTotalElements(), data: results, count: sharedMetaPage.getTotalElements())
    }

    @Transactional(readOnly = true)
    String getExistingShareExplanation(String projectId, String skillId, String destinationProjectId = '') {
        SkillDef skill = skillDefAccessor.getSkillDef(projectId, skillId)
        SkillShareDef existing = skillShareDefRepo.findBySkillAndSharedToProjectIsNull(skill)

        if (!existing && destinationProjectId) {
            ProjDef destination = projDefAccessor.getProjDef(destinationProjectId)
            existing = skillShareDefRepo.findBySkillAndSharedToProject(skill, destination)
        } else if (!existing) {
            existing = skillShareDefRepo.findFirstBySkillOrderByIdAsc(skill)
        }

        if (!existing) {
            return null
        }

        return existing.sharedToProject == null
                ? 'This skill is already shared with All Projects.'
                : "This skill is already shared with project ${existing.sharedToProject.name}."
    }

    @Transactional(readOnly = true)
    TableResult getSharedSkillsWithOtherProjects(String projectId, PageRequest pageRequest, String filter = '') {
        Page<SkillShareDef> shareDefPage = skillShareDefRepo.getSkillShareDefsWithOtherProjectsByProjectId(projectId, filter, pageRequest)
        List<SkillShareDef> shareDefs = shareDefPage.getContent()
        List<SharedSkillResult> results = shareDefs.collect { SkillShareDef shareDef ->
            new SharedSkillResult(
                    skillName: shareDef.skill.name, skillId: shareDef.skill.skillId,
                    projectName: shareDef.sharedToProject?.name, projectId: shareDef.sharedToProject?.projectId,
                    sharedWithAllProjects: shareDef.sharedToProject == null
            )
        }

        return new TableResult(totalCount: shareDefPage.getTotalElements(), data: results, count: shareDefPage.getTotalElements())
    }

    @Transactional()
    void shareSkillToExternalProject(String projectId, String skillId, String sharedToProjectId) {
        if (projectId?.equalsIgnoreCase(sharedToProjectId)) {
            throw new SkillException("Can not share skill to itself. Requested project [$sharedToProjectId] is itself!", projectId, skillId)
        }
        SkillDef skill = skillDefAccessor.getSkillDef(projectId, skillId)
        SkillsValidator.isTrue(!skill.readOnly, "Skills imported from the catalog may not be shared as cross project dependencies", projectId, skillId)

        ProjDef sharedToProject = null
        if (sharedToProjectId != ALL_SKILLS_PROJECTS) {
            sharedToProject = projDefAccessor.getProjDef(sharedToProjectId)
        }

        SkillShareDef skillShareDef = new SkillShareDef(skill: skill, sharedToProject: sharedToProject)
        skillShareDefRepo.save(skillShareDef)
    }

    @Transactional()
    void deleteSkillShare(String projectId, String skillId, String sharedToProjectId) {
        SkillDef skill = skillDefAccessor.getSkillDef(projectId, skillId)
        SkillShareDef skillShareDef
        if (sharedToProjectId == ALL_SKILLS_PROJECTS) {
            skillShareDef = skillShareDefRepo.findBySkillAndSharedToProjectIsNull(skill)
        } else {
            ProjDef sharedToProject = projDefAccessor.getProjDef(sharedToProjectId)
            skillShareDef = skillShareDefRepo.findBySkillAndSharedToProject(skill, sharedToProject)
        }

        if (!skillShareDef){
            throw new SkillException("Failed to find skill share definition for project [$projectId] skill [$skillId] => [$sharedToProjectId] project", projectId, skillId)
        }
        if (sharedToProjectId == ALL_SKILLS_PROJECTS) {
            skillsDepsService.removeAllLearningPathItemsBySkillId(projectId, skillId)
        } else {
            skillsDepsService.removeAllLearningPathItemsBySkillIdAndProjectId(sharedToProjectId, skillId, projectId)
        }
        skillShareDefRepo.delete(skillShareDef)
    }
}
