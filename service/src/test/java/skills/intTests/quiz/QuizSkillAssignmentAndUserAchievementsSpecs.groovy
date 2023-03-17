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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.*
import skills.storage.model.QuizDefParent
import skills.storage.model.QuizToSkillDef
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class QuizSkillAssignmentAndUserAchievementsSpecs extends DefaultIntSpec {

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    def "users that achieved quiz/survey should get credit when assigned to a skill - edited existing skill"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

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
        def user1OverallProgress_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createSkill(skills[0])

        def user1Progress_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1OverallProgress_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createSkill(skills[1])

        def user1Progress_t2 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t2 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t2 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1OverallProgress_t2 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t2 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t2 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[2].quizId = quiz3.quizId
        skillsService.createSkill(skills[2])

        def user1Progress_t3 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t3 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t3 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1OverallProgress_t3 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t3 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t3 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = quiz3.quizId
        skillsService.createSkill(skills[3])

        def user1Progress_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1OverallProgress_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        then:
        // TIME 0 -------------
        user1Progress_t0.points == 0
        user1Progress_t0.totalPoints == 500
        user1Progress_t0.skillsLevel == 0
        user1Progress_t0.skills.skillId == skills.skillId
        user1Progress_t0.skills.points == [0, 0, 0, 0, 0]

        user2Progress_t0.points == 0
        user2Progress_t0.totalPoints == 500
        user2Progress_t0.skillsLevel == 0
        user2Progress_t0.skills.skillId == skills.skillId
        user2Progress_t0.skills.points == [0, 0, 0, 0, 0]

        user3Progress_t0.points == 0
        user3Progress_t0.totalPoints == 500
        user3Progress_t0.skillsLevel == 0
        user3Progress_t0.skills.skillId == skills.skillId
        user3Progress_t0.skills.points == [0, 0, 0, 0, 0]

        user1OverallProgress_t0.points == 0
        user2OverallProgress_t0.points == 0
        user3OverallProgress_t0.points == 0
        user1OverallProgress_t0.skillsLevel == 0
        user2OverallProgress_t0.skillsLevel == 0
        user3OverallProgress_t0.skillsLevel == 0

        // TIME 1 -------------

        user1Progress_t1.points == 100
        user1Progress_t1.totalPoints == 500
        user1Progress_t1.skillsLevel == 1
        user1Progress_t1.skills.skillId == skills.skillId
        user1Progress_t1.skills.points == [100, 0, 0, 0, 0]

        user2Progress_t1.points == 100
        user2Progress_t1.totalPoints == 500
        user2Progress_t1.skillsLevel == 1
        user2Progress_t1.skills.skillId == skills.skillId
        user2Progress_t1.skills.points == [100, 0, 0, 0, 0]

        user3Progress_t1.points == 100
        user3Progress_t1.totalPoints == 500
        user3Progress_t1.skillsLevel == 1
        user3Progress_t1.skills.skillId == skills.skillId
        user3Progress_t1.skills.points == [100, 0, 0, 0, 0]

        user1OverallProgress_t1.points == 100
        user2OverallProgress_t1.points == 100
        user3OverallProgress_t1.points == 100
        user1OverallProgress_t1.skillsLevel == 1
        user2OverallProgress_t1.skillsLevel == 1
        user3OverallProgress_t1.skillsLevel == 1

        // TIME 2 -------------

        user1Progress_t2.points == 100
        user1Progress_t2.totalPoints == 500
        user1Progress_t2.skillsLevel == 1
        user1Progress_t2.skills.skillId == skills.skillId
        user1Progress_t2.skills.points == [100, 0, 0, 0, 0]

        user2Progress_t2.points == 200
        user2Progress_t2.totalPoints == 500
        user2Progress_t2.skillsLevel == 2
        user2Progress_t2.skills.skillId == skills.skillId
        user2Progress_t2.skills.points == [100, 100, 0, 0, 0]

        user3Progress_t2.points == 200
        user3Progress_t2.totalPoints == 500
        user3Progress_t2.skillsLevel == 2
        user3Progress_t2.skills.skillId == skills.skillId
        user3Progress_t2.skills.points == [100, 100, 0, 0, 0]

        user1OverallProgress_t2.points == 100
        user2OverallProgress_t2.points == 200
        user3OverallProgress_t2.points == 200
        user1OverallProgress_t2.skillsLevel == 1
        user2OverallProgress_t2.skillsLevel == 2
        user3OverallProgress_t2.skillsLevel == 2

        // TIME 3 -------------

        user1Progress_t3.points == 100
        user1Progress_t3.totalPoints == 500
        user1Progress_t3.skillsLevel == 1
        user1Progress_t3.skills.skillId == skills.skillId
        user1Progress_t3.skills.points == [100, 0, 0, 0, 0]

        user2Progress_t3.points == 200
        user2Progress_t3.totalPoints == 500
        user2Progress_t3.skillsLevel == 2
        user2Progress_t3.skills.skillId == skills.skillId
        user2Progress_t3.skills.points == [100, 100, 0, 0, 0]

        user3Progress_t3.points == 300
        user3Progress_t3.totalPoints == 500
        user3Progress_t3.skillsLevel == 3
        user3Progress_t3.skills.skillId == skills.skillId
        user3Progress_t3.skills.points == [100, 100, 100, 0, 0]

        user1OverallProgress_t3.points == 100
        user2OverallProgress_t3.points == 200
        user3OverallProgress_t3.points == 300
        user1OverallProgress_t3.skillsLevel == 1
        user2OverallProgress_t3.skillsLevel == 2
        user3OverallProgress_t3.skillsLevel == 3

        // TIME 3 -------------

        user1Progress_t4.points == 100
        user1Progress_t4.totalPoints == 500
        user1Progress_t4.skillsLevel == 1
        user1Progress_t4.skills.skillId == skills.skillId
        user1Progress_t4.skills.points == [100, 0, 0, 0, 0]

        user2Progress_t4.points == 200
        user2Progress_t4.totalPoints == 500
        user2Progress_t4.skillsLevel == 2
        user2Progress_t4.skills.skillId == skills.skillId
        user2Progress_t4.skills.points == [100, 100, 0, 0, 0]

        user3Progress_t4.points == 400
        user3Progress_t4.totalPoints == 500
        user3Progress_t4.skillsLevel == 4
        user3Progress_t4.skills.skillId == skills.skillId
        user3Progress_t4.skills.points == [100, 100, 100, 100, 0]

        user1OverallProgress_t4.points == 100
        user2OverallProgress_t4.points == 200
        user3OverallProgress_t4.points == 400
        user1OverallProgress_t4.skillsLevel == 1
        user2OverallProgress_t4.skillsLevel == 2
        user3OverallProgress_t4.skillsLevel == 4
    }

    def "users that achieved quiz/survey should get credit when assigned to a skill - brand new skill"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills[0..3])

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
        def user1OverallProgress_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        skills[4].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[4].quizId = quiz1.quizId
        skillsService.createSkill(skills[4])

        def user1Progress_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1OverallProgress_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        then:
        // TIME 0 -------------
        user1Progress_t0.points == 0
        user1Progress_t0.totalPoints == 400
        user1Progress_t0.skillsLevel == 0
        user1Progress_t0.skills.skillId == skills.skillId[0..3]
        user1Progress_t0.skills.points == [0, 0, 0, 0]

        user2Progress_t0.points == 0
        user2Progress_t0.totalPoints == 400
        user2Progress_t0.skillsLevel == 0
        user2Progress_t0.skills.skillId == skills.skillId[0..3]
        user2Progress_t0.skills.points == [0, 0, 0, 0]

        user3Progress_t0.points == 0
        user3Progress_t0.totalPoints == 400
        user3Progress_t0.skillsLevel == 0
        user3Progress_t0.skills.skillId == skills.skillId[0..3]
        user3Progress_t0.skills.points == [0, 0, 0, 0]

        user1OverallProgress_t0.points == 0
        user2OverallProgress_t0.points == 0
        user3OverallProgress_t0.points == 0
        user1OverallProgress_t0.skillsLevel == 0
        user2OverallProgress_t0.skillsLevel == 0
        user3OverallProgress_t0.skillsLevel == 0

        // TIME 1 -------------

        user1Progress_t1.points == 100
        user1Progress_t1.totalPoints == 500
        user1Progress_t1.skillsLevel == 1
        user1Progress_t1.skills.skillId == skills.skillId
        user1Progress_t1.skills.points == [0, 0, 0, 0, 100]

        user2Progress_t1.points == 100
        user2Progress_t1.totalPoints == 500
        user2Progress_t1.skillsLevel == 1
        user2Progress_t1.skills.skillId == skills.skillId
        user2Progress_t1.skills.points == [0, 0, 0, 0, 100]

        user3Progress_t1.points == 100
        user3Progress_t1.totalPoints == 500
        user3Progress_t1.skillsLevel == 1
        user3Progress_t1.skills.skillId == skills.skillId
        user3Progress_t1.skills.points == [0, 0, 0, 0, 100]

        user1OverallProgress_t1.points == 100
        user2OverallProgress_t1.points == 100
        user3OverallProgress_t1.points == 100
        user1OverallProgress_t1.skillsLevel == 1
        user2OverallProgress_t1.skillsLevel == 1
        user3OverallProgress_t1.skillsLevel == 1
    }

    def "users that achieved quiz/survey should get credit when assigned to a skill - multiple subjects"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2Skills = SkillsFactory.createSkills(6, 1, 2, 100)
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

        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
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

        subj2Skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[0].quizId = quiz2.quizId
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

        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[1].quizId = quiz3.quizId
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

        subj2Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[2].quizId = quiz1.quizId
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
        // TIME 0 -------------

        user1Progress_t0.points == 0
        user1Progress_t0.totalPoints == 500
        user1Progress_t0.skillsLevel == 0
        user1Progress_t0.skills.skillId == skills.skillId
        user1Progress_t0.skills.points == [0, 0, 0, 0, 0]
        user1ProgressSubj2_t0.points == 0
        user1ProgressSubj2_t0.totalPoints == 600
        user1ProgressSubj2_t0.skillsLevel == 0
        user1ProgressSubj2_t0.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t0.skills.points == [0, 0, 0, 0, 0, 0]

        user2Progress_t0.points == 0
        user2Progress_t0.totalPoints == 500
        user2Progress_t0.skillsLevel == 0
        user2Progress_t0.skills.skillId == skills.skillId
        user2Progress_t0.skills.points == [0, 0, 0, 0, 0]
        user2ProgressSubj2_t0.points == 0
        user2ProgressSubj2_t0.totalPoints == 600
        user2ProgressSubj2_t0.skillsLevel == 0
        user2ProgressSubj2_t0.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t0.skills.points == [0, 0, 0, 0, 0, 0]

        user3Progress_t0.points == 0
        user3Progress_t0.totalPoints == 500
        user3Progress_t0.skillsLevel == 0
        user3Progress_t0.skills.skillId == skills.skillId
        user3Progress_t0.skills.points == [0, 0, 0, 0, 0]
        user3ProgressSubj2_t0.points == 0
        user3ProgressSubj2_t0.totalPoints == 600
        user3ProgressSubj2_t0.skillsLevel == 0
        user3ProgressSubj2_t0.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t0.skills.points == [0, 0, 0, 0, 0, 0]

        user1OverallProgress_t0.points == 0
        user2OverallProgress_t0.points == 0
        user3OverallProgress_t0.points == 0
        user1OverallProgress_t0.skillsLevel == 0
        user2OverallProgress_t0.skillsLevel == 0
        user3OverallProgress_t0.skillsLevel == 0

        // TIME 1 -------------

        user1Progress_t1.points == 100
        user1Progress_t1.totalPoints == 500
        user1Progress_t1.skillsLevel == 1
        user1Progress_t1.skills.skillId == skills.skillId
        user1Progress_t1.skills.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t1.points == 0
        user1ProgressSubj2_t1.totalPoints == 600
        user1ProgressSubj2_t1.skillsLevel == 0
        user1ProgressSubj2_t1.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t1.skills.points == [0, 0, 0, 0, 0, 0]

        user2Progress_t1.points == 100
        user2Progress_t1.totalPoints == 500
        user2Progress_t1.skillsLevel == 1
        user2Progress_t1.skills.skillId == skills.skillId
        user2Progress_t1.skills.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t1.points == 0
        user2ProgressSubj2_t1.totalPoints == 600
        user2ProgressSubj2_t1.skillsLevel == 0
        user2ProgressSubj2_t1.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t1.skills.points == [0, 0, 0, 0, 0, 0]

        user3Progress_t1.points == 100
        user3Progress_t1.totalPoints == 500
        user3Progress_t1.skillsLevel == 1
        user3Progress_t1.skills.skillId == skills.skillId
        user3Progress_t1.skills.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t1.points == 0
        user3ProgressSubj2_t1.totalPoints == 600
        user3ProgressSubj2_t1.skillsLevel == 0
        user3ProgressSubj2_t1.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t1.skills.points == [0, 0, 0, 0, 0, 0]

        user1OverallProgress_t1.points == 100
        user2OverallProgress_t1.points == 100
        user3OverallProgress_t1.points == 100
        user1OverallProgress_t1.skillsLevel == 0
        user2OverallProgress_t1.skillsLevel == 0
        user3OverallProgress_t1.skillsLevel == 0

        // TIME 2 -------------

        user1Progress_t2.points == 100
        user1Progress_t2.totalPoints == 500
        user1Progress_t2.skillsLevel == 1
        user1Progress_t2.skills.skillId == skills.skillId
        user1Progress_t2.skills.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t2.points == 0
        user1ProgressSubj2_t2.totalPoints == 600
        user1ProgressSubj2_t2.skillsLevel == 0
        user1ProgressSubj2_t2.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t2.skills.points == [0, 0, 0, 0, 0, 0]

        user2Progress_t2.points == 100
        user2Progress_t2.totalPoints == 500
        user2Progress_t2.skillsLevel == 1
        user2Progress_t2.skills.skillId == skills.skillId
        user2Progress_t2.skills.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t2.points == 100
        user2ProgressSubj2_t2.totalPoints == 600
        user2ProgressSubj2_t2.skillsLevel == 1
        user2ProgressSubj2_t2.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t2.skills.points == [100, 0, 0, 0, 0, 0]

        user3Progress_t2.points == 100
        user3Progress_t2.totalPoints == 500
        user3Progress_t2.skillsLevel == 1
        user3Progress_t2.skills.skillId == skills.skillId
        user3Progress_t2.skills.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t2.points == 100
        user3ProgressSubj2_t2.totalPoints == 600
        user3ProgressSubj2_t2.skillsLevel == 1
        user3ProgressSubj2_t2.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t2.skills.points == [100, 0, 0, 0, 0, 0]

        user1OverallProgress_t2.points == 100
        user2OverallProgress_t2.points == 200
        user3OverallProgress_t2.points == 200
        user1OverallProgress_t2.skillsLevel == 0
        user2OverallProgress_t2.skillsLevel == 1
        user3OverallProgress_t2.skillsLevel == 1

        // TIME 3 -------------

        user1Progress_t3.points == 100
        user1Progress_t3.totalPoints == 500
        user1Progress_t3.skillsLevel == 1
        user1Progress_t3.skills.skillId == skills.skillId
        user1Progress_t3.skills.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t3.points == 0
        user1ProgressSubj2_t3.totalPoints == 600
        user1ProgressSubj2_t3.skillsLevel == 0
        user1ProgressSubj2_t3.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t3.skills.points == [0, 0, 0, 0, 0, 0]

        user2Progress_t3.points == 100
        user2Progress_t3.totalPoints == 500
        user2Progress_t3.skillsLevel == 1
        user2Progress_t3.skills.skillId == skills.skillId
        user2Progress_t3.skills.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t3.points == 100
        user2ProgressSubj2_t3.totalPoints == 600
        user2ProgressSubj2_t3.skillsLevel == 1
        user2ProgressSubj2_t3.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t3.skills.points == [100, 0, 0, 0, 0, 0]

        user3Progress_t3.points == 100
        user3Progress_t3.totalPoints == 500
        user3Progress_t3.skillsLevel == 1
        user3Progress_t3.skills.skillId == skills.skillId
        user3Progress_t3.skills.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t3.points == 200
        user3ProgressSubj2_t3.totalPoints == 600
        user3ProgressSubj2_t3.skillsLevel == 2
        user3ProgressSubj2_t3.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t3.skills.points == [100, 100, 0, 0, 0, 0]

        user1OverallProgress_t3.points == 100
        user2OverallProgress_t3.points == 200
        user3OverallProgress_t3.points == 300
        user1OverallProgress_t3.skillsLevel == 0
        user2OverallProgress_t3.skillsLevel == 1
        user3OverallProgress_t3.skillsLevel == 2

        // TIME 3 -------------

        user1Progress_t4.points == 100
        user1Progress_t4.totalPoints == 500
        user1Progress_t4.skillsLevel == 1
        user1Progress_t4.skills.skillId == skills.skillId
        user1Progress_t4.skills.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t4.points == 100
        user1ProgressSubj2_t4.totalPoints == 600
        user1ProgressSubj2_t4.skillsLevel == 1
        user1ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t4.skills.points == [0, 0, 100, 0, 0, 0]

        user2Progress_t4.points == 100
        user2Progress_t4.totalPoints == 500
        user2Progress_t4.skillsLevel == 1
        user2Progress_t4.skills.skillId == skills.skillId
        user2Progress_t4.skills.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t4.points == 200
        user2ProgressSubj2_t4.totalPoints == 600
        user2ProgressSubj2_t4.skillsLevel == 2
        user2ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t4.skills.points == [100, 0, 100, 0, 0, 0]

        user3Progress_t4.points == 100
        user3Progress_t4.totalPoints == 500
        user3Progress_t4.skillsLevel == 1
        user3Progress_t4.skills.skillId == skills.skillId
        user3Progress_t4.skills.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t4.points == 300
        user3ProgressSubj2_t4.totalPoints == 600
        user3ProgressSubj2_t4.skillsLevel == 3
        user3ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t4.skills.points == [100, 100, 100, 0, 0, 0]

        user1OverallProgress_t4.points == 200
        user2OverallProgress_t4.points == 300
        user3OverallProgress_t4.points == 400
        user1OverallProgress_t4.skillsLevel == 1
        user2OverallProgress_t4.skillsLevel == 2
        user3OverallProgress_t4.skillsLevel == 2
    }

    def "users that achieved quiz/survey should get credit when assigned to a skill - multiple subjects with skill groups"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 11)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skillsGroup])
        skills.each { skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, it) }
        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2SkillsGroup1 = SkillsFactory.createSkillsGroup(1, 2, 22)
        skillsService.createSkill(subj2SkillsGroup1)
        def subj2SkillsGroup2 = SkillsFactory.createSkillsGroup(1, 2, 33)
        skillsService.createSkill(subj2SkillsGroup2)
        def subj2Skills = SkillsFactory.createSkills(6, 1, 2, 100)
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
        def user1Progress_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_t0 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t0 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t0 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)
        UserAchievementsInfo achievements_t0 = loadAchievements(userServices)

        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
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
        UserAchievementsInfo achievements_t1 = loadAchievements(userServices)

        subj2Skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[0].quizId = quiz2.quizId
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
        UserAchievementsInfo achievements_t2 = loadAchievements(userServices)

        subj2Skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[1].quizId = quiz3.quizId
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
        UserAchievementsInfo achievements_t3 = loadAchievements(userServices)

        subj2Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[2].quizId = quiz1.quizId
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
        UserAchievementsInfo achievements_t4 = loadAchievements(userServices)

        then:
        // TIME 0 -------------

        user1Progress_t0.points == 0
        user1Progress_t0.totalPoints == 500
        user1Progress_t0.skillsLevel == 0
        user1Progress_t0.skills[0].children.skillId == skills.skillId
        user1Progress_t0.skills[0].children.points == [0, 0, 0, 0, 0]
        user1ProgressSubj2_t0.points == 0
        user1ProgressSubj2_t0.totalPoints == 600
        user1ProgressSubj2_t0.skillsLevel == 0
        user1ProgressSubj2_t0.skills[0].children.skillId == subj2Skills[0..1].skillId
        user1ProgressSubj2_t0.skills[1].children.skillId == subj2Skills[2..5].skillId
        user1ProgressSubj2_t0.skills[0].children.points == [0, 0]
        user1ProgressSubj2_t0.skills[1].children.points == [0, 0, 0, 0]

        user2Progress_t0.points == 0
        user2Progress_t0.totalPoints == 500
        user2Progress_t0.skillsLevel == 0
        user2Progress_t0.skills[0].children.skillId == skills.skillId
        user2Progress_t0.skills[0].children.points == [0, 0, 0, 0, 0]
        user2ProgressSubj2_t0.points == 0
        user2ProgressSubj2_t0.totalPoints == 600
        user2ProgressSubj2_t0.skillsLevel == 0
        user2ProgressSubj2_t0.skills[0].children.skillId == subj2Skills[0..1].skillId
        user2ProgressSubj2_t0.skills[1].children.skillId == subj2Skills[2..5].skillId
        user2ProgressSubj2_t0.skills[0].children.points == [0, 0]
        user2ProgressSubj2_t0.skills[1].children.points == [0, 0, 0, 0]

        user3Progress_t0.points == 0
        user3Progress_t0.totalPoints == 500
        user3Progress_t0.skillsLevel == 0
        user3Progress_t0.skills[0].children.skillId == skills.skillId
        user3Progress_t0.skills[0].children.points == [0, 0, 0, 0, 0]
        user3ProgressSubj2_t0.points == 0
        user3ProgressSubj2_t0.totalPoints == 600
        user3ProgressSubj2_t0.skillsLevel == 0
        user3ProgressSubj2_t0.skills[0].children.skillId == subj2Skills[0..1].skillId
        user3ProgressSubj2_t0.skills[1].children.skillId == subj2Skills[2..5].skillId
        user3ProgressSubj2_t0.skills[0].children.points == [0, 0]
        user3ProgressSubj2_t0.skills[1].children.points == [0, 0, 0, 0]

        user1OverallProgress_t0.points == 0
        user2OverallProgress_t0.points == 0
        user3OverallProgress_t0.points == 0
        user1OverallProgress_t0.skillsLevel == 0
        user2OverallProgress_t0.skillsLevel == 0
        user3OverallProgress_t0.skillsLevel == 0

        !achievements_t0.user1
        !achievements_t0.user2
        !achievements_t0.user3

        // TIME 1 -------------

        user1Progress_t1.points == 100
        user1Progress_t1.totalPoints == 500
        user1Progress_t1.skillsLevel == 1
        user1Progress_t1.skills[0].children.skillId == skills.skillId
        user1Progress_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t1.points == 0
        user1ProgressSubj2_t1.totalPoints == 600
        user1ProgressSubj2_t1.skillsLevel == 0
        user1ProgressSubj2_t1.skills[0].children.points == [0, 0]
        user1ProgressSubj2_t1.skills[1].children.points == [0, 0, 0, 0]
        achievements_t1.user1.size() == 2
        achievements_t1.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t1.user1.find { it.skillId == subj.subjectId  && it.level == 1 }

        user2Progress_t1.points == 100
        user2Progress_t1.totalPoints == 500
        user2Progress_t1.skillsLevel == 1
        user2Progress_t1.skills[0].children.skillId == skills.skillId
        user2Progress_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t1.points == 0
        user2ProgressSubj2_t1.totalPoints == 600
        user2ProgressSubj2_t1.skillsLevel == 0
        user2ProgressSubj2_t1.skills[0].children.points == [0, 0]
        user2ProgressSubj2_t1.skills[1].children.points == [0, 0, 0, 0]
        achievements_t1.user2.size() == 2
        achievements_t1.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t1.user2.find { it.skillId == subj.subjectId  && it.level == 1 }

        user3Progress_t1.points == 100
        user3Progress_t1.totalPoints == 500
        user3Progress_t1.skillsLevel == 1
        user3Progress_t1.skills[0].children.skillId == skills.skillId
        user3Progress_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t1.points == 0
        user3ProgressSubj2_t1.totalPoints == 600
        user3ProgressSubj2_t1.skillsLevel == 0
        user3ProgressSubj2_t1.skills[0].children.points == [0, 0]
        user3ProgressSubj2_t1.skills[1].children.points == [0, 0, 0, 0]
        achievements_t1.user3.size() == 2
        achievements_t1.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t1.user3.find { it.skillId == subj.subjectId  && it.level == 1 }

        user1OverallProgress_t1.points == 100
        user2OverallProgress_t1.points == 100
        user3OverallProgress_t1.points == 100
        user1OverallProgress_t1.skillsLevel == 0
        user2OverallProgress_t1.skillsLevel == 0
        user3OverallProgress_t1.skillsLevel == 0

        // TIME 2 -------------

        user1Progress_t2.points == 100
        user1Progress_t2.totalPoints == 500
        user1Progress_t2.skillsLevel == 1
        user1Progress_t2.skills[0].children.skillId == skills.skillId
        user1Progress_t2.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t2.points == 0
        user1ProgressSubj2_t2.totalPoints == 600
        user1ProgressSubj2_t2.skillsLevel == 0
        user1ProgressSubj2_t2.skills[0].children.points == [0, 0]
        user1ProgressSubj2_t2.skills[1].children.points == [0, 0, 0, 0]
        achievements_t2.user1.size() == 2
        achievements_t2.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t2.user1.find { it.skillId == subj.subjectId  && it.level == 1 }

        user2Progress_t2.points == 100
        user2Progress_t2.totalPoints == 500
        user2Progress_t2.skillsLevel == 1
        user2Progress_t2.skills[0].children.skillId == skills.skillId
        user2Progress_t2.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t2.points == 100
        user2ProgressSubj2_t2.totalPoints == 600
        user2ProgressSubj2_t2.skillsLevel == 1
        user2ProgressSubj2_t2.skills[0].children.points == [100, 0]
        user2ProgressSubj2_t2.skills[1].children.points == [0, 0, 0, 0]
        achievements_t2.user2.size() == 5
        achievements_t2.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t2.user2.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t2.user2.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t2.user2.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t2.user2.find { it.skillId == null  && it.level == 1 }

        user3Progress_t2.points == 100
        user3Progress_t2.totalPoints == 500
        user3Progress_t2.skillsLevel == 1
        user3Progress_t2.skills[0].children.skillId == skills.skillId
        user3Progress_t2.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t2.points == 100
        user3ProgressSubj2_t2.totalPoints == 600
        user3ProgressSubj2_t2.skillsLevel == 1
        user3ProgressSubj2_t2.skills[0].children.points == [100, 0]
        user3ProgressSubj2_t2.skills[1].children.points == [0, 0, 0, 0]
        achievements_t2.user3.size() == 5
        achievements_t2.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t2.user3.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t2.user3.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t2.user3.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t2.user3.find { it.skillId == null  && it.level == 1 }

        user1OverallProgress_t2.points == 100
        user2OverallProgress_t2.points == 200
        user3OverallProgress_t2.points == 200
        user1OverallProgress_t2.skillsLevel == 0
        user2OverallProgress_t2.skillsLevel == 1
        user3OverallProgress_t2.skillsLevel == 1

        // TIME 3 -------------

        user1Progress_t3.points == 100
        user1Progress_t3.totalPoints == 500
        user1Progress_t3.skillsLevel == 1
        user1Progress_t3.skills[0].children.skillId == skills.skillId
        user1Progress_t3.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t3.points == 0
        user1ProgressSubj2_t3.totalPoints == 600
        user1ProgressSubj2_t3.skillsLevel == 0
        user1ProgressSubj2_t3.skills[0].children.points == [0, 0]
        user1ProgressSubj2_t3.skills[1].children.points == [0, 0, 0, 0]
        achievements_t3.user1.size() == 2
        achievements_t3.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t3.user1.find { it.skillId == subj.subjectId  && it.level == 1 }

        user2Progress_t3.points == 100
        user2Progress_t3.totalPoints == 500
        user2Progress_t3.skillsLevel == 1
        user2Progress_t3.skills[0].children.skillId == skills.skillId
        user2Progress_t3.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t3.points == 100
        user2ProgressSubj2_t3.totalPoints == 600
        user2ProgressSubj2_t3.skillsLevel == 1
        user2ProgressSubj2_t3.skills[0].children.points == [100, 0]
        user2ProgressSubj2_t3.skills[1].children.points == [0, 0, 0, 0]
        achievements_t3.user2.size() == 5
        achievements_t3.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t3.user2.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t3.user2.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t3.user2.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t3.user2.find { it.skillId == null  && it.level == 1 }

        user3Progress_t3.points == 100
        user3Progress_t3.totalPoints == 500
        user3Progress_t3.skillsLevel == 1
        user3Progress_t3.skills[0].children.skillId == skills.skillId
        user3Progress_t3.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t3.points == 200
        user3ProgressSubj2_t3.totalPoints == 600
        user3ProgressSubj2_t3.skillsLevel == 2
        user3ProgressSubj2_t3.skills[0].children.points == [100, 100]
        user3ProgressSubj2_t3.skills[1].children.points == [0, 0, 0, 0]
        achievements_t3.user3.size() == 9
        achievements_t3.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t3.user3.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t3.user3.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t3.user3.find { it.skillId == subj2Skills[1].skillId  && it.level == null }
        achievements_t3.user3.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t3.user3.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_t3.user3.find { it.skillId == null  && it.level == 1 }
        achievements_t3.user3.find { it.skillId == null  && it.level == 2 }
        achievements_t3.user3.find { it.skillId == subj2SkillsGroup1.skillId  && it.level == null }

        user1OverallProgress_t3.points == 100
        user2OverallProgress_t3.points == 200
        user3OverallProgress_t3.points == 300
        user1OverallProgress_t3.skillsLevel == 0
        user2OverallProgress_t3.skillsLevel == 1
        user3OverallProgress_t3.skillsLevel == 2

        // TIME 3 -------------

        user1Progress_t4.points == 100
        user1Progress_t4.totalPoints == 500
        user1Progress_t4.skillsLevel == 1
        user1Progress_t4.skills[0].children.skillId == skills.skillId
        user1Progress_t4.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t4.points == 100
        user1ProgressSubj2_t4.totalPoints == 600
        user1ProgressSubj2_t4.skillsLevel == 1
        user1ProgressSubj2_t4.skills[0].children.points == [0, 0]
        user1ProgressSubj2_t4.skills[1].children.points == [100, 0, 0, 0]
        achievements_t4.user1.size() == 5
        achievements_t4.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t4.user1.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t4.user1.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t4.user1.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t4.user1.find { it.skillId == null  && it.level == 1 }

        user2Progress_t4.points == 100
        user2Progress_t4.totalPoints == 500
        user2Progress_t4.skillsLevel == 1
        user2Progress_t4.skills[0].children.skillId == skills.skillId
        user2Progress_t4.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t4.points == 200
        user2ProgressSubj2_t4.totalPoints == 600
        user2ProgressSubj2_t4.skillsLevel == 2
        user2ProgressSubj2_t4.skills[0].children.points == [100, 0]
        user2ProgressSubj2_t4.skills[1].children.points == [100, 0, 0, 0]
        achievements_t4.user2.size() == 8
        achievements_t4.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t4.user2.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t4.user2.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t4.user2.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t4.user2.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t4.user2.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_t4.user2.find { it.skillId == null  && it.level == 1 }
        achievements_t4.user2.find { it.skillId == null  && it.level == 2 }

        user3Progress_t4.points == 100
        user3Progress_t4.totalPoints == 500
        user3Progress_t4.skillsLevel == 1
        user3Progress_t4.skills[0].children.skillId == skills.skillId
        user3Progress_t4.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t4.points == 300
        user3ProgressSubj2_t4.totalPoints == 600
        user3ProgressSubj2_t4.skillsLevel == 3
        user3ProgressSubj2_t4.skills[0].children.points == [100, 100]
        user3ProgressSubj2_t4.skills[1].children.points == [100, 0, 0, 0]
        achievements_t4.user3.size() == 11
        achievements_t4.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t4.user3.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t4.user3.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t4.user3.find { it.skillId == subj2Skills[1].skillId  && it.level == null }
        achievements_t4.user3.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t4.user3.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t4.user3.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_t4.user3.find { it.skillId == subj2.subjectId  && it.level == 3 }
        achievements_t4.user3.find { it.skillId == null  && it.level == 1 }
        achievements_t4.user3.find { it.skillId == null  && it.level == 2 }
        achievements_t4.user3.find { it.skillId == subj2SkillsGroup1.skillId  && it.level == null }

        user1OverallProgress_t4.points == 200
        user2OverallProgress_t4.points == 300
        user3OverallProgress_t4.points == 400
        user1OverallProgress_t4.skillsLevel == 1
        user2OverallProgress_t4.skillsLevel == 2
        user3OverallProgress_t4.skillsLevel == 2
    }

    def "users that achieved quiz/survey should get credit when assigned to a skill - UserAchievement, UserPerformedSkill and UserPoints records are created"() {
        def quiz1 = createQuiz(1)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        passQuiz(userServices[0], quiz1)

        Integer skillRefId = skillDefRepo.findByProjectIdAndSkillId(proj.projectId, skills[0].skillId).id
        when:
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll()
        List<UserAchievement> achievements_t0 = userAchievedRepo.findAll()
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createSkill(skills[0])
        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll()
        List<UserAchievement> achievements_t1 = userAchievedRepo.findAll()
        then:
        !userPerformedSkills_t0
        !userPoints_t0
        !achievements_t0

        userPerformedSkills_t1.size() == 1
        userPerformedSkills_t1[0].userId == userServices[0].userName
        userPerformedSkills_t1[0].skillId == skills[0].skillId
        userPerformedSkills_t1[0].projectId == proj.projectId
        userPerformedSkills_t1[0].skillRefId == skillRefId
        userPerformedSkills_t1[0].performedOn

        userPoints_t1.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserPoints skillUserPoints = userPoints_t1.find { it.skillId == skills[0].skillId }
        skillUserPoints.userId == userServices[0].userName
        skillUserPoints.skillId == skills[0].skillId
        skillUserPoints.projectId == proj.projectId
        skillUserPoints.skillRefId == skillRefId
        skillUserPoints.points == skills[0].pointIncrement

        achievements_t1.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserAchievement skillAchievement = achievements_t1.find { it.skillId == skills[0].skillId }
        skillAchievement.userId == userServices[0].userName
        skillAchievement.skillId == skills[0].skillId
        skillAchievement.projectId == proj.projectId
        skillAchievement.skillRefId == skillRefId
        skillAchievement.pointsWhenAchieved == skills[0].pointIncrement
        !skillAchievement.level
    }

    def "users that achieve quiz/survey get credit for assigned skills - UserAchievement, UserPerformedSkill and UserPoints records are created"() {
        def quiz1 = createQuiz(1)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        Integer skillRefId = skillDefRepo.findByProjectIdAndSkillId(proj.projectId, skills[0].skillId).id
        when:
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll()
        List<UserAchievement> achievements_t0 = userAchievedRepo.findAll()
        passQuiz(userServices[0], quiz1)
        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll()
        List<UserAchievement> achievements_t1 = userAchievedRepo.findAll()
        then:
        !userPerformedSkills_t0
        !userPoints_t0
        !achievements_t0

        userPerformedSkills_t1.size() == 1
        userPerformedSkills_t1[0].userId == userServices[0].userName
        userPerformedSkills_t1[0].skillId == skills[0].skillId
        userPerformedSkills_t1[0].projectId == proj.projectId
        userPerformedSkills_t1[0].skillRefId == skillRefId
        userPerformedSkills_t1[0].performedOn

        userPoints_t1.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserPoints skillUserPoints = userPoints_t1.find { it.skillId == skills[0].skillId }
        skillUserPoints.userId == userServices[0].userName
        skillUserPoints.skillId == skills[0].skillId
        skillUserPoints.projectId == proj.projectId
        skillUserPoints.skillRefId == skillRefId
        skillUserPoints.points == skills[0].pointIncrement

        achievements_t1.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserAchievement skillAchievement = achievements_t1.find { it.skillId == skills[0].skillId }
        skillAchievement.userId == userServices[0].userName
        skillAchievement.skillId == skills[0].skillId
        skillAchievement.projectId == proj.projectId
        skillAchievement.skillRefId == skillRefId
        skillAchievement.pointsWhenAchieved == skills[0].pointIncrement
        !skillAchievement.level
    }

    def "copy project with quiz skills - quiz user achievements must be reflected in a new project - multiple subjects"() {
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
        def projToCopy = createProject(2)
        skillsService.copyProject(proj.projectId, projToCopy)

        def user1Progress_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_t4 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_t4 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_t4 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)

        def user1Progress_copied = skillsService.getSkillSummary(userServices[0].userName, projToCopy.projectId, subj.subjectId)
        def user2Progress_copied = skillsService.getSkillSummary(userServices[1].userName, projToCopy.projectId, subj.subjectId)
        def user3Progress_copied = skillsService.getSkillSummary(userServices[2].userName, projToCopy.projectId, subj.subjectId)
        def user1ProgressSubj2_copied = skillsService.getSkillSummary(userServices[0].userName, projToCopy.projectId, subj2.subjectId)
        def user2ProgressSubj2_copied = skillsService.getSkillSummary(userServices[1].userName, projToCopy.projectId, subj2.subjectId)
        def user3ProgressSubj2_copied = skillsService.getSkillSummary(userServices[2].userName, projToCopy.projectId, subj2.subjectId)
        def user1OverallProgress_copied = skillsService.getSkillSummary(userServices[0].userName, projToCopy.projectId)
        def user2OverallProgress_copied = skillsService.getSkillSummary(userServices[1].userName, projToCopy.projectId)
        def user3OverallProgress_copied = skillsService.getSkillSummary(userServices[2].userName, projToCopy.projectId)

        then:
        // original  -------------
        user1Progress_t4.points == 100
        user1Progress_t4.totalPoints == 500
        user1Progress_t4.skillsLevel == 1
        user1Progress_t4.skills.skillId == skills.skillId
        user1Progress_t4.skills.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t4.points == 100
        user1ProgressSubj2_t4.totalPoints == 600
        user1ProgressSubj2_t4.skillsLevel == 1
        user1ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_t4.skills.points == [0, 0, 100, 0, 0, 0]

        user2Progress_t4.points == 100
        user2Progress_t4.totalPoints == 500
        user2Progress_t4.skillsLevel == 1
        user2Progress_t4.skills.skillId == skills.skillId
        user2Progress_t4.skills.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t4.points == 200
        user2ProgressSubj2_t4.totalPoints == 600
        user2ProgressSubj2_t4.skillsLevel == 2
        user2ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_t4.skills.points == [100, 0, 100, 0, 0, 0]

        user3Progress_t4.points == 100
        user3Progress_t4.totalPoints == 500
        user3Progress_t4.skillsLevel == 1
        user3Progress_t4.skills.skillId == skills.skillId
        user3Progress_t4.skills.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t4.points == 300
        user3ProgressSubj2_t4.totalPoints == 600
        user3ProgressSubj2_t4.skillsLevel == 3
        user3ProgressSubj2_t4.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_t4.skills.points == [100, 100, 100, 0, 0, 0]

        user1OverallProgress_t4.points == 200
        user2OverallProgress_t4.points == 300
        user3OverallProgress_t4.points == 400
        user1OverallProgress_t4.skillsLevel == 1
        user2OverallProgress_t4.skillsLevel == 2
        user3OverallProgress_t4.skillsLevel == 2

        // copied
        user1Progress_copied.points == 100
        user1Progress_copied.totalPoints == 500
        user1Progress_copied.skillsLevel == 1
        user1Progress_copied.skills.skillId == skills.skillId
        user1Progress_copied.skills.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_copied.points == 100
        user1ProgressSubj2_copied.totalPoints == 600
        user1ProgressSubj2_copied.skillsLevel == 1
        user1ProgressSubj2_copied.skills.skillId == subj2Skills.skillId
        user1ProgressSubj2_copied.skills.points == [0, 0, 100, 0, 0, 0]

        user2Progress_copied.points == 100
        user2Progress_copied.totalPoints == 500
        user2Progress_copied.skillsLevel == 1
        user2Progress_copied.skills.skillId == skills.skillId
        user2Progress_copied.skills.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_copied.points == 200
        user2ProgressSubj2_copied.totalPoints == 600
        user2ProgressSubj2_copied.skillsLevel == 2
        user2ProgressSubj2_copied.skills.skillId == subj2Skills.skillId
        user2ProgressSubj2_copied.skills.points == [100, 0, 100, 0, 0, 0]

        user3Progress_copied.points == 100
        user3Progress_copied.totalPoints == 500
        user3Progress_copied.skillsLevel == 1
        user3Progress_copied.skills.skillId == skills.skillId
        user3Progress_copied.skills.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_copied.points == 300
        user3ProgressSubj2_copied.totalPoints == 600
        user3ProgressSubj2_copied.skillsLevel == 3
        user3ProgressSubj2_copied.skills.skillId == subj2Skills.skillId
        user3ProgressSubj2_copied.skills.points == [100, 100, 100, 0, 0, 0]

        user1OverallProgress_copied.points == 200
        user2OverallProgress_copied.points == 300
        user3OverallProgress_copied.points == 400
        user1OverallProgress_copied.skillsLevel == 1
        user2OverallProgress_copied.skillsLevel == 2
        user3OverallProgress_copied.skillsLevel == 2
    }

    def "copy project with quiz skills - quiz user achievements must be reflected in a new project - multiple subjects with skill groups"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def quiz3 = createQuiz(3)

        Closure constructProjWithQuizSkills = { Integer projNum ->
            def proj = createProject(projNum)
            def subj = createSubject(projNum, 1)
            def skillsGroup = SkillsFactory.createSkillsGroup(projNum, 1, 11)
            skillsService.createProjectAndSubjectAndSkills(proj, subj, [skillsGroup])
            def skills = SkillsFactory.createSkills(5, projNum, 1, 100)
            skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
            skills[0].quizId = quiz1.quizId

            skills.each { skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, it) }
            def subj2 = createSubject(projNum, 2)
            skillsService.createSubject(subj2)
            def subj2SkillsGroup1 = SkillsFactory.createSkillsGroup(projNum, 2, 22)
            skillsService.createSkill(subj2SkillsGroup1)
            def subj2SkillsGroup2 = SkillsFactory.createSkillsGroup(projNum, 2, 33)
            skillsService.createSkill(subj2SkillsGroup2)

            def subj2Skills = SkillsFactory.createSkills(6, projNum, 2, 100)
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

            return [proj, subj, skills, subj2, subj2Skills, subj2SkillsGroup1]
        }

        def (proj, subj, skills, subj2, subj2Skills, subj2SkillsGroup1) = constructProjWithQuizSkills(1)
        def (proj2, proj2subj, proj2skills, proj2subj2, proj2subj2Skills, proj2subj2SkillsGroup1) = constructProjWithQuizSkills(3)

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
        UserAchievementsInfo achievements_t0 = loadAchievements(userServices, proj.projectId)

        def projToCopy = createProject(2)
        skillsService.copyProject(proj.projectId, projToCopy)

        def user1Progress_t1 = skillsService.getSkillSummary(userServices[0].userName, projToCopy.projectId, subj.subjectId)
        def user2Progress_t1 = skillsService.getSkillSummary(userServices[1].userName, projToCopy.projectId, subj.subjectId)
        def user3Progress_t1 = skillsService.getSkillSummary(userServices[2].userName, projToCopy.projectId, subj.subjectId)
        def user1ProgressSubj2_t1 = skillsService.getSkillSummary(userServices[0].userName, projToCopy.projectId, subj2.subjectId)
        def user2ProgressSubj2_t1 = skillsService.getSkillSummary(userServices[1].userName, projToCopy.projectId, subj2.subjectId)
        def user3ProgressSubj2_t1 = skillsService.getSkillSummary(userServices[2].userName, projToCopy.projectId, subj2.subjectId)
        def user1OverallProgress_t1 = skillsService.getSkillSummary(userServices[0].userName, projToCopy.projectId)
        def user2OverallProgress_t1 = skillsService.getSkillSummary(userServices[1].userName, projToCopy.projectId)
        def user3OverallProgress_t1 = skillsService.getSkillSummary(userServices[2].userName, projToCopy.projectId)
        UserAchievementsInfo achievements_t1 = loadAchievements(userServices, projToCopy.projectId)

        def user1Progress_origProj1_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj.subjectId)
        def user2Progress_origProj1_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj.subjectId)
        def user3Progress_origProj1_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj.subjectId)
        def user1ProgressSubj2_origProj1_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId, subj2.subjectId)
        def user2ProgressSubj2_origProj1_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId, subj2.subjectId)
        def user3ProgressSubj2_origProj1_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId, subj2.subjectId)
        def user1OverallProgress_origProj1_t1 = skillsService.getSkillSummary(userServices[0].userName, proj.projectId)
        def user2OverallProgress_origProj1_t1 = skillsService.getSkillSummary(userServices[1].userName, proj.projectId)
        def user3OverallProgress_origProj1_t1 = skillsService.getSkillSummary(userServices[2].userName, proj.projectId)
        UserAchievementsInfo achievements_origProj1_t1 = loadAchievements(userServices, proj.projectId)

        def user1Progress_origProj2_t1 = skillsService.getSkillSummary(userServices[0].userName, proj2.projectId, proj2subj.subjectId)
        def user2Progress_origProj2_t1 = skillsService.getSkillSummary(userServices[1].userName, proj2.projectId, proj2subj.subjectId)
        def user3Progress_origProj2_t1 = skillsService.getSkillSummary(userServices[2].userName, proj2.projectId, proj2subj.subjectId)
        def user1ProgressSubj2_origProj2_t1 = skillsService.getSkillSummary(userServices[0].userName, proj2.projectId, proj2subj2.subjectId)
        def user2ProgressSubj2_origProj2_t1 = skillsService.getSkillSummary(userServices[1].userName, proj2.projectId, proj2subj2.subjectId)
        def user3ProgressSubj2_origProj2_t1 = skillsService.getSkillSummary(userServices[2].userName, proj2.projectId, proj2subj2.subjectId)
        def user1OverallProgress_origProj2_t1 = skillsService.getSkillSummary(userServices[0].userName, proj2.projectId)
        def user2OverallProgress_origProj2_t1 = skillsService.getSkillSummary(userServices[1].userName, proj2.projectId)
        def user3OverallProgress_origProj2_t1 = skillsService.getSkillSummary(userServices[2].userName, proj2.projectId)
        UserAchievementsInfo achievements_origProj2_t1 = loadAchievements(userServices, proj2.projectId)

        then:
        user1Progress_t0.points == 100
        user1Progress_t0.totalPoints == 500
        user1Progress_t0.skillsLevel == 1
        user1Progress_t0.skills[0].children.skillId == skills.skillId
        user1Progress_t0.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t0.points == 100
        user1ProgressSubj2_t0.totalPoints == 600
        user1ProgressSubj2_t0.skillsLevel == 1
        user1ProgressSubj2_t0.skills[0].children.points == [0, 0]
        user1ProgressSubj2_t0.skills[1].children.points == [100, 0, 0, 0]
        achievements_t0.user1.size() == 5
        achievements_t0.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t0.user1.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t0.user1.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t0.user1.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t0.user1.find { it.skillId == null  && it.level == 1 }

        user2Progress_t0.points == 100
        user2Progress_t0.totalPoints == 500
        user2Progress_t0.skillsLevel == 1
        user2Progress_t0.skills[0].children.skillId == skills.skillId
        user2Progress_t0.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t0.points == 200
        user2ProgressSubj2_t0.totalPoints == 600
        user2ProgressSubj2_t0.skillsLevel == 2
        user2ProgressSubj2_t0.skills[0].children.points == [100, 0]
        user2ProgressSubj2_t0.skills[1].children.points == [100, 0, 0, 0]
        achievements_t0.user2.size() == 8
        achievements_t0.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t0.user2.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t0.user2.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t0.user2.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t0.user2.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t0.user2.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_t0.user2.find { it.skillId == null  && it.level == 1 }
        achievements_t0.user2.find { it.skillId == null  && it.level == 2 }

        user3Progress_t0.points == 100
        user3Progress_t0.totalPoints == 500
        user3Progress_t0.skillsLevel == 1
        user3Progress_t0.skills[0].children.skillId == skills.skillId
        user3Progress_t0.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t0.points == 300
        user3ProgressSubj2_t0.totalPoints == 600
        user3ProgressSubj2_t0.skillsLevel == 3
        user3ProgressSubj2_t0.skills[0].children.points == [100, 100]
        user3ProgressSubj2_t0.skills[1].children.points == [100, 0, 0, 0]
        achievements_t0.user3.size() == 11
        achievements_t0.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t0.user3.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t0.user3.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t0.user3.find { it.skillId == subj2Skills[1].skillId  && it.level == null }
        achievements_t0.user3.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t0.user3.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t0.user3.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_t0.user3.find { it.skillId == subj2.subjectId  && it.level == 3 }
        achievements_t0.user3.find { it.skillId == null  && it.level == 1 }
        achievements_t0.user3.find { it.skillId == null  && it.level == 2 }
        achievements_t0.user3.find { it.skillId == subj2SkillsGroup1.skillId  && it.level == null }

        user1OverallProgress_t0.points == 200
        user2OverallProgress_t0.points == 300
        user3OverallProgress_t0.points == 400
        user1OverallProgress_t0.skillsLevel == 1
        user2OverallProgress_t0.skillsLevel == 2
        user3OverallProgress_t0.skillsLevel == 2

        // copied project
        user1Progress_t1.points == 100
        user1Progress_t1.totalPoints == 500
        user1Progress_t1.skillsLevel == 1
        user1Progress_t1.skills[0].children.skillId == skills.skillId
        user1Progress_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_t1.points == 100
        user1ProgressSubj2_t1.totalPoints == 600
        user1ProgressSubj2_t1.skillsLevel == 1
        user1ProgressSubj2_t1.skills[0].children.points == [0, 0]
        user1ProgressSubj2_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_t1.user1.size() == 5
        achievements_t1.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t1.user1.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t1.user1.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t1.user1.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t1.user1.find { it.skillId == null  && it.level == 1 }

        user2Progress_t1.points == 100
        user2Progress_t1.totalPoints == 500
        user2Progress_t1.skillsLevel == 1
        user2Progress_t1.skills[0].children.skillId == skills.skillId
        user2Progress_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_t1.points == 200
        user2ProgressSubj2_t1.totalPoints == 600
        user2ProgressSubj2_t1.skillsLevel == 2
        user2ProgressSubj2_t1.skills[0].children.points == [100, 0]
        user2ProgressSubj2_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_t1.user2.size() == 8
        achievements_t1.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t1.user2.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t1.user2.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t1.user2.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t1.user2.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t1.user2.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_t1.user2.find { it.skillId == null  && it.level == 1 }
        achievements_t1.user2.find { it.skillId == null  && it.level == 2 }

        user3Progress_t1.points == 100
        user3Progress_t1.totalPoints == 500
        user3Progress_t1.skillsLevel == 1
        user3Progress_t1.skills[0].children.skillId == skills.skillId
        user3Progress_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_t1.points == 300
        user3ProgressSubj2_t1.totalPoints == 600
        user3ProgressSubj2_t1.skillsLevel == 3
        user3ProgressSubj2_t1.skills[0].children.points == [100, 100]
        user3ProgressSubj2_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_t1.user3.size() == 11
        achievements_t1.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_t1.user3.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_t1.user3.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_t1.user3.find { it.skillId == subj2Skills[1].skillId  && it.level == null }
        achievements_t1.user3.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 3 }
        achievements_t1.user3.find { it.skillId == null  && it.level == 1 }
        achievements_t1.user3.find { it.skillId == null  && it.level == 2 }
        achievements_t1.user3.find { it.skillId == subj2SkillsGroup1.skillId  && it.level == null }

        user1OverallProgress_t1.points == 200
        user2OverallProgress_t1.points == 300
        user3OverallProgress_t1.points == 400
        user1OverallProgress_t1.skillsLevel == 1
        user2OverallProgress_t1.skillsLevel == 2
        user3OverallProgress_t1.skillsLevel == 2

        // validate no changes to orig proj 1  --------------------

        user1Progress_origProj1_t1.points == 100
        user1Progress_origProj1_t1.totalPoints == 500
        user1Progress_origProj1_t1.skillsLevel == 1
        user1Progress_origProj1_t1.skills[0].children.skillId == skills.skillId
        user1Progress_origProj1_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_origProj1_t1.points == 100
        user1ProgressSubj2_origProj1_t1.totalPoints == 600
        user1ProgressSubj2_origProj1_t1.skillsLevel == 1
        user1ProgressSubj2_origProj1_t1.skills[0].children.points == [0, 0]
        user1ProgressSubj2_origProj1_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_origProj1_t1.user1.size() == 5
        achievements_origProj1_t1.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_origProj1_t1.user1.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_origProj1_t1.user1.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_origProj1_t1.user1.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_origProj1_t1.user1.find { it.skillId == null  && it.level == 1 }

        user2Progress_origProj1_t1.points == 100
        user2Progress_origProj1_t1.totalPoints == 500
        user2Progress_origProj1_t1.skillsLevel == 1
        user2Progress_origProj1_t1.skills[0].children.skillId == skills.skillId
        user2Progress_origProj1_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_origProj1_t1.points == 200
        user2ProgressSubj2_origProj1_t1.totalPoints == 600
        user2ProgressSubj2_origProj1_t1.skillsLevel == 2
        user2ProgressSubj2_origProj1_t1.skills[0].children.points == [100, 0]
        user2ProgressSubj2_origProj1_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_origProj1_t1.user2.size() == 8
        achievements_origProj1_t1.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_origProj1_t1.user2.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_origProj1_t1.user2.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_origProj1_t1.user2.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_origProj1_t1.user2.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_origProj1_t1.user2.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_origProj1_t1.user2.find { it.skillId == null  && it.level == 1 }
        achievements_origProj1_t1.user2.find { it.skillId == null  && it.level == 2 }

        user3Progress_origProj1_t1.points == 100
        user3Progress_origProj1_t1.totalPoints == 500
        user3Progress_origProj1_t1.skillsLevel == 1
        user3Progress_origProj1_t1.skills[0].children.skillId == skills.skillId
        user3Progress_origProj1_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_origProj1_t1.points == 300
        user3ProgressSubj2_origProj1_t1.totalPoints == 600
        user3ProgressSubj2_origProj1_t1.skillsLevel == 3
        user3ProgressSubj2_origProj1_t1.skills[0].children.points == [100, 100]
        user3ProgressSubj2_origProj1_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_origProj1_t1.user3.size() == 11
        achievements_origProj1_t1.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_origProj1_t1.user3.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_origProj1_t1.user3.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_origProj1_t1.user3.find { it.skillId == subj2Skills[1].skillId  && it.level == null }
        achievements_origProj1_t1.user3.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_origProj1_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_origProj1_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_origProj1_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 3 }
        achievements_origProj1_t1.user3.find { it.skillId == null  && it.level == 1 }
        achievements_origProj1_t1.user3.find { it.skillId == null  && it.level == 2 }
        achievements_origProj1_t1.user3.find { it.skillId == subj2SkillsGroup1.skillId  && it.level == null }

        user1OverallProgress_origProj1_t1.points == 200
        user2OverallProgress_origProj1_t1.points == 300
        user3OverallProgress_origProj1_t1.points == 400
        user1OverallProgress_origProj1_t1.skillsLevel == 1
        user2OverallProgress_origProj1_t1.skillsLevel == 2
        user3OverallProgress_origProj1_t1.skillsLevel == 2

        // validate no changes to orig proj 2  --------------------

        user1Progress_origProj2_t1.points == 100
        user1Progress_origProj2_t1.totalPoints == 500
        user1Progress_origProj2_t1.skillsLevel == 1
        user1Progress_origProj2_t1.skills[0].children.skillId == skills.skillId
        user1Progress_origProj2_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user1ProgressSubj2_origProj2_t1.points == 100
        user1ProgressSubj2_origProj2_t1.totalPoints == 600
        user1ProgressSubj2_origProj2_t1.skillsLevel == 1
        user1ProgressSubj2_origProj2_t1.skills[0].children.points == [0, 0]
        user1ProgressSubj2_origProj2_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_origProj2_t1.user1.size() == 5
        achievements_origProj2_t1.user1.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_origProj2_t1.user1.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_origProj2_t1.user1.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_origProj2_t1.user1.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_origProj2_t1.user1.find { it.skillId == null  && it.level == 1 }

        user2Progress_origProj2_t1.points == 100
        user2Progress_origProj2_t1.totalPoints == 500
        user2Progress_origProj2_t1.skillsLevel == 1
        user2Progress_origProj2_t1.skills[0].children.skillId == skills.skillId
        user2Progress_origProj2_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user2ProgressSubj2_origProj2_t1.points == 200
        user2ProgressSubj2_origProj2_t1.totalPoints == 600
        user2ProgressSubj2_origProj2_t1.skillsLevel == 2
        user2ProgressSubj2_origProj2_t1.skills[0].children.points == [100, 0]
        user2ProgressSubj2_origProj2_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_origProj2_t1.user2.size() == 8
        achievements_origProj2_t1.user2.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_origProj2_t1.user2.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_origProj2_t1.user2.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_origProj2_t1.user2.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_origProj2_t1.user2.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_origProj2_t1.user2.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_origProj2_t1.user2.find { it.skillId == null  && it.level == 1 }
        achievements_origProj2_t1.user2.find { it.skillId == null  && it.level == 2 }

        user3Progress_origProj2_t1.points == 100
        user3Progress_origProj2_t1.totalPoints == 500
        user3Progress_origProj2_t1.skillsLevel == 1
        user3Progress_origProj2_t1.skills[0].children.skillId == skills.skillId
        user3Progress_origProj2_t1.skills[0].children.points == [100, 0, 0, 0, 0]
        user3ProgressSubj2_origProj2_t1.points == 300
        user3ProgressSubj2_origProj2_t1.totalPoints == 600
        user3ProgressSubj2_origProj2_t1.skillsLevel == 3
        user3ProgressSubj2_origProj2_t1.skills[0].children.points == [100, 100]
        user3ProgressSubj2_origProj2_t1.skills[1].children.points == [100, 0, 0, 0]
        achievements_origProj2_t1.user3.size() == 11
        achievements_origProj2_t1.user3.find { it.skillId == skills[0].skillId  && it.level == null }
        achievements_origProj2_t1.user3.find { it.skillId == subj.subjectId  && it.level == 1 }
        achievements_origProj2_t1.user3.find { it.skillId == subj2Skills[0].skillId  && it.level == null }
        achievements_origProj2_t1.user3.find { it.skillId == subj2Skills[1].skillId  && it.level == null }
        achievements_origProj2_t1.user3.find { it.skillId == subj2Skills[2].skillId  && it.level == null }
        achievements_origProj2_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 1 }
        achievements_origProj2_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 2 }
        achievements_origProj2_t1.user3.find { it.skillId == subj2.subjectId  && it.level == 3 }
        achievements_origProj2_t1.user3.find { it.skillId == null  && it.level == 1 }
        achievements_origProj2_t1.user3.find { it.skillId == null  && it.level == 2 }
        achievements_origProj2_t1.user3.find { it.skillId == subj2SkillsGroup1.skillId  && it.level == null }

        user1OverallProgress_origProj2_t1.points == 200
        user2OverallProgress_origProj2_t1.points == 300
        user3OverallProgress_origProj2_t1.points == 400
        user1OverallProgress_origProj2_t1.skillsLevel == 1
        user2OverallProgress_origProj2_t1.skillsLevel == 2
        user3OverallProgress_origProj2_t1.skillsLevel == 2
    }

    def "copy project with quiz skills - quiz user achievements must be reflected in a new project - UserAchievement, UserPerformedSkill and UserPoints records are created"() {
        def quiz1 = createQuiz(1)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        passQuiz(userServices[0], quiz1)

        Integer skillRefId = skillDefRepo.findByProjectIdAndSkillId(proj.projectId, skills[0].skillId).id
        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(proj.projectId, projToCopy)

        List<UserPerformedSkill> userPerformedSkills_orig = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_orig = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_orig = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        List<UserPerformedSkill> userPerformedSkills_copy = userPerformedSkillRepo.findAll().findAll( { it.projectId == projToCopy.projectId})
        List<UserPoints> userPoints_copy = userPointsRepo.findAll().findAll( { it.projectId == projToCopy.projectId})
        List<UserAchievement> achievements_copy = userAchievedRepo.findAll().findAll( { it.projectId == projToCopy.projectId})

        then:
        userPerformedSkills_orig.size() == 1
        userPerformedSkills_orig[0].userId == userServices[0].userName
        userPerformedSkills_orig[0].skillId == skills[0].skillId
        userPerformedSkills_orig[0].projectId == proj.projectId
        userPerformedSkills_orig[0].skillRefId == skillRefId
        userPerformedSkills_orig[0].performedOn

        userPoints_orig.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserPoints skillUserPoints = userPoints_orig.find { it.skillId == skills[0].skillId }
        skillUserPoints.userId == userServices[0].userName
        skillUserPoints.skillId == skills[0].skillId
        skillUserPoints.projectId == proj.projectId
        skillUserPoints.skillRefId == skillRefId
        skillUserPoints.points == skills[0].pointIncrement

        achievements_orig.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserAchievement skillAchievement = achievements_orig.find { it.skillId == skills[0].skillId }
        skillAchievement.userId == userServices[0].userName
        skillAchievement.skillId == skills[0].skillId
        skillAchievement.projectId == proj.projectId
        skillAchievement.skillRefId == skillRefId
        skillAchievement.pointsWhenAchieved == skills[0].pointIncrement
        !skillAchievement.level

        // copy
        userPerformedSkills_copy.size() == 1
        userPerformedSkills_copy[0].userId == userServices[0].userName
        userPerformedSkills_copy[0].skillId == skills[0].skillId
        userPerformedSkills_copy[0].projectId == projToCopy.projectId
        userPerformedSkills_copy[0].performedOn

        userPoints_copy.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserPoints skillUserPoints_copy = userPoints_copy.find { it.skillId == skills[0].skillId }
        skillUserPoints_copy.userId == userServices[0].userName
        skillUserPoints_copy.skillId == skills[0].skillId
        skillUserPoints_copy.projectId == projToCopy.projectId
        skillUserPoints_copy.points == skills[0].pointIncrement

        achievements_copy.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserAchievement skillAchievement_copy = achievements_copy.find { it.skillId == skills[0].skillId }
        skillAchievement_copy.userId == userServices[0].userName
        skillAchievement_copy.skillId == skills[0].skillId
        skillAchievement_copy.projectId == projToCopy.projectId
        skillAchievement_copy.pointsWhenAchieved == skills[0].pointIncrement
        !skillAchievement_copy.level
    }

    def "skills catalog: assign quiz to the original skill - validate imported skill's UserAchievement and UserPoints"() {
        def quiz1 = createQuiz(1)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        List<SkillsService> userServices = getRandomUsers(1).collect { createService(it) }
        passQuiz(userServices[0], quiz1)

        Integer skillRefId = skillDefRepo.findByProjectIdAndSkillId(proj.projectId, skills[0].skillId).id
        when:
        // proj 2
        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])
        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)

        // set quiz which would cause all the points and achievements creation
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createSkill(skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<UserPerformedSkill> userPerformedSkills_orig = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserPoints> userPoints_orig = userPointsRepo.findAll().findAll( { it.projectId == proj.projectId})
        List<UserAchievement> achievements_orig = userAchievedRepo.findAll().findAll( { it.projectId == proj.projectId})

        List<UserPerformedSkill> userPerformedSkills_proj2 = userPerformedSkillRepo.findAll().findAll( { it.projectId == proj2.projectId})
        List<UserPoints> userPoints_proj2 = userPointsRepo.findAll().findAll( { it.projectId == proj2.projectId})
        List<UserAchievement> achievements_proj2 = userAchievedRepo.findAll().findAll( { it.projectId == proj2.projectId})

        then:
        userPerformedSkills_orig.size() == 1
        userPerformedSkills_orig[0].userId == userServices[0].userName
        userPerformedSkills_orig[0].skillId == skills[0].skillId
        userPerformedSkills_orig[0].projectId == proj.projectId
        userPerformedSkills_orig[0].skillRefId == skillRefId
        userPerformedSkills_orig[0].performedOn

        userPoints_orig.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserPoints skillUserPoints = userPoints_orig.find { it.skillId == skills[0].skillId }
        skillUserPoints.userId == userServices[0].userName
        skillUserPoints.skillId == skills[0].skillId
        skillUserPoints.projectId == proj.projectId
        skillUserPoints.skillRefId == skillRefId
        skillUserPoints.points == skills[0].pointIncrement

        achievements_orig.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserAchievement skillAchievement = achievements_orig.find { it.skillId == skills[0].skillId }
        skillAchievement.userId == userServices[0].userName
        skillAchievement.skillId == skills[0].skillId
        skillAchievement.projectId == proj.projectId
        skillAchievement.skillRefId == skillRefId
        skillAchievement.pointsWhenAchieved == skills[0].pointIncrement
        !skillAchievement.level

        ///////////////////////////////
        // project #2 with imported skills

        // performed skills are not imported
        userPerformedSkills_proj2.size() == 0

        userPoints_proj2.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserPoints skillUserPoints_copy = userPoints_proj2.find { it.skillId == skills[0].skillId }
        skillUserPoints_copy.userId == userServices[0].userName
        skillUserPoints_copy.skillId == skills[0].skillId
        skillUserPoints_copy.projectId == proj2.projectId
        skillUserPoints_copy.points == skills[0].pointIncrement
        userPoints_proj2.points == [skills[0].pointIncrement, skills[0].pointIncrement, skills[0].pointIncrement]

        achievements_proj2.size() == 3 // 1 for project, 1 for subject and 1 for skill
        UserAchievement skillAchievement_copy = achievements_proj2.find { it.skillId == skills[0].skillId }
        skillAchievement_copy.userId == userServices[0].userName
        skillAchievement_copy.skillId == skills[0].skillId
        skillAchievement_copy.projectId == proj2.projectId
        skillAchievement_copy.pointsWhenAchieved == skills[0].pointIncrement
        !skillAchievement_copy.level

        // subject
        achievements_proj2.find { it.skillId == proj2_subj.subjectId }.level == 1
        // project
        achievements_proj2.find { !it.skillId }.level == 1
    }

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

    private def createQuiz(Integer num) {
        def quiz = QuizDefFactory.createQuiz(num)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(num, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        quizInfo.quizId = quiz.quizId
        return quizInfo
    }

    private void passQuiz(SkillsService userService, def quizInfo) {
        def quizAttempt =  userService.startQuizAttempt(quizInfo.quizId).body
        userService.reportQuizAnswer(quizInfo.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        def gradedQuizAttempt = userService.completeQuizAttempt(quizInfo.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.passed == true
    }

    private static class UserAchievementsInfo {
        List<UserAchievement> user1
        List<UserAchievement> user2
        List<UserAchievement> user3
    }

    private UserAchievementsInfo loadAchievements(List<SkillsService> userServices, String projectId = null) {
        List<UserAchievement> achievements = userAchievedRepo.findAll()
        return new UserAchievementsInfo(
                user1: achievements.findAll({ it.userId == userServices[0].userName && (projectId == null || projectId == it.projectId)}),
                user2: achievements.findAll({ it.userId == userServices[1].userName && (projectId == null || projectId == it.projectId)}),
                user3: achievements.findAll({ it.userId == userServices[2].userName && (projectId == null || projectId == it.projectId)}),
        )
    }
}



