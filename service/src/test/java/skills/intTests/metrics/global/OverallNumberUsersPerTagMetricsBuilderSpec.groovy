/**
 * Copyright 2026 SkillTree
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
package skills.intTests.metrics.global


import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserTag
import skills.storage.repos.UserQuizAttemptRepo
import skills.storage.repos.UserTagRepo

import java.text.SimpleDateFormat

class OverallNumberUsersPerTagMetricsBuilderSpec extends DefaultIntSpec {

    String metricsId = "overallNumUsersPerTagBuilder"

    @Autowired
    UserTagRepo userTagRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    def "empty res"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 1)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.getOverallMetricsData(metricsId, [tagKey: "someVal", projIds: "${proj1.projectId},${proj2.projectId}", currentPage: 1, pageSize: 5, sortDesc: true])
        then:
        res.totalNumItems == 0
        !res.items
    }

    def "count users for each tag value across multiple projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }
        List<Map> skills2 = SkillsFactory.createSkills(1, 2)
        skills2.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 1)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        List<String> users = getRandomUsers(20)

        // Split users between projects
        users[0..9].each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], it, new Date())
        }
        users[10..19].each {
            skillsService.addSkill([projectId: proj2.projectId, skillId: skills2.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[10], key: key, value: "blah" ))

        userTagRepo.save(new UserTag(userId: users[2], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[3], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[11], key: key, value: "blah1" ))

        userTagRepo.save(new UserTag(userId: users[4], key: key, value: "blah2" ))

        Map props = [
                projIds: "${proj1.projectId},${proj2.projectId}",
                tagKey: key,
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]

        when:
        def res = skillsService.getOverallMetricsData(metricsId, props)

        skillsService.archiveUsers([users[2]], proj1.projectId)
        def resultAfterArchive = skillsService.getOverallMetricsData(metricsId, props)

        then:
        res.totalNumItems == 3
        res.items.size() == 3
        res.items.find { it.value == "blah"}.count == 3
        res.items.find { it.value == "blah1"}.count == 3
        res.items.find { it.value == "blah2"}.count == 1

        resultAfterArchive.totalNumItems == 3
        resultAfterArchive.items.size() == 3
        resultAfterArchive.items.find { it.value == "blah"}.count == 3
        resultAfterArchive.items.find { it.value == "blah1"}.count == 2
        resultAfterArchive.items.find { it.value == "blah2"}.count == 1
    }

    def "count users for each tag value across projects and quizzes"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }
        List<Map> skills2 = SkillsFactory.createSkills(1, 2)
        skills2.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 1)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        // Create quizzes
        def quiz1 = QuizDefFactory.createQuiz(1)
        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)
        def quiz1Questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        def quiz2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        skillsService.createQuizQuestionDefs(quiz1Questions)
        skillsService.createQuizQuestionDefs(quiz2Questions)

        List<String> users = getRandomUsers(20)

        // Split users between projects and quizzes
        users[0..4].each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], it, new Date())
        }
        users[5..9].each {
            skillsService.addSkill([projectId: proj2.projectId, skillId: skills2.get(0).skillId], it, new Date())
        }
        users[10..14].each {
            runQuiz(it, quiz1, true)
        }
        users[15..19].each {
            runQuiz(it, quiz2, true)
        }

        String key = "someCoolKey"
        // Project users
        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[5], key: key, value: "blah" ))

        // Quiz users
        userTagRepo.save(new UserTag(userId: users[10], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[11], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[15], key: key, value: "blah1" ))

        userTagRepo.save(new UserTag(userId: users[2], key: key, value: "blah2" ))
        userTagRepo.save(new UserTag(userId: users[12], key: key, value: "blah2" ))

        Map props = [
                projIds: "${proj1.projectId},${proj2.projectId}",
                quizIds: "${quiz1.quizId},${quiz2.quizId}",
                tagKey: key,
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]

        when:
        def res = skillsService.getOverallMetricsData(metricsId, props)

        then:
        res.totalNumItems == 3
        res.items.size() == 3
        res.items.find { it.value == "blah"}.count == 3
        res.items.find { it.value == "blah1"}.count == 3
        res.items.find { it.value == "blah2"}.count == 2
    }

    def "count users for each tag value - paging"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }
        List<Map> skills2 = SkillsFactory.createSkills(1, 2)
        skills2.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 1)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills2)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        when:
        def res = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 5, sortDesc: true])
        def res_pg2 = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 2, pageSize: 5, sortDesc: true])
        def res_pg3 = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 3, pageSize: 5, sortDesc: true])
        def res_pg4 = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 4, pageSize: 5, sortDesc: true])
        then:
        res.totalNumItems == 19
        res.items.size() == 5
        res.items.collect { it.count } == [19, 18, 17, 16, 15]

        res_pg2.totalNumItems == 19
        res_pg2.items.size() == 5
        res_pg2.items.collect { it.count } == [14, 13, 12, 11, 10]

        res_pg3.totalNumItems == 19
        res_pg3.items.size() == 5
        res_pg3.items.collect { it.count } == [9, 8, 7, 6, 5]

        res_pg4.totalNumItems == 19
        res_pg4.items.size() == 4
        res_pg4.items.collect { it.count } == [4, 3, 2, 1]
    }

    def "count users for each tag value - filter"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 1)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(20)

        users.each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], it, new Date())
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String tagFilter = "LAh1"
        def resAll = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true])
        def filterdRes = resAll.items.findAll({ it.value.toLowerCase().contains(tagFilter.toLowerCase())}).sort({ it.count }).reverse()

        when:
        def res = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 5, sortDesc: true, tagFilter: tagFilter])
        def res_pg2 = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 2, pageSize: 5, sortDesc: true, tagFilter: tagFilter])
        def res1 = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 5, sortDesc: true, tagFilter: "13"])
        then:

        filterdRes.size() == 10
        res.totalNumItems == 10
        res.items.collect { it.value } == filterdRes.subList(0, 5).collect { it.value }
        res.items.collect { it.count } == filterdRes.subList(0, 5).collect { it.count }

        res_pg2.totalNumItems == 10
        res_pg2.items.collect { it.value } == filterdRes.subList(5, 10).collect { it.value }
        res_pg2.items.collect { it.count } == filterdRes.subList(5, 10).collect { it.count }

        res1.totalNumItems == 1
        res1.items.collect { it.value } == ["blah13"]
    }

    def "count users for each tag value - filter by date"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        List<Map> skills = SkillsFactory.createSkills(1)
        skills.each { it.pointIncrement = 200; it.numPerformToCompletion = 1 }

        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 1)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills)

        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        List<String> users = getRandomUsers(20)
        List<Date> dates = (1..20).collect{ Date.parse("yyyy-MM-dd HH:mm:ss", '2020-08-01 00:00:00').plus(it).toLocalDateTime().toDate()}

        users.eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(0).skillId], it, dates[index])
        }

        String key = "someCoolKey"
        users.eachWithIndex{ String entry, int i ->
            i.times {
                userTagRepo.save(new UserTag(userId: entry, key: key, value: "blah${it}" ))
            }
        }

        String today = format.format(format.parse('2020-08-21 00:00:00'))
        String fiveDaysAgo = format.format(format.parse('2020-08-16 00:00:00'))
        String tenDaysAgo = format.format(format.parse('2020-08-11 00:00:00'))
        String twentyDaysAgo = format.format(format.parse('2020-08-01 00:00:00'))
        def resAll = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true])

        when:
        def res_5days = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: fiveDaysAgo, toDayFilter: today])
        def res_10days = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: tenDaysAgo, toDayFilter: today])
        def res_20days = skillsService.getOverallMetricsData(metricsId, [projIds: "${proj1.projectId},${proj2.projectId}", tagKey: key, currentPage: 1, pageSize: 20, sortDesc: true, fromDayFilter: twentyDaysAgo, toDayFilter: today])

        then:
        resAll.items.size() == 19
        res_5days.items.size() == 19
        res_5days.items == [
            [value:'blah0', count:6],
            [value:'blah1', count:6],
            [value:'blah10', count:6],
            [value:'blah11', count:6],
            [value:'blah12', count:6],
            [value:'blah13', count:6],
            [value:'blah5', count:6],
            [value:'blah6', count:6],
            [value:'blah7', count:6],
            [value:'blah8', count:6],
            [value:'blah9', count:6],
            [value:'blah2', count:6],
            [value:'blah3', count:6],
            [value:'blah4', count:6],
            [value:'blah14', count:5],
            [value:'blah15', count:4],
            [value:'blah16', count:3],
            [value:'blah17', count:2],
            [value:'blah18', count:1]
        ]

        res_10days.items.size() == 19
        res_10days.items == [
                [value:'blah2', count:11],
                [value:'blah1', count:11],
                [value:'blah0', count:11],
                [value:'blah8', count:11],
                [value:'blah7', count:11],
                [value:'blah6', count:11],
                [value:'blah5', count:11],
                [value:'blah4', count:11],
                [value:'blah3', count:11],
                [value:'blah9', count:10],
                [value:'blah10', count:9],
                [value:'blah11', count:8],
                [value:'blah12', count:7],
                [value:'blah13', count:6],
                [value:'blah14', count:5],
                [value:'blah15', count:4],
                [value:'blah16', count:3],
                [value:'blah17', count:2],
                [value:'blah18', count:1]
        ]

        res_20days.items.size() == 19
        res_20days.items == [
                [value:'blah0', count:19],
                [value:'blah1', count:18],
                [value:'blah2', count:17],
                [value:'blah3', count:16],
                [value:'blah4', count:15],
                [value:'blah5', count:14],
                [value:'blah6', count:13],
                [value:'blah7', count:12],
                [value:'blah8', count:11],
                [value:'blah9', count:10],
                [value:'blah10', count:9],
                [value:'blah11', count:8],
                [value:'blah12', count:7],
                [value:'blah13', count:6],
                [value:'blah14', count:5],
                [value:'blah15', count:4],
                [value:'blah16', count:3],
                [value:'blah17', count:2],
                [value:'blah18', count:1]
        ]
    }

    def "quiz only metrics"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)
        def quiz1Questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        def quiz2Questions = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        skillsService.createQuizQuestionDefs(quiz1Questions)
        skillsService.createQuizQuestionDefs(quiz2Questions)

        List<String> users = getRandomUsers(10)

        users[0..4].each {
            runQuiz(it, quiz1, true)
        }
        users[5..9].each {
            runQuiz(it, quiz2, true)
        }

        String key = "someCoolKey"
        userTagRepo.save(new UserTag(userId: users[0], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[1], key: key, value: "blah" ))
        userTagRepo.save(new UserTag(userId: users[5], key: key, value: "blah" ))

        userTagRepo.save(new UserTag(userId: users[2], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[3], key: key, value: "blah1" ))
        userTagRepo.save(new UserTag(userId: users[6], key: key, value: "blah1" ))

        userTagRepo.save(new UserTag(userId: users[4], key: key, value: "blah2" ))
        userTagRepo.save(new UserTag(userId: users[7], key: key, value: "blah2" ))

        Map props = [
                quizIds: "${quiz1.quizId},${quiz2.quizId}",
                tagKey: key,
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]

        when:
        def res = skillsService.getOverallMetricsData(metricsId, props)

        then:
        res.totalNumItems == 3
        res.items.size() == 3
        res.items.find { it.value == "blah"}.count == 3
        res.items.find { it.value == "blah1"}.count == 3
        res.items.find { it.value == "blah2"}.count == 2
    }

    void runQuiz(String userId, def quiz, boolean pass, Date startDate = null) {
        def quizAttempt = skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body

        if(startDate) {
            def userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
            userQuizAttempt.started = startDate
            userQuizAttemptRepo.save(userQuizAttempt)
        }
    }
}
