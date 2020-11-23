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
package skills.intTests.metrics.project

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.storage.model.SkillDef
import spock.lang.IgnoreRest
import spock.lang.Shared

class UserAchievementsMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "userAchievementsChartBuilder"
    String allAchievementTypes = "${MetricsParams.ACHIEVEMENT_TYPE_OVERALL},${SkillDef.ContainerType.Subject},${SkillDef.ContainerType.Skill},${SkillDef.ContainerType.Badge},${SkillDef.ContainerType.GlobalBadge}"

    @Shared
    List<Date> dates

    def setupSpec() {
        use(TimeCategory) {
            dates = (10..0).collect({
                return it.days.ago;
            })
        }
    }

    def "empty res"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        res
    }

    def "validate that current page is supplied"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
//        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply currentPage param"
    }

    def "validate that current page > 0"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 0
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: current page must be >= 1. Provided [0]"
    }

    def "validate that page size is present"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
//        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply pageSize param"
    }

    def "validate that page size >= 1"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1

        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        boolean first = false
        when:
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 1
        skillsService.getMetricsData(proj.projectId, metricsId, props)
        first = true
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 0
        skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        first
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: page size must not be less than 1. Provided [0]"
    }

    def "validate that sort param is present"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
//        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply sortDesc param with either 'true' or 'false' value"
    }

    def "validate that sort param is a boolean"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = "blah"
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply sortDesc param with either 'true' or 'false' value"
    }

    def "validate that sort username param is present"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = true
//        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Must supply sortBy param"
    }

    def "validate that sort username param is one of the well-known values"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = true
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userId"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Invalid value [userId] for [sortBy] property. Suppored values are [achievedOn, userName]"
    }

    def "get achievements"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(5, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        achieveLevelForUsers(skills, 2, 1, "Subject")
        achieveLevelForUsers(skills, 1, 2, "Subject")
        achieveLevelForUsers(skillsSubj1, 1, 1, "Subject")

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 15
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        def resPage1 = skillsService.getMetricsData(proj.projectId, metricsId, props)
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 2
        def resPage2 = skillsService.getMetricsData(proj.projectId, metricsId, props)
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 3
        def resPage3 = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        res.totalNumItems == 13
        def items = res.items
        items.collect { it.userId } == ["user1_level1",
                                        "user1_level1",
                                        "user1_level1",
                                        "user1_level1",
                                        "user1_level1",
                                        "user1_level2",
                                        "user1_level2",
                                        "user1_level2",
                                        "user1_level2",
                                        "user1_level2",
                                        "user2_level1",
                                        "user2_level1",
                                        "user2_level1"]

        resPage1.items.collect { it.userId } == [
                "user1_level1",
                "user1_level1",
                "user1_level1",
                "user1_level1",
                "user1_level1"]

        resPage2.items.collect { it.userId } == [
                "user1_level2",
                "user1_level2",
                "user1_level2",
                "user1_level2",
                "user1_level2",
        ]
        resPage3.items.collect { it.userId } == [
                "user2_level1",
                "user2_level1",
                "user2_level1"]

    }

    def "get achievements - sorting"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(5, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        achieveLevelForUsers(skills, 2, 1, "Subject")
        achieveLevelForUsers(skills, 1, 2, "Subject")
        achieveLevelForUsers(skillsSubj1, 1, 1, "Subject")

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:
        Closure getPages = {
            props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
            def resPage1 = skillsService.getMetricsData(proj.projectId, metricsId, props)
            props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 2
            def resPage2 = skillsService.getMetricsData(proj.projectId, metricsId, props)
            props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 3
            def resPage3 = skillsService.getMetricsData(proj.projectId, metricsId, props)
            return [resPage1, resPage2, resPage3]
        }

        def defaultSort = getPages()
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = true
        def usernameReversed = getPages()

        Closure getAchievedOnArr = { def resA ->
            List<Long> aOn = []
            aOn.addAll( resA[0].items.collect { it.achievedOn })
            aOn.addAll( resA[1].items.collect { it.achievedOn })
            aOn.addAll( resA[2].items.collect { it.achievedOn })
            return aOn
        }

        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "achievedOn"
        def achievedOnAsc = getPages()

        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = true
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "achievedOn"
        def achievedOnDesc = getPages()

        then:
        getAchievedOnArr(achievedOnAsc) == getAchievedOnArr(defaultSort).sort()
        getAchievedOnArr(achievedOnDesc) == getAchievedOnArr(defaultSort).sort().reverse()
        getAchievedOnArr(achievedOnAsc) == getAchievedOnArr(achievedOnDesc).reverse()
        !getAchievedOnArr(achievedOnAsc).find { !it }
        !getAchievedOnArr(achievedOnDesc).find { !it }

        defaultSort[0].totalNumItems == 13
        defaultSort[1].totalNumItems == 13
        defaultSort[2].totalNumItems == 13
        defaultSort[0].items.collect { it.userId } == [
                "user1_level1",
                "user1_level1",
                "user1_level1",
                "user1_level1",
                "user1_level1"]
        defaultSort[1].items.collect { it.userId } == [
                "user1_level2",
                "user1_level2",
                "user1_level2",
                "user1_level2",
                "user1_level2",
        ]
        defaultSort[2].items.collect { it.userId } == [
                "user2_level1",
                "user2_level1",
                "user2_level1"]

        usernameReversed[0].totalNumItems == 13
        usernameReversed[1].totalNumItems == 13
        usernameReversed[2].totalNumItems == 13
        usernameReversed[0].items.collect { it.userId } == [
                "user2_level1",
                "user2_level1",
                "user2_level1",
                "user1_level2",
                "user1_level2",
        ]
        usernameReversed[1].items.collect { it.userId } == [
                "user1_level2",
                "user1_level2",
                "user1_level2",
                "user1_level1",
                "user1_level1",
        ]
        usernameReversed[2].items.collect { it.userId } == [
                "user1_level1",
                "user1_level1",
                "user1_level1"
        ]
    }

    def "get achievements - filtering by userName"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(5, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        achieveLevelForUsers(skills, 2, 1, "Subject")
        achieveLevelForUsers(skills, 1, 2, "Subject")
        achieveLevelForUsers(skillsSubj1, 1, 1, "Subject")

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:
        props[MetricsParams.P_USERNAME_FILTER] = "user2"
        def user2FilterRes = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_USERNAME_FILTER] = "level1"
        def level2FilterRes = skillsService.getMetricsData(proj.projectId, metricsId, props)
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 2
        def level2FilterResPg2 = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsParams.P_USERNAME_FILTER] = "user1_level2"
        def user1_level2FilterRes = skillsService.getMetricsData(proj.projectId, metricsId, props)

        // with types
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = "${SkillDef.ContainerType.Subject},${SkillDef.ContainerType.Skill},${SkillDef.ContainerType.Badge}"
        def user1_level2PlusAchievementTypesFilterRes = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_ACHIEVEMENT_TYPES] = "${SkillDef.ContainerType.Skill},${SkillDef.ContainerType.Badge}"
        def user1_level2PlusAchievementTypesFilterRes1 = skillsService.getMetricsData(proj.projectId, metricsId, props)

        // with name
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes
        props[MetricsParams.P_USERNAME_FILTER] = "user1"
        props[MetricsParams.P_NAME_FILTER] = "test skill 1"
        def user1PlusNameFilterRes = skillsService.getMetricsData(proj.projectId, metricsId, props)

        // with min level
        props.remove(MetricsParams.P_NAME_FILTER)
        props[MetricsParams.P_MIN_LEVEL] = "1"
        def user1PlusMinLevelRes = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props.remove(MetricsParams.P_MIN_LEVEL)
        props[MetricsParams.P_FROM_DAY_FILTER] = MetricsParams.DAY_FORMAT.format(dates[2])
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 25
        def user1PlusFromDayRes = skillsService.getMetricsData(proj.projectId, metricsId, props)


        props.remove(MetricsParams.P_FROM_DAY_FILTER)
        props[MetricsParams.P_TO_DAY_FILTER] = MetricsParams.DAY_FORMAT.format(dates[1])
        def user1PlusToDayRes = skillsService.getMetricsData(proj.projectId, metricsId, props)

//        println JsonOutput.prettyPrint(JsonOutput.toJson(user1PlusFromDayRes))
//        user1PlusFromDayRes.items.each {
//            println new Date(it.achievedOn)
//        }

        then:
        user2FilterRes.totalNumItems == 3
        user2FilterRes.items.collect { it.userId } == ['user2_level1', 'user2_level1', 'user2_level1']

        level2FilterRes.totalNumItems == 8
        level2FilterRes.items.collect { it.userId } == ['user1_level1', 'user1_level1', 'user1_level1', 'user1_level1', 'user1_level1']
        level2FilterResPg2.totalNumItems == 8
        level2FilterResPg2.items.collect { it.userId } == ['user2_level1', 'user2_level1', 'user2_level1']

        user1_level2FilterRes.totalNumItems == 5
        user1_level2FilterRes.items.collect { it.userId } == ['user1_level2', 'user1_level2', 'user1_level2', 'user1_level2', 'user1_level2']

        user1_level2PlusAchievementTypesFilterRes.totalNumItems == 4
        user1_level2PlusAchievementTypesFilterRes.items.collect { it.type }.sort() == ['Skill', 'Skill', 'Subject', 'Subject']

        user1_level2PlusAchievementTypesFilterRes1.totalNumItems == 2
        user1_level2PlusAchievementTypesFilterRes1.items.collect { it.type }.sort() == ['Skill', 'Skill']

        user1PlusNameFilterRes.totalNumItems == 3
        user1PlusNameFilterRes.items.collect { it.name }.sort() == ['Test Skill 1', 'Test Skill 1', 'Test Skill 1 Subject2']

        user1PlusMinLevelRes.totalNumItems == 6
        user1PlusMinLevelRes.items.collect { it.level }.sort() == [1, 1, 1, 1, 2]

        user1PlusMinLevelRes.totalNumItems == 6
        user1PlusMinLevelRes.items.collect { it.level }.sort() == [1, 1, 1, 1, 2]

        user1PlusFromDayRes.totalNumItems == 5
        user1PlusFromDayRes.items.collect {it.achievedOn}.unique() == [dates[2].time]

        user1PlusToDayRes.totalNumItems == 5
        user1PlusToDayRes.items.collect {it.achievedOn}.unique() == [dates[1].time]
    }

    def "get achievements - filtering by level"() {
        def proj = SkillsFactory.createProject()
        List<Map> skills = SkillsFactory.createSkills(5)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj = SkillsFactory.createSubject()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skillsSubj1 = SkillsFactory.createSkills(5, 1, 2)
        skillsSubj1.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        skillsService.createSubject(subj1)
        skillsService.createSkills(skillsSubj1)

        achieveLevelForUsers(skills, 2, 1, "Subject")
        achieveLevelForUsers(skills, 1, 2, "Subject")
        achieveLevelForUsers(skills, 3, 3, "Subject")
        achieveLevelForUsers(skills, 2, 4, "Subject")
        achieveLevelForUsers(skills, 1, 5, "Subject")
        achieveLevelForUsers(skillsSubj1, 1, 1, "Subject")

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 50
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = allAchievementTypes

        when:
        props[MetricsParams.P_MIN_LEVEL] = 1
        def level1Res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 2
        def level2Res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 3
        def level3Res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 4
        def level4Res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 5
        def level5Res = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_ACHIEVEMENT_TYPES] = "${SkillDef.ContainerType.Subject}"
        props[MetricsParams.P_MIN_LEVEL] = 1
        def level1ResSubj = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 2
        def level2ResSubj = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 3
        def level3ResSubj = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 4
        def level4ResSubj = skillsService.getMetricsData(proj.projectId, metricsId, props)

        props[MetricsParams.P_MIN_LEVEL] = 5
        def level5ResSubj = skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        level1Res.items.collect { it.level }.unique().sort() == [1, 2, 3, 4, 5]
        level2Res.items.collect { it.level }.unique().sort() == [2, 3, 4, 5]
        level3Res.items.collect { it.level }.unique().sort() == [3, 4, 5]
        level4Res.items.collect { it.level }.unique().sort() == [4, 5]
        level5Res.items.collect { it.level }.unique().sort() == [5]

        level1Res.items.collect { it.type }.unique().sort() == ['Overall', 'Subject']
        level2Res.items.collect { it.type }.unique().sort() == ['Overall', 'Subject']
        level3Res.items.collect { it.type }.unique().sort() == ['Overall', 'Subject']
        level4Res.items.collect { it.type }.unique().sort() == ['Subject']
        level5Res.items.collect { it.type }.unique().sort() == ['Subject']

        level1ResSubj.items.collect { it.level }.unique().sort() == [1, 2, 3, 4, 5]
        level2ResSubj.items.collect { it.level }.unique().sort() == [2, 3, 4, 5]
        level3ResSubj.items.collect { it.level }.unique().sort() == [3, 4, 5]
        level4ResSubj.items.collect { it.level }.unique().sort() == [4, 5]
        level5ResSubj.items.collect { it.level }.unique().sort() == [5]


        level1ResSubj.items.collect { it.type }.unique().sort() == ['Subject']
        level2ResSubj.items.collect { it.type }.unique().sort() == ['Subject']
        level3ResSubj.items.collect { it.type }.unique().sort() == ['Subject']
        level4ResSubj.items.collect { it.type }.unique().sort() == ['Subject']
        level5ResSubj.items.collect { it.type }.unique().sort() == ['Subject']
    }

    private void achieveLevelForUsers(List<Map> skills, int numUsers, int level, String type = "Overall") {
        (1..numUsers).each {
            String user = "user${it}_level${level}"
            achieveLevel(skills, user, level, type)
        }
    }

    private void achieveLevel(List<Map> skills, String user, int level, String type = "Overall") {
        use (TimeCategory) {
            boolean found = false
            int skillIndex = 0
            while (!found) {
                def res = skillsService.addSkill([projectId: skills[skillIndex].projectId, skillId: skills[skillIndex].skillId], user, dates[level])
//            println "${user} for [${level}] ${res}"
                found = res.body.completed.findAll({ it.type == type })?.find { it.level == level }
                skillIndex++
            }
        }
    }

}
