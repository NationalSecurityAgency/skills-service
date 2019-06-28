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
        createInceptionProjectIfNeeded(userId)
        assignAllRootUsersToInception();
    }

    private assignAllRootUsersToInception(){
        List<UserRole> rootUsers = accessSettingsStorageService.getRootUsers()

        rootUsers.each {
            List<UserRole> inceptionRoles = accessSettingsStorageService.getUserRoles(inceptionProjectId, it.userId)
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
                new SkillRequest(name: "Create Project", skillId: "CreateProject", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 50, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "Project is an overall container that represents skills' ruleset for a single application with gamified training. " +
                                "Project's administrator(s) manage training skills definitions, subjects, levels, dependencies and other attributes " +
                                "that make up application's training profile. To create project click 'Project +' button.",
                        helpUrl: "/dashboard/user-guide/projects.html"
                ),
                new SkillRequest(name: "Create Subject", skillId: "CreateSubject", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 20, pointIncrementInterval: 8, numPerformToCompletion: 3,
                        description: "To create a subject navigate a subject tab on a project page then click 'Subject +' button.",
                        helpUrl: "/dashboard/user-guide/subjects.html"
                ),
                new SkillRequest(name: "Configure Root Help Url", skillId: "ConfigureRootHelpUrl", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 50, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "Configure project's 'Root Help Url' by navigating to Project -> Settings. " +
                                "Skill definition's `Help Url/Path` will be treated relative to this `Root Help Url`.",
                        helpUrl: "/dashboard/user-guide/projects.html#settings"
                ),
                new SkillRequest(name: "Add Project Administrator", skillId: "AddAdmin", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 50, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "Add another project administrator under Project -> Access so you don't get too lonely.",
                        helpUrl: "/dashboard/user-guide/access-management.html"
                ),

                new SkillRequest(name: "Add or Modify Levels", skillId: "AddOrModifyProjectLevels", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 25, pointIncrementInterval: 8, numPerformToCompletion: 4,
                        description: "Managing project's and subject levels is tricky. Study available percentage based and point-based strategy and make several modifications to levels as needed.",
                        helpUrl: "/dashboard/user-guide/access-management.html"
                ),


                new SkillRequest(name: "Create Skill", skillId: "CreateSkill", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 10, pointIncrementInterval: 8, numPerformToCompletion: 50,
                        description: "To crate skill navigate to a subject and then click 'Skill +' button.",
                        helpUrl: "/dashboard/user-guide/skills.html"
                )
        ]

        skills.each {
            projectAdminStorageService.saveSkill(it)
        }

    }
}
