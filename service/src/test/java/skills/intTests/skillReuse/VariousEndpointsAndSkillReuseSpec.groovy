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
package skills.intTests.skillReuse

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import skills.intTests.catalog.CatalogIntSpec
import skills.intTests.utils.SkillsService
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class VariousEndpointsAndSkillReuseSpec extends CatalogIntSpec {

    def "get skills for project filter reuse tag in the name"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(1, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        when:
        def skills = skillsService.getSkillsForProject(p1.projectId)
        then:
        skills.size() == 2
        skills.name == [p1Skills[0].name, p1Skills[0].name]
        skills.isReused == [false, true]
        skills.skillId == [p1Skills[0].skillId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)]
    }

    def "get skills for project filter with name search to not find records when searching for the reuse tag"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(1, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        when:
        def skills = skillsService.getSkillsForProject(p1.projectId, SkillReuseIdUtil.REUSE_TAG)
        def skills1 = skillsService.getSkillsForProject(p1.projectId, p1Skills[0].name.toString().substring(0, 2))
        then:
        !skills
        skills1.name == [p1Skills[0].name, p1Skills[0].name]
        skills1.isReused == [false, true]
    }

    def "get skills for project - return reused info for group skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        when:
        def skills1 = skillsService.getSkillsForProject(p1.projectId, p1Skills[0].name)
        then:
        skills1.groupName == [p1subj1g1.name, p1subj2g2.name]
        skills1.groupId == [p1subj1g1.skillId, p1subj2g2.skillId]
        skills1.isReused == [false, true]
    }

    def "get skills for project without reused skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(1, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        when:
        def skills = skillsService.getSkillsForProject(p1.projectId, "", false, false, true)
        then:
        skills.isReused == [false]
        skills.skillId == [p1Skills[0].skillId]
    }

    def "metrics skill endpoint ignores reused skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        List<Date> dates = (5..1).collect { new Date() - it }
        List<String> users = getRandomUsers(5)
        skillsService.addSkill(p1Skills[0], users[0], dates[4])

        skillsService.addSkill(p1Skills[0], users[1], dates[0])
        skillsService.addSkill(p1Skills[0], users[1], dates[1])

        skillsService.addSkill(p1Skills[0], users[2], dates[2])
        skillsService.addSkill(p1Skills[0], users[2], dates[3])

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Map props = [:]
        when:
        def res = skillsService.getMetricsData(p1.projectId, "skillUsageNavigatorChartBuilder", props)
        then:
        res.skills.skillName == [p1Skills[0].name, p1Skills[1].name, p1Skills[2].name]
        res.skills.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId,]
        res.skills.isReusedSkill == [false, false, false]
        res.skills.numUsersInProgress == [1, 0, 0]
        res.skills.numUserAchieved == [2, 0, 0]
        res.skills.lastReportedTimestamp == [dates[4].time, null, null]
        res.skills.lastAchievedTimestamp == [dates[3].time, null, null]
    }

    def "metrics endpoint ignores reused group skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        List<Date> dates = (5..1).collect { new Date() - it }
        List<String> users = getRandomUsers(5)
        skillsService.addSkill(p1Skills[0], users[0], dates[4])

        skillsService.addSkill(p1Skills[0], users[1], dates[0])
        skillsService.addSkill(p1Skills[0], users[1], dates[1])

        skillsService.addSkill(p1Skills[0], users[2], dates[2])
        skillsService.addSkill(p1Skills[0], users[2], dates[3])

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Map props = [:]
        when:
        def res = skillsService.getMetricsData(p1.projectId, "skillUsageNavigatorChartBuilder", props)
        then:
        res.skills.skillName == [p1Skills[0].name, p1Skills[1].name, p1Skills[2].name]
        res.skills.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId,]
        res.skills.isReusedSkill == [false, false, false]
        res.skills.numUsersInProgress == [1, 0, 0]
        res.skills.numUserAchieved == [2, 0, 0]
        res.skills.lastReportedTimestamp == [dates[4].time, null, null]
        res.skills.lastAchievedTimestamp == [dates[3].time, null, null]
    }

    def "users endpoints for reused skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        List<Date> dates = (5..1).collect { new Date() - it }
        List<String> users = getRandomUsers(5)
        skillsService.addSkill(p1Skills[0], users[0], dates[4])

        skillsService.addSkill(p1Skills[0], users[1], dates[0])
        skillsService.addSkill(p1Skills[0], users[1], dates[1])

        skillsService.addSkill(p1Skills[0], users[2], dates[2])
        skillsService.addSkill(p1Skills[0], users[2], dates[3])

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

        when:
        def subj1Users = skillsService.getSubjectUsers(p1.projectId, p1subj1.subjectId)
        def subj2Users = skillsService.getSubjectUsers(p1.projectId, p1subj2.subjectId)
        def projectUsers = skillsService.getProjectUsers(p1.projectId)
        def skillUsers = skillsService.getSkillUsers(p1.projectId, p1Skills[0].skillId)
        def reusedSkillUsers = skillsService.getSkillUsers(p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        subj1Users.data.userId.sort() == [users[0], users[1], users[2]].sort()
        subj1Users.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        subj1Users.data.totalPoints.sort() == [100, 200, 200].sort()

        subj2Users.data.userId.sort() == [users[0], users[1], users[2]].sort()
        subj2Users.data.lastUpdated.sort() == [null, null, null]
        subj2Users.data.totalPoints.sort() == [100, 200, 200].sort()

        projectUsers.data.userId.sort() == [users[0], users[1], users[2]].sort()
        projectUsers.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        projectUsers.data.totalPoints.sort() == [200, 400, 400].sort()

        skillUsers.data.userId.sort() == [users[0], users[1], users[2]].sort()
        skillUsers.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        skillUsers.data.totalPoints.sort() == [100, 200, 200].sort()

        reusedSkillUsers.data.userId.sort() == [users[0], users[1], users[2]].sort()
        reusedSkillUsers.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        reusedSkillUsers.data.totalPoints.sort() == [100, 200, 200].sort()
    }

    def "users endpoints for group reused skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        List<Date> dates = (5..1).collect { new Date() - it }
        List<String> users = getRandomUsers(5)
        skillsService.addSkill(p1Skills[0], users[0], dates[4])

        skillsService.addSkill(p1Skills[0], users[1], dates[0])
        skillsService.addSkill(p1Skills[0], users[1], dates[1])

        skillsService.addSkill(p1Skills[0], users[2], dates[2])
        skillsService.addSkill(p1Skills[0], users[2], dates[3])

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

        when:
        def subj1Users = skillsService.getSubjectUsers(p1.projectId, p1subj1.subjectId)
        def subj2Users = skillsService.getSubjectUsers(p1.projectId, p1subj2.subjectId)
        def projectUsers = skillsService.getProjectUsers(p1.projectId)
        def skillUsers = skillsService.getSkillUsers(p1.projectId, p1Skills[0].skillId)
        def reusedSkillUsers = skillsService.getSkillUsers(p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        subj1Users.data.userId.sort() == [users[0], users[1], users[2]].sort()
        subj1Users.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        subj1Users.data.totalPoints.sort() == [100, 200, 200].sort()

        subj2Users.data.userId.sort() == [users[0], users[1], users[2]].sort()
        subj2Users.data.lastUpdated.sort() == [null, null, null].sort()
        subj2Users.data.totalPoints.sort() == [100, 200, 200].sort()

        projectUsers.data.userId.sort() == [users[0], users[1], users[2]].sort()
        projectUsers.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        projectUsers.data.totalPoints.sort() == [200, 400, 400].sort()

        skillUsers.data.userId.sort() == [users[0], users[1], users[2]].sort()
        skillUsers.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        skillUsers.data.totalPoints.sort() == [100, 200, 200].sort()

        reusedSkillUsers.data.userId.sort() == [users[0], users[1], users[2]].sort()
        reusedSkillUsers.data.lastUpdated.sort() == [DTF.print(dates[4].time), DTF.print(dates[1].time), DTF.print(dates[3].time)].sort()
        reusedSkillUsers.data.totalPoints.sort() == [100, 200, 200].sort()
    }

    def "do not return skill achievements in the achievements metrics"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        List<Date> dates = (5..1).collect { new Date() - it }
        List<String> users = getRandomUsers(5)
        skillsService.addSkill(p1Skills[0], users[0], dates[4])

        skillsService.addSkill(p1Skills[0], users[1], dates[0])
        skillsService.addSkill(p1Skills[0], users[1], dates[1])

        skillsService.addSkill(p1Skills[0], users[2], dates[2])
        skillsService.addSkill(p1Skills[0], users[2], dates[3])

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        String metricsId = "userAchievementsChartBuilder"
        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = "${SkillDef.ContainerType.Skill}"
        when:
        def res = skillsService.getMetricsData(p1.projectId, metricsId, props)
        then:
        res.totalNumItems == 2
        res.items.skillId == [p1Skills[0].skillId, p1Skills[0].skillId]
        res.items.userId.sort() == [users[1], users[2]].sort()
    }

    def "reused skills must not be considered in catalog status"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        skillsService.exportSkillToCatalog(p1.projectId, p1Skills[0].skillId)
        skillsService.exportSkillToCatalog(p1.projectId, p1Skills[1].skillId)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.importSkillFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p1.projectId, p1Skills[1].skillId)

        when:
        def p2Exported = skillsService.getExportedSkills(p1.projectId, 10, 1, "skillName", true)

        def skill1Stats = skillsService.getExportedSkillStats(p1.projectId, p1Skills[0].skillId)
        def skill2Stats = skillsService.getExportedSkillStats(p1.projectId, p1Skills[1].skillId)
        then:
        p2Exported.count == 2
        p2Exported.data.skillName == [p1Skills[0].name, p1Skills[1].name]
        p2Exported.data.importedProjectCount == [0, 1]

        skill2Stats.users.importingProjectId == [p2.projectId]
        !skill1Stats.users.importingProjectId
    }

    def "return reused skills and points from project's stats endpoint"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        when:
        def projects = skillsService.getProjects()
        def projStats = skillsService.getProject(p1.projectId)
        def subj1Stats = skillsService.getSubject(p1subj1)
        def subj2Stats = skillsService.getSubject(p1subj2)
        then:
        projStats.numSkills == 3
        projStats.numSkillsReused == 1
        projStats.totalPointsReused == 200
        projStats.totalPoints == 600

        subj1Stats.numSkills == 3
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0
        subj1Stats.totalPoints == 600

        subj2Stats.numSkills == 0
        subj2Stats.numSkillsReused == 1
        subj2Stats.totalPointsReused == 200
        subj2Stats.totalPoints == 0

        projects[0].numSkills == 3
        projects[0].totalPoints == 600
        projects[0].numSkillsReused == 1
        projects[0].totalPointsReused == 200
    }

    def "return reused skills and points from project's stats endpoint - root user"() {
        SkillsService rootSkillsService = createRootSkillService()
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        rootSkillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            rootSkillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        rootSkillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        rootSkillsService.createSkill(p1subj2g2)

        rootSkillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        rootSkillsService.pinProject(p1.projectId)

        when:
        def projects = rootSkillsService.getProjects()
        def projStats = rootSkillsService.getProject(p1.projectId)
        def subj1Stats = rootSkillsService.getSubject(p1subj1)
        def subj2Stats = rootSkillsService.getSubject(p1subj2)
        then:
        projStats.numSkills == 3
        projStats.numSkillsReused == 1
        projStats.totalPointsReused == 200
        projStats.totalPoints == 600

        subj1Stats.numSkills == 3
        subj1Stats.numSkillsReused == 0
        subj1Stats.totalPointsReused == 0
        subj1Stats.totalPoints == 600

        subj2Stats.numSkills == 0
        subj2Stats.numSkillsReused == 1
        subj2Stats.totalPointsReused == 200
        subj2Stats.totalPoints == 0

        projects[0].numSkills == 3
        projects[0].totalPoints == 600
        projects[0].numSkillsReused == 1
        projects[0].totalPointsReused == 200
    }

    def "endpoint to check whether skills have dependencies"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(1).skillId)

        when:
        def res = skillsService.checkIfSkillsHaveDependencies(p1.projectId, p1Skills.collect { it.skillId })
        then:
        res.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId]
        res.hasDependency == [true, false, false]
    }

    def "skill details endpoint provides info whether the skill was reused elsewhere in this project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        when:
        def sk1 = skillsService.getSkill(p1Skills[0])
        def sk2 = skillsService.getSkill(p1Skills[1])
        def sk3 = skillsService.getSkill(p1Skills[2])
        then:
        sk1.thisSkillWasReusedElsewhere
        !sk2.thisSkillWasReusedElsewhere
        !sk3.thisSkillWasReusedElsewhere
    }

    def "due to async propagation of the reused skills client display endpoints may not return extra today's points for the honor skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        List<Date> dates = (5..1).collect { new Date() - it }
        List<String> users = getRandomUsers(5)

        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], users[0], new Date())
        // today

        when:
        def res = skillsService.getSkillSummary(users[0], p1.projectId, p1subj2.subjectId, -1, true)
        def skillRes = skillsService.getSingleSkillSummary(users[0], p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        res.points == 100
        res.todaysPoints == 100

        res.skills[0].points == 100
        res.skills[0].todaysPoints == 100

        skillRes.points == 100
        skillRes.todaysPoints == 100
    }
}

