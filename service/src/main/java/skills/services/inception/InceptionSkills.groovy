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


import org.springframework.stereotype.Component
import skills.controller.request.model.SkillRequest

import static skills.services.inception.InceptionProjectService.*

@Component
class InceptionSkills {

    List<SkillRequest> getAllSkills() {
        List<SkillRequest> res = []
        res.addAll(getProjectSubjectSkills())
        res.addAll(getDashboardSubjectSkills())
        res.addAll(getSkillsSubjectSkills())

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
                        description: "Project is an overall container that represents skills' ruleset for a single application with gamified training. " +
                                "Project's administrator(s) manage training skills definitions, subjects, levels, dependencies and other attributes " +
                                "that make up application's training profile. To create project click 'Project +' button.",
                        helpUrl: "/dashboard/user-guide/projects.html"
                ),
                new SkillRequest(name: "Create Subject", skillId: "CreateSubject", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 3,
                        description: "Subjects are a way to group and organize skill definitions and skill training profile. To create a subject navigate to ``Project -> Subjects`` and then click ``Subject +`` button.",
                        helpUrl: "/dashboard/user-guide/subjects.html"
                ),
                new SkillRequest(name: "Configure Root Help Url", skillId: "ConfigureProjectRootHelpUrl", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "Configure project's 'Root Help Url' by navigating to ``Project -> Settings```. " +
                                "Skill definition's `Help Url/Path` will be treated relative to this `Root Help Url`.",
                        helpUrl: "/dashboard/user-guide/projects.html#settings"
                ),
                new SkillRequest(name: "Add Project Administrator", skillId: "AddAdmin", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Add another project administrator under ``Project -> Access`` so you don't get too lonely.",
                        helpUrl: "/dashboard/user-guide/access-management.html"
                ),
                new SkillRequest(name: "Create Badge", skillId: "CreateBadge", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 5,
                        description: "Badges add another facet to the overall gamificaiton profile and allows to further reward your users by providing these prestigious symbols. " +
                                "Badges are a collection of skills and when all of the skills are accomplished that badge is earned. " +
                                "To create a badge navigate to ``Project -> Badges`` and then click  ``Badge +`` button.",
                        helpUrl: "/dashboard/user-guide/badges.html"
                ),
                new SkillRequest(name: "Create Gem", skillId: "CreateGem", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Gems are special badges that are only available within configure time window. Users must complete all of the gem's skills within that window in order to earn this precious stone! " +
                                "To create a gem navigate to ``Project -> Badges`` and then click  ``Badge +`` button. " +
                                "You can then enable and configure a gem within badge edit modal by selecting Enable Gem Feature",
                        helpUrl: "/dashboard/user-guide/badges.html#gem"
                ),
                new SkillRequest(name: "Assign Badge or Gem Skills", skillId: "AssignGemOrBadgeSkills", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 5, // up-to 1 per day
                        numPerformToCompletion: 10,
                        description: "Once a badge or a gem is created the next step is to assign skills to that badge/gem. " +
                                "Badges are a collection of skills and when all of the skills are accomplished that badge is earned.",
                        helpUrl: "/dashboard/user-guide/badges.html"
                ),
                new SkillRequest(name: "Visit Subjects", skillId: "VisitSubjects", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Subjects``",
                ),
                new SkillRequest(name: "Visit Subject's Skills", skillId: "VisitSkillsForASubject", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 30,
                        description: "Navigate to ``Project -> Subjects -> Subject``",
                        helpUrl: "/dashboard/user-guide/subjects.html"
                ),
                new SkillRequest(name: "Visit Subject Levels", skillId: "VisitSubjectLevels", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Subjects -> Subject -> Levels``",
                ),
                new SkillRequest(name: "Visit Subject Users", skillId: "VisitSubjectUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Subjects -> Subject -> Users``",
                ),
                new SkillRequest(name: "Visit Subject Metrics", skillId: "VisitSubjectMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Subjects -> Subejct -> Metrics``",
                ),
                new SkillRequest(name: "Visit Badges", skillId: "VisitBadges", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges``",
                ),
                new SkillRequest(name: "Visit Badge Page", skillId: "VisitSingleBadgePage", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 24, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges -> Badge``",
                ),
                new SkillRequest(name: "Visit Badge Users", skillId: "VisitBadgeUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges -> Badge -> Users``",
                ),
                new SkillRequest(name: "Visit Badge Metrics", skillId: "VisitBadgeStats", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Navigate to ``Project -> Badges -> Badge -> Metrics``",
                ),
                new SkillRequest(name: "Visit Project Dependencies", skillId: "VisitProjectDependencies", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 4 per day
                        numPerformToCompletion: 6,
                        description: "Navigate to ``Project -> Dependencies``",
                ),
                new SkillRequest(name: "Visit Cross Project Skills", skillId: "VisitProjectCrossProjectSkills", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 6,
                        description: "Navigate to ``Project -> Cross Projects``",
                ),
                new SkillRequest(name: "Visit Project Levels", skillId: "VisitProjectLevels", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Levels``",
                ),
                new SkillRequest(name: "Visit Project Users", skillId: "VisitProjectUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Navigate to ``Project -> Users``",
                ),
                new SkillRequest(name: "Visit Project Metrics", skillId: "VisitProjectStats", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Navigate to ``Project -> Metrics``",
                ),
                new SkillRequest(name: "Visit Project Settings", skillId: "VisitProjectSettings", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Navigate to ``Project -> Settings``",
                ),
                new SkillRequest(name: "Visit Project Access Management", skillId: "VisitProjectAccessManagement", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        description: "Navigate to ``Project -> Settings``",
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
                        description: "Skills dashboard gamifies training for the dashboard itself and we call it 'Inception'. " +
                                "All the dashboard users will have a button on the top right of the application which navigates to your skills profile. " +
                                "This button will also display your current level standing.",
                        helpUrl: "/dashboard/user-guide/inception.html"
                ),
                new SkillRequest(name: "Add or Modify Levels", skillId: "AddOrModifyLevels", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: '''Levels are users' achievement path - the overall goal of the application usage is to achieve the highest level. Levels are tracked for the entire project as well as for each subject which provides users many ways to progress forward.
Skills dashboard supports two flexible ways to manage levels:
* Percentage Based (default): Each level is defined as a percentage of overall points and the actual level's point range is calculated based on the percentage.
* Point based: Level's from and to points are configured explicitly.

To achieve this skill simply  please study available percentage based and point-based strategy and make several modifications to levels as needed.'''.toString(),
                        helpUrl: "/dashboard/user-guide/levels.html",
                ),
                new SkillRequest(name: "Visit User Settings", skillId: "VisitUserSettings", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: "Visit user settings by clicking on the ``User`` icon on the top right and selecting ``Settings``.",
                ),
                new SkillRequest(name: "Visit Client Display", skillId: "VisitClientDisplay", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 25, // up-to 25 per day
                        numPerformToCompletion: 50,
                        description: '''You can see what user's skills profile and progress display would like for a particular user by navigating to a specific user page ``Project -> Users -> Select a User -> Client Display``. 
This is the same exact plugable Skills Display that you would be embedding into your application so it can serve as a preview, an ability to look through that user's point of view.  
Client display will depict project skills profile and users points at that exact moment.

We suggest to often visit Skills Display view while building skill profile to better understand how your users will view gamificaiton profile and their progress. 
'''.toString(),
                        helpUrl: "/dashboard/user-guide/users.html#skills-display-client-display"
                ),
                new SkillRequest(name: "Visit Client Display for Earlier Version", skillId: "VisitClientDisplayForEarlierVersion", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 1,
                        description: "If your gamification profile is utilizing Skills Versioning then you can view what the Skills Display would look like for a specific version by selecting a different version in the drop-down located on the top-right of the page.",
                        helpUrl: "/dashboard/user-guide/users.html#skills-display-client-display",
                ),
                new SkillRequest(name: "Visit User Performed Skills", skillId: "VisitUserPerformedSkills", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 2,
                        description: "To see a history of user's performed skill events please visit ``Project -> Users -> Select a User -> Performed Skills``. Furthermore you have an ability to remove a single skill event if needed.",
                        helpUrl: "/dashboard/user-guide/users.html#performed-skills"
                ),
                new SkillRequest(name: "Visit User Metrics", skillId: "VisitUserStats", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 2,
                        description: "Almost every page on the skill's dashboard exposes metrics and statistics about that particular entity/concept. These metrics are very much context aware so as an example graphs and charts you see on a subject page will be for that specific subject and metrics on the project page will be for the entire project.",
                        helpUrl: "/dashboard/user-guide/metrics.html",
                ),
                new SkillRequest(name: "Visit Markdown Documentation", skillId: "VisitMarkdownDocs", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 2,
                        description: "Descriptions support markdown for subjects and skills. When creating a subject or a skill description field has a link to the markdown documentation.",
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
                        description: '''Projects are composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework. 
To complete a skill users may need to perform the same action multiple times - repetition is important for retention after all. 
A Skill definition specifies how many times a skill has to be performed and each occurrence is called a Skill Event. 
To create skill navigate to a subject and then click ``Skill +`` button.''',
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skill with disabled Time Window", skillId: "CreateSkillDisabledTimeWindow", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        numPerformToCompletion: 5,
                        description: "When 'Time Window' is disabled skill events are applied immediately.",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skill with Max Occurrences Within Time Window", skillId: "CreateSkillMaxOccurrencesWithinTimeWindow", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        numPerformToCompletion: 5,
                        description: "",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skill with Help Url", skillId: "CreateSkillHelpUrl", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 5, // up-to 1 per day
                        numPerformToCompletion: 10,
                        description: "URL pointing to a help article further depicting information about this skill or capability. Please note that this property works in conjunction with the Root Help Url project setting.",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skills with multiple versions", skillId: "CreateSkillVersion", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "Skill versioning is a mechanism that allows addition of the new skills without affecting existing software running with an older skill profile. Versioning is mostly pertinent to the Display Libraries that visualize the skill profile for the version they were declared with.",
                        helpUrl: "/dashboard/user-guide/skills.html#skills-versioning"
                ),
                new SkillRequest(name: "Visit Skill Overview", skillId: "VisitSkillOverview", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 30,
                        description: "Visit ``Skill Overview``. Navigate to ``Project -> Subject -> Skill -> Overview``",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Visit Skill Dependencies", skillId: "VisitSkillDependencies", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 1 per day
                        numPerformToCompletion: 6,
                        description: "Dependencies add another facet to the overall gamification profile which forces users to complete skills in the specified order. If you set up Skill A to depend on the completion of Skill B then no points will be awarded toward Skill A until Skill B is fully accomplished.Keep in mind that Skill B must be fully completed first before any points will be awarded toward Skill A. Navigate to ``Project -> Subject -> Skill -> Dependencies``",
                        helpUrl: "/dashboard/user-guide/dependencies.html"
                ),
                new SkillRequest(name: "Visit Skill Users", skillId: "VisitSkillUsers", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Visit ``Skill Dependencies``. Navigate to ``Project -> Subject -> Skill -> Users``",
                        helpUrl: "/dashboard/user-guide/users.html"
                ),
                new SkillRequest(name: "Create Skill Dependencies", skillId: "CreateSkillDependencies", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 3, // up-to 1 per day
                        numPerformToCompletion: 6,
                        description: "Dependencies add another facet to the overall gamification profile which forces users to complete skills in the specified order. If you set up Skill A to depend on the completion of Skill B then no points will be awarded toward Skill A until Skill B is fully accomplished.Keep in mind that Skill B must be fully completed first before any points will be awarded toward Skill A. To add a dependency navigate to ``Project -> Subject -> Skill -> Dependencies``",
                        helpUrl: "/dashboard/user-guide/dependencies.html"
                ),

                new SkillRequest(name: "Create Cross-Project Skill Dependencies", skillId: "CreateCrossProjectSkillDependencies", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "Cross-Project Dependencies facilitate cross-application training and enable users to become domain experts across several applications. These dependencies are critical when actions are required to be performed in more than one tool in order to complete a task.",
                        helpUrl: "/dashboard/user-guide/dependencies.html#cross-project-dependencies"
                ),

                new SkillRequest(name: "Manually Add Skill Event", skillId: "ManuallyAddSkillEvent", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Manually Add Skill Events. Navigate to ``Project -> Subject -> Skill -> Add Event``",
                        helpUrl: "/dashboard/user-guide/skills.html#manually-add-skill-event"
                ),
                new SkillRequest(name: "Visit Skill Metrics", skillId: "VisitSkillStats", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "Visit ``Skill Dependencies``. Navigate to ``Project -> Subject -> Skill -> Metrics``",
                ),
                new SkillRequest(name: "Expand Skill Details on Skills Page", skillId: "ExpandSkillDetailsSkillsPage", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 5,
                        description: "On the Skills Page click on ``+`` to expand a single row. ",
                ),
        ]
    }
}
