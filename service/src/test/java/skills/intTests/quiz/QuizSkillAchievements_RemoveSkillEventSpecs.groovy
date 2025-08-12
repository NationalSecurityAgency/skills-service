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
package skills.intTests.quiz


import groovy.util.logging.Slf4j
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.model.UserQuizAttempt

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class QuizSkillAchievements_RemoveSkillEventSpecs extends QuizSkillAchievementsBaseIntSpec {

    def "remove skill event which will remove its associated quiz attempt"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)

        def addedSkills = skillsService.getPerformedSkills(userServices[0].userName, proj.projectId)
        UserPerformedSkill toRemove = userPerformedSkillRepo.findAll().find { it.skillId == skills[0].skillId }
        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        skillsService.deleteSkillEvent([projectId: proj.projectId, skillId: skills[0].skillId, userId: userServices[0].userName, timestamp: toRemove.performedOn.time ])
        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)

        then:
        quiz1Runs_t0.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]
        quiz1Runs_t0.data.attemptId == [u1Quiz1AttemptId]
        quiz2Runs_t0.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]
        quiz2Runs_t0.data.attemptId == [u1Quiz2AttemptId]

        !quiz1Runs_t1.data
        quiz2Runs_t1.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]
        quiz2Runs_t1.data.attemptId == [u1Quiz2AttemptId]
    }

    def "remove skill event which will remove its associated quiz (passed only) attempt"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)

        def addedSkills = skillsService.getPerformedSkills(userServices[0].userName, proj.projectId)
        UserPerformedSkill toRemove = userPerformedSkillRepo.findAll().find { it.skillId == skills[0].skillId }
        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        skillsService.deleteSkillEvent([projectId: proj.projectId, skillId: skills[0].skillId, userId: userServices[0].userName, timestamp: toRemove.performedOn.time ])
        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)

        then:
        quiz1Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t0.data.attemptId.sort() == [u1Quiz1AttemptId, u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t0.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()

        quiz1Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t1.data.attemptId.sort() == [u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t1.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
    }

    def "remove skill event -> removes its associated quiz (passed only) attempt -> removes other skill events quiz is linked to"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        def proj2_skills = SkillsFactory.createSkills(5, 2, 1, 100)
        proj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[0].quizId = quiz1.quizId
        proj2_skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)

        def addedSkills = skillsService.getPerformedSkills(userServices[0].userName, proj.projectId)
        UserPerformedSkill toRemove = userPerformedSkillRepo.findAll().find { it.skillId == skills[0].skillId  && it.projectId == proj.projectId}
        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t0 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> projAchievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })

        skillsService.deleteSkillEvent([projectId: proj.projectId, skillId: skills[0].skillId, userId: userServices[0].userName, timestamp: toRemove.performedOn.time ])

        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t1 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> projAchievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })

        then:
        quiz1Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t0.data.attemptId.sort() == [u1Quiz1AttemptId, u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t0.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t0.skillId.sort() == [proj2_skills[0].skillId, proj2_skills[1].skillId].sort()
        projAchievements_t0.level.sort() == [1, 2]

        quiz1Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t1.data.attemptId.sort() == [u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t1.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t1.skillId.sort() == [proj2_skills[1].skillId].sort()
        projAchievements_t1.level.sort() == [1]
    }

    def "remove skill event -> removes its associated quiz (passed only) attempt -> removes other skill events quiz is linked to -> propagates anywhere those skills maye imported via catalog"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 2)
        def proj2_skills = SkillsFactory.createSkills(5, 2, 2, 100)
        proj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[0].quizId = quiz1.quizId
        proj2_skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)
        proj2_skills.each { skillsService.exportSkillToCatalog(proj2.projectId, it.skillId) }

        def proj3 = createProject(3)
        def proj3_subj = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(proj3, proj3_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj3.projectId, proj3_subj.subjectId, proj2_skills.collect { [projectId: proj2.projectId, skillId: it.skillId] })

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 1)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def addedSkills = skillsService.getPerformedSkills(userServices[0].userName, proj.projectId)
        UserPerformedSkill toRemove = userPerformedSkillRepo.findAll().find { it.skillId == skills[0].skillId  && it.projectId == proj.projectId}
        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t0 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> proj2Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })
        List<UserAchievement> proj3Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj3.projectId && !it.skillId })
        List<UserAchievement> proj4Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj4.projectId && !it.skillId })

        skillsService.deleteSkillEvent([projectId: proj.projectId, skillId: skills[0].skillId, userId: userServices[0].userName, timestamp: toRemove.performedOn.time ])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t1 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> proj2Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })
        List<UserAchievement> proj3Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj3.projectId && !it.skillId })
        List<UserAchievement> proj4Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj4.projectId && !it.skillId })

        then:
        quiz1Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t0.data.attemptId.sort() == [u1Quiz1AttemptId, u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t0.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t0.skillId.sort() == [proj2_skills[0].skillId, proj2_skills[1].skillId].sort()
        proj2Achievements_t0.level.sort() == [1, 2]
        proj3Achievements_t0.level.sort() == [1, 2]
        proj4Achievements_t0.level.sort() == [1, 2]

        quiz1Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t1.data.attemptId.sort() == [u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t1.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t1.skillId.sort() == [proj2_skills[1].skillId].sort()
        proj2Achievements_t1.level.sort() == [1]
        proj3Achievements_t1.level.sort() == [1]
        proj4Achievements_t1.level.sort() == [1]
    }

    def "remove ALL skill event for a user - will remove its associated quiz attempts"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> userServices = getRandomUsers(2).collect { createService(it) }
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)
        Integer u1Quiz3AttemptId = passQuiz(userServices[0], quiz3)

        def addedSkills = skillsService.getPerformedSkills(userServices[0].userName, proj.projectId)
        UserPerformedSkill toRemove = userPerformedSkillRepo.findAll().find { it.skillId == skills[0].skillId }
        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        def quiz3Runs_t0 = skillsService.getQuizRuns(quiz3.quizId)
        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: userServices[0].userName])
        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)
        def quiz3Runs_t1 = skillsService.getQuizRuns(quiz3.quizId)

        then:
        quiz1Runs_t0.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]
        quiz1Runs_t0.data.attemptId == [u1Quiz1AttemptId]
        quiz2Runs_t0.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]
        quiz2Runs_t0.data.attemptId == [u1Quiz2AttemptId]
        quiz3Runs_t0.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]
        quiz3Runs_t0.data.attemptId == [u1Quiz3AttemptId]

        !quiz1Runs_t1.data
        !quiz2Runs_t1.data
        quiz3Runs_t1.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]
        quiz3Runs_t1.data.attemptId == [u1Quiz3AttemptId]
    }

    def "remove ALL skill event for a user - will remove its associated quiz (passed only) attempt"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> userServices = getRandomUsers(2).collect { createService(it) }
        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)
        Integer u1Quiz3AttemptId = passQuiz(userServices[0], quiz3)

        Integer u2Quiz1AttemptId = passQuiz(userServices[1], quiz1)
        Integer u2Quiz2AttemptId = passQuiz(userServices[1], quiz2)
        Integer u2Quiz3AttemptId = passQuiz(userServices[1], quiz3)

        def addedSkills = skillsService.getPerformedSkills(userServices[0].userName, proj.projectId)
        UserPerformedSkill toRemove = userPerformedSkillRepo.findAll().find { it.skillId == skills[0].skillId }
        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        def quiz3Runs_t0 = skillsService.getQuizRuns(quiz3.quizId)
        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: userServices[0].userName])
        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)
        def quiz3Runs_t1 = skillsService.getQuizRuns(quiz3.quizId)

        then:
        quiz1Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t0.data.attemptId.sort() == [u2Quiz1AttemptId, u1Quiz1AttemptId, u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t0.data.attemptId.sort() == [u2Quiz2AttemptId, u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        quiz3Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.PASSED.toString()].sort()
        quiz3Runs_t0.data.attemptId.sort() == [u2Quiz3AttemptId, u1Quiz3AttemptId].sort()

        quiz1Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t1.data.attemptId.sort() == [u2Quiz1AttemptId, u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t1.data.attemptId.sort() == [u2Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        quiz3Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.PASSED.toString()].sort()
        quiz3Runs_t1.data.attemptId.sort() == [u2Quiz3AttemptId, u1Quiz3AttemptId].sort()
    }

    def "remove ALL skill event for a user -> removes its associated quizzes (passed only) attempt -> removes other skill events quiz is linked to"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        def proj2_skills = SkillsFactory.createSkills(5, 2, 1, 100)
        proj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[0].quizId = quiz1.quizId
        proj2_skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)

        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t0 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> projAchievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })

        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: userServices[0].userName])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t1 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> proj2Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })

        then:
        quiz1Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t0.data.attemptId.sort() == [u1Quiz1AttemptId, u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t0.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t0.skillId.sort() == [proj2_skills[0].skillId, proj2_skills[1].skillId].sort()
        projAchievements_t0.level.sort() == [1, 2]

        quiz1Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t1.data.attemptId.sort() == [u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t1.data.attemptId.sort() == [u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t1.skillId.sort() == [].sort()
        proj2Achievements_t1.level.sort() == []
        !userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        !userPointsRepo.findAll().findAll { it.projectId == proj2.projectId}
        !userAchievedRepo.findAll().findAll { it.projectId == proj2.projectId}
    }

    def "remove ALL skill event for a user -> removes its associated quiz (passed only) attempt -> removes other skill events quiz is linked to -> propagates anywhere those skills maye imported via catalog"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 2)
        def proj2_skills = SkillsFactory.createSkills(5, 2, 2, 100)
        proj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[0].quizId = quiz1.quizId
        proj2_skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)
        proj2_skills.each { skillsService.exportSkillToCatalog(proj2.projectId, it.skillId) }

        def proj3 = createProject(3)
        def proj3_subj = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(proj3, proj3_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj3.projectId, proj3_subj.subjectId, proj2_skills.collect { [projectId: proj2.projectId, skillId: it.skillId] })

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 1)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t0 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> proj2Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })
        List<UserAchievement> proj3Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj3.projectId && !it.skillId })
        List<UserAchievement> proj4Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj4.projectId && !it.skillId })

        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: userServices[0].userName])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> proj2PerformedSkills_t1 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId}
        List<UserAchievement> proj2Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId })
        List<UserAchievement> proj3Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj3.projectId && !it.skillId })
        List<UserAchievement> proj4Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj4.projectId && !it.skillId })

        then:
        quiz1Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t0.data.attemptId.sort() == [u1Quiz1AttemptId, u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t0.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t0.skillId.sort() == [proj2_skills[0].skillId, proj2_skills[1].skillId].sort()
        proj2Achievements_t0.level.sort() == [1, 2]
        proj3Achievements_t0.level.sort() == [1, 2]
        proj4Achievements_t0.level.sort() == [1, 2]

        quiz1Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t1.data.attemptId.sort() == [u1Quiz1AttemptIdFailed].sort()
        quiz2Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t1.data.attemptId.sort() == [u1Quiz2AttemptIdFailed].sort()
        proj2PerformedSkills_t1.skillId.sort() == [].sort()
        proj2Achievements_t1.level.sort() == []
        proj3Achievements_t1.level.sort() == []
        proj4Achievements_t1.level.sort() == []
        !userPerformedSkillRepo.findAll()
        !userPointsRepo.findAll()
        !userAchievedRepo.findAll()
    }

    def "remove ALL skill event for a user -> removes its associated quiz (passed only) attempt -> removes other skill events quiz is linked to -> propagates anywhere those skills maye imported via catalog - multiple users"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        List<SkillsService> userServices = getRandomUsers(2).collect { createService(it) }

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 2)
        def proj2_skills = SkillsFactory.createSkills(6, 2, 2, 100)
        proj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[0].quizId = quiz1.quizId
        proj2_skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[1].quizId = quiz2.quizId
        proj2_skills[5].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[5].quizId = quiz3.quizId
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills[2].skillId], userServices[0].userName, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills[2].skillId], userServices[1].userName, new Date())
        proj2_skills[0..4].each { skillsService.exportSkillToCatalog(proj2.projectId, it.skillId) }

        def proj3 = createProject(3)
        def proj3_subj = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(proj3, proj3_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj3.projectId, proj3_subj.subjectId, proj2_skills[0..4].collect { [projectId: proj2.projectId, skillId: it.skillId] })
        def proj3_subj2 = createSubject(3, 6)
        skillsService.createSubject(proj3_subj2)
        def proj3_subj2_skills = SkillsFactory.createSkills(1, 3, 6, 100)
        proj3_subj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj3_subj2_skills[0].quizId = quiz1.quizId
        skillsService.createSkills(proj3_subj2_skills)

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 1)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })

        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)
        Integer u1Quiz3AttemptId = passQuiz(userServices[0], quiz3)

        Integer u2Quiz1AttemptIdFailed = failQuiz(userServices[1], quiz1)
        Integer u2Quiz1AttemptId = passQuiz(userServices[1], quiz1)
        Integer u2Quiz2AttemptIdFailed = failQuiz(userServices[1], quiz2)
        Integer u2Quiz2AttemptId = passQuiz(userServices[1], quiz2)
        Integer u2Quiz3AttemptId = passQuiz(userServices[1], quiz3)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        def quiz1Runs_t0 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t0 = skillsService.getQuizRuns(quiz2.quizId)
        List<UserPerformedSkill> u1proj2PerformedSkills_t0 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId && it.userId == userServices[0].userName}
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll()
        List<UserAchievement> u1proj2Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId && it.userId == userServices[0].userName })
        List<UserAchievement> u1proj3Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj3.projectId && !it.skillId && it.userId == userServices[0].userName })
        List<UserAchievement> u1proj4Achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj4.projectId && !it.skillId && it.userId == userServices[0].userName })
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()

        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: userServices[0].userName])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def quiz1Runs_t1 = skillsService.getQuizRuns(quiz1.quizId)
        def quiz2Runs_t1 = skillsService.getQuizRuns(quiz2.quizId)
        def quiz3Runs_t1 = skillsService.getQuizRuns(quiz3.quizId)
        List<UserPerformedSkill> u1proj2PerformedSkills_t1 = userPerformedSkillRepo.findAll().findAll { it.projectId == proj2.projectId && it.userId == userServices[0].userName}
        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll()
        List<UserAchievement> u1proj2Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId && !it.skillId && it.userId == userServices[0].userName })
        List<UserAchievement> u1proj3Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj3.projectId && !it.skillId && it.userId == userServices[0].userName })
        List<UserAchievement> u1proj4Achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj4.projectId && !it.skillId && it.userId == userServices[0].userName })
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()

        // quiz1 -> proj.skill[0]
        // quiz2 -> proj.skill[1]
        // quiz1 -> proj2.proj2_skills[0]
        // quiz2 -> proj2.proj2_skills[1]
        // quiz3 -> proj2.proj2_skills[5]

        // proj2.proj2_skills[0..4] -> exported -> proj3_subj.proj3_subj
        // proj.skills -> exported -> proj4.proj4_subj

        then:
        quiz1Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString(), UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t0.data.attemptId.sort() == [u1Quiz1AttemptId, u1Quiz1AttemptIdFailed, u2Quiz1AttemptId, u2Quiz1AttemptIdFailed].sort()
        quiz2Runs_t0.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString(), UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t0.data.attemptId.sort() == [u1Quiz2AttemptId, u1Quiz2AttemptIdFailed, u2Quiz2AttemptId, u2Quiz2AttemptIdFailed].sort()
        u1proj2PerformedSkills_t0.skillId.sort() == [proj2_skills[0].skillId, proj2_skills[1].skillId, proj2_skills[2].skillId, proj2_skills[5].skillId].sort()
        u1proj2Achievements_t0.level.sort() == [1, 2, 3]
        u1proj3Achievements_t0.level.sort() == [1, 2, 3]
        u1proj4Achievements_t0.level.sort() == [1, 2]

        // imported skills do not get their own copies for performed skill
        // but rather utilize the original from the exported project
        List<String> expectedPerformed_t0 = [
                "${userServices[0].userName}-${proj.projectId}-${skills[0].skillId}",
                "${userServices[0].userName}-${proj.projectId}-${skills[1].skillId}",

                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[0].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[1].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[2].skillId}", // not-quiz reported event
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",

                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}",

                "${userServices[1].userName}-${proj.projectId}-${skills[0].skillId}",
                "${userServices[1].userName}-${proj.projectId}-${skills[1].skillId}",

                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[1].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[2].skillId}", // not-quiz reported event

                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}",
        ].collect { it.toString() }.sort()
        List<String> actualPerformed_t0  = userPerformedSkills_t0.collect { "${it.userId}-${it.projectId}-${it.skillId}".toString() }.sort()
        actualPerformed_t0 == expectedPerformed_t0

        userPoints_t0.collect { UserPoints up -> "${up.userId}-${up.skillId ? "${up.projectId}-${up.skillId}" : up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${proj.projectId}-${skills[0].skillId}-100",
                "${userServices[0].userName}-${proj.projectId}-${skills[1].skillId}-100",
                "${userServices[0].userName}-${proj.projectId}-${subj.subjectId}-200",
                "${userServices[0].userName}-${proj.projectId}-200",

                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[0].skillId}-100",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[1].skillId}-100",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[5].skillId}-100",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_subj.subjectId}-400",
                "${userServices[0].userName}-${proj2.projectId}-400",

                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[0].skillId}-100",
                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[1].skillId}-100",
                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj.subjectId}-300",

                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}-100",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-100",
                "${userServices[0].userName}-${proj3.projectId}-400",

                "${userServices[0].userName}-${proj4.projectId}-${skills[0].skillId}-100",
                "${userServices[0].userName}-${proj4.projectId}-${skills[1].skillId}-100",
                "${userServices[0].userName}-${proj4.projectId}-${proj4_subj.subjectId}-200",
                "${userServices[0].userName}-${proj4.projectId}-200",

                "${userServices[1].userName}-${proj.projectId}-${skills[0].skillId}-100",
                "${userServices[1].userName}-${proj.projectId}-${skills[1].skillId}-100",
                "${userServices[1].userName}-${proj.projectId}-${subj.subjectId}-200",
                "${userServices[1].userName}-${proj.projectId}-200",

                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[0].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[1].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[5].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-400",
                "${userServices[1].userName}-${proj2.projectId}-400",

                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[0].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[1].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-300",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-100",
                "${userServices[1].userName}-${proj3.projectId}-400",

                "${userServices[1].userName}-${proj4.projectId}-${skills[0].skillId}-100",
                "${userServices[1].userName}-${proj4.projectId}-${skills[1].skillId}-100",
                "${userServices[1].userName}-${proj4.projectId}-${proj4_subj.subjectId}-200",
                "${userServices[1].userName}-${proj4.projectId}-200",

        ].collect { it.toString() }.sort ()

        userAchievements_t0.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${proj.projectId}-${skills[0].skillId}",
                "${userServices[0].userName}-${proj.projectId}-${skills[1].skillId}",
                "${userServices[0].userName}-${proj.projectId}-${subj.subjectId}-1",
                "${userServices[0].userName}-${proj.projectId}-${subj.subjectId}-2",
                "${userServices[0].userName}-${proj.projectId}-1",
                "${userServices[0].userName}-${proj.projectId}-2",

                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[0].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[1].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[2].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_subj.subjectId}-1",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_subj.subjectId}-2",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_subj.subjectId}-3",
                "${userServices[0].userName}-${proj2.projectId}-1",
                "${userServices[0].userName}-${proj2.projectId}-2",
                "${userServices[0].userName}-${proj2.projectId}-3",

                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[0].skillId}",
                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[1].skillId}",
                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[2].skillId}",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj.subjectId}-1",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj.subjectId}-2",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj.subjectId}-3",
                "${userServices[0].userName}-${proj3.projectId}-1",
                "${userServices[0].userName}-${proj3.projectId}-2",
                "${userServices[0].userName}-${proj3.projectId}-3",

                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-1",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-2",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-3",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-4",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-5",


                "${userServices[0].userName}-${proj4.projectId}-${skills[0].skillId}",
                "${userServices[0].userName}-${proj4.projectId}-${skills[1].skillId}",
                "${userServices[0].userName}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${userServices[0].userName}-${proj4.projectId}-${proj4_subj.subjectId}-2",
                "${userServices[0].userName}-${proj4.projectId}-1",
                "${userServices[0].userName}-${proj4.projectId}-2",

                "${userServices[1].userName}-${proj.projectId}-${skills[0].skillId}",
                "${userServices[1].userName}-${proj.projectId}-${skills[1].skillId}",
                "${userServices[1].userName}-${proj.projectId}-${subj.subjectId}-1",
                "${userServices[1].userName}-${proj.projectId}-${subj.subjectId}-2",
                "${userServices[1].userName}-${proj.projectId}-1",
                "${userServices[1].userName}-${proj.projectId}-2",

                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[1].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[2].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-1",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-2",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-3",
                "${userServices[1].userName}-${proj2.projectId}-1",
                "${userServices[1].userName}-${proj2.projectId}-2",
                "${userServices[1].userName}-${proj2.projectId}-3",

                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[1].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[2].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-1",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-2",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-3",
                "${userServices[1].userName}-${proj3.projectId}-1",
                "${userServices[1].userName}-${proj3.projectId}-2",
                "${userServices[1].userName}-${proj3.projectId}-3",

                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-1",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-2",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-3",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-4",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-5",

                "${userServices[1].userName}-${proj4.projectId}-${skills[0].skillId}",
                "${userServices[1].userName}-${proj4.projectId}-${skills[1].skillId}",
                "${userServices[1].userName}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${userServices[1].userName}-${proj4.projectId}-${proj4_subj.subjectId}-2",
                "${userServices[1].userName}-${proj4.projectId}-1",
                "${userServices[1].userName}-${proj4.projectId}-2",
        ].collect { it.toString() }.sort ()

        quiz1Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString(), UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz1Runs_t1.data.attemptId.sort() == [u1Quiz1AttemptIdFailed, u2Quiz1AttemptId, u2Quiz1AttemptIdFailed].sort()
        quiz2Runs_t1.data.status.sort() == [UserQuizAttempt.QuizAttemptStatus.FAILED.toString(), UserQuizAttempt.QuizAttemptStatus.PASSED.toString(), UserQuizAttempt.QuizAttemptStatus.FAILED.toString()].sort()
        quiz2Runs_t1.data.attemptId.sort() == [u1Quiz2AttemptIdFailed, u2Quiz2AttemptId, u2Quiz2AttemptIdFailed].sort()
        quiz3Runs_t1.data.attemptId.sort() == [u1Quiz3AttemptId, u2Quiz3AttemptId].sort()
        u1proj2PerformedSkills_t1.skillId.sort() == [proj2_skills[2].skillId, proj2_skills[5].skillId].sort()
        u1proj2Achievements_t1.level.sort() == [1, 2]
        u1proj3Achievements_t1.level.sort() == [1]
        u1proj4Achievements_t1.level.sort() == []

        userPerformedSkills_t1.collect { "${it.userId}-${it.projectId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[2].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",

                "${userServices[1].userName}-${proj.projectId}-${skills[0].skillId}",
                "${userServices[1].userName}-${proj.projectId}-${skills[1].skillId}",

                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[1].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[2].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",

                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}",
        ].sort()

        userPoints_t1.collect { UserPoints up -> "${up.userId}-${up.skillId ? "${up.projectId}-${up.skillId}" : up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[5].skillId}-100",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_subj.subjectId}-200",
                "${userServices[0].userName}-${proj2.projectId}-200",

                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj.subjectId}-100",
                "${userServices[0].userName}-${proj3.projectId}-100",

                "${userServices[1].userName}-${proj.projectId}-${skills[0].skillId}-100",
                "${userServices[1].userName}-${proj.projectId}-${skills[1].skillId}-100",
                "${userServices[1].userName}-${proj.projectId}-${subj.subjectId}-200",
                "${userServices[1].userName}-${proj.projectId}-200",

                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[0].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[1].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[5].skillId}-100",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-400",
                "${userServices[1].userName}-${proj2.projectId}-400",

                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[0].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[1].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[2].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-300",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}-100",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-100",
                "${userServices[1].userName}-${proj3.projectId}-400",

                "${userServices[1].userName}-${proj4.projectId}-${skills[0].skillId}-100",
                "${userServices[1].userName}-${proj4.projectId}-${skills[1].skillId}-100",
                "${userServices[1].userName}-${proj4.projectId}-${proj4_subj.subjectId}-200",
                "${userServices[1].userName}-${proj4.projectId}-200",

        ].collect { it.toString() }.sort ()

        userAchievements_t1.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[2].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_subj.subjectId}-1",
                "${userServices[0].userName}-${proj2.projectId}-${proj2_subj.subjectId}-2",
                "${userServices[0].userName}-${proj2.projectId}-1",
                "${userServices[0].userName}-${proj2.projectId}-2",

                "${userServices[0].userName}-${proj3.projectId}-${proj2_skills[2].skillId}",
                "${userServices[0].userName}-${proj3.projectId}-${proj3_subj.subjectId}-1",
                "${userServices[0].userName}-${proj3.projectId}-1",

                "${userServices[1].userName}-${proj.projectId}-${skills[0].skillId}",
                "${userServices[1].userName}-${proj.projectId}-${skills[1].skillId}",
                "${userServices[1].userName}-${proj.projectId}-${subj.subjectId}-1",
                "${userServices[1].userName}-${proj.projectId}-${subj.subjectId}-2",
                "${userServices[1].userName}-${proj.projectId}-1",
                "${userServices[1].userName}-${proj.projectId}-2",

                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[1].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[2].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_skills[5].skillId}",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-1",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-2",
                "${userServices[1].userName}-${proj2.projectId}-${proj2_subj.subjectId}-3",
                "${userServices[1].userName}-${proj2.projectId}-1",
                "${userServices[1].userName}-${proj2.projectId}-2",
                "${userServices[1].userName}-${proj2.projectId}-3",

                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[1].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj2_skills[2].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-1",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-2",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj.subjectId}-3",

                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2_skills[0].skillId}",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-1",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-2",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-3",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-4",
                "${userServices[1].userName}-${proj3.projectId}-${proj3_subj2.subjectId}-5",

                "${userServices[1].userName}-${proj3.projectId}-1",
                "${userServices[1].userName}-${proj3.projectId}-2",
                "${userServices[1].userName}-${proj3.projectId}-3",

                "${userServices[1].userName}-${proj4.projectId}-${skills[0].skillId}",
                "${userServices[1].userName}-${proj4.projectId}-${skills[1].skillId}",
                "${userServices[1].userName}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${userServices[1].userName}-${proj4.projectId}-${proj4_subj.subjectId}-2",
                "${userServices[1].userName}-${proj4.projectId}-1",
                "${userServices[1].userName}-${proj4.projectId}-2",
        ].collect { it.toString() }.sort ()
    }

    def "remove ALL skill event for a user -> removes badge achievements in all the projects where the same quizzes were linked to -> propagates that removals to projects with those skills imported via catalog"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        List<SkillsService> userServices = getRandomUsers(2).collect { createService(it) }

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj1_badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(proj1_badge1)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: proj1_badge1.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: proj1_badge1.badgeId, skillId: skills.get(1).skillId])
        proj1_badge1.enabled = true
        skillsService.createBadge(proj1_badge1)

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 2)
        def proj2_skills = SkillsFactory.createSkills(6, 2, 2, 100)
        proj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[0].quizId = quiz1.quizId
        proj2_skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[1].quizId = quiz2.quizId
        proj2_skills[5].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[5].quizId = quiz3.quizId
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills[2].skillId], userServices[0].userName, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills[2].skillId], userServices[1].userName, new Date())
        proj2_skills[0..4].each { skillsService.exportSkillToCatalog(proj2.projectId, it.skillId) }

        def proj2_badge = SkillsFactory.createBadge(2, 2)
        skillsService.createBadge(proj2_badge)
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: proj2_badge.badgeId, skillId: proj2_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: proj2_badge.badgeId, skillId: proj2_skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj2.projectId, badgeId: proj2_badge.badgeId, skillId: proj2_skills.get(5).skillId])
        proj2_badge.enabled = true
        skillsService.createBadge(proj2_badge)

        def proj3 = createProject(3)
        def proj3_subj = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(proj3, proj3_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj3.projectId, proj3_subj.subjectId, proj2_skills[0..4].collect { [projectId: proj2.projectId, skillId: it.skillId] })

        def proj3_badge = SkillsFactory.createBadge(3, 1)
        skillsService.createBadge(proj3_badge)
        skillsService.assignSkillToBadge([projectId: proj3.projectId, badgeId: proj3_badge.badgeId, skillId: proj2_skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj3.projectId, badgeId: proj3_badge.badgeId, skillId: proj2_skills.get(1).skillId])
        proj3_badge.enabled = true
        skillsService.createBadge(proj3_badge)

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 1)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })

        def proj4_badge = SkillsFactory.createBadge(4, 1)
        skillsService.createBadge(proj4_badge)
        skillsService.assignSkillToBadge([projectId: proj4.projectId, badgeId: proj4_badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj4.projectId, badgeId: proj4_badge.badgeId, skillId: skills.get(1).skillId])
        proj4_badge.enabled = true
        skillsService.createBadge(proj4_badge)


        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)
        Integer u1Quiz3AttemptId = passQuiz(userServices[0], quiz3)

        Integer u2Quiz1AttemptIdFailed = failQuiz(userServices[1], quiz1)
        Integer u2Quiz1AttemptId = passQuiz(userServices[1], quiz1)
        Integer u2Quiz2AttemptIdFailed = failQuiz(userServices[1], quiz2)
        Integer u2Quiz2AttemptId = passQuiz(userServices[1], quiz2)
        Integer u2Quiz3AttemptId = passQuiz(userServices[1], quiz3)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()

        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: userServices[0].userName])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()

        then:
        userAchievements_t0.find { it.projectId == proj.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[0].userName}
        userAchievements_t0.find { it.projectId == proj.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[1].userName}
        userAchievements_t0.find { it.projectId == proj2.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t0.find { it.projectId == proj2.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[1].userName}
        userAchievements_t0.find { it.projectId == proj3.projectId && it.skillId == proj3_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t0.find { it.projectId == proj3.projectId && it.skillId == proj3_badge.badgeId && it.userId == userServices[1].userName}
        userAchievements_t0.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t0.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == userServices[1].userName}

        !userAchievements_t1.find { it.projectId == proj.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[0].userName}
        userAchievements_t1.find { it.projectId == proj.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[1].userName}
        !userAchievements_t1.find { it.projectId == proj2.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t1.find { it.projectId == proj2.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[1].userName}
        !userAchievements_t1.find { it.projectId == proj3.projectId && it.skillId == proj3_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t1.find { it.projectId == proj3.projectId && it.skillId == proj3_badge.badgeId && it.userId == userServices[1].userName}
        !userAchievements_t1.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t1.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == userServices[1].userName}
    }

    def "remove ALL skill event for a user -> removes global badge achievements in all the projects where the same quizzes were linked to -> propagates that removals to projects with those skills imported via catalog"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        List<SkillsService> userServices = getRandomUsers(2).collect { createService(it) }

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj1_badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createGlobalBadge(proj1_badge1)
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: proj1_badge1.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: proj1_badge1.badgeId, skillId: skills.get(1).skillId])
        proj1_badge1.enabled = true
        skillsService.updateGlobalBadge(proj1_badge1)

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 2)
        def proj2_skills = SkillsFactory.createSkills(6, 2, 2, 100)
        proj2_skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[0].quizId = quiz1.quizId
        proj2_skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[1].quizId = quiz2.quizId
        proj2_skills[5].selfReportingType = SkillDef.SelfReportingType.Quiz
        proj2_skills[5].quizId = quiz3.quizId
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills[2].skillId], userServices[0].userName, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills[2].skillId], userServices[1].userName, new Date())
        proj2_skills[0..4].each { skillsService.exportSkillToCatalog(proj2.projectId, it.skillId) }

        def proj2_badge = SkillsFactory.createBadge(2, 2)
        skillsService.createGlobalBadge(proj2_badge)
        skillsService.assignSkillToGlobalBadge([projectId: proj2.projectId, badgeId: proj2_badge.badgeId, skillId: proj2_skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj2.projectId, badgeId: proj2_badge.badgeId, skillId: proj2_skills.get(1).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj2.projectId, badgeId: proj2_badge.badgeId, skillId: proj2_skills.get(5).skillId])
        proj2_badge.enabled = true
        skillsService.updateGlobalBadge(proj2_badge)

        def proj3 = createProject(3)
        def proj3_subj = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(proj3, proj3_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj3.projectId, proj3_subj.subjectId, proj2_skills[0..4].collect { [projectId: proj2.projectId, skillId: it.skillId] })

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 1)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })


        Integer u1Quiz1AttemptIdFailed = failQuiz(userServices[0], quiz1)
        Integer u1Quiz1AttemptId = passQuiz(userServices[0], quiz1)
        Integer u1Quiz2AttemptIdFailed = failQuiz(userServices[0], quiz2)
        Integer u1Quiz2AttemptId = passQuiz(userServices[0], quiz2)
        Integer u1Quiz3AttemptId = passQuiz(userServices[0], quiz3)

        Integer u2Quiz1AttemptIdFailed = failQuiz(userServices[1], quiz1)
        Integer u2Quiz1AttemptId = passQuiz(userServices[1], quiz1)
        Integer u2Quiz2AttemptIdFailed = failQuiz(userServices[1], quiz2)
        Integer u2Quiz2AttemptId = passQuiz(userServices[1], quiz2)
        Integer u2Quiz3AttemptId = passQuiz(userServices[1], quiz3)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()

        skillsService.deleteAllSkillEvents([projectId: proj.projectId, userId: userServices[0].userName])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()

        then:
        userAchievements_t0.find { !it.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[0].userName}
        userAchievements_t0.find { !it.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[1].userName}
        userAchievements_t0.find { !it.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t0.find { !it.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[1].userName}

        !userAchievements_t1.find { !it.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[0].userName}
        userAchievements_t1.find { !it.projectId && it.skillId == proj1_badge1.badgeId && it.userId == userServices[1].userName}
        !userAchievements_t1.find { !it.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[0].userName}
        userAchievements_t1.find { !it.projectId && it.skillId == proj2_badge.badgeId && it.userId == userServices[1].userName}
    }

}