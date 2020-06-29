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
package skills.intTests.metrics

import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.TestUtils

@Slf4j
class MetricsSpec extends DefaultIntSpec {

    TestUtils testUtils = new TestUtils()

    def badge

    static final String projId = "TestProject1"
    static final String projId2 = "TestProject2"

    static final Map<String, List<String>> sectionBuilders = [
            projects : [
                    'skills.metrics.builders.projects.AchievedSkillsChartBuilder',
                    'skills.metrics.builders.projects.ContinuedUsageChartBuilder',
                    'skills.metrics.builders.projects.DistinctUsersOverTimeChartBuilder',
                    'skills.metrics.builders.projects.NumUsersPerBadgeChartBuilder',
                    'skills.metrics.builders.projects.NumUsersPerLevelChartBuilder',
                    'skills.metrics.builders.projects.OverlookedSkillsChartBuilder',
                    'skills.metrics.builders.projects.TimeToAchieveChartBuilder',
                    'skills.metrics.builders.projects.TopAchievedChartBuilder',
            ],
            subjects : [
                    'skills.metrics.builders.subjects.AchievedSkillsChartBuilder',
                    'skills.metrics.builders.subjects.DistinctUsersOverTimeChartBuilder',
                    'skills.metrics.builders.subjects.NumUsersPerLevelChartBuilder',

            ],
            badges : [
                    'skills.metrics.builders.badges.AchievedPerMonthChartBuilder',
                    'skills.metrics.builders.badges.DistinctUsersOverTimeChartBuilder',
            ],
            skills : [
                    'skills.metrics.builders.skills.DistinctUsersOverTimeChartBuilder',
            ],
            users : [
                    'skills.metrics.builders.users.PointHistoryChartBuilder',
            ],
    ]

    static final Map<String, String> sectionIds = [
            projects : "TestProject1",
            subjects : "subj1",
            badges : "badge1",
            skills : "s12",
            users : "User0",
    ]

    def setup() {
        List<Map> subj1 = (1..5).collect { [projectId: projId, subjectId: "subj1", skillId: "s1${it}".toString(), name: "subj1 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 1, pointIncrementInterval: 0, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj2 = (1..4).collect { [projectId: projId, subjectId: "subj2", skillId: "s2${it}".toString(), name: "subj2 ${it}".toString(), type: "Skill", pointIncrement: 5, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }
        List<Map> subj3 = (1..5).collect { [projectId: projId, subjectId: "subj3", skillId: "23${it}".toString(), name: "subj3 ${it}".toString(), type: "Skill", pointIncrement: 20, numPerformToCompletion: 10, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1] }


        badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']

        skillsService.createSchema([subj1, subj2, subj3])
        subj1.each {
            it.projectId = projId2
        }
        subj2.each{
            it.projectId = projId2
        }
        subj3.each{
            it.projectId = projId2
        }
        skillsService.createSchema([subj1, subj2, subj3])

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: subj1.get(1).skillId)

        List<Date> dates = testUtils.getLastNDays(5)
        List addSkillRes = []
        (0..3).each { userNum ->
            String userId = "User${userNum}"
            (0..4).each {
                addSkillRes << skillsService.addSkill([projectId: projId, skillId: subj1.get(1).skillId], userId, dates.get(it))
                if (userNum == 2) {
                    skillsService.addSkill([projectId: projId2, skillId: subj1.get(1).skillId], userId, dates.get(it))
                }
            }
        }
    }

    def "load individual chart via chart builder id for each section type"() {

        when:
        Map<String, Object> resultsForSections = [:]
        sectionBuilders.each { section, builders ->
            log.info("Building section ${section}...")
            List results = []
            builders.each { builder ->
                def chart = skillsService.getMetricsChart(projId, builder, section, sectionIds[section])
                results.add(chart)
            }
            resultsForSections[section] = results
        }

        then:
        resultsForSections

        sectionBuilders.keySet().each { section ->
            List chartsForSection = resultsForSections[section]
            assert chartsForSection
            chartsForSection.each { chart ->
                assert chart.dataLoaded == true // data is always loaded loading individual chart
            }
        }
    }

    def "load all charts for each section type"() {

        when:
        Set<String> sections = sectionBuilders.keySet()
        Map<String, Object> resultsForSections = [:]
        sections.each { section ->
            log.info("Building section ${section}...")
            def charts = skillsService.getAllMetricsChartsForSection(projId, section, sectionIds[section])
            resultsForSections[section] = charts
        }

        then:
        resultsForSections

        sections.each { section ->
            List chartsForSection = resultsForSections[section]
            assert chartsForSection && chartsForSection.size() == sectionBuilders[section].size() // all charts for the section are built
            chartsForSection.each { chart ->
                assert chart.dataLoaded == true // data is loaded for all charts by default
            }
        }
    }

    def "load all charts for each section type, but limit loading data to first 2 chart types"() {

        when:
        Set<String> sections = sectionBuilders.keySet()
        Map<String, String> props = [loadDataForFirst : '2']
        Map<String, Object> resultsForSections = [:]
        sections.each { section ->
            log.info("Building section ${section}...")
            def charts = skillsService.getAllMetricsChartsForSection(projId, section, sectionIds[section], props)
            resultsForSections[section] = charts
        }

        then:
        resultsForSections

        sections.each { section ->
            List chartsForSection = resultsForSections[section]
            assert chartsForSection && chartsForSection.size() == sectionBuilders[section].size() // all charts for the section are built
            chartsForSection.eachWithIndex { chart, idx ->
                assert chart.dataLoaded == idx < 2 // data should only be loaded for the first 2 charts
            }
        }
    }

    def "returns error for unknown chart builder id"() {

        when:
        skillsService.getMetricsChart(projId, "unknown.builder.id", "projects", projId)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.httpStatus == HttpStatus.BAD_REQUEST
    }

    def "data is always loaded when loading individual chart builder id"() {

        when:
        String section = 'projects'
        Map<String, String> props = [loadDataForFirst : '0']
        def chart = skillsService.getMetricsChart(projId, sectionBuilders[section].first(), section, projId, props)

        then:
        chart
        chart.dataLoaded == true
    }

    def "numDays loaded defaults to 120"() {
        when:
        String chartBuilderId = 'skills.metrics.builders.projects.DistinctUsersOverTimeChartBuilder'
        String section = 'projects'
        def chart = skillsService.getMetricsChart(projId, chartBuilderId, section, projId)

        then:
        chart
        chart.dataLoaded == true
        chart.dataItems.size() == 120
    }

    def "limit numDays loaded"() {
        when:
        String chartBuilderId = 'skills.metrics.builders.projects.DistinctUsersOverTimeChartBuilder'
        String section = 'projects'
        Map<String, String> props = [numDays : '10']
        def chart = skillsService.getMetricsChart(projId, chartBuilderId, section, projId, props)

        then:
        chart
        chart.dataLoaded == true
        chart.dataItems.size() == 10
    }

    def "numMonths loaded defaults to 6"() {
        when:
        String chartBuilderId = 'skills.metrics.builders.badges.AchievedPerMonthChartBuilder'
        String section = 'badges'
        def chart = skillsService.getMetricsChart(projId, chartBuilderId, section, badge.badgeId)

        then:
        chart
        chart.dataLoaded == true
        chart.dataItems.size() == 6
    }

    def "limit numMonths loaded"() {
        when:
        String chartBuilderId = 'skills.metrics.builders.badges.AchievedPerMonthChartBuilder'
        String section = 'badges'
        Map<String, String> props = [numMonths : '3']
        def chart = skillsService.getMetricsChart(projId, chartBuilderId, section, badge.badgeId, props)

        then:
        chart
        chart.dataLoaded == true
        chart.dataItems.size() == 3
    }

    def "Loads all global charts for section type"() {
        when:
        def charts = skillsService.getAllGlobalMetricsChartsForSection("global")

        then:
        charts
        charts.size() == 2
        charts[0].dataLoaded == true
        charts[0].dataItems.size() == 2
        charts[0].dataItems[0].count == 4
        charts[0].dataItems[1].count == 1
        charts[1].dataLoaded == true
        charts[1].dataItems.size() == 2
        charts[1].dataItems[0].count == 14
        charts[1].dataItems[1].count == 14
    }

    def "Does not load global charts if less then 2 projects"() {

        skillsService.deleteAllMyProjects()
        skillsService.createProject(SkillsFactory.createProject(99))

        when:
        def charts = skillsService.getAllGlobalMetricsChartsForSection("global")

        then:
        !charts
    }

    def "Loads global chart for chart builder id and section"() {
        when:
        def chart = skillsService.getGlobalMetricsChart("skills.metrics.builders.global.SkillCountPerProjectBuilder", "global","global")

        then:
        chart
        chart.dataLoaded == true
        chart.dataItems.size() == 2
        chart.dataItems[0].count == 14
        chart.dataItems[1].count == 14
    }

}
