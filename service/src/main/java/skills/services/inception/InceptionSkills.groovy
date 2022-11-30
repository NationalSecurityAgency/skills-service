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
import skills.storage.model.SkillDef

import static skills.services.inception.InceptionProjectService.*

@Component
class InceptionSkills {

    List<SkillRequest> getAllSkills() {
        List<SkillRequest> res = []
        res.addAll(getProjectSubjectSkills())
        res.addAll(getDashboardSubjectSkills())
        res.addAll(getSkillsSubjectSkills())

        res.each {
            it.enabled = "true"
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
                        description: '''Project is an overall container that represents skills' ruleset for a single application with gamified training. Project's administrator(s) manage training skills definitions, subjects, levels, dependencies and other attributes that make up an application's training profile. To create a project click the `Project +` button.

A Project is composed of Subjects which are made up of Skills and a single skill defines a training unit within the gamification framework. Once the project is created you have an empty canvas on which to compose and manage your application's training profile. Generally the next step is to create a number of Subjects and then start constructing Skill definitions within those Subjects.

The Dashboard user that creates a project is automatically granted the role of administrator of that project. Project administrators enjoy the following benefits:

| Function | Explanation |
| -------- | ----------- |
| Subjects | Add, edit or remove Subjects |
| Skills | Add, edit or remove Skill definitions |
| Self Reporting | Self Report is a feature that empowers users to mark skills as completed directly in the SkillTree dashboard OR through the embedded Skills Display component. |
| Access Management | Manage Project's Admin/Approver roles and Private Invite Only access |
| Badges | Add, edit or remove Project's Badges |
| Levels | Customize number of Levels and their attributes |
| Dependencies | Specify the order of Skills completion. For example Skill A must be completed before Skill B can be attempted |
| Cross-project Dependencies | Create and manage Skill dependencies across multiple Projects which practically equates to cross-application Skills |
| Contact Users | Communicate with users of your Project |
| Metrics | Charts and graph. These are page specific - Project, Subject, Badge, and User will have stats specifically for those pages |
| Issues | Errors related to the Project such as non-existant Skills that have been reported |
| Settings | Project level settings |
''',
                        helpUrl: "/dashboard/user-guide/projects.html"
                ),
                new SkillRequest(name: "Create Subject", skillId: "CreateSubject", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 3,
                        description: '''A Project is composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework. Subjects are a way to group and organize skill definitions within a gameified training profile.

Subjects offer the following features:

* **Skills** \\- create and manage Skill definitions\\.
* **Levels Model** \\- Subject specific level model definition so users are awarded subject level achievements\\.
* **Icons** \\- assign subject specific icon based on fontawesome or material icon sets or use a custom icon\\.
* **Stats** \\- Subject specific charts and graph and user stats\\.

To create a Subject navigate to `Project -> Subjects` and then click the `Subject +` button.

> To navigate to a specific skill you can drill down into a subject card or use the `Search and Navigate directly to a skill` component

##### Best Practices

* Use the same icon style/set for all subjects. We support font awesome, material and custom icons
* Strive for each subject to have a similar number of points. The `Project -> Subjects` page shows subject cards and each card exposes the `Points %` which reflects the subject's percentage of the total available Project points.
''',
                        helpUrl: "/dashboard/user-guide/subjects.html"
                ),
                new SkillRequest(name: "Configure Root Help Url", skillId: "ConfigureProjectRootHelpUrl", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50, pointIncrementInterval: 8, numPerformToCompletion: 1,
                        description: "Configure project's 'Root Help Url' by navigating to ``Project -> Settings```. " +
                                '''Configure project's **Root Help Url** by navigating to `Project -> Settings`. Skill definition's Help Url/Path will be treated as relative to this `Root Help Url`.

Skill definition's `Help Url/Path` will be treated relative to this `Root Help Url`. For example, if

* `Root Help Url` = `http://www.myHelpDocs.com`
* and a Skill definition's `Help Url` = `/important/article`

then the client display will concatenate `Root Help Url` and `Help Url` to produce `http://www.myHelpDocs.com/important/article`.''',
                        helpUrl: "/dashboard/user-guide/projects.html#settings"
                ),
                new SkillRequest(name: "Add Project Administrator", skillId: "AddAdmin", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: '''The Project Access page supports adding or removing Project Administrators, Project Approvers, and inviting users to join a project if the project has been configured as an Invite Only project as well as revoking a user's access.

To add and remove project Administrators and Approvers navigate to `Project -> Access` page.

You must have an Admin role in order to manage other Admin and/or Approver users for a project. There supported project roles are:

* <strong>Admin</strong>: enables management of the training profile for that project such as creating and modifying subjects, skills, badges, etc.
* <strong>Approver</strong>: allowed to approve and deny Self Reporting approval requests while only getting a read-only view of the project.''',
                        helpUrl: "/dashboard/user-guide/access-management.html"
                ),
                new SkillRequest(name: "Create Badge", skillId: "CreateBadge", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 0, // disable Time Window
                        numPerformToCompletion: 5,
                        description: '''Badges add another facet to the overall gamificaiton profile and provide a mechanism to further reward your users by awarding these prestigious symbols. Badges are a collection of skills and when all of the skills are accomplished that badge is earned. To create a badge navigate to `Project -> Badges` and then click `Badge +` button.

By default, when badges are created, they are in a disabled state. Disabled badges will not show up in the client display, nor can they be achieved by users. This is to allow all dependencies to be added to the badge before a user can trigger achievement. When a badge is published, all users with existing achievements that meet the Badge criteria will be immediately awarded that badge.

> A Badge can only be published one time. Once a Badge has Gone Live, it can no longer be placed into a disabled state

Creating badges is simple:

1. Navigate to `Project -> Badges` and click `Badge +`
    * You can (and should) assign an Icon to your badge.
2. Once a badge is created you can assign existing skills to that badge under `Project -> Badge -> Skills`
    * When initially created, a badge is in a Disabled state. This is to allow dependencies to be fully added to the badge before it can be achieved by users.
3. After assigning skill dependencies to the badge, locate the badge in the `Badges` view and click the `Go Live` link on the bottom right of the Badge overview.
    * When the badge is published, any users with existing achievements that meet the badge requirements will be awarded the badge at that time.

| Property | Explanation |
| -------- | ----------- |
| Badge Name | Display name of the badge |
| Badge ID | The badge ID |
| Description | *(Optional)* Description, can be used to describe how to achieve the badge or what it's significance is. The Description property supports markdown. |
| Help URL/Path | *(Optional)* URL pointing to a help article further documenting information about this badge. Please note that this property works in conjunction with the Root Help Url project setting |
| Enable Gem Feature | *(Optional)* Enables the Gem feature, allowing badges to be only available within a specific time window |
''',
                        helpUrl: "/dashboard/user-guide/badges.html"
                ),
                new SkillRequest(name: "Create Gem", skillId: "CreateGem", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 20,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: "Gems are special badges that are only available within a configured time window. Users must complete all of the gem's skills within that window in order to earn this precious stone! " +
                                "To create a gem navigate to ``Project -> Badges`` and then click  ``Badge +`` button. " +
                                "You can then enable and configure a gem within the badge edit modal by selecting the ``Enable Gem Feature``",
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
                        description: '''A Project is composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework. Subjects are a way to group and organize skill definitions within a gamified training profile.

Navigate to `Project -> Subjects`

Subjects offer the following features:

* **Skills** \\- create and manage Skill definitions\\.
* **Levels Model** \\- Subject specific level model definition so users are awarded subject level achievements\\.
* **Icons** \\- assign subject specific icon based on fontawesome or material icon sets or use a custom icon\\.
* **Stats** \\- Subject specific charts and graph and user stats\\.''',
                ),
                new SkillRequest(name: "Visit Subject's Skills", skillId: "VisitSkillsForASubject", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 30,
                        description: '''Navigate to `Project -> Subjects -> Subject`

Projects are composed of Subjects which are made of Skills (or Skill Groups) and a single skill defines a training unit within the gamification framework. To complete a skill, users may need to perform the same action multiple times - repetition is important for retention after all. A Skill definition specifies how many times a skill has to be performed. Each occurrence is called a Skill Event.''',
                        helpUrl: "/dashboard/user-guide/subjects.html"
                ),
                new SkillRequest(name: "Visit Subject Levels", skillId: "VisitSubjectLevels", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: '''To earn points simply navigate to `Project -> Subjects -> Subject -> Levels`

Levels are users' achievement path - the overall goal of the gameified training profile is to encourage users to achieve the highest level. Levels are tracked for the entire project as well as for each subject which provides users many ways to progress forward.

The Skills dashboard supports two flexible ways to manage levels:

* **Percentage Based** <em>(default)</em>: Each level is defined as a percentage of **overall** points and the actual level's point range is calculated based on that percentage.
* <strong>Point Based</strong>: Level's from and to points are configured explicitly.

> There can be only one level strategy selected and it will apply to the **entire** project including project levels and subject levels.
> Please visit `Project -> Settings` to configure a level management strategy.

## Best practices

* Consider starting with the Percentage Based strategy as each level's points are generated from a percentage so it's quick to get started. Once the majority of skills are created and the overall points are stable then the switch to the Point based strategy may be considered.
* Initially, play around with both strategies but then select one strategy and stick with it. Both strategies work very well so it's a matter of preference.
* See the Percentage Based vs. Points Based section to determine which option works best for you.

## Percentage Based Levels

Each level is defined as a percentage of **overall** points and the actual level's point range is calculated based on that percentage. By default, projects and subjects are created with **5** levels:

| Level | Name | Percentage |
| ----- | ---- | ---------- |
| 1 | White Belt | 10% |
| 2 | Blue Belt | 25% |
| 3 | Purple Belt | 45% |
| 4 | Brown Belt | 67% |
| 5 | Black Belt | 92% |

This allows levels to be fluid as Skills are defined and **overall** points change.

## Point Based Levels

Using `Project -> Settings`, levels can be changed to a points based strategy, where each level requires the project administrator to define an explicit point range. From and to points are defined with `from` being exclusive and `to` being inclusive.
Please Note

> A project must have at least 100 total points defined before this setting can be enabled.

**Empty Project and Subject** \\- In the event that a project is switched to points based levels\\, any **empty** subjects (subjects with no skills) will have levels defined based on a theoretical points maximum of 1000 e.g., "White Belt" at 100-250 points, "Blue Belt" at 250-450 points, "Purple Belt" at 450-670, etc. These values can be easily edited after the configuration change if desired.

## Percentage Based vs. Points Based

So which strategy is right for your application? As always the answer is... it depends 😃!

The percentage based approach is the easiest to manage - the points are always calculated (and re-calculated) based on the defined percentages. As skills are added, and therefore the overall amount of points goes up, the point requirements for levels are re-calculated.

But what about users that already achieved a particular level based on the previously defined points (as calculated based on the percentages)? The system's overall approach is to never take away achievements therefore that achieved level will persist. Users will simply need to earn those missing points in addition to the next level's point requirements in order to progress to the next level.

> Please note: Our overall methodology is to **never** take away achievements

If you don't like the idea that the point requirements to achieve a particular level will vary with time (as skills are added) then the points based management strategy is for you. Once you switch to the Point based strategy each level will have an explicit *from* and *to* points defined.

As new skills are added, the extra points will *not* affect existing levels and without further actions will *not* influence what it takes to achieve those levels. You really have two options to address this issue:

1. change *from* and *to* points of each level *OR*
2. create additional levels that encapsulate the newly added points.

Approach #1 has the same issues as the percentage based strategy. Approach #2 requires careful planning so that when new points are added a new level is created to accommodate those points.''',
                ),
                new SkillRequest(name: "Visit Subject Users", skillId: "VisitSubjectUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: '''To earn points simply navigate to Project -> Subjects -> Subject -> Users

The table displays users that made progress in the subject - anyone that successfully reported and earned points for at least 1 skill that falls under this subject. Users progress for the subject is shown here as well. The progress shows:

% Complete - user's subject completion percentage

Current Points - current points earned toward this subject (sum for all the skills under a subject)

Total Points - total available points for the subject

Highest Project Level Earned - highest level earned for the subject.''',
                ),
                new SkillRequest(name: "Visit Subject Metrics", skillId: "VisitSubjectMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 4 per day
                        numPerformToCompletion: 2,
                        description: '''To earn points navigate to `Project -> Subjects -> Subject -> Metrics`.

Subject Metrics page depicts several metrics including: 

* **Subject Levels** \\- number of users that earned each level
* **Subject's users per day** \\- number of users that made progress in this level for each day
''',
                ),
                new SkillRequest(name: "Visit Badges", skillId: "VisitBadges", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 4, // up-to 4 per day
                        numPerformToCompletion: 20,
                        description: "Badges add another facet to the overall gamification profile and allow you to further reward your users by awarding these prestigious symbols. Badges are a collection of skills and when all of the skills are accomplished that badge is earned." +
                                "\n\nNavigate to ``Project -> Badges``",
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
                        description: '''To earn points simply navigate to `Project -> Badges -> Badge -> Users`

The table displays users that made progress in **the badge** \\- anyone that successfully reported and earned points for at least 1 skill that falls under this badge\\. Users' progress for **the badge** is shown here as well. The progress shows:

* **% Complete** \\- user's badge completion percentage
* **Current Points** \\- current points earned toward this badge \\(sum for all the skills under the badge\\)
* **Total Points** \\- total available points for the badge
''',
                ),
                new SkillRequest(name: "Visit Project Dependencies", skillId: "VisitProjectDependencies", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 4 per day
                        numPerformToCompletion: 6,
                        description: '''## Project Dependencies

Dependencies add another facet to the overall gamification profile, which forces users to complete skills in a specified order. If you set up `Skill A` to depend on the completion of `Skill B` then no points will be awarded toward `Skill A` until `Skill B` is fully accomplished.

![image.png](data:image/png;base64,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)
Keep in mind that `Skill B` must be *fully* completed first before *any* points will be awarded toward `Skill A`.

To add a dependency navigate to `Project -> Subject -> Skill -> Dependencies`

## Cross-Project Dependencies

Dependencies Page also hosts Cross-Project Dependencies management which facilitates cross-application training and enables users to become domain experts across several applications. These dependencies are critical when actions are required to be performed in more than one tool in order to complete a task.

Navigate to `Project -> Dependencies`

To create a cross-project skill:

1. In `Project A` navigate to `Project -> Dependencies`
2. Scroll down to the `Can be added as dependencies in other Projects` section
3. Select skill to share with other projects, for example, `Skill A` is selected
4. Select which project to share the skill with *OR* share will all projects, click `Share` button
    * for example, `Skill A` is shared with `Project B`
5. In `Project B` navigate to `Project -> Dependencies`, you will see that `Skill A` was shared with this project under `Can be added as dependencies to your Skills` section
6. Now in `Project B` `Skill A` can be added as a dependency to any local skills using `Project -> Subject -> Skill -> Dependencies`

## Best practices

* Do not create very complex dependency chains - a simple, direct and shallow dependency chain/tree is the best approach.
* Use dependencies sparingly, they complicate the training profile and may confuse users.
''',
                ),
                new SkillRequest(name: "Visit Project Levels", skillId: "VisitProjectLevels", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: '''To earn points simply navigate to `Project -> Levels`.

Levels are users' achievement path - the overall goal of the gameified training profile is to encourage users to achieve the highest level. Levels are tracked for the entire project as well as for each subject which provides users many ways to progress forward.

The Skills dashboard supports two flexible ways to manage levels:

* **Percentage Based** <em>(default)</em>: Each level is defined as a percentage of **overall** points and the actual level's point range is calculated based on that percentage.
* <strong>Point Based</strong>: Level's from and to points are configured explicitly.

> There can be only one level strategy selected and it will apply to the **entire** project including project levels and subject levels.
> Please visit `Project -> Settings` to configure a level management strategy.

## Best practices

* Consider starting with the Percentage Based strategy as each level's points are generated from a percentage so it's quick to get started. Once the majority of skills are created and the overall points are stable then the switch to the Point based strategy may be considered.
* Initially, play around with both strategies but then select one strategy and stick with it. Both strategies work very well so it's a matter of preference.
* See the Percentage Based vs. Points Based section to determine which option works best for you.

## Percentage Based Levels

Each level is defined as a percentage of **overall** points and the actual level's point range is calculated based on that percentage. By default, projects and subjects are created with **5** levels:

| Level | Name | Percentage |
| ----- | ---- | ---------- |
| 1 | White Belt | 10% |
| 2 | Blue Belt | 25% |
| 3 | Purple Belt | 45% |
| 4 | Brown Belt | 67% |
| 5 | Black Belt | 92% |

This allows levels to be fluid as Skills are defined and **overall** points change.

## Point Based Levels

Using `Project -> Settings`, levels can be changed to a points based strategy, where each level requires the project administrator to define an explicit point range. From and to points are defined with `from` being exclusive and `to` being inclusive.
Please Note

> A project must have at least 100 total points defined before this setting can be enabled.

**Empty Project and Subject** \\- In the event that a project is switched to points based levels\\, any **empty** subjects (subjects with no skills) will have levels defined based on a theoretical points maximum of 1000 e.g., "White Belt" at 100-250 points, "Blue Belt" at 250-450 points, "Purple Belt" at 450-670, etc. These values can be easily edited after the configuration change if desired.

## Percentage Based vs. Points Based

So which strategy is right for your application? As always the answer is... it depends 😃!

The percentage based approach is the easiest to manage - the points are always calculated (and re-calculated) based on the defined percentages. As skills are added, and therefore the overall amount of points goes up, the point requirements for levels are re-calculated.

But what about users that already achieved a particular level based on the previously defined points (as calculated based on the percentages)? The system's overall approach is to never take away achievements therefore that achieved level will persist. Users will simply need to earn those missing points in addition to the next level's point requirements in order to progress to the next level.

> Please note: Our overall methodology is to **never** take away achievements

If you don't like the idea that the point requirements to achieve a particular level will vary with time (as skills are added) then the points based management strategy is for you. Once you switch to the Point based strategy each level will have an explicit *from* and *to* points defined.

As new skills are added, the extra points will *not* affect existing levels and without further actions will *not* influence what it takes to achieve those levels. You really have two options to address this issue:

1. change *from* and *to* points of each level *OR*
2. create additional levels that encapsulate the newly added points.

Approach #1 has the same issues as the percentage based strategy. Approach #2 requires careful planning so that when new points are added a new level is created to accommodate those points.''',
                ),
                new SkillRequest(name: "Visit Project Users", skillId: "VisitProjectUsers", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: '''To earn points simply navigate to `Project -> Users`.

The table displays users that made progress in the project - anyone that successfully reported and earned points for at least 1 skill event. Users progress for **the overall** project is shown here as well. The progress shows:

* **% Complete** \\- user's overall project completion percentage
* **Current Points** \\- current points earned for the entire project
* **Total Points** \\- total available points for the project
* **Highest Project Level Earned** \\- highest level earned for the overall project''',
                ),
                new SkillRequest(name: "Visit Project Metrics", skillId: "VisitProjectStats", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Navigate to ``Project -> Metrics``",
                ),


                new SkillRequest(name: "Visit Project's Achievements Metrics", skillId: "VisitProjectUserAchievementMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Explore metrics about achievements such as skills, levels and badges. Navigate to ``Project -> Metrics -> Achievements``",
                ),
                new SkillRequest(name: "Visit Project's Subjects Metrics", skillId: "VisitProjectSubjectMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "View and compare usage pivoted by subjects. Navigate to ``Project -> Metrics -> Subjects``",
                ),
                new SkillRequest(name: "Visit Project's Skill Metrics", skillId: "VisitProjectSkillMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "Provide skills metrics at the project level. Highlights overlooked skills and skills with high utilization. To earn points please navigate to ``Project -> Metrics -> Skills``",
                ),
                new SkillRequest(name: "Visit Project's User Tag Metrics", skillId: "VisitUserTagMetrics", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 2, // up-to 2 per day
                        numPerformToCompletion: 5,
                        description: "View user metrics for a specific user tag value. To earn points please navigate to ``Project -> Metrics -> then click on a user tag value``",
                ),

                new SkillRequest(name: "Visit Project Settings", skillId: "VisitProjectSettings", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 2,
                        description: '''To manage and view project-wide settings navigate to `Project -> Settings`.

There are many project-level settings available including:

* **Visibility** \\- defines how this project is visible to the users consuming the training \\(covered below in a great detail\\)
* **Project Description** \\- determines where description is rendered \\(covered below in a great detail\\)
* **Rank Opt-Out for ALL Admins** \\- when enabled\\, all project admins will be excluded from the Leaderboard and will not be assigned a rank within the embedded Skills Display component
* **Always Show Group Descriptions** \\- toggle this setting to always show the group's descriptions in this project embedded Skills Display component and Progress and Ranking pages\\.
<br>

### Visibility Setting

There are three possible values for the Project Visibility setting:

1. Public Not Discoverable (default value)
2. Private Invite Only
3. Discoverable on Progress And Ranking

`Public Not Discoverable` projects can be accessed by users who have a direct link to the project's client display or by applications that have integrated the SkillTree client libraries. The project will not be available in Manage My Projects if the Progress and Ranking views have been enabled.

`Private Invite Only` projects can only be accessed by users who have been invited to join the project and who have accepted the invite, any other user attempting to access the project will receive an Access Denied error. Users who have been designated as Project Administrators will continue to have access to the project. Users can be invited to join the project using the Project Access page.

`Discoverable on Progress And Ranking` projects can be discoverd by users in the Manage My Projects view. This option will only be displayed if the instance of SkillTree has been configured to enable the Progress and Ranking views.

### Project Description Setting

There are two possible values for the Project Description setting:

1. Only show Project Description in Manager My Projects (default value)
2. Show Project Description everywhere

`Only show Project Description in Manager My Projects` is the default value for a Project. With this setting, any project description that has been configured will only be displayed in the Manage My Projects view - in the future the description may be visible to other Project Administrators in the Import Skills From the Catalog dialog. `Show Project Description everywhere` will cause any configured project description to be displayed anywhere that the training profile is displayed, including in the Manage My Proejcts view. This setting may be most applicable for SkillTree users whose training profile is viewed primarily through the Progress and Ranking view in the SkillTree dashboard.
''',
                        helpUrl: "/dashboard/user-guide/projects.html#settings",
                ),
                new SkillRequest(name: "Visit Project Access Management", skillId: "VisitProjectAccessManagement", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        description: '''The Project Access page supports adding or removing Project Administrators, Project Approvers, and inviting users to join a project if the project has been configured as an Invite Only project as well as revoking a user's access.

To add and remove project Administrators and Approvers navigate to `Project -> Access` page.

To earn points navigate to `Project -> Access`

You must have an Admin role in order to manage other Admin and/or Approver users for a project. There supported project roles are:

* <strong>Admin</strong>: enables management of the training profile for that project such as creating and modifying subjects, skills, badges, etc.
* <strong>Approver</strong>: allowed to approve and deny Self Reporting approval requests while only getting a read-only view of the project.
''',
                        helpUrl: "/dashboard/user-guide/projects.html#access",
                ),
                new SkillRequest(name: "Visit Self Report Approval Page", skillId: "VisitSelfReport", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 1,
                        description: '''To earn points please navigate to `Project -> Self Report.`

Self Report is a feature that empowers users to mark skills as completed directly in the SkillTree dashboard OR through the embedded Skills Display component. A project administrator can enable `Self Reporting` for a skill, set of skills or even all the skills in a project. Skills that have been configured with Self Reporting expose an `I did it` button, allowing users to self report completion of those skills.

There are two `Self report` types available:

1. `Honor System` \\- points are awarded immediately
2. `Approval Queue` \\- request goes into the project's approval queue; project administrators can approve or deny requests\\. Note When choosing Approval Queue\\, you may also choose to require users to submit a justification when self\\-reporting this skill by selecting the 'Justification Required' check box\\.

Project administrators can craft training profiles consisting of:

* only self-reported skills *OR*
* a mix of self-reported skills and skills that are reported programmatically *OR*
* a project could have no self-reported skills at all

## Configuring

Self reporting is enabled and configured for each skill individually. When creating or editing a skill

1. select `Self Reporting` checkbox
2. then select `Self Reporting` type (`Approval Queue` or `Honor System`)

By default, Self Reporting is disabled when creating or modifying a skill. If your project primarily consists of Self Reported skills then you can easily change the default by navigating to the `Project -> Settings` tab. There you can enable Self Reporting and select its default type for all the skills that will be created after that point.

## Skills Display

Once Self Reporting is enabled for a skill, users will see an `I did it` button on the Skills Display that will allow them to report the completion of that skill.

You could create a project that consists purely of Self Reported skills! Alternatively you can have only some skills configured with Self Reporting or no skills at all.

## Approval Queue

If a skill is configured with Self Reporting type of the `Approval Queue` then points will not be awarded right away but rather go through the simple approval workflow:

1. User click `I did it` button and requests points
2. Request appears on the project's Self Report page (see the Screenshot below)
3. Project administrator approves or reject requests

### Approval History

Project administrators can can either approve or reject points/skill requests. Approvals and rejections are documented in the `Approval History` section.
Approval History tracks:

* Skill name and skill id
* Whether request was approved or rejected
* Requester's and approver's user ids
* requested and approved/rejected dates

The `Approval History` table can be sorted by all of its columns or filtered by `Skill Name`, `User Id` and/or `Approver Id`.

### Notifications

SkillTree will send email notifications to project administrators when points are requested, approved or rejected.

![image.png](data:image/png;base64,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ycLshrx1spLLKyopLSuriI1LdOjSSY/0nQYN8QFsvpEAqJlvBAjZgQAQAAJAoN0TEIlESWkZRSXlJaVlvCp+ve1hyjFU2EoqykrmJob1TjDVWxoYfDsBcjUD62a+nTCUAASAABAAAm2dAJVKteho3Na9BP8aSwB2aDeWHOQDAkAACAABIAAE2gYBUDNtox/ACyAABIAAEAACQKCxBEDNNJYc5AMCQAAIAAEgAATaBgFQM22jH8ALIAAEgAAQAAJAoLEEQM00lhzkAwJAAAgAASAABNoGAVAzbaMfwAsgAASAABAAAkCgsQRAzTSWHOQDAkAACAABIAAE2gYBUDNtox/ACyAABIAAEAACQKCxBEDNNJYc5AMCQAAIAAEgAATaBgFQM22jH8ALIAAEgAAQAAJAoLEEQM00lhzkAwJAAAgAASAABNoGAVAzbaMfwAsgAASAABAAAkCgsQRAzTSWHOQDAkAACAABIAAE2gYBUDNtox/ACyAABIAAEAACQKCxBEDNNJYc5AMCQAAIAAEgAATaBgF6vW6kpaXVawMGQAAIAAEgAASAABBoPgJMJpOk8PrVjKWlJUl+SAICQAAIAAEgAASAQHMTSE1NJakCZppI4EASEAACQAAIAAEg0A4IgJppB50ELgIBIAAEgAAQAAIkBEDNkMCBJCAABIAAEAACQKAdEAA10w46CVwEAkAACAABIAAESAiAmiGBA0lAAAgAASAABIBAOyAAaqYddBK4CASAABAAAkAACJAQADVDAgeSgAAQAAJAAAgAgXZAANRMO+gkcBEIAAEgAASAABAgIQBqhgQOJAEBIAAEgAAQAALtgAComXbQSeAiEAACQAAIAAEgQEIA1AwJHEgCAkAACAABIAAE2gEBUDPtoJPARSAABIAAEAACQICEAKgZEjiQBASAABAAAkAACLQDAqBm2kEngYtAAAgAASAABIAACQFQMyRwIAkIAAEgAASAABBoBwRAzbSDTgIXgQAQAAJAAAgAARICoGZI4EASEAACQAAIAAEg0A4IgJppB50ELgIBIAAEgAAQAAIkBEDNkMCBJCAABIAAEAACQKAdEAA10w46CVwEAkAACAABIAAESAiAmiGBA0lAAAgAASAABIBAOyAAaqYddBK4CASAABAAAkAACJAQADVDAgeSgAAQAAJAAAgAgXZAANRMO+gkcBEIAAEg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AAJAAAh8OwFyNdP062a+3WMoAQgAASAABIAAEAACDScAaqbhrMASCAABIAAEgAAQaIsEQM20xV4Bn4AAEAACQAAIAIGGEwA103BWYAkEgAAQAAJAAAi0RQKgZtpir4BPQAAIAAEgAASAQMMJgJppOCuwBAJAAAgAASAABNoiAVAzbbFXwCcgAASAABAAAkCg4QRAzTScFVgCASAABIAAEAACbZEAqJm22CvgExAAAkAACAABINBwAqBmGs4KLIEAEAACQAAIAIG2SADUTFvsFfAJCAABIAAEgAAQaDgBUDMNZwWWQAAIAAEgAASAQFskAGqmLfYK+AQEgAAQAAJAAAg0nAComYazAksgAASAABAAAkCgLRIANdMWewV8AgJAAAgAASAABBpOgF6vacqn7HptwAAIAAEgAASAABAAAs1HgEJadP1qBiGkqCBPWggkAgEgAASAQJ0Eyisq4Vu0TjpNlwCcm45lmyupvKKS3CeYaSLnA6lAAAgAASAABIBAWycAaqat9xD4BwSAABAAAkAACJATADVDzgdSgQAQAAJAAAgAgbZOANRMW+8h8A8IAAEgAASAABAgJ/B/ih3usbDLkFcAAAAASUVORK5CYII=)

Project administrators can unsubscribe from notifications by navigating to the `Self Report` page within their project. The `Self Reported Skills Requiring Approval` section contains a Subscribed/Unsubscribed toggle on the top-right of the component.
''',
                        helpUrl: "/dashboard/user-guide/self-reporting.html#self-reporting",
                ),
                new SkillRequest(name: "Visit Project Issues Page", skillId: "VisitProjectErrors", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        description: '''Issues page documents any errors or warnings that require a project administrator's attention. Navigate to `Project -> Issues`.

Issues page displays any errors that have been recorded for a Project, how many times they have occurred, and when the most recent occurrence was. There are a number of different issues that are captured here. One example includes attempts to report a Skill that doesn't exist in a Project. This commonly occurs when a typo has been made during the integration of skill reporting into an application, or when switching an application from using a staging project to a production project where the staging skills do not exist or have been created with different Skill IDs.
''',
                        helpUrl: "/dashboard/user-guide/issues.html",
                ),
                new SkillRequest(name: "Preview Client Display for Project", skillId: "PreviewProjectClientDisplay", subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 4,
                        pointIncrementInterval : 60 * 12,
                        numMaxOccurrencesIncrementInterval: 1,
                        description: '''The Project Client Display Preview allows a Project Administrator to preview what their training profile will look like to a user. The Project Client Display Preview button is located directly below the Project ID, on the top left when viewing a specific Project in the Dashboard.'''
                ),
                new SkillRequest(name: 'Visit Contact Users Page', skillId: 'VisitContactUsers', subjectId: subjectProjectId, projectId: inceptionProjectId,
                        pointIncrement: 15,
                        numPerformToCompletion: 2,
                        pointIncrementInterval: 60 * 12,
                        numMaxOccurrencesIncrementInterval: 1,
                        description: '''The Contact Users Page allows a Project Administrator to email the users of a Project by employing a variety of different filters to select all or some of the Project users.

Up to 15 different filters can be combined to target users of your project based on the achievement of levels across the Project/Subjects as well as the achievement specific Badges and Skills or the lack of achievement of specific Skills.

Once a Project Administrator has selected the desired filters, they can create an email that can be sent to the selected users. The specified email can also be previewed using the `Preview` button. This will send the specified email to the currently authenticated user so that formatting, content, and display can be validated.

To earn points simply navigate to `Project -> Contact Users`.
''',
                        helpUrl: "/dashboard/user-guide/contact-project-users.html"

                )

        ]
    }

    private List<SkillRequest> getDashboardSubjectSkills() {
        return [
                new SkillRequest(name: "Visit Dashboard Skills", skillId: "VisitDashboardSkills", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60*12,
                        numMaxOccurrencesIncrementInterval: 5,
                        numPerformToCompletion: 20,
                        description: "The SkillTree dashboard gamifies training for the dashboard itself and we call it **Inception**. " +
                                "All the dashboard users will have a button on the top right of the application which navigates to your skills profile. " +
                                "This button will also display your current level standing.",
                        helpUrl: "/dashboard/user-guide/inception.html"
                ),
                new SkillRequest(name: "Add or Modify Levels", skillId: "AddOrModifyLevels", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 50, numPerformToCompletion: 1,
                        description: '''Levels are users' achievement path - the overall goal of application usage is to achieve the highest level. Levels are tracked for the entire project as well as for each subject which provides users many ways to progress forward.
Skills dashboard supports two flexible ways to manage levels:
* `Percentage Based (default)`: Each level is defined as a percentage of overall points and the actual level's point range is calculated based on the percentage.
* `Point based`: Level's from and to points are configured explicitly.

To achieve this skill simply study the available percentage based and point-based strategy and make modifications to levels as needed.'''.toString(),
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
                        description: '''You can see what the skills profile and progress display would like for a particular user by navigating to a specific user page ``Project -> Users -> Select a User -> Client Display``. 
This is the same exact pluggable Skills Display that you would be embedding into your application so it can serve as a preview of what the user will see.  

Client display will depict project skills profile and users points at that exact moment. We suggest you often visit the Skills Display view while building a skill profile to better understand what the gamificaiton profile and progress will look like to your users. 
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
                        description: "To see a history of user's performed skill events please visit ``Project -> Users -> Select a User -> Performed Skills``. Furthermore you have the ability to remove individual skill events if needed.",
                        helpUrl: "/dashboard/user-guide/users.html#performed-skills"
                ),
                new SkillRequest(name: "Visit Markdown Documentation", skillId: "VisitMarkdownDocs", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 25 per day
                        numPerformToCompletion: 2,
                        description: "Descriptions support markdown for subjects and skills. When creating a subject or a skill, the description field has a link to the markdown documentation.",
                ),
                new SkillRequest(name: "Visit My Preferences Page", skillId: "VisitMyPreferences", subjectId: subjectDashboardId, projectId: inceptionProjectId,
                        pointIncrement: 10,
                        numPerformToCompletion: 1,
                        description: "On the Preferences page you can customize you personal dashboard preferences. To navigate to the page\n\n- Click on the ``User Settings`` button on the top right\n- Click on the ``Settings`` option\n- Navigate to the ``Preferences`` tab",
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
To create a skill, navigate to a subject and then click the ``Skill +`` button.''',
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skill with disabled Time Window", skillId: "CreateSkillDisabledTimeWindow", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        numPerformToCompletion: 5,
                        description: "When `Time Window` is disabled skill events are applied immediately.",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skill with Max Occurrences Within Time Window", skillId: "CreateSkillMaxOccurrencesWithinTimeWindow", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        numPerformToCompletion: 5,
                        description: "Used in conjunction with the `Time Window` property; Once this `Max Occurrences` is reached, points will not be incremented until outside of the configured `Time Window`.",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skill with Help Url", skillId: "CreateSkillHelpUrl", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 5, // up-to 1 per day
                        numPerformToCompletion: 10,
                        description: "URL pointing to a help article providing further information about this skill or capability. Please note that this property works in conjunction with the Root Help Url project setting.",
                        helpUrl: "/dashboard/user-guide/skills.html"
                ),
                new SkillRequest(name: "Create Skills with multiple versions", skillId: "CreateSkillVersion", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 25,
                        numPerformToCompletion: 1,
                        description: "Skill versioning is a mechanism that allows the addition of new skills without affecting existing software running with an older skill profile. Versioning is mostly pertinent to the Display Libraries that visualize the skill profile for the version they were declared with.",
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
                        description: "Dependencies add another facet to the overall gamification profile which forces users to complete skills in the specified order. If you set up Skill A to depend on the completion of Skill B then no points will be awarded toward Skill A until Skill B is fully accomplished. Keep in mind that Skill B must be fully completed first before any points will be awarded toward Skill A. Navigate to ``Project -> Subject -> Skill -> Dependencies``",
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
                new SkillRequest(name: "Self Reporting with Honor", skillId: "SelfReportHonorExample", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 3,
                        description: "This Skill is an example of the `Self Reporting` feature. Click the **I did it** button below to receive points. The skill is configured under the 'Honor' system and the points will be awarded immediately. Enjoy!",
                        selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                ),
                new SkillRequest(name: "Self Reporting with Approval", skillId: "SelfReportApprovalExample", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 3,
                        description: "This Skill is an example of the `Self Reporting` feature. Click **I did it** button below to request points. The skill is configured under the 'Approval' system and a request will be placed into the project administrators Approval Queue!",
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                ),
                new SkillRequest(name: "Add or Update Skill Tags", skillId: "AddOrModifyTags", subjectId: subjectSkillsId, projectId: inceptionProjectId,
                        pointIncrement: 5,
                        pointIncrementInterval: 60 * 12, // 1 work day
                        numMaxOccurrencesIncrementInterval: 1, // up-to 1 per day
                        numPerformToCompletion: 3,
                        description: "Create custom tags and attach them to skills.  Tags can be used to help organize or group multiple skills and can be useful for viewing skills in the Client Skills Display as well as metrics.",
                        selfReportingType: SkillDef.SelfReportingType.Approval.toString(),
                ),
        ]
    }
}
