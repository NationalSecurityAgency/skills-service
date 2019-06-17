package skills.service.datastore.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.service.auth.UserInfo
import skills.service.controller.request.model.ProjectRequest
import skills.service.controller.request.model.SkillRequest
import skills.service.controller.request.model.SubjectRequest
import skills.service.controller.result.model.ProjectResult
import skills.storage.model.ProjDef
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.ProjDefRepo

import javax.transaction.Transactional

@Service
@Slf4j
class InceptionProjectService {

    @Autowired
    AdminProjService projectAdminStorageService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    ProjDefRepo projDefRepo

    static final String inceptionProjectId = "Inception"

    /**
     * If inception project exist then user will simply be assigned as an admin
     */
    @Transactional
    void createInceptionAndAssignUser(String userId) {
        doCreateAndAssign(userId)
    }

    /**
     * If inception project exist then user will simply be assigned as an admin
     */
    @Transactional
    void createInceptionAndAssignUser(UserInfo userInfo) {
        doCreateAndAssign(userInfo.username?.toLowerCase())
    }

    @Transactional
    void removeUser(String userId) {
        accessSettingsStorageService.deleteUserRole(userId, inceptionProjectId, RoleName.ROLE_PROJECT_ADMIN)
    }

    private void doCreateAndAssign(String userId) {
        boolean createdNewProject = createInceptionProjectIfNeeded(userId)

        if (!createdNewProject) {
            List<UserRole> existingRoles = accessSettingsStorageService.getUserRoles(inceptionProjectId)
            if (!existingRoles.find({ it.userId == userId && it.roleName == RoleName.ROLE_PROJECT_ADMIN })) {
                log.info("Making [{}] project admin of [{}]", userId, inceptionProjectId)
                accessSettingsStorageService.addUserRole(userId, inceptionProjectId, RoleName.ROLE_PROJECT_ADMIN)
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
        projectAdminStorageService.saveProject(new ProjectRequest(projectId: inceptionProjectId, name: inceptionProjectId), userId)

        List<SubjectRequest> subs = [
                new SubjectRequest(name: "Projects", subjectId: "Projects", iconClass: "fas fa-project-diagram",
                        description: "Project creation and management. Includes CRUD of subjects, badges as well as configuration of levels and project settings."),
                new SubjectRequest(name: "Skills", subjectId: "Skills", iconClass: "fas fa-user-ninja",
                        description: "Creation and management of skills including dependency and cross-project skills. "),
                new SubjectRequest(name: "Dashboard", subjectId: "Dashboard", iconClass: "fas fa-cubes",
                        description: "Number of ancillary dashboard features including user management."),
        ]
        subs.each {
            projectAdminStorageService.saveSubject(inceptionProjectId, it)
        }

        List<SkillRequest> skills = [
                new SkillRequest(name: "Create Subject", skillId: "CreateSubject", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 10, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "To create a subject navigate a subject tab on a project page then click 'Subject +' button.")
        ]

        skills.each {
            projectAdminStorageService.saveSkill(it)
        }

    }
}
