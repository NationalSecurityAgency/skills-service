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
import org.apache.commons.io.IOUtils
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component
import skills.controller.request.model.SkillRequest
import skills.storage.model.SkillDef

import static skills.services.inception.InceptionProjectService.*

@Component
@Slf4j
class InceptionSkills {

    private Map<String,String> loadDescFileLookup() {
        ResourcePatternResolver scanner = new PathMatchingResourcePatternResolver()
        Resource[] resources = scanner.getResources("classpath*:/inception/*.md")
        log.debug("Found [{}] inception descriptions", resources)
        Map<String,String> descLookup = [:]
        for (Resource r : resources) {
            String desc = IOUtils.toString(r.getInputStream(), "UTF-8")
            descLookup[r.filename] = desc
        }
        return descLookup;
    }

    List<SkillRequest> getAllSkills() {
        List<SkillRequest> res = []
        res.addAll(getProjectSubjectSkills())
        res.addAll(getDashboardSubjectSkills())
        res.addAll(getSkillsSubjectSkills())

        res.each {
            it.enabled = "true"
        }

        Map<String,String> descLookup = loadDescFileLookup()
        List<String> mappedDescriptions = []
        res.each {
            if (it.description?.startsWith("Lookup:")) {
                String fileToLookup = it.description.split(":")[1]
                String desc = descLookup.get(fileToLookup)
                if (!desc) {
                    throw new IllegalStateException("The following file must exist in src/main/resources/inception/${fileToLookup}")
                }
                it.description = desc
                log.debug("Assigned description for skill [{}] from file [{}]", it.skillId, fileToLookup)
                mappedDescriptions.add(fileToLookup)
            }
        }
        Set<String> descFilesNotMapped = descLookup.findAll { !mappedDescriptions.contains(it.key)}.keySet()
        if (descFilesNotMapped) {
            throw new IllegalStateException("There are files that are NOT mapped to a description in ${this.class.simpleName}: ${descFilesNotMapped}")
        }

        return res
    }

    String getHash() {
        String allSkills = getAllSkills().collect {it.toString() }.join("\n")
        return allSkills.md5()
    }

    private List<SkillRequest> getProjectSubjectSkills() {
        return [
                new SkillRequest(name: "Create Project", skillId: "CreateProject", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20, numPerformToCompletion: 2,
                        description: "Lookup:Desc_CreateProject.md",
                        helpUrl: "/dashboard/user-guide/projects.html",
                        iconClass: "fa-solid fa-list-check"
                ),
                new SkillRequest(name: "Create Subject", skillId: "CreateSubject", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 3,
                        description: "Lookup:Desc_CreateSubject.md",
                        helpUrl: "/dashboard/user-guide/subjects.html",
                        iconClass: "fa-solid fa-cubes"
                ),

                new SkillRequest(name: "Create Subject that is initially Hidden", skillId: "CreateSubjectInitiallyHidden", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        numPerformToCompletion: 3,
                        description: "Lookup:Desc_CreateSubjectInitiallyHidden.md",
                        helpUrl: "/dashboard/user-guide/subjects.html#subject-creation-lifecycle",
                        iconClass: "fa-solid fa-eye-slash"
                ),
                new SkillRequest(name: "Configure Root Help Url", skillId: "ConfigureProjectRootHelpUrl", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "Lookup:Desc_ConfigureProjectRootHelpUrl.md",
                        helpUrl: "/dashboard/user-guide/projects.html#settings",
                        iconClass: "fa-solid fa-circle-question"
                ),
                new SkillRequest(name: "Add Project Administrator", skillId: "AddAdmin", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Lookup:Desc_AddAdmin.md",
                        helpUrl: "/dashboard/user-guide/projects.html#access",
                        iconClass: "fa-solid fa-users-cog"
                ),
                new SkillRequest(name: "Create Badge", skillId: "CreateBadge", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_CreateBadge.md",
                        helpUrl: "/dashboard/user-guide/badges.html",
                        iconClass: "fa-solid fa-award"
                ),
                new SkillRequest(name: "Create Gem", skillId: "CreateGem", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Gems are special badges that are only available within a configured time window. Users must complete all of the gem's skills within that window in order to earn this precious stone! " +
                                "To create a gem navigate to ``Project -> Badges`` and then click  ``Badge +`` button. " +
                                "You can then enable and configure a gem within the badge edit modal by selecting the ``Enable Gem Feature``",
                        helpUrl: "/dashboard/user-guide/badges.html#gem",
                        iconClass: "fa-solid fa-gem"
                ),
                new SkillRequest(name: "Assign Badge or Gem Skills", skillId: "AssignGemOrBadgeSkills", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 5, // up-to 1 per day
                        numPerformToCompletion: 10,
                        description: "Once a badge or a gem is created the next step is to assign skills to that badge/gem. " +
                                "Badges are a collection of skills and when all of the skills are accomplished that badge is earned.",
                        helpUrl: "/dashboard/user-guide/badges.html",
                        iconClass: "fa-solid fa-link"
                ),
                new SkillRequest(name: "Visit Subjects", skillId: "VisitSubjects", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Lookup:Desc_VisitSubjects.md",
                        helpUrl: "/dashboard/user-guide/subjects.html",
                        iconClass: "fa-solid fa-cubes"
                ),
                new SkillRequest(name: "Visit Subject's Skills", skillId: "VisitSkillsForASubject", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 30,
                        description: '''Navigate to `Project -> Subjects -> Subject`

Projects are composed of Subjects which are made of Skills (or Skill Groups) and a single skill defines a training unit within the gamification framework. To complete a skill, users may need to perform the same action multiple times - repetition is important for retention after all. A Skill definition specifies how many times a skill has to be performed. Each occurrence is called a Skill Event.''',
                        helpUrl: "/dashboard/user-guide/subjects.html",
                        iconClass: "fa-solid fa-graduation-cap"
                ),
                new SkillRequest(name: "Visit Subject Levels", skillId: "VisitSubjectLevels", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_VisitSubjectLevels.md",
                        helpUrl: "/dashboard/user-guide/levels.html",
                        iconClass: "fa-solid fa-trophy"
                ),
                new SkillRequest(name: "Visit Subject Users", skillId: "VisitSubjectUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_VisitSubjectUsers.md",
                        iconClass: "fa-solid fa-users"
                ),
                new SkillRequest(name: "Visit Subject Metrics", skillId: "VisitSubjectMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_VisitSubjectMetrics.md",
                        iconClass: "fa-solid fa-chart-bar"
                ),
                new SkillRequest(name: "Visit Badges", skillId: "VisitBadges", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Badges add another facet to the overall gamification profile and allow you to further reward your users by awarding these prestigious symbols. Badges are a collection of skills and when all of the skills are accomplished that badge is earned." +
                                "\n\nNavigate to ``Project -> Badges``",
                        helpUrl: "/dashboard/user-guide/badges.html",
                        iconClass: "fa-solid fa-award"
                ),
                new SkillRequest(name: "Visit Badge Page", skillId: "VisitSingleBadgePage", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: '''To earn points please navigate to `Project -> Badges -> Badge`.

Badges add another facet to the overall gamification profile and allows you to further reward your users by providing these prestigious symbols. Badges are a collection of skills and when all of the skills are accomplished that badge is earned.
''',
                        helpUrl: "/dashboard/user-guide/badges.html",
                        iconClass: "fa-solid fa-medal"
                ),
                new SkillRequest(name: "Visit Badge Users", skillId: "VisitBadgeUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Lookup:Desc_VisitBadgeUsers.md",
                        iconClass: "fa-solid fa-users-between-lines"
                ),
                new SkillRequest(name: "Visit Project Learning Path", skillId: "VisitProjectDependencies", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 4 per day
                        numPerformToCompletion: 6,
                        description: "Lookup:Desc_VisitProjectDependencies.md",
                        helpUrl: "/dashboard/user-guide/dependencies.html",
                        iconClass: "fa-solid fa-project-diagram"
                ),
                new SkillRequest(name: "Visit Project Levels", skillId: "VisitProjectLevels", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_VisitProjectLevels.md",
                        iconClass: "fa-solid fa-trophy"
                ),
                new SkillRequest(name: "Visit Project Users", skillId: "VisitProjectUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitProjectUsers.md",
                        iconClass: "fa-solid fa-arrows-down-to-people"
                ),
                new SkillRequest(name: "Visit Project Metrics", skillId: "VisitProjectStats", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitProjectStats.md",
                        iconClass: "fa-solid fa-chart-area"
                ),
                new SkillRequest(name: "Visit Project's Achievements Metrics", skillId: "VisitProjectUserAchievementMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitProjectUserAchievementMetrics.md",
                        iconClass: "fa-solid fa-chart-line"
                ),
                new SkillRequest(name: "Visit Project's Subjects Metrics", skillId: "VisitProjectSubjectMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitProjectSubjectMetrics.md",
                        iconClass: "fa-solid fa-chart-pie"
                ),
                new SkillRequest(name: "Visit Project's Skill Metrics", skillId: "VisitProjectSkillMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitProjectSkillMetrics.md",
                        iconClass: "fa-solid fa-chart-simple"
                ),
                new SkillRequest(name: "Visit Project Settings", skillId: "VisitProjectSettings", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_VisitProjectSettings.md",
                        helpUrl: "/dashboard/user-guide/projects.html#settings",
                        iconClass: "fa-solid fa-cogs"
                ),
                new SkillRequest(name: "Visit Project Access Management", skillId: "VisitProjectAccessManagement", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        description: "Lookup:Desc_VisitProjectAccessManagement.md",
                        helpUrl: "/dashboard/user-guide/projects.html#access",
                        iconClass: "fa-solid fa-shield-alt"
                ),
                new SkillRequest(name: "Visit Self Report Approval Page", skillId: "VisitSelfReport", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_VisitSelfReport.md",
                        helpUrl: "/dashboard/user-guide/self-reporting.html#self-reporting",
                        iconClass: "fa-solid fa-user-check"
                ),
                new SkillRequest(name: "Visit Project Issues Page", skillId: "VisitProjectErrors", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        description: "Lookup:Desc_VisitProjectErrors.md",
                        helpUrl: "/dashboard/user-guide/issues.html",
                        iconClass: "fa-solid fa-exclamation-triangle"
                ),
                new SkillRequest(name: "Preview Skills Display for Project", skillId: "PreviewProjectClientDisplay", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 4,
                        pointIncrementInterval : 60 * 12,
                        numMaxOccurrencesIncrementInterval: 1,
                        description: "Lookup:Desc_PreviewProjectClientDisplay.md",
                        iconClass: "fa-solid fa-chalkboard-user"
                ),
                new SkillRequest(name: 'Visit Contact Users Page', skillId: 'VisitContactUsers', subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12,
                        numMaxOccurrencesIncrementInterval: 1,
                        description: "Lookup:Desc_VisitContactUsers.md",
                        helpUrl: "/dashboard/user-guide/contact-project-users.html",
                        iconClass: "fa-solid fa-envelopes-bulk"
                ),
                new SkillRequest(name: 'Copy Project', skillId: 'CopyProject', subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_CopyProject.md",
                        helpUrl: "/dashboard/user-guide/projects.html#copy-project",
                        iconClass: "fa-solid fa-copy"
                ),
                new SkillRequest(name: 'Share Project', skillId: 'ShareProject', subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_ShareProject.md",
                        helpUrl: "/dashboard/user-guide/projects.html#share-project",
                        iconClass: "fa-solid fa-people-arrows"
                ),
                new SkillRequest(name: 'Change Subject Display Order', skillId: 'ChangeSubjectDisplayOrder', subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12,
                        numMaxOccurrencesIncrementInterval: 1,
                        description: "Lookup:Desc_ChangeSubjectDisplayOrder.md",
                        iconClass: "fa-solid fa-arrow-down-short-wide"
                ),
                new SkillRequest(name: 'Change Badge Display Order', skillId: 'ChangeBadgeDisplayOrder', subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12,
                        numMaxOccurrencesIncrementInterval: 1,
                        description: "Lookup:Desc_ChangeBadgeDisplayOrder.md",
                        iconClass: "fa-solid fa-arrow-up-9-1"
                ),

        ]
    }

    private List<SkillRequest> getDashboardSubjectSkills() {
        return [
                new SkillRequest(name: "Visit Dashboard Skills", skillId: "VisitDashboardSkills", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60*12,
                        numMaxOccurrencesIncrementInterval: 5,
                        numPerformToCompletion: 20,
                        description: "Lookup:Desc_VisitDashboardSkills.md",
                        helpUrl: "/dashboard/user-guide/inception.html",
                        iconClass: "fa-solid fa-graduation-cap"
                ),
                new SkillRequest(name: "Learn About Level Management", skillId: "AddOrModifyLevels", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Lookup:Desc_AddOrModifyLevels.md",
                        selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                        helpUrl: "/dashboard/user-guide/levels.html",
                        iconClass: "fa-solid fa-stairs"
                ),
                new SkillRequest(name: "Visit User Settings", skillId: "VisitUserSettings", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Visit user settings by clicking on the ``User`` icon on the top right and selecting ``Settings``.",
                        iconClass: "fa-solid fa-users-gear",
                ),
                new SkillRequest(name: "Visit Client Display", skillId: "VisitClientDisplay", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 3, // up-to 25 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitClientDisplay.md",
                        helpUrl: "/dashboard/user-guide/users.html#skills-display-client-display",
                        iconClass: "fa-solid fa-users-rectangle"
                ),
                new SkillRequest(name: "Visit User Performed Skills", skillId: "VisitUserPerformedSkills", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_VisitUserPerformedSkills.md",
                        helpUrl: "/dashboard/user-guide/users.html#performed-skills",
                        iconClass: "fa-solid fa-person-circle-check"
                ),
                new SkillRequest(name: "Visit My Preferences Page", skillId: "VisitMyPreferences", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_VisitMyPreferences.md",
                        iconClass: "fa-solid fa-sliders"
                ),
                new SkillRequest(name: "Share SkillTree Success Story", skillId: "ShareSkillTreeSuccessStory", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 150,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                        justificationRequired: Boolean.TRUE.toString(),
                        description: "Lookup:Desc_ShareSkillTreeSuccessStory.md",
                        iconClass: "fa-solid fa-thumbs-up"
                ),
                new SkillRequest(name: "Spread the Word or Teach", skillId: "SpreadtheWordorTeach", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 100,
                        numPerformToCompletion: 3,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                        justificationRequired: Boolean.TRUE.toString(),
                        description: "Lookup:Desc_SpreadtheWordorTeach.md",
                        iconClass: "fa-solid fa-person-chalkboard"
                ),
                new SkillRequest(name: "Suggest Tool Integration", skillId: "SuggestToolIntegration", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 150,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                        justificationRequired: Boolean.TRUE.toString(),
                        description: "Lookup:Desc_SuggestToolIntegration.md",
                        helpUrl: "/skills-client/",
                        iconClass: "fa-solid fa-server"
                ),
                new SkillRequest(name: "Suggest a Feature", skillId: "SuggestFeature", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 50,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                        description: '''We are always looking to improve SkillTree platform and there is no better way to do that than as a community. No matter how small or large your feature idea is please do not hesitate to reach out to the SkillTree team! 
''',
                        iconClass: "fa-solid fa-plane-up"
                ),
                new SkillRequest(name: "Create Global Badge", skillId: "CreateGlobalBadge", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_CreateGlobalBadge.md",
                        helpUrl: "/dashboard/user-guide/global-badges.html",
                        iconClass: "fa-solid fa-award"
                ),
                new SkillRequest(name: "Export to Catalog", skillId: "ExporttoCatalog", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 50,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_ExporttoCatalog.md",
                        helpUrl: "/dashboard/user-guide/skills-catalog.html",
                        iconClass: "fa-solid fa-book-atlas"
                ),
                new SkillRequest(name: "Project Access Options", skillId: "ProjectAccessOptions", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                        description: "Lookup:Desc_ProjectAccessOptions.md",
                        helpUrl: "/dashboard/user-guide/projects.html#access",
                        iconClass: "fa-solid fa-user-shield"
                ),
        ]
    }

    private List<SkillRequest> getSkillsSubjectSkills() {
        return  [
                new SkillRequest(name: "Create Skill", skillId: "CreateSkill", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 25, // up-to 25 per day
                        numPerformToCompletion: 50,
                        description: "Lookup:Desc_CreateSkill.md",
                        helpUrl: "/dashboard/user-guide/skills.html",
                        iconClass: "fa-solid fa-graduation-cap"
                ),
                new SkillRequest(name: "Create Skill that is initially Hidden", skillId: "CreateSkillInitiallyHidden", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_CreateSkillInitiallyHidden.md",
                        helpUrl: "/dashboard/user-guide/skills.html#skill-creation-lifecycle",
                        iconClass: "fa-solid fa-eye-slash"
                ),
                new SkillRequest(name: "Create Skill with disabled Time Window", skillId: "CreateSkillDisabledTimeWindow", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_CreateSkillDisabledTimeWindow.md",
                        helpUrl: "/dashboard/user-guide/skills.html#time-window",
                        iconClass: "fa-solid fa-calendar-days"
                ),
                new SkillRequest(name: "Create Skill with Max Occurrences Within Time Window", skillId: "CreateSkillMaxOccurrencesWithinTimeWindow", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_CreateSkillMaxOccurrencesWithinTimeWindow.md",
                        helpUrl: "/dashboard/user-guide/skills.html#time-window",
                        iconClass: "fa-solid fa-business-time"
                ),
                new SkillRequest(name: "Create Skill with Help Url", skillId: "CreateSkillHelpUrl", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 5, // up-to 1 per day
                        numPerformToCompletion: 10,
                        description: "Lookup:Desc_CreateSkillHelpUrl.md",
                        helpUrl: "/dashboard/user-guide/skills.html",
                        iconClass: "fa-solid fa-clipboard-question"
                ),
                new SkillRequest(name: "Visit Skill Overview", skillId: "VisitSkillOverview", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 30,
                        description: "Lookup:Desc_VisitSkillOverview.md",
                        helpUrl: "/dashboard/user-guide/skills.html",
                        iconClass: "fa-solid fa-list"
                ),
                new SkillRequest(name: "Visit Skill Users", skillId: "VisitSkillUsers", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitSkillUsers.md",
                        iconClass: "fa-solid fa-users-viewfinder"
                ),
                new SkillRequest(name: "Create Learning Path", skillId: "CreateSkillDependencies", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 3, // up-to 1 per day
                        numPerformToCompletion: 6,
                        description: "Lookup:Desc_CreateSkillDependencies.md",
                        helpUrl: "/dashboard/user-guide/dependencies.html",
                        iconClass: "fa-solid fa-project-diagram"
                ),
                new SkillRequest(name: "Create Cross-Project Learning Path", skillId: "CreateCrossProjectSkillDependencies", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_CreateCrossProjectSkillDependencies.md",
                        helpUrl: "/dashboard/user-guide/dependencies.html#cross-project-dependencies",
                        iconClass: "fa-solid fa-handshake"
                ),
                new SkillRequest(name: "Manually Add Skill Event", skillId: "ManuallyAddSkillEvent", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_ManuallyAddSkillEvent.md",
                        helpUrl: "/dashboard/user-guide/skills.html#manually-add-skill-event",
                        iconClass: "fa-solid fa-user-check"
                ),
                new SkillRequest(name: "Visit Skill Metrics", skillId: "VisitSkillStats", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_VisitSkillStats.md",
                        iconClass: "fa-solid fa-chart-column"
                ),
                new SkillRequest(name: "Expand Skill Details on Skills Page", skillId: "ExpandSkillDetailsSkillsPage", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_ExpandSkillDetailsSkillsPage.md",
                        helpUrl: "/dashboard/user-guide/skills.html",
                        iconClass: "fa-solid fa-arrows-left-right-to-line"
                ),
                new SkillRequest(name: "Self Reporting with Honor", skillId: "SelfReportHonorExample", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 3,
                        description: "Lookup:Desc_SelfReportHonorExample.md",
                        selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                        helpUrl: "/dashboard/user-guide/self-reporting.html",
                        iconClass: "fa-solid fa-chess-knight"
                ),
                new SkillRequest(name: "Self Reporting with Approval", skillId: "SelfReportApprovalExample", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 3,
                        description: "Lookup:Desc_SelfReportApprovalExample.md",
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                        helpUrl: "/dashboard/user-guide/self-reporting.html",
                ),
                new SkillRequest(name: "Search and Navigate directly to a skill", skillId: "SearchandNavigatedirectlytoaskill", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Lookup:Desc_SearchandNavigatedirectlytoaskill.md",
                        iconClass: "fa-solid fa-user-graduate"
                ),
                new SkillRequest(name: "Create Aesthetically Pleasing Description", skillId: "CreateVisuallyAppealingDescription", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 50,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 1,
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                        justificationRequired: Boolean.TRUE.toString(),
                        description: "Lookup:Desc_CreateVisuallyAppealingDescription.md",
                        helpUrl: "/dashboard/user-guide/rich-text-editor.html",
                        iconClass: "fa-solid fa-image"
                ),
                new SkillRequest(name: "Copy Skill", skillId: "CopySkill", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_CopySkill.md",
                        helpUrl: "/dashboard/user-guide/skills.html#copy-skill",
                        iconClass: "fa-solid fa-clone"
                ),
                new SkillRequest(name: "Create Skill Group", skillId: "CreateSkillGroup", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Lookup:Desc_CreateSkillGroup.md",
                        helpUrl: "/dashboard/user-guide/skills-groups.html",
                        iconClass: "fa-solid fa-layer-group"
                ),
                new SkillRequest(name: "Change Skill Display Order", skillId: "ChangeSkillDisplayOrder", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 3, // up-to 1 per day
                        numPerformToCompletion: 6,
                        description: "Lookup:Desc_ChangeSkillDisplayOrder.md",
                        iconClass: "fa-solid fa-arrow-up-a-z"
                ),
                new SkillRequest(name: "Use Skills Table Additional Columns", skillId: "SkillsTableAdditionalColumns", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 3, // up-to 1 per day
                        numPerformToCompletion: 6,
                        description: "Lookup:Desc_SkillsTableAdditionalColumns.md",
                        iconClass: "fa-solid fa-table-list"
                ),
                new SkillRequest(name: "Reuse Skill", skillId: "ReuseSkill", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_ReuseSkill.md",
                        helpUrl: "/dashboard/user-guide/skills.html#same-project-skill-reuse",
                        iconClass: "fa-solid fa-recycle"
                ),
                new SkillRequest(name: "Move Skill", skillId: "MoveSkill", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        numPerformToCompletion: 4,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 1 per day
                        description: "Lookup:Desc_MoveSkill.md",
                        helpUrl: "/dashboard/user-guide/skills.html#move-skills",
                        iconClass: "fa-solid fa-truck-moving"
                ),
                new SkillRequest(name: "Import Skill from Catalog", skillId: "ImportSkillfromCatalog", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "Lookup:Desc_ImportSkillfromCatalog.md",
                        helpUrl: "/dashboard/user-guide/skills-catalog.html",
                        iconClass: "fa-solid fa-file-import"
                ),
                new SkillRequest(name: "Add or Update Skill Tags", skillId: "AddOrModifyTags", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 3,
                        description: "Lookup:Desc_AddOrModifyTags.md",
                        helpUrl: "/dashboard/user-guide/skills.html#skill-tags",
                        iconClass: "fa-solid fa-tags"
                ),
                new SkillRequest(name: "Configure Self Approval Workload", skillId: "ConfigureSelfApprovalWorkload", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        description: "Lookup:Desc_ConfigureSelfApprovalWorkload.md",
                        helpUrl: "/dashboard/user-guide/self-reporting.html#split-approval-workload",
                        iconClass: "fa-solid fa-arrows-split-up-and-left"
                ),
        ]
    }
}
