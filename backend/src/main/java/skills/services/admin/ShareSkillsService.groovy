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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.result.model.SharedSkillResult
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

    @Transactional(readOnly = true)
    List<SharedSkillResult> getSharedSkillsFromOtherProjects(String projectId) {
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        List<SkillShareDefRepo.SkillSharedMeta> sharedMetas = skillShareDefRepo.getSkillDefsSharedFromOtherProjectsByProjectId(projDef)
        return sharedMetas.collect { SkillShareDefRepo.SkillSharedMeta meta ->
            new SharedSkillResult(
                    skillName: meta.skillName, skillId: meta.skillId,
                    projectName: meta.projectName, projectId: meta.projectId
            )
        }
    }

    @Transactional(readOnly = true)
    List<SharedSkillResult> getSharedSkillsWithOtherProjects(String projectId) {
        List<SkillShareDef> shareDefs = skillShareDefRepo.getSkillShareDefsWithOtherProjectsByProjectId(projectId)
        return shareDefs.collect { SkillShareDef shareDef ->
            new SharedSkillResult(
                    skillName: shareDef.skill.name, skillId: shareDef.skill.skillId,
                    projectName: shareDef.sharedToProject?.name, projectId: shareDef.sharedToProject?.projectId,
                    sharedWithAllProjects: shareDef.sharedToProject == null
            )
        }
    }

    @Transactional()
    void shareSkillToExternalProject(String projectId, String skillId, String sharedToProjectId) {
        if (projectId?.equalsIgnoreCase(sharedToProjectId)) {
            throw new SkillException("Can not share skill to itself. Requested project [$sharedToProjectId] is itself!", projectId, skillId)
        }
        SkillDef skill = skillDefAccessor.getSkillDef(projectId, skillId)

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
        skillShareDefRepo.delete(skillShareDef)
    }
}
