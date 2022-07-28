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
package skills.intTests.moveSkills


import skills.intTests.utils.DefaultIntSpec
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class MoveSkillsPointsAndAchievementsSpecs extends DefaultIntSpec {


    def "move skill from subject into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 3)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1SkillsSubj2 = createSkills(2, 1, 2, 100, 3)
        skillsService.createSkills(p1SkillsSubj2)

        List<String> users = getRandomUsers(2)
        String user1 = users[0]
        String user2 = users[1]

        // user 1
        skillsService.addSkill(p1Skills[0], user1, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user1, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user1)

        // user 2
        skillsService.addSkill(p1SkillsSubj2[0], user2, new Date() - 2)
        skillsService.addSkill(p1SkillsSubj2[1], user2, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user2, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user2, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user2)
        skillsService.addSkill(p1Skills[1], user2, new Date() - 2)
        skillsService.addSkill(p1Skills[1], user2, new Date() - 1)
        skillsService.addSkill(p1Skills[1], user2, new Date())
        skillsService.addSkill(p1Skills[2], user2, new Date())

        when:
        def projUser1_t0 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t0 = skillsService.getSkillSummary(user2, p1.projectId)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)

        then:
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[0].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[1].skillId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 300

        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId).level.sort() == [1, 2]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)

        projUser1_t0.skillsLevel == 1
        projUser1_t0.points == 300
        projUser1_t0.subjects.skillsLevel == [2, 0]
        projUser1_t0.subjects.points == [300, 0]

        projUser1_t1.skillsLevel == 1
        projUser1_t1.points == 300
        projUser1_t1.subjects.skillsLevel == [0, 2]
        projUser1_t1.subjects.points == [0, 300]

        // user 2
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 900

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId).points == 400
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId).points == 500

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[0].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[1].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[2].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[0].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[1].skillId).points == 100

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId).level.sort() == [1, 2, 3]

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2, 3]

        projUser2_t0.skillsLevel == 3
        projUser2_t0.points == 900
        projUser2_t0.subjects.skillsLevel == [4, 2]
        projUser2_t0.subjects.points == [700, 200]

        projUser2_t1.skillsLevel == 3
        projUser2_t1.points == 900
        projUser2_t1.subjects.skillsLevel == [3, 3]
        projUser2_t1.subjects.points == [400, 500]
    }

    def "move skill from subject into another subject - move achieves extra levels in the original subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 3)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1SkillsSubj2 = createSkills(2, 1, 2, 100, 3)
        skillsService.createSkills(p1SkillsSubj2)

        List<String> users = getRandomUsers(2)
        String user1 = users[0]
        String user2 = users[1]

        // user 1
        skillsService.addSkill(p1Skills[0], user1, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user1, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user1)

        // user 2
        skillsService.addSkill(p1SkillsSubj2[0], user2, new Date() - 2)
        skillsService.addSkill(p1SkillsSubj2[1], user2, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user2, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user2, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user2)
        skillsService.addSkill(p1Skills[1], user2, new Date() - 2)
        skillsService.addSkill(p1Skills[1], user2, new Date() - 1)
        skillsService.addSkill(p1Skills[1], user2, new Date())
        skillsService.addSkill(p1Skills[2], user2, new Date())

        when:
        def projUser1_t0 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t0 = skillsService.getSkillSummary(user2, p1.projectId)
        skillsService.moveSkills(p1.projectId, [p1Skills[1].skillId, p1Skills[2].skillId], p1subj2.subjectId)
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[0].skillId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[1].skillId)
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[2].skillId)

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId)

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 300

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[2].skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3, 4, 5]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1]

        projUser1_t0.skillsLevel == 1
        projUser1_t0.points == 300
        projUser1_t0.subjects.skillsLevel == [2, 0]
        projUser1_t0.subjects.points == [300, 0]

        projUser1_t1.skillsLevel == 1
        projUser1_t1.points == 300
        projUser1_t1.subjects.skillsLevel == [5, 0]
        projUser1_t1.subjects.points == [300, 0]

        // user 2
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 900

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId).points == 600

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[0].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[1].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[2].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[0].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[1].skillId).points == 100

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3, 4, 5]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId).level.sort() == [1, 2, 3]

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2, 3]

        projUser2_t0.skillsLevel == 3
        projUser2_t0.points == 900
        projUser2_t0.subjects.skillsLevel == [4, 2]
        projUser2_t0.subjects.points == [700, 200]

        projUser2_t1.skillsLevel == 3
        projUser2_t1.points == 900
        projUser2_t1.subjects.skillsLevel == [5, 3]
        projUser2_t1.subjects.points == [300, 600]
    }
}
