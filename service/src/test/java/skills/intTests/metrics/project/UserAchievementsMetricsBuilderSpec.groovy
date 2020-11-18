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

import groovy.json.JsonSlurper
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import spock.lang.IgnoreRest

class UserAchievementsMetricsBuilderSpec  extends DefaultIntSpec {

    String metricsId = "userAchievementsChartBuilder"

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
        when:
        def res = skillsService.getMetricsData(proj.projectId, metricsId, props)
        then:
        SkillsClientException e = thrown()
        def body = new JsonSlurper().parseText(e.resBody)
        body.explanation == "Metrics[${metricsId}]: Invalid value [userId] for [sortBy] property. Suppored values are [achievedOn, userName]"
    }

}
