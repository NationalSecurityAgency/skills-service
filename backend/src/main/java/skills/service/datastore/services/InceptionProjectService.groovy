package skills.service.datastore.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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

    String inceptionProjectId = "Inception"

    /**
     * If inception project exist then user will simply be assigned as an admin
     */
    @Transactional
    void createInceptionAndAssignUser(UserRole userRole) {
        ProjDef projDef = projDefRepo.findByProjectId(inceptionProjectId)
        if (!projDef) {
            log.info("Creating {} project", inceptionProjectId)
            createProject()
        }

        List<UserRole> existingRoles = accessSettingsStorageService.getUserRoles(inceptionProjectId)
        if (!existingRoles.find({
            it.userId == userRole.userId && it.projectId == userRole.projectId && it.roleName == userRole.roleName
        })) {
            log.info("Making [{}] project admin of [{}]", userRole.userId, inceptionProjectId)
            accessSettingsStorageService.addUserRole(userRole.userId, inceptionProjectId, RoleName.ROLE_PROJECT_ADMIN)
        }
    }

    private void createProject() {
        projectAdminStorageService.saveProject(new ProjectRequest(projectId: inceptionProjectId, name: inceptionProjectId))

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
