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
class QuizSkillAchievements_CopyProjectSpecs extends QuizSkillAchievementsBaseIntSpec {

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

        // subj2Skills[4] -> subj2Skills[3] (quiz skill)
        skillsService.addLearningPathPrerequisite(proj.projectId, subj2Skills[3].skillId, subj2Skills[4].skillId)

        // assign quiz to skill after users passed, should not award skills since the subj2Skills[4] has not been completed yet (in both original and copied project)
        subj2Skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[3].quizId = quiz1.quizId
        skillsService.updateSkill(subj2Skills[3])

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

        // subj2Skills[4] -> subj2Skills[3] (quiz skill)
        skillsService.addLearningPathPrerequisite(proj.projectId, subj2Skills[3].skillId, subj2Skills[4].skillId)

        // assign quiz to skill after users passed, should not award skills since the subj2Skills[4] has not been completed yet (in both original and copied project)
        subj2Skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        subj2Skills[3].quizId = quiz1.quizId
        skillsService.updateSkill(subj2Skills[3])

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

        // skills[4] -> skills[3] (quiz skill)
        skillsService.addLearningPathPrerequisite(proj.projectId, skills[3].skillId, skills[4].skillId)

        // assign quiz to skill after users passed, should not award skills since the skills[4] has not been completed yet (in both original and copied project)
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = quiz1.quizId
        skillsService.updateSkill(skills[3])

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

}



