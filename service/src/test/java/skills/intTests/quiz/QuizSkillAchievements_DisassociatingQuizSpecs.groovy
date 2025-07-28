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

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class QuizSkillAchievements_DisassociatingQuizSpecs extends QuizSkillAchievementsBaseIntSpec {

    def "disassociating quiz removes associated skill achievements - multiple subjects"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2Skills = SkillsFactory.createSkills(6, 1, 2, 100)
        subj2Skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[0].quizId = quiz2.quizId
        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[1].quizId = quiz3.quizId
        subj2Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[2].quizId = quiz1.quizId
        skillsService.createSkills(subj2Skills)

        List<SkillsService> userServices = getRandomUsers(3).collect { createService(it) }
        passQuiz(userServices[0], quiz1)

        passQuiz(userServices[1], quiz1)
        passQuiz(userServices[1], quiz2)

        passQuiz(userServices[2], quiz1)
        passQuiz(userServices[2], quiz2)
        passQuiz(userServices[2], quiz3)

        when:
        def user1Progress_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[0].quizId = null
        skillsService.createSkill(skills[0])

        def user1Progress_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        subj2Skills[0].selfReportingType = null
        subj2Skills[0].quizId = null
        skillsService.createSkill(subj2Skills[0])

        def user1Progress_t2 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t2 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t2 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_t2 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_t2 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_t2 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_t2 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t2 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t2 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Approval
        subj2Skills[1].quizId = null
        skillsService.createSkill(subj2Skills[1])

        def user1Progress_t3 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t3 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t3 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_t3 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_t3 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_t3 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_t3 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t3 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t3 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        subj2Skills[2].selfReportingType = null
        subj2Skills[2].quizId = null
        skillsService.createSkill(subj2Skills[2])

        def user1Progress_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        then:
        // original  -------------
        user1Progress_t0.points == 100
        user1Progress_t0.totalPoints == 500
        user1Progress_t0.skillsLevel == 1
        user1Progress_t0.skills.skillId == skills.skillId
        user1Progress_t0.skills.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t0.points == 100
        user1ProgressSubj2_t0.totalPoints == 600
        user1ProgressSubj2_t0.skillsLevel == 1
        user1ProgressSubj2_t0.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t0.skills.points == [0, 0, 100, 0, 0, 0]

        user2Progress_t0.points == 100
        user2Progress_t0.totalPoints == 500
        user2Progress_t0.skillsLevel == 1
        user2Progress_t0.skills.skillId == skills.skillId
        user2Progress_t0.skills.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t0.points == 200
        user2ProgressSubj2_t0.totalPoints == 600
        user2ProgressSubj2_t0.skillsLevel == 2
        user2ProgressSubj2_t0.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t0.skills.points == [100, 0, 100, 0, 0, 0]

        user3Progress_t0.points == 100
        user3Progress_t0.totalPoints == 500
        user3Progress_t0.skillsLevel == 1
        user3Progress_t0.skills.skillId == skills.skillId
        user3Progress_t0.skills.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t0.points == 300
        user3ProgressSubj2_t0.totalPoints == 600
        user3ProgressSubj2_t0.skillsLevel == 3
        user3ProgressSubj2_t0.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t0.skills.points == [100, 100, 100, 0, 0, 0]

        user1OverallProgress_t0.points == 200
        user2OverallProgress_t0.points == 300
        user3OverallProgress_t0.points == 400
        user1OverallProgress_t0.skillsLevel == 1
        user2OverallProgress_t0.skillsLevel == 2
        user3OverallProgress_t0.skillsLevel == 2

        // after quiz was disassociated from skill 1
        user1Progress_t1.points == 0
        user1Progress_t1.totalPoints == 500
        user1Progress_t1.skillsLevel == 0
        user1Progress_t1.skills.skillId == skills.skillId
        user1Progress_t1.skills.points == [0, 0, 0, 0, 0]
        user1ProgressSubj2_t1.points == 100
        user1ProgressSubj2_t1.totalPoints == 600
        user1ProgressSubj2_t1.skillsLevel == 1
        user1ProgressSubj2_t1.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t1.skills.points == [0, 0, 100, 0, 0, 0]

        user2Progress_t1.points == 0
        user2Progress_t1.totalPoints == 500
        user2Progress_t1.skillsLevel == 0
        user2Progress_t1.skills.skillId == skills.skillId
        user2Progress_t1.skills.points == [0, 0, 0, 0, 0]
        user2ProgressSubj2_t1.points == 200
        user2ProgressSubj2_t1.totalPoints == 600
        user2ProgressSubj2_t1.skillsLevel == 2
        user2ProgressSubj2_t1.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t1.skills.points == [100, 0, 100, 0, 0, 0]

        user3Progress_t1.points == 0
        user3Progress_t1.totalPoints == 500
        user3Progress_t1.skillsLevel == 0
        user3Progress_t1.skills.skillId == skills.skillId
        user3Progress_t1.skills.points == [0, 0, 0, 0, 0]
        user3ProgressSubj2_t1.points == 300
        user3ProgressSubj2_t1.totalPoints == 600
        user3ProgressSubj2_t1.skillsLevel == 3
        user3ProgressSubj2_t1.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t1.skills.points == [100, 100, 100, 0, 0, 0]

        user1OverallProgress_t1.points == 100
        user2OverallProgress_t1.points == 200
        user3OverallProgress_t1.points == 300
        user1OverallProgress_t1.skillsLevel == 0
        user2OverallProgress_t1.skillsLevel == 1
        user3OverallProgress_t1.skillsLevel == 2

        // after quiz was disassociated from skill 1 of subj2
        user1Progress_t2.points == 0
        user1Progress_t2.totalPoints == 500
        user1Progress_t2.skillsLevel == 0
        user1Progress_t2.skills.skillId == skills.skillId
        user1Progress_t2.skills.points == [0, 0, 0, 0, 0]
        user1ProgressSubj2_t2.points == 100
        user1ProgressSubj2_t2.totalPoints == 600
        user1ProgressSubj2_t2.skillsLevel == 1
        user1ProgressSubj2_t2.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t2.skills.points == [0, 0, 100, 0, 0, 0]

        user2Progress_t2.points == 0
        user2Progress_t2.totalPoints == 500
        user2Progress_t2.skillsLevel == 0
        user2Progress_t2.skills.skillId == skills.skillId
        user2Progress_t2.skills.points == [0, 0, 0, 0, 0]
        user2ProgressSubj2_t2.points == 100
        user2ProgressSubj2_t2.totalPoints == 600
        user2ProgressSubj2_t2.skillsLevel == 1
        user2ProgressSubj2_t2.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t2.skills.points == [0, 0, 100, 0, 0, 0]

        user3Progress_t2.points == 0
        user3Progress_t2.totalPoints == 500
        user3Progress_t2.skillsLevel == 0
        user3Progress_t2.skills.skillId == skills.skillId
        user3Progress_t2.skills.points == [0, 0, 0, 0, 0]
        user3ProgressSubj2_t2.points == 200
        user3ProgressSubj2_t2.totalPoints == 600
        user3ProgressSubj2_t2.skillsLevel == 2
        user3ProgressSubj2_t2.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t2.skills.points == [0, 100, 100, 0, 0, 0]

        user1OverallProgress_t2.points == 100
        user2OverallProgress_t2.points == 100
        user3OverallProgress_t2.points == 200
        user1OverallProgress_t2.skillsLevel == 0
        user2OverallProgress_t2.skillsLevel == 0
        user3OverallProgress_t2.skillsLevel == 1

        // after quiz was disassociated from skill 2 of subj2
        user1Progress_t3.points == 0
        user1Progress_t3.totalPoints == 500
        user1Progress_t3.skillsLevel == 0
        user1Progress_t3.skills.skillId == skills.skillId
        user1Progress_t3.skills.points == [0, 0, 0, 0, 0]
        user1ProgressSubj2_t3.points == 100
        user1ProgressSubj2_t3.totalPoints == 600
        user1ProgressSubj2_t3.skillsLevel == 1
        user1ProgressSubj2_t3.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t3.skills.points == [0, 0, 100, 0, 0, 0]

        user2Progress_t3.points == 0
        user2Progress_t3.totalPoints == 500
        user2Progress_t3.skillsLevel == 0
        user2Progress_t3.skills.skillId == skills.skillId
        user2Progress_t3.skills.points == [0, 0, 0, 0, 0]
        user2ProgressSubj2_t3.points == 100
        user2ProgressSubj2_t3.totalPoints == 600
        user2ProgressSubj2_t3.skillsLevel == 1
        user2ProgressSubj2_t3.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t3.skills.points == [0, 0, 100, 0, 0, 0]

        user3Progress_t3.points == 0
        user3Progress_t3.totalPoints == 500
        user3Progress_t3.skillsLevel == 0
        user3Progress_t3.skills.skillId == skills.skillId
        user3Progress_t3.skills.points == [0, 0, 0, 0, 0]
        user3ProgressSubj2_t3.points == 100
        user3ProgressSubj2_t3.totalPoints == 600
        user3ProgressSubj2_t3.skillsLevel == 1
        user3ProgressSubj2_t3.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t3.skills.points == [0, 0, 100, 0, 0, 0]

        user1OverallProgress_t3.points == 100
        user2OverallProgress_t3.points == 100
        user3OverallProgress_t3.points == 100
        user1OverallProgress_t3.skillsLevel == 0
        user2OverallProgress_t3.skillsLevel == 0
        user3OverallProgress_t3.skillsLevel == 0

        // after quiz was disassociated from skill 3 of subj2
        user1Progress_t4.points == 0
        user1Progress_t4.totalPoints == 500
        user1Progress_t4.skillsLevel == 0
        user1Progress_t4.skills.skillId == skills.skillId
        user1Progress_t4.skills.points == [0, 0, 0, 0, 0]
        user1ProgressSubj2_t4.points == 0
        user1ProgressSubj2_t4.totalPoints == 600
        user1ProgressSubj2_t4.skillsLevel == 0
        user1ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t4.skills.points == [0, 0, 0, 0, 0, 0]

        user2Progress_t4.points == 0
        user2Progress_t4.totalPoints == 500
        user2Progress_t4.skillsLevel == 0
        user2Progress_t4.skills.skillId == skills.skillId
        user2Progress_t4.skills.points == [0, 0, 0, 0, 0]
        user2ProgressSubj2_t4.points == 0
        user2ProgressSubj2_t4.totalPoints == 600
        user2ProgressSubj2_t4.skillsLevel == 0
        user2ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t4.skills.points == [0, 0, 0, 0, 0, 0]

        user3Progress_t4.points == 0
        user3Progress_t4.totalPoints == 500
        user3Progress_t4.skillsLevel == 0
        user3Progress_t4.skills.skillId == skills.skillId
        user3Progress_t4.skills.points == [0, 0, 0, 0, 0]
        user3ProgressSubj2_t4.points == 0
        user3ProgressSubj2_t4.totalPoints == 600
        user3ProgressSubj2_t4.skillsLevel == 0
        user3ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t4.skills.points == [0, 0, 0, 0, 0, 0]

        user1OverallProgress_t4.points == 0
        user2OverallProgress_t4.points == 0
        user3OverallProgress_t4.points == 0
        user1OverallProgress_t4.skillsLevel == 0
        user2OverallProgress_t4.skillsLevel == 0
        user3OverallProgress_t4.skillsLevel == 0
    }

    def "disassociating quiz removes associated skill achievements - multiple subjects- validate UserAchievement, UserPerformedSkill and UserPoints records"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2Skills = SkillsFactory.createSkills(6, 1, 2, 100)
        subj2Skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[0].quizId = quiz2.quizId
        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[1].quizId = quiz3.quizId
        subj2Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[2].quizId = quiz1.quizId
        skillsService.createSkills(subj2Skills)

        List<SkillsService> userServices = getRandomUsers(3).collect { createService(it) }
        passQuiz(userServices[0], quiz1)

        passQuiz(userServices[1], quiz1)
        passQuiz(userServices[1], quiz2)

        passQuiz(userServices[2], quiz1)
        passQuiz(userServices[2], quiz2)
        passQuiz(userServices[2], quiz3)

        when:
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[0].quizId = null
        skillsService.createSkill(skills[0])

        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        subj2Skills[0].selfReportingType = null
        subj2Skills[0].quizId = null
        skillsService.createSkill(subj2Skills[0])

        List<UserPerformedSkill> userPerformedSkills_t2 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t2 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t2 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        subj2Skills[1].selfReportingType = null
        subj2Skills[1].quizId = null
        skillsService.createSkill(subj2Skills[1])

        List<UserPerformedSkill> userPerformedSkills_t3 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t3 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t3 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        subj2Skills[2].selfReportingType = null
        subj2Skills[2].quizId = null
        skillsService.createSkill(subj2Skills[2])

        List<UserPerformedSkill> userPerformedSkills_t4 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t4 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t4 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})


        then:
        userPerformedSkills_t0.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${skills[0].skillId}",
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${skills[0].skillId}",
                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",

                "${userServices[2].userName}-${skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
        ].sort()

        userPoints_t0.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${skills[0].skillId}-100",
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj.subjectId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-200",

                "${userServices[1].userName}-${skills[0].skillId}-100",
                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[1].userName}-${subj.subjectId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-200",
                "${userServices[1].userName}-${proj.projectId}-300",

                "${userServices[2].userName}-${skills[0].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[1].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[2].userName}-${subj.subjectId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-300",
                "${userServices[2].userName}-${proj.projectId}-400",
        ].collect { it.toString() }.sort ()

        achievements_t0.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${skills[0].skillId}",
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj.subjectId}-1",
                "${userServices[0].userName}-${subj2.subjectId}-1",
                "${userServices[0].userName}-${proj.projectId}-1",

                "${userServices[1].userName}-${skills[0].skillId}",
                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",
                "${userServices[1].userName}-${subj.subjectId}-1",
                "${userServices[1].userName}-${subj2.subjectId}-1",
                "${userServices[1].userName}-${subj2.subjectId}-2",
                "${userServices[1].userName}-${proj.projectId}-1",
                "${userServices[1].userName}-${proj.projectId}-2",

                "${userServices[2].userName}-${skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-2",
                "${userServices[2].userName}-${subj2.subjectId}-3",
                "${userServices[2].userName}-${proj.projectId}-1",
                "${userServices[2].userName}-${proj.projectId}-2",
        ].collect { it.toString() }.sort ()


        // after quiz 1 is disassociated from skill1 of subj1
        userPerformedSkills_t1.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
        ].sort()

        userPoints_t1.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-100",

                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-200",
                "${userServices[1].userName}-${proj.projectId}-200",

                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[1].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-300",
                "${userServices[2].userName}-${proj.projectId}-300",
        ].collect { it.toString() }.sort ()

        achievements_t1.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj2.subjectId}-1",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",
                "${userServices[1].userName}-${subj2.subjectId}-1",
                "${userServices[1].userName}-${subj2.subjectId}-2",
                "${userServices[1].userName}-${proj.projectId}-1",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj2.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-2",
                "${userServices[2].userName}-${subj2.subjectId}-3",
                "${userServices[2].userName}-${proj.projectId}-1",
                "${userServices[2].userName}-${proj.projectId}-2",
        ].collect { it.toString() }.sort ()


        // after quiz is disassociated from skill1 of subj1
        userPerformedSkills_t2.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
        ].sort()

        userPoints_t2.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-100",

                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-100",
                "${userServices[1].userName}-${proj.projectId}-100",

                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[1].skillId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-200",
                "${userServices[2].userName}-${proj.projectId}-200",
        ].collect { it.toString() }.sort ()

        achievements_t2.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj2.subjectId}-1",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2.subjectId}-1",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
                "${userServices[2].userName}-${subj2.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-2",
                "${userServices[2].userName}-${proj.projectId}-1",
        ].collect { it.toString() }.sort ()

        // after quiz is disassociated from skill2 of subj1
        userPerformedSkills_t3.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
        ].sort()

        userPoints_t3.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-100",

                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-100",
                "${userServices[1].userName}-${proj.projectId}-100",

                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-100",
                "${userServices[2].userName}-${proj.projectId}-100",
        ].collect { it.toString() }.sort ()

        achievements_t3.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj2.subjectId}-1",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2.subjectId}-1",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2.subjectId}-1",
        ].collect { it.toString() }.sort ()

        !userPerformedSkills_t4
        !userPoints_t4
        !achievements_t4
    }

    def "disassociating quiz removes associated skill achievements - multiple subjects with skill groups- validate UserAchievement, UserPerformedSkill and UserPoints records"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 11)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skillsGroup])
        skills.each { skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, it) }
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2SkillsGroup1 = SkillsFactory.createSkillsGroup(1, 2, 22)
        skillsService.createSkill(subj2SkillsGroup1)
        def subj2SkillsGroup2 = SkillsFactory.createSkillsGroup(1, 2, 33)
        skillsService.createSkill(subj2SkillsGroup2)
        def subj2Skills = SkillsFactory.createSkills(6, 1, 2, 100)
        subj2Skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[0].quizId = quiz2.quizId
        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[1].quizId = quiz3.quizId
        subj2Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[2].quizId = quiz1.quizId
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup1.skillId, subj2Skills[0])
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup1.skillId, subj2Skills[1])
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup2.skillId, subj2Skills[2])
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup2.skillId, subj2Skills[3])
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup2.skillId, subj2Skills[4])
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup2.skillId, subj2Skills[5])

        List<SkillsService> userServices = getRandomUsers(3).collect { createService(it) }
        passQuiz(userServices[0], quiz1)

        passQuiz(userServices[1], quiz1)
        passQuiz(userServices[1], quiz2)

        passQuiz(userServices[2], quiz1)
        passQuiz(userServices[2], quiz2)
        passQuiz(userServices[2], quiz3)

        when:
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t0 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[0].quizId = null
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, skills[0])

        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t1 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        subj2Skills[0].selfReportingType = null
        subj2Skills[0].quizId = null
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup1.skillId, subj2Skills[0])

        List<UserPerformedSkill> userPerformedSkills_t2 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t2 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t2 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        subj2Skills[1].selfReportingType = null
        subj2Skills[1].quizId = null
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup1.skillId, subj2Skills[1])

        List<UserPerformedSkill> userPerformedSkills_t3 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t3 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t3 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        subj2Skills[2].selfReportingType = null
        subj2Skills[2].quizId = null
        skillsService.assignSkillToSkillsGroup(subj2SkillsGroup1.skillId, subj2Skills[2])

        List<UserPerformedSkill> userPerformedSkills_t4 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_t4 = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_t4 = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})


        then:
        userPerformedSkills_t0.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${skills[0].skillId}",
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${skills[0].skillId}",
                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",

                "${userServices[2].userName}-${skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
        ].sort()

        userPoints_t0.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${skills[0].skillId}-100",
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj.subjectId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-200",

                "${userServices[1].userName}-${skills[0].skillId}-100",
                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[1].userName}-${subj.subjectId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-200",
                "${userServices[1].userName}-${proj.projectId}-300",

                "${userServices[2].userName}-${skills[0].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[1].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[2].userName}-${subj.subjectId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-300",
                "${userServices[2].userName}-${proj.projectId}-400",
        ].collect { it.toString() }.sort ()

        achievements_t0.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${skills[0].skillId}",
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj.subjectId}-1",
                "${userServices[0].userName}-${subj2.subjectId}-1",
                "${userServices[0].userName}-${proj.projectId}-1",

                "${userServices[1].userName}-${skills[0].skillId}",
                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",
                "${userServices[1].userName}-${subj.subjectId}-1",
                "${userServices[1].userName}-${subj2.subjectId}-1",
                "${userServices[1].userName}-${subj2.subjectId}-2",
                "${userServices[1].userName}-${proj.projectId}-1",
                "${userServices[1].userName}-${proj.projectId}-2",

                "${userServices[2].userName}-${skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj2SkillsGroup1.skillId}",
                "${userServices[2].userName}-${subj.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-2",
                "${userServices[2].userName}-${subj2.subjectId}-3",
                "${userServices[2].userName}-${proj.projectId}-1",
                "${userServices[2].userName}-${proj.projectId}-2",
        ].collect { it.toString() }.sort ()


        // after quiz 1 is disassociated from skill1 of subj1
        userPerformedSkills_t1.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
        ].sort()

        userPoints_t1.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-100",

                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-200",
                "${userServices[1].userName}-${proj.projectId}-200",

                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[1].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[0].skillId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-300",
                "${userServices[2].userName}-${proj.projectId}-300",
        ].collect { it.toString() }.sort ()

        achievements_t1.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj2.subjectId}-1",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2Skills[0].skillId}",
                "${userServices[1].userName}-${subj2.subjectId}-1",
                "${userServices[1].userName}-${subj2.subjectId}-2",
                "${userServices[1].userName}-${proj.projectId}-1",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
                "${userServices[2].userName}-${subj2Skills[0].skillId}",
                "${userServices[2].userName}-${subj2SkillsGroup1.skillId}",
                "${userServices[2].userName}-${subj2.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-2",
                "${userServices[2].userName}-${subj2.subjectId}-3",
                "${userServices[2].userName}-${proj.projectId}-1",
                "${userServices[2].userName}-${proj.projectId}-2",
        ].collect { it.toString() }.sort ()


        // after quiz is disassociated from skill1 of subj1
        userPerformedSkills_t2.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
        ].sort()

        userPoints_t2.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-100",

                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-100",
                "${userServices[1].userName}-${proj.projectId}-100",

                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2Skills[1].skillId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-200",
                "${userServices[2].userName}-${proj.projectId}-200",
        ].collect { it.toString() }.sort ()

        achievements_t2.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj2.subjectId}-1",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2.subjectId}-1",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2Skills[1].skillId}",
                "${userServices[2].userName}-${subj2.subjectId}-1",
                "${userServices[2].userName}-${subj2.subjectId}-2",
                "${userServices[2].userName}-${proj.projectId}-1",
        ].collect { it.toString() }.sort ()

        // after quiz is disassociated from skill2 of subj1
        userPerformedSkills_t3.collect { "${it.userId}-${it.skillId}"}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
        ].sort()

        userPoints_t3.collect { UserPoints up -> "${up.userId}-${up.skillId ?: up.projectId}-${up.points}".toString()}.sort() == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[0].userName}-${subj2.subjectId}-100",
                "${userServices[0].userName}-${proj.projectId}-100",

                "${userServices[1].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[1].userName}-${subj2.subjectId}-100",
                "${userServices[1].userName}-${proj.projectId}-100",

                "${userServices[2].userName}-${subj2Skills[2].skillId}-100",
                "${userServices[2].userName}-${subj2.subjectId}-100",
                "${userServices[2].userName}-${proj.projectId}-100",
        ].collect { it.toString() }.sort ()

        achievements_t3.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ?: ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${userServices[0].userName}-${subj2Skills[2].skillId}",
                "${userServices[0].userName}-${subj2.subjectId}-1",

                "${userServices[1].userName}-${subj2Skills[2].skillId}",
                "${userServices[1].userName}-${subj2.subjectId}-1",

                "${userServices[2].userName}-${subj2Skills[2].skillId}",
                "${userServices[2].userName}-${subj2.subjectId}-1",
        ].collect { it.toString() }.sort ()

        !userPerformedSkills_t4
        !userPoints_t4
        !achievements_t4
    }

    def "disassociating quiz removes badge achievements"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2Skills = SkillsFactory.createSkills(6, 1, 2, 100)
        subj2Skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[0].quizId = quiz2.quizId
        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[1].quizId = quiz3.quizId
        subj2Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[2].quizId = quiz1.quizId
        skillsService.createSkills(subj2Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: subj2Skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: subj2Skills.get(1).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: subj2Skills.get(2).skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: subj2Skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: subj2Skills.get(2).skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        List<SkillsService> userServices = getRandomUsers(3).collect { createService(it) }
        [quiz1, quiz2, quiz3].each { passQuiz(userServices[0], it) }
        [quiz1, quiz2, quiz3].each { passQuiz(userServices[1], it) }

        when:
        def user1Badge1_t0 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge1.badgeId)
        def user1Badge2_t0 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge2.badgeId)

        def user2Badge1_t0 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge1.badgeId)
        def user2Badge2_t0 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge2.badgeId)

        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        subj2Skills[1].quizId = null
        skillsService.createSkill(subj2Skills[1])

        def user1Badge1_t1 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge1.badgeId)
        def user1Badge2_t1 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge2.badgeId)

        def user2Badge1_t1 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge1.badgeId)
        def user2Badge2_t1 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge2.badgeId)

        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[0].quizId = null
        skillsService.createSkill(skills[0])

        def user1Badge1_t2 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge1.badgeId)
        def user1Badge2_t2 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge2.badgeId)

        def user2Badge1_t2 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge1.badgeId)
        def user2Badge2_t2 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge2.badgeId)
        then:
        user1Badge1_t0.badgeAchieved
        user1Badge2_t0.badgeAchieved
        user2Badge1_t0.badgeAchieved
        user2Badge2_t0.badgeAchieved

        !user1Badge1_t1.badgeAchieved
        user1Badge2_t1.badgeAchieved
        !user2Badge1_t1.badgeAchieved
        user2Badge2_t1.badgeAchieved

        !user1Badge1_t2.badgeAchieved
        !user1Badge2_t2.badgeAchieved
        !user2Badge1_t2.badgeAchieved
        !user2Badge2_t2.badgeAchieved
    }

    def "disassociating quiz removes global badge achievements"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2Skills = SkillsFactory.createSkills(6, 1, 2, 100)
        subj2Skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[0].quizId = quiz2.quizId
        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[1].quizId = quiz3.quizId
        subj2Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[2].quizId = quiz1.quizId
        skillsService.createSkills(subj2Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createGlobalBadge(badge1)
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: subj2Skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: subj2Skills.get(1).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge1.badgeId, skillId: subj2Skills.get(2).skillId])
        badge1.enabled = true
        skillsService.createGlobalBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createGlobalBadge(badge2)
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: subj2Skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: subj2Skills.get(2).skillId])
        badge2.enabled = true
        skillsService.createGlobalBadge(badge2)

        List<SkillsService> userServices = getRandomUsers(3).collect { createService(it) }
        [quiz1, quiz2, quiz3].each { passQuiz(userServices[0], it) }
        [quiz1, quiz2, quiz3].each { passQuiz(userServices[1], it) }

        when:
        def user1Badge1_t0 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge1.badgeId, -1, true)
        def user1Badge2_t0 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge2.badgeId, -1, true)

        def user2Badge1_t0 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge1.badgeId, -1, true)
        def user2Badge2_t0 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge2.badgeId, -1, true)

        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        subj2Skills[1].quizId = null
        skillsService.createSkill(subj2Skills[1])

        def user1Badge1_t1 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge1.badgeId, -1, true)
        def user1Badge2_t1 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge2.badgeId, -1, true)

        def user2Badge1_t1 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge1.badgeId, -1, true)
        def user2Badge2_t1 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge2.badgeId, -1, true)

        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[0].quizId = null
        skillsService.createSkill(skills[0])

        def user1Badge1_t2 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge1.badgeId, -1, true)
        def user1Badge2_t2 = skillsService.getBadgeSummary(userServices[0].userName, proj.projectId, badge2.badgeId, -1, true)

        def user2Badge1_t2 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge1.badgeId, -1, true)
        def user2Badge2_t2 = skillsService.getBadgeSummary(userServices[1].userName, proj.projectId, badge2.badgeId, -1, true)
        then:
        user1Badge1_t0.badgeAchieved
        user1Badge2_t0.badgeAchieved
        user2Badge1_t0.badgeAchieved
        user2Badge2_t0.badgeAchieved

        !user1Badge1_t1.badgeAchieved
        user1Badge2_t1.badgeAchieved
        !user2Badge1_t1.badgeAchieved
        user2Badge2_t1.badgeAchieved

        !user1Badge1_t2.badgeAchieved
        !user1Badge2_t2.badgeAchieved
        !user2Badge1_t2.badgeAchieved
        !user2Badge2_t2.badgeAchieved
    }
}



