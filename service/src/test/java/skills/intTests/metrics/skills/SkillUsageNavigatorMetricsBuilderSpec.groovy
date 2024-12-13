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
package skills.intTests.metrics.skills

import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsParams

import static skills.intTests.utils.SkillsFactory.*

class SkillUsageNavigatorMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "skillUsageNavigatorChartBuilder"

    def "no skills"() {
        def proj = SkillsFactory.createProject()

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())

        Map props = [:]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        res == [ skills: [], tags: [] ]
    }

    def "one empty skill"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        Map props = [:]

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        res.skills.size() == 1
        res.skills[0].skillId == 'skill1'
        res.skills[0].skillName == 'Test Skill 1'
        res.skills[0].numUserAchieved == 0
        res.skills[0].numUsersInProgress == 0
        !res.skills[0].lastReportedTimestamp
        !res.skills[0].lastAchievedTimestamp
    }

    def "skills with usage and achievements"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        skillsService.archiveUsers([users[2]], proj.projectId)
        def resAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        res.skills.size() == 10
        def skill1 = res.skills.find { it.skillId == 'skill1' }
        skill1.subjectId == subj.subjectId
        skill1.numUserAchieved == 1
        skill1.numUsersInProgress == 4
        new Date(skill1.lastReportedTimestamp) == days[5]
        new Date(skill1.lastAchievedTimestamp) == days[5]

        def skill2 = res.skills.find { it.skillId == 'skill2' }
        skill2.numUserAchieved == 0
        skill2.numUsersInProgress == 5
        new Date(skill2.lastReportedTimestamp) == days[5]
        !skill2.lastAchievedTimestamp

        def skill3 = res.skills.find { it.skillId == 'skill3' }
        skill3.numUserAchieved == 0
        skill3.numUsersInProgress == 5
        new Date(skill3.lastReportedTimestamp) == days[5]
        !skill3.lastAchievedTimestamp

        def skill4 = res.skills.find { it.skillId == 'skill4' }
        skill4.numUserAchieved == 0
        skill4.numUsersInProgress == 5
        new Date(skill4.lastReportedTimestamp) == days[5]
        !skill4.lastAchievedTimestamp

        def skill5 = res.skills.find { it.skillId == 'skill5' }
        skill5.numUserAchieved == 0
        skill5.numUsersInProgress == 5
        new Date(skill5.lastReportedTimestamp) == days[5]
        !skill5.lastAchievedTimestamp

        def skill6 = res.skills.find { it.skillId == 'skill6' }
        skill6.numUserAchieved == 0
        skill6.numUsersInProgress == 0
        !skill6.lastReportedTimestamp
        !skill6.lastAchievedTimestamp

        resAfterArchive.skills.size() == 10
        def skill1AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill1' }
        skill1AfterArchive.subjectId == subj.subjectId
        skill1AfterArchive.numUserAchieved == 1
        skill1AfterArchive.numUsersInProgress == 3
        new Date(skill1AfterArchive.lastReportedTimestamp) == days[5]
        new Date(skill1AfterArchive.lastAchievedTimestamp) == days[5]

        def skill2AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill2' }
        skill2AfterArchive.numUserAchieved == 0
        skill2AfterArchive.numUsersInProgress == 4
        new Date(skill2AfterArchive.lastReportedTimestamp) == days[5]
        !skill2AfterArchive.lastAchievedTimestamp

        def skill3AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill3' }
        skill3AfterArchive.numUserAchieved == 0
        skill3AfterArchive.numUsersInProgress == 4
        new Date(skill3AfterArchive.lastReportedTimestamp) == days[5]
        !skill3AfterArchive.lastAchievedTimestamp

        def skill4AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill4' }
        skill4AfterArchive.numUserAchieved == 0
        skill4AfterArchive.numUsersInProgress == 4
        new Date(skill4AfterArchive.lastReportedTimestamp) == days[5]
        !skill4AfterArchive.lastAchievedTimestamp

        def skill5AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill5' }
        skill5AfterArchive.numUserAchieved == 0
        skill5AfterArchive.numUsersInProgress == 4
        new Date(skill5AfterArchive.lastReportedTimestamp) == days[5]
        !skill5AfterArchive.lastAchievedTimestamp

        def skill6AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill6' }
        skill6AfterArchive.numUserAchieved == 0
        skill6AfterArchive.numUsersInProgress == 0
        !skill6AfterArchive.lastReportedTimestamp
        !skill6AfterArchive.lastAchievedTimestamp
    }

    def "skills with usage, achievements and tags"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        skillsService.addTagToSkills(proj.projectId, skills.collect{ it -> it.skillId }, "Test Tag", "testtag")
        skillsService.addTagToSkills(proj.projectId, skills[0..4].collect{ it -> it.skillId }, "New Tag", "newtag")

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        res.skills.size() == 10
        res.tags.size() == 2
        res.tags == [[tagValue: 'New Tag', tagId: 'newtag'], [tagValue: 'Test Tag', tagId: 'testtag']]
    }

    def "group skills with usage and achievements"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def group = SkillsFactory.createSkillsGroup(1, 1, 22)
        List<Map> skills = SkillsFactory.createSkills(10)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills([group])
        skills.each {
            skillsService.assignSkillToSkillsGroup(group.skillId, it)
        }

        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        skillsService.archiveUsers([users[2]], proj.projectId)

        def resAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        res.skills.size() == 10
        def skill1 = res.skills.find { it.skillId == 'skill1' }
        skill1.subjectId == subj.subjectId
        skill1.numUserAchieved == 1
        skill1.numUsersInProgress == 4
        new Date(skill1.lastReportedTimestamp) == days[5]
        new Date(skill1.lastAchievedTimestamp) == days[5]

        def skill2 = res.skills.find { it.skillId == 'skill2' }
        skill2.numUserAchieved == 0
        skill2.numUsersInProgress == 5
        new Date(skill2.lastReportedTimestamp) == days[5]
        !skill2.lastAchievedTimestamp

        def skill3 = res.skills.find { it.skillId == 'skill3' }
        skill3.numUserAchieved == 0
        skill3.numUsersInProgress == 5
        new Date(skill3.lastReportedTimestamp) == days[5]
        !skill3.lastAchievedTimestamp

        def skill4 = res.skills.find { it.skillId == 'skill4' }
        skill4.numUserAchieved == 0
        skill4.numUsersInProgress == 5
        new Date(skill4.lastReportedTimestamp) == days[5]
        !skill4.lastAchievedTimestamp

        def skill5 = res.skills.find { it.skillId == 'skill5' }
        skill5.numUserAchieved == 0
        skill5.numUsersInProgress == 5
        new Date(skill5.lastReportedTimestamp) == days[5]
        !skill5.lastAchievedTimestamp

        def skill6 = res.skills.find { it.skillId == 'skill6' }
        skill6.numUserAchieved == 0
        skill6.numUsersInProgress == 0
        !skill6.lastReportedTimestamp
        !skill6.lastAchievedTimestamp


        resAfterArchive.skills.size() == 10
        def skill1AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill1' }
        skill1AfterArchive.subjectId == subj.subjectId
        skill1AfterArchive.numUserAchieved == 1
        skill1AfterArchive.numUsersInProgress == 3
        new Date(skill1AfterArchive.lastReportedTimestamp) == days[5]
        new Date(skill1AfterArchive.lastAchievedTimestamp) == days[5]

        def skill2AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill2' }
        skill2AfterArchive.numUserAchieved == 0
        skill2AfterArchive.numUsersInProgress == 4
        new Date(skill2AfterArchive.lastReportedTimestamp) == days[5]
        !skill2AfterArchive.lastAchievedTimestamp

        def skill3AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill3' }
        skill3AfterArchive.numUserAchieved == 0
        skill3AfterArchive.numUsersInProgress == 4
        new Date(skill3AfterArchive.lastReportedTimestamp) == days[5]
        !skill3AfterArchive.lastAchievedTimestamp

        def skill4AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill4' }
        skill4AfterArchive.numUserAchieved == 0
        skill4AfterArchive.numUsersInProgress == 4
        new Date(skill4AfterArchive.lastReportedTimestamp) == days[5]
        !skill4AfterArchive.lastAchievedTimestamp

        def skill5AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill5' }
        skill5AfterArchive.numUserAchieved == 0
        skill5AfterArchive.numUsersInProgress == 4
        new Date(skill5AfterArchive.lastReportedTimestamp) == days[5]
        !skill5AfterArchive.lastAchievedTimestamp

        def skill6AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill6' }
        skill6AfterArchive.numUserAchieved == 0
        skill6AfterArchive.numUsersInProgress == 0
        !skill6AfterArchive.lastReportedTimestamp
        !skill6AfterArchive.lastAchievedTimestamp
    }

    def "last reported is later than last achieved"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 2 }

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        List<Date> days
        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }

            // achieved
            skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], days[0])
            skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], days[1])

            // in progress
            skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], days[3])
        }

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        res.skills = res.skills.sort { it.skillId }

        then:
        res.skills.size() == 5
        res.skills[0].skillId == 'skill1'
        res.skills[0].numUserAchieved == 1
        res.skills[0].numUsersInProgress == 1
        new Date(res.skills[0].lastReportedTimestamp) == days[3]
        new Date(res.skills[0].lastAchievedTimestamp) == days[1]
    }

    def "skills with usage and achievements - include catalog skills"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(2)
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 100; it.numPerformToCompletion = 5 }

        List<Map> p2Skills = SkillsFactory.createSkills(5, 2)
        (0..4).each {
            p2Skills.get(it).pointIncrement = 100
            p2Skills.get(it).numPerformToCompletion = 5
            p2Skills.get(it).skillId = "p2skill_"+it
            p2Skills.get(it).name = "P2Skill "+it
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)
        skillsService.createSkills(p2Skills)

        skillsService.exportSkillToCatalog(proj2.projectId, p2Skills[0].skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2Skills[1].skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2Skills[2].skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2Skills[3].skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2Skills[4].skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subj.subjectId, proj2.projectId, p2Skills[0].skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subj.subjectId, proj2.projectId, p2Skills[1].skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subj.subjectId, proj2.projectId, p2Skills[2].skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subj.subjectId, proj2.projectId, p2Skills[3].skillId)
        skillsService.importSkillFromCatalog(proj.projectId, subj.subjectId, proj2.projectId, p2Skills[4].skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj.projectId, true)

        def allSkills = []
        allSkills.addAll(skills)
        allSkills.addAll(p2Skills)

        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    allSkills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: skill.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = skills[0].skillId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        skillsService.archiveUsers([users[0]], proj.projectId)

        def resAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        res.skills.size() == 10
        def skill1 = res.skills.find { it.skillId == 'skill1' }
        skill1.subjectId == subj.subjectId
        skill1.numUserAchieved == 1
        skill1.numUsersInProgress == 4
        new Date(skill1.lastReportedTimestamp) == days[5]
        new Date(skill1.lastAchievedTimestamp) == days[5]

        def skill2 = res.skills.find { it.skillId == 'skill2' }
        skill2.numUserAchieved == 0
        skill2.numUsersInProgress == 5
        new Date(skill2.lastReportedTimestamp) == days[5]
        !skill2.lastAchievedTimestamp

        def skill3 = res.skills.find { it.skillId == 'skill3' }
        skill3.numUserAchieved == 0
        skill3.numUsersInProgress == 5
        new Date(skill3.lastReportedTimestamp) == days[5]
        !skill3.lastAchievedTimestamp

        def skill4 = res.skills.find { it.skillId == 'skill4' }
        skill4.numUserAchieved == 0
        skill4.numUsersInProgress == 5
        new Date(skill4.lastReportedTimestamp) == days[5]
        !skill4.lastAchievedTimestamp

        def skill5 = res.skills.find { it.skillId == 'skill5' }
        skill5.numUserAchieved == 0
        skill5.numUsersInProgress == 5
        new Date(skill5.lastReportedTimestamp) == days[5]
        !skill5.lastAchievedTimestamp

        def skill6 = res.skills.find { it.skillId == 'p2skill_0' }
        skill6.numUserAchieved == 0
        skill6.numUsersInProgress == 0
        !skill6.lastReportedTimestamp
        !skill6.lastAchievedTimestamp

        resAfterArchive.skills.size() == 10
        def skill1AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill1' }
        skill1AfterArchive.subjectId == subj.subjectId
        skill1AfterArchive.numUserAchieved == 0
        skill1AfterArchive.numUsersInProgress == 4
        new Date(skill1AfterArchive.lastReportedTimestamp) == days[5]
        !skill1AfterArchive.lastAchievedTimestamp

        def skill2AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill2' }
        skill2AfterArchive.numUserAchieved == 0
        skill2AfterArchive.numUsersInProgress == 4
        new Date(skill2AfterArchive.lastReportedTimestamp) == days[5]
        !skill2AfterArchive.lastAchievedTimestamp

        def skill3AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill3' }
        skill3AfterArchive.numUserAchieved == 0
        skill3AfterArchive.numUsersInProgress == 4
        new Date(skill3AfterArchive.lastReportedTimestamp) == days[5]
        !skill3AfterArchive.lastAchievedTimestamp

        def skill4AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill4' }
        skill4AfterArchive.numUserAchieved == 0
        skill4AfterArchive.numUsersInProgress == 4
        new Date(skill4AfterArchive.lastReportedTimestamp) == days[5]
        !skill4AfterArchive.lastAchievedTimestamp

        def skill5AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill5' }
        skill5AfterArchive.numUserAchieved == 0
        skill5AfterArchive.numUsersInProgress == 4
        new Date(skill5AfterArchive.lastReportedTimestamp) == days[5]
        !skill5AfterArchive.lastAchievedTimestamp

        def skill6AfterArchive = resAfterArchive.skills.find { it.skillId == 'p2skill_0' }
        skill6AfterArchive.numUserAchieved == 0
        skill6AfterArchive.numUsersInProgress == 0
        !skill6AfterArchive.lastReportedTimestamp
        !skill6AfterArchive.lastAchievedTimestamp
    }

    def "skills with usage and achievements - only catalog skills"() {
        List<String> users = getRandomUsers(10)
        def proj = SkillsFactory.createProject()
        def proj2 = SkillsFactory.createProject(2)
        def subj = SkillsFactory.createSubject()
        def subj2 = SkillsFactory.createSubject(2)

        List<Map> p2Skills = SkillsFactory.createSkills(10, 2)
        p2Skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 5
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)
        skillsService.createSkills(p2Skills)

        p2Skills.each {
            skillsService.exportSkillToCatalog(proj2.projectId, it.skillId)
            skillsService.importSkillFromCatalog(proj.projectId, subj.subjectId, proj2.projectId, it.skillId)
        }
        skillsService.finalizeSkillsImportFromCatalog(proj.projectId, true)


        List<Date> days

        use(TimeCategory) {
            days = (5..0).collect { int day -> day.days.ago }
            days.eachWithIndex { Date date, int index ->
                users.subList(0, index).each { String user ->
                    p2Skills.subList(0, index).each { skill ->
                        skillsService.addSkill([projectId: skill.projectId, skillId: skill.skillId], user, date)
                    }
                }
            }
        }

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Map props = [:]
        props[MetricsParams.P_SKILL_ID] = p2Skills[0].skillId

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        skillsService.archiveUsers([users[2]], proj.projectId)
        def resAfterArchive = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        res.skills.size() == 10
        def skill1 = res.skills.find { it.skillId == 'skill1' }
        skill1.subjectId == subj.subjectId
        skill1.numUserAchieved == 1
        skill1.numUsersInProgress == 4
        new Date(skill1.lastReportedTimestamp) == days[5]
        new Date(skill1.lastAchievedTimestamp) == days[5]

        def skill2 = res.skills.find { it.skillId == 'skill2' }
        skill2.numUserAchieved == 0
        skill2.numUsersInProgress == 5
        new Date(skill2.lastReportedTimestamp) == days[5]
        !skill2.lastAchievedTimestamp

        def skill3 = res.skills.find { it.skillId == 'skill3' }
        skill3.numUserAchieved == 0
        skill3.numUsersInProgress == 5
        new Date(skill3.lastReportedTimestamp) == days[5]
        !skill3.lastAchievedTimestamp

        def skill4 = res.skills.find { it.skillId == 'skill4' }
        skill4.numUserAchieved == 0
        skill4.numUsersInProgress == 5
        new Date(skill4.lastReportedTimestamp) == days[5]
        !skill4.lastAchievedTimestamp

        def skill5 = res.skills.find { it.skillId == 'skill5' }
        skill5.numUserAchieved == 0
        skill5.numUsersInProgress == 5
        new Date(skill5.lastReportedTimestamp) == days[5]
        !skill5.lastAchievedTimestamp

        def skill6 = res.skills.find { it.skillId == 'skill6' }
        skill6.numUserAchieved == 0
        skill6.numUsersInProgress == 0
        !skill6.lastReportedTimestamp
        !skill6.lastAchievedTimestamp

        resAfterArchive.skills.size() == 10
        def skill1AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill1' }
        skill1AfterArchive.subjectId == subj.subjectId
        skill1AfterArchive.numUserAchieved == 1
        skill1AfterArchive.numUsersInProgress == 3
        new Date(skill1AfterArchive.lastReportedTimestamp) == days[5]
        new Date(skill1AfterArchive.lastAchievedTimestamp) == days[5]

        def skill2AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill2' }
        skill2AfterArchive.numUserAchieved == 0
        skill2AfterArchive.numUsersInProgress == 4
        new Date(skill2AfterArchive.lastReportedTimestamp) == days[5]
        !skill2AfterArchive.lastAchievedTimestamp

        def skill3AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill3' }
        skill3AfterArchive.numUserAchieved == 0
        skill3AfterArchive.numUsersInProgress == 4
        new Date(skill3AfterArchive.lastReportedTimestamp) == days[5]
        !skill3AfterArchive.lastAchievedTimestamp

        def skill4AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill4' }
        skill4AfterArchive.numUserAchieved == 0
        skill4AfterArchive.numUsersInProgress == 4
        new Date(skill4AfterArchive.lastReportedTimestamp) == days[5]
        !skill4AfterArchive.lastAchievedTimestamp

        def skill5AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill5' }
        skill5AfterArchive.numUserAchieved == 0
        skill5AfterArchive.numUsersInProgress == 4
        new Date(skill5AfterArchive.lastReportedTimestamp) == days[5]
        !skill5AfterArchive.lastAchievedTimestamp

        def skill6AfterArchive = resAfterArchive.skills.find { it.skillId == 'skill6' }
        skill6AfterArchive.numUserAchieved == 0
        skill6AfterArchive.numUsersInProgress == 0
        !skill6AfterArchive.lastReportedTimestamp
        !skill6AfterArchive.lastAchievedTimestamp
    }

    def "metrics endpoint returns proper counts for imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        p1Skills.each {
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }

        def p2 = createProject(2)
        def p2subj2 = createSubject(2, 2)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj2, [])

        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj2.subjectId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])

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
        def res = skillsService.getMetricsData(p2.projectId, "skillUsageNavigatorChartBuilder", props)

        skillsService.archiveUsers([users[2]], p2.projectId)
        def resAfterArchive = skillsService.getMetricsData(p2.projectId, metricsId, props)

        then:
        res.skills[0].skillName == p1Skills[0].name
        res.skills[0].skillId == p1Skills[0].skillId
        res.skills[0].isReusedSkill == false
        res.skills[0].numUsersInProgress == 1
        res.skills[0].numUserAchieved == 2
        res.skills[0].lastReportedTimestamp == dates[4].time
        res.skills[0].lastAchievedTimestamp == dates[3].time

        resAfterArchive.skills[0].skillName == p1Skills[0].name
        resAfterArchive.skills[0].skillId == p1Skills[0].skillId
        resAfterArchive.skills[0].isReusedSkill == false
        resAfterArchive.skills[0].numUsersInProgress == 1
        resAfterArchive.skills[0].numUserAchieved == 1
        resAfterArchive.skills[0].lastReportedTimestamp == dates[4].time
        resAfterArchive.skills[0].lastAchievedTimestamp == dates[1].time
    }

    def "metrics endpoint returns proper counts for imported group skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 2)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
            skillsService.exportSkillToCatalog(it.projectId, it.skillId)
        }

        def p2 = createProject(2)
        def p2subj2 = createSubject(2, 2)
        def p2subj2g2 = createSkillsGroup(2, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj2, [p2subj2g2])

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj2.subjectId, p2subj2g2.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])

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
        def res = skillsService.getMetricsData(p2.projectId, "skillUsageNavigatorChartBuilder", props)

        skillsService.archiveUsers([users[2]], p2.projectId)
        def resAfterArchive = skillsService.getMetricsData(p2.projectId, metricsId, props)

        then:
        res.skills[0].skillName == p1Skills[0].name
        res.skills[0].skillId == p1Skills[0].skillId
        res.skills[0].isReusedSkill == false
        res.skills[0].numUsersInProgress == 1
        res.skills[0].numUserAchieved == 2
        res.skills[0].lastReportedTimestamp == dates[4].time
        res.skills[0].lastAchievedTimestamp == dates[3].time

        resAfterArchive.skills[0].skillName == p1Skills[0].name
        resAfterArchive.skills[0].skillId == p1Skills[0].skillId
        resAfterArchive.skills[0].isReusedSkill == false
        resAfterArchive.skills[0].numUsersInProgress == 1
        resAfterArchive.skills[0].numUserAchieved == 1
        resAfterArchive.skills[0].lastReportedTimestamp == dates[4].time
        resAfterArchive.skills[0].lastAchievedTimestamp == dates[1].time
    }
}
