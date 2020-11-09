package skills.intTests.metrics

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.metricsNew.builders.MetricsPagingParamsHelper
import skills.metricsNew.builders.MetricsParams

class FindExpertsForMultipleProjectsChartBuilderSpec extends DefaultIntSpec {

//    def "find users within multiple projects"() {
//        SkillsService supervisor = createSupervisor();
//        String userId = "user1"
//        String userId2 = "user2"
//        String userId3 = "user3"
//
//        int numProjects = 5;
//        List projSkills = []
//        List projects = []
//        (1..numProjects).each {
//            def proj = SkillsFactory.createProject(it)
//            def subj = SkillsFactory.createSubject(it, 1)
//            def skills = SkillsFactory.createSkills(3, it, 1)
//            skills.each { it.numPerformToCompletion = 1; it.pointIncrement=100 }
//
//            skillsService.createProject(proj)
//            skillsService.createSubject(subj)
//            skillsService.createSkills(skills)
//
//            projects.add(proj)
//            projSkills.add(skills)
//        }
//
//        (projSkills).each {
//            skillsService.addSkill([projectId: it[0].projectId, skillId: it[0].skillId], userId, new Date())
//            if (it[0].projectId.endsWith('2')){
//                skillsService.addSkill([projectId: it[0].projectId, skillId: it[1].skillId], userId, new Date())
//            } else {
//                // user doesn't belong to all projects
//                skillsService.addSkill([projectId: it[0].projectId, skillId: it[1].skillId], userId3, new Date())
//            }
//            if (it[0].projectId.endsWith('4')){
//                skillsService.addSkill([projectId: it[0].projectId, skillId: it[1].skillId], userId, new Date())
//                skillsService.addSkill([projectId: it[0].projectId, skillId: it[2].skillId], userId, new Date())
//            }
//
//            skillsService.addSkill([projectId: it[0].projectId, skillId: it[1].skillId], userId2, new Date())
//            skillsService.addSkill([projectId: it[0].projectId, skillId: it[2].skillId], userId2, new Date())
//            if (it[0].projectId.endsWith('5')){
//                skillsService.addSkill([projectId: it[0].projectId, skillId: it[0].skillId], userId2, new Date())
//            }
//        }
//
//        when:
//        Map props = [:]
//        props[MetricsParams.P_PROJECT_IDS_AND_LEVEL] = projects.collect({"${it.projectId}AndLevel1"}).join(",")
//        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = true
//        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
//        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5
//
//        def res1 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)
//
//        props[MetricsParams.P_PROJECT_IDS_AND_LEVEL] = projects.findAll({ it.projectId != projects[1].projectId}).collect({"${it.projectId}AndLevel1"}).join(",")
//        def res2 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)
//
//        props[MetricsParams.P_PROJECT_IDS_AND_LEVEL] = projects.collect({"${it.projectId}AndLevel3"}).join(",")
//        def res3 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)
//
//        List<String> params = [
//                "${projects[0].projectId}AndLevel2",
//                "${projects[1].projectId}AndLevel3",
//                "${projects[2].projectId}AndLevel2",
//                "${projects[3].projectId}AndLevel5",
//                "${projects[4].projectId}AndLevel2",
//        ]
//        props[MetricsParams.P_PROJECT_IDS_AND_LEVEL] = params.join(",")
//        def res4 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)
//
//        params = [
//                "${projects[0].projectId}AndLevel3",
//                "${projects[1].projectId}AndLevel3",
//                "${projects[2].projectId}AndLevel2",
//                "${projects[3].projectId}AndLevel5",
//                "${projects[4].projectId}AndLevel2",
//        ]
//        props[MetricsParams.P_PROJECT_IDS_AND_LEVEL] = params.join(",")
//        def res5 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)
//
//        then:
//        res1.totalNum == 2
//        res1.data.size() == 2
//        res1.data.find { it.userId == userId }.levels.size() == 5
//        res1.data.find { it.userId == userId }.levels.find { it.projectId == projects[0].projectId}.level == 2
//        res1.data.find { it.userId == userId }.levels.find { it.projectId == projects[1].projectId}.level == 3
//        res1.data.find { it.userId == userId }.levels.find { it.projectId == projects[2].projectId}.level == 2
//        res1.data.find { it.userId == userId }.levels.find { it.projectId == projects[3].projectId}.level == 5
//        res1.data.find { it.userId == userId }.levels.find { it.projectId == projects[4].projectId}.level == 2
//
//        res1.data.find { it.userId == userId2 }.levels.size() == 5
//        res1.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[0].projectId}.level == 3
//        res1.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[1].projectId}.level == 3
//        res1.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[2].projectId}.level == 3
//        res1.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[3].projectId}.level == 3
//        res1.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[4].projectId}.level == 5
//
//        res2.totalNum == 3
//        res2.data.size() == 3
//        res2.data.find { it.userId == userId }.levels.size() == 4
//        res2.data.find { it.userId == userId }.levels.find { it.projectId == projects[0].projectId}.level == 2
//        res2.data.find { it.userId == userId }.levels.find { it.projectId == projects[2].projectId}.level == 2
//        res2.data.find { it.userId == userId }.levels.find { it.projectId == projects[3].projectId}.level == 5
//        res2.data.find { it.userId == userId }.levels.find { it.projectId == projects[4].projectId}.level == 2
//
//        res2.data.find { it.userId == userId2 }.levels.size() == 4
//        res2.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[0].projectId}.level == 3
//        res2.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[2].projectId}.level == 3
//        res2.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[3].projectId}.level == 3
//        res2.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[4].projectId}.level == 5
//
//        res2.data.find { it.userId == userId3 }.levels.size() == 4
//        res2.data.find { it.userId == userId3 }.levels.find { it.projectId == projects[0].projectId}.level == 2
//        res2.data.find { it.userId == userId3 }.levels.find { it.projectId == projects[2].projectId}.level == 2
//        res2.data.find { it.userId == userId3 }.levels.find { it.projectId == projects[3].projectId}.level == 2
//        res2.data.find { it.userId == userId3 }.levels.find { it.projectId == projects[4].projectId}.level == 2
//
//        res3.totalNum == 1
//        res3.data.size() == 1
//        res3.data.find { it.userId == userId2 }.levels.size() == 5
//        res3.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[0].projectId}.level == 3
//        res3.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[1].projectId}.level == 3
//        res3.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[2].projectId}.level == 3
//        res3.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[3].projectId}.level == 3
//        res3.data.find { it.userId == userId2 }.levels.find { it.projectId == projects[4].projectId}.level == 5
//
//        res4.totalNum == 1
//        res4.data.size() == 1
//        res4.data.find { it.userId == userId }.levels.size() == 5
//        res4.data.find { it.userId == userId }.levels.find { it.projectId == projects[0].projectId}.level == 2
//        res4.data.find { it.userId == userId }.levels.find { it.projectId == projects[1].projectId}.level == 3
//        res4.data.find { it.userId == userId }.levels.find { it.projectId == projects[2].projectId}.level == 2
//        res4.data.find { it.userId == userId }.levels.find { it.projectId == projects[3].projectId}.level == 5
//        res4.data.find { it.userId == userId }.levels.find { it.projectId == projects[4].projectId}.level == 2
//
//        !res5.data
//        res5.totalNum == 0
//    }

    def "find users within multiple projects - page through results - verify sorting"() {
        SkillsService supervisor = createSupervisor();
        int numProjects = 2;
        List projSkills = []
        List projects = []
        (1..numProjects).each {
            def proj = SkillsFactory.createProject(it)
            def subj = SkillsFactory.createSubject(it, 1)
            def skills = SkillsFactory.createSkills(3, it, 1)
            skills.each { it.numPerformToCompletion = 1; it.pointIncrement = 100 }

            skillsService.createProject(proj)
            skillsService.createSubject(subj)
            skillsService.createSkills(skills)

            projects.add(proj)
            projSkills.add(skills)
        }

        int numUsers = 9
        List<String> users = (1..numUsers).collect({ "user${it}" })
        List<String> copy = users.toList()
        copy.shuffle()
        (projSkills).each {
            copy.each { String user ->
                skillsService.addSkill([projectId: it[0].projectId, skillId: it[0].skillId], user, new Date())
            }
        }

        when:
        Map props = [:]
        props[MetricsParams.P_PROJECT_IDS_AND_LEVEL] = projects.collect({ "${it.projectId}AndLevel1" }).join(",")
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 5

        def res1 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)

        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 2
        def res2 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)

        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = true
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        def res3 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)

        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 2
        def res4 = supervisor.getGlobalMetricsData("findExpertsForMultipleProjectsChartBuilder", props)

        List<String> usersReversed = users.reverse()
        then:
        res1.totalNum == users.size()
        res1.data.size() == 5
        res1.data[0].userId == users[0]
        res1.data[1].userId == users[1]
        res1.data[2].userId == users[2]
        res1.data[3].userId == users[3]
        res1.data[4].userId == users[4]

        res2.data.size() == 4
        res2.data[0].userId == users[5]
        res2.data[1].userId == users[6]
        res2.data[2].userId == users[7]
        res2.data[3].userId == users[8]


        res3.totalNum == usersReversed.size()
        res3.data.size() == 5
        res3.data[0].userId == usersReversed[0]
        res3.data[1].userId == usersReversed[1]
        res3.data[2].userId == usersReversed[2]
        res3.data[3].userId == usersReversed[3]
        res3.data[4].userId == usersReversed[4]

        res4.data.size() == 4
        res4.data[0].userId == usersReversed[5]
        res4.data[1].userId == usersReversed[6]
        res4.data[2].userId == usersReversed[7]
        res4.data[3].userId == usersReversed[8]
    }
}
