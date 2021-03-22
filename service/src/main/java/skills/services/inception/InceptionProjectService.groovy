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
package skills.services.inception

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.auth.UserInfo
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SubjectRequest
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.UserRoleRes
import skills.services.AccessSettingsStorageService
import skills.services.admin.ProjAdminService
import skills.services.admin.SkillsAdminService
import skills.services.admin.SubjAdminService
import skills.services.settings.SettingsService
import skills.settings.CommonSettings
import skills.storage.model.ProjDef
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.ProjDefRepo

import javax.annotation.PostConstruct
import javax.transaction.Transactional

@Service
@Slf4j
class InceptionProjectService {

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    SubjAdminService subjAdminService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    SettingsService settingsService

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    ProjAdminService projAdminService

    @Autowired
    InceptionSkills inceptionSkills

    @Value('#{"${skills.config.ui.docsHost}"}')
    String docsRootHost = ""

    static final String inceptionProjectId = "Inception"
    static final String subjectProjectId = "Projects"
    static final String subjectSkillsId = "Skills"
    static final String subjectDashboardId = "Dashboard"

    @PostConstruct
    void init() {
        updateSkillsIfNeeded()
    }


    /**
     * If inception project exist then user will simply be assigned as an admin
     */
    @Transactional
    void createInceptionAndAssignUser(String userId) {
        createInceptionProjectIfNeeded(userId)
        assignAllRootUsersToInception();
    }

    @Transactional
    void removeUser(String userId) {
        accessSettingsStorageService.deleteUserRole(userId, inceptionProjectId, RoleName.ROLE_PROJECT_ADMIN)
    }

    private assignAllRootUsersToInception(){
        List<UserRoleRes> rootUsers = accessSettingsStorageService.getRootUsers()

        rootUsers.each {
            List<UserRoleRes> inceptionRoles = accessSettingsStorageService.getUserRolesForProjectIdAndUserId(inceptionProjectId, it.userId)
            if (!inceptionRoles.find({it.roleName == RoleName.ROLE_PROJECT_ADMIN})) {
                log.info("Making [{}] project admin of [{}]", it.userId, inceptionProjectId)
                accessSettingsStorageService.addUserRole(it.userId, inceptionProjectId, RoleName.ROLE_PROJECT_ADMIN)
            }
        }
    }



    private boolean createInceptionProjectIfNeeded(String userId) {
        ProjDef projDef = projDefRepo.findByProjectId(inceptionProjectId)
        if (!projDef) {
            log.info("Creating {} project", inceptionProjectId)
            createProject(userId)
            return true
        }

        return false
    }

    private void createProject(String userId) {
        projAdminService.saveProject(inceptionProjectId, new ProjectRequest(projectId: inceptionProjectId, name: inceptionProjectId), userId)

        if (docsRootHost) {
            log.info("setting Inception setting ${CommonSettings.HELP_URL_ROOT} to $docsRootHost")
            ProjectSettingsRequest docRootRequest = new ProjectSettingsRequest()
            docRootRequest.projectId = inceptionProjectId
            docRootRequest.setting = CommonSettings.HELP_URL_ROOT
            docRootRequest.settingGroup = CommonSettings.INCEPTION_SETTING_GROUP
            docRootRequest.value = docsRootHost
            settingsService.saveSetting(docRootRequest)
        }

        List<SubjectRequest> subs = [
                new SubjectRequest(name: "Projects", subjectId: subjectProjectId, iconClass: "fas fa-project-diagram",
                        description: "Project creation and management. Includes CRUD of subjects, badges as well as configuration of levels and project settings."),
                new SubjectRequest(name: "Skills", subjectId: subjectSkillsId, iconClass: "fas fa-user-ninja",
                        description: "Creation and management of skills including dependency and cross-project skills."),
                new SubjectRequest(name: "Dashboard", subjectId: subjectDashboardId, iconClass: "fas fa-cubes",
                        description: "Number of ancillary dashboard features including user management."),
        ]
        subs.each {
            subjAdminService.saveSubject(inceptionProjectId, it.subjectId, it, false)
        }

        saveSkills()
    }

    private void saveSkills() {
        List<SkillRequest> skills = inceptionSkills.getAllSkills()
        skills.each {
            skillsAdminService.saveSkill(it.skillId, it, false)
        }

        saveSkillsMd5Setting()
    }

    private void updateSkillsIfNeeded() {
        ProjDef projDef = projDefRepo.findByProjectId(inceptionProjectId)

        if (projDef) {
            String newHash = inceptionSkills.getHash()
            SettingsResult settingsResult = settingsService.getProjectSetting(inceptionProjectId, CommonSettings.INCEPTION_SKILLS_MD5_HASH, CommonSettings.INCEPTION_SETTING_GROUP)
            if (!settingsResult || settingsResult.value != newHash) {
                log.info("Skills' MD5 Hash difference was detected (old <> new: [${settingsResult?.value}] <> [${newHash}]. Will update ALL skills")
                saveSkills()
            }
        }
    }

    private void saveSkillsMd5Setting() {
        String newHash = inceptionSkills.getHash()
        assert newHash
        ProjectSettingsRequest skillsMd5Setting = new ProjectSettingsRequest(
                projectId: InceptionProjectService.inceptionProjectId,
                settingGroup: CommonSettings.INCEPTION_SETTING_GROUP,
                setting: CommonSettings.INCEPTION_SKILLS_MD5_HASH,
                value: newHash
        )
        settingsService.saveSetting(skillsMd5Setting)
        log.info("Saved [{}]", skillsMd5Setting)
    }
}
