package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.auth.UserInfo
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
        projectAdminStorageService.saveProject(inceptionProjectId, new skills.controller.request.model.ProjectRequest(projectId: inceptionProjectId, name: inceptionProjectId), userId)

        List<skills.controller.request.model.SubjectRequest> subs = [
                new skills.controller.request.model.SubjectRequest(name: "Projects", subjectId: "Projects", iconClass: "fas fa-project-diagram",
                        description: "Project creation and management. Includes CRUD of subjects, badges as well as configuration of levels and project settings."),
                new skills.controller.request.model.SubjectRequest(name: "Skills", subjectId: "Skills", iconClass: "fas fa-user-ninja",
                        description: "Creation and management of skills including dependency and cross-project skills. "),
                new skills.controller.request.model.SubjectRequest(name: "Dashboard", subjectId: "Dashboard", iconClass: "fas fa-cubes",
                        description: "Number of ancillary dashboard features including user management."),
        ]
        subs.each {
            projectAdminStorageService.saveSubject(inceptionProjectId, it)
        }

        List<skills.controller.request.model.SkillRequest> skills = [
                new skills.controller.request.model.SkillRequest(name: "Create Project", skillId: "CreateProject", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Project is an overall container that represents skills' ruleset for a single application with gamified training. " +
                                "Project's administrator(s) manage training skills definitions, subjects, levels, dependencies and other attributes " +
                                "that make up application's training profile. To create project click 'Project +' button.",
                        helpUrl: "/dashboard/user-guide/projects.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Create Subject", skillId: "CreateSubject", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 3,
                        description: "Subjects are a way to group and organize skill definitions and skill training profile. To create a subject navigate to ``Project -> Subjects`` and then click ``Subject +`` button.",
                        helpUrl: "/dashboard/user-guide/subjects.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Configure Root Help Url", skillId: "ConfigureProjectRootHelpUrl", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 50, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "Configure project's 'Root Help Url' by navigating to ``Project -> Settings```. " +
                                "Skill definition's `Help Url/Path` will be treated relative to this `Root Help Url`.",
                        helpUrl: "/dashboard/user-guide/projects.html#settings"
                ),
                new skills.controller.request.model.SkillRequest(name: "Add Project Administrator", skillId: "AddAdmin", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Add another project administrator under ``Project -> Access`` so you don't get too lonely.",
                        helpUrl: "/dashboard/user-guide/access-management.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Create Badge", skillId: "CreateBadge", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 2,
                        description: "",
                        helpUrl: "/dashboard/user-guide/badges.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Create Gem", skillId: "CreateGem", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 1,
                        description: "",
                        helpUrl: "/dashboard/user-guide/badges.html#gem"
                ),
                new skills.controller.request.model.SkillRequest(name: "Assign Badge or Gem Skills", skillId: "AssignGemOrBadgeSkills", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 5,
                        description: "",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Subjects", skillId: "VisitSubjects", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Subjects``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Badges", skillId: "VisitBadges", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Badge Page", skillId: "VisitSingleBadgePage", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges -> Badge``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Badge Users", skillId: "VisitBadgeUsers", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges -> Badge -> Users``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Badge Stats", skillId: "VisitBadgeStats", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges -> Badge -> Stats``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Dependencies", skillId: "VisitProjectDependencies", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Dependencies``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Cross Project Skills", skillId: "VisitProjectCrossProjectSkills", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Cross Projects``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Project Levels", skillId: "VisitProjectLevels", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Levels``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Project Users", skillId: "VisitProjectUsers", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Users``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Project Stats", skillId: "VisitProjectStats", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Stats``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Project Settings", skillId: "VisitProjectSettings", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 20,
                        numPerformToCompletion: 1,
                        description: "Navigate to ``Project -> Settings``",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Project Access Management", skillId: "VisitProjectAccessManagement", subjectId: "Projects", projectId: inceptionProjectId,
                        pointIncrement: 20,
                        numPerformToCompletion: 1,
                        description: "Navigate to ``Project -> Settings``",
                ),


                new skills.controller.request.model.SkillRequest(name: "Visit Dashboard Skills", skillId: "VisitDashboardSkills", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60*12,
                        numMaxOccurrencesIncrementInterval: 5,
                        numPerformToCompletion: 20,
                        description: "",
                        helpUrl: "/dashboard/user-guide/inception.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Add or Modify Levels", skillId: "AddOrModifyProjectLevels", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Managing project's and subject levels is tricky. Study available percentage based and point-based strategy and make several modifications to levels as needed.",
                        helpUrl: "/dashboard/user-guide/levels.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit User Settings", skillId: "VisitUserSettings", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "",
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Client Display", skillId: "VisitClientDisplay", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 25, // up-to 25 per day
                        numPerformToCompletion: 50,
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Client Display with Non-Zero Version", skillId: "VisitClientDisplayNonZeroVersion", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 25, // up-to 25 per day
                        numPerformToCompletion: 1,
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit User Performed Skills", skillId: "VisitUserPerformedSkills", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 2,
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit User Stats", skillId: "VisitUserStats", subjectId: "Dashboard", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 2,
                ),


                new skills.controller.request.model.SkillRequest(name: "Create Skill", skillId: "CreateSkill", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 25, // up-to 25 per day
                        numPerformToCompletion: 50,
                        description: "To create skill navigate to a subject and then click ``Skill +`` button.",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Skill Overview", skillId: "VisitSkillOverview", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Visit ``Skill Overview``. Navigate to ``Project -> Subject -> Skill -> Overview``",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Skill Dependencies", skillId: "VisitSkillDependencies", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 3,
                        description: "Visit ``Skill Dependencies``. Navigate to ``Project -> Subject -> Skill -> Dependencies``",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Skill Users", skillId: "VisitSkillUsers", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Visit ``Skill Dependencies``. Navigate to ``Project -> Subject -> Skill -> Users``",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Manually Add Skill Event", skillId: "ManuallyAddSkillEvent", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Manually Add Skill Events. Navigate to ``Project -> Subject -> Skill -> Add Event``",
                        helpUrl: "/dashboard/user-guide/skills.html#manually-add-skill-event"
                ),
                new skills.controller.request.model.SkillRequest(name: "Visit Skill Stats", skillId: "VisitSkillStats", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Visit ``Skill Dependencies``. Navigate to ``Project -> Subject -> Skill -> Users``",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Expand Skill Details on Skills Page", skillId: "ExpandSkillDetailsSkillsPage", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "On the Skills Page click on + to expand a single row. ",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Create Skill with disabled Time Window", skillId: "CreateSkillDisabledTimeWindow", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 25,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 1,
                        description: "",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Create Skill with Max Occurrences Within Time Window", skillId: "CreateSkillMaxOccurrencesWithinTimeWindow", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Create Skill with Help Url", skillId: "CreateSkillHelpUrl", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 5, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Use Markup in Skill's description", skillId: "CreateSkillMarkup", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 24, // 1 day
                        numMaxOccurrencesIncrementInterval: 5, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "",
//                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new skills.controller.request.model.SkillRequest(name: "Create Skills with multiple versions", skillId: "CreateSkillVersion", subjectId: "Skills", projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "",
                        helpUrl: "/dashboard/user-guide/skills.html#skills-versioning"
                ),


        ]

        skills.each {
            projectAdminStorageService.saveSkill(it)
        }

    }
}
