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

    def "move skill from subject into another subject - move removes extra levels in the new subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 1000, 3)
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

        when:
        def projUser1_t0 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t0 = skillsService.getSkillSummary(user2, p1.projectId)
        skillsService.moveSkills(p1.projectId, [p1Skills[1].skillId, p1Skills[2].skillId], p1subj2.subjectId)
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[0].skillId).points == 3000
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[1].skillId)
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[2].skillId)

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId).points == 3000
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId)

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 3000

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[2].skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3, 4, 5]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1, 2]

        projUser1_t0.skillsLevel == 2
        projUser1_t0.points == 3000
        projUser1_t0.subjects.skillsLevel == [2, 0]
        projUser1_t0.subjects.points == [3000, 0]

        projUser1_t1.skillsLevel == 2
        projUser1_t1.points == 3000
        projUser1_t1.subjects.skillsLevel == [5, 0]
        projUser1_t1.subjects.points == [3000, 0]

        // user 2
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 3200

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId).points == 3000
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId).points == 200

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[0].skillId).points == 3000
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[1].skillId)
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[2].skillId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[0].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[1].skillId).points == 100

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3, 4, 5]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2]

        projUser2_t0.skillsLevel == 2
        projUser2_t0.points == 3200
        projUser2_t0.subjects.skillsLevel == [2, 2]
        projUser2_t0.subjects.points == [3000, 200]

        projUser2_t1.skillsLevel == 2
        projUser2_t1.points == 3200
        projUser2_t1.subjects.skillsLevel == [5, 0]
        projUser2_t1.subjects.points == [3000, 200]
    }

    def "move all of the skills from a subject into another subject - move removes ALL users' levels in the original subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 1000, 3)
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

        when:
        def projUser1_t0 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t0 = skillsService.getSkillSummary(user2, p1.projectId)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId], p1subj2.subjectId)
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[0].skillId).points == 3000
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[1].skillId)
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[2].skillId)

        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId).points == 3000

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 3000

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[2].skillId)

        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId).level.sort() == [1, 2]

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1, 2]

        projUser1_t0.skillsLevel == 2
        projUser1_t0.points == 3000
        projUser1_t0.subjects.skillsLevel == [2, 0]
        projUser1_t0.subjects.points == [3000, 0]

        projUser1_t1.skillsLevel == 2
        projUser1_t1.points == 3000
        projUser1_t1.subjects.skillsLevel == [0, 2]
        projUser1_t1.subjects.points == [0, 3000]

        // user 2
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 3200

        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId).points == 3200

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[0].skillId).points == 3000
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[1].skillId)
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[2].skillId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[0].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1SkillsSubj2[1].skillId).points == 100

        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId).level.sort() == [1, 2]

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2]

        projUser2_t0.skillsLevel == 2
        projUser2_t0.points == 3200
        projUser2_t0.subjects.skillsLevel == [2, 2]
        projUser2_t0.subjects.points == [3000, 200]

        projUser2_t1.skillsLevel == 2
        projUser2_t1.points == 3200
        projUser2_t1.subjects.skillsLevel == [0, 2]
        projUser2_t1.subjects.points == [0, 3200]
    }

    def "move skill from group into another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100, 3)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

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

        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1g1.skillId)

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

        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1g1.skillId)

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

    def "move skill from group into another subject - move achieves extra levels in the original subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100, 3)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

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

        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1g1.skillId)

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

        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1g1.skillId)

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

    def "move skill from subject into a group under another subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 3)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 8)
        skillsService.createSkill(p1subj2g1)
        def p1SkillsSubj2 = createSkills(2, 1, 2, 100, 3)
        p1SkillsSubj2.each {
            skillsService.assignSkillToSkillsGroup(p1subj2g1.skillId, it)
        }

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
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2g1.skillId)

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
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2g1.skillId)

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

    def "move skill from subject into a group under the same subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100, 3)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills[0..2])

        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createSkill(p1subj1g1)
        p1Skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        List<String> users = getRandomUsers(2)
        String user1 = users[0]
        String user2 = users[1]

        // user 1
        skillsService.addSkill(p1Skills[0], user1, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user1, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user1)

        // user 2
        skillsService.addSkill(p1Skills[3], user2, new Date() - 2) // group skill
        skillsService.addSkill(p1Skills[4], user2, new Date() - 1) // group skill
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
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj1g1.skillId)
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[0].skillId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[1].skillId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1g1.skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)

        projUser1_t0.skillsLevel == 1
        projUser1_t0.points == 300
        projUser1_t0.subjects.skillsLevel == [1, 0]
        projUser1_t0.subjects.points == [300, 0]

        projUser1_t1.skillsLevel == 1
        projUser1_t1.points == 300
        projUser1_t1.subjects.skillsLevel == [1, 0]
        projUser1_t1.subjects.points == [300, 0]

        // user 2
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 900

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId).points == 900
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId)

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[0].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[1].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[2].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[3].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[4].skillId).points == 100
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1g1.skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2, 3]

        projUser2_t0.skillsLevel == 3
        projUser2_t0.points == 900
        projUser2_t0.subjects.skillsLevel == [3, 0]
        projUser2_t0.subjects.points == [900, 0]

        projUser2_t1.skillsLevel == 3
        projUser2_t1.points == 900
        projUser2_t1.subjects.skillsLevel == [3, 0]
        projUser2_t1.subjects.points == [900, 0]
    }

    def "move skill from group into its parent subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100, 3)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills[3..4]) // last 2 skills

        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createSkill(p1subj1g1)
        p1Skills[0..2].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        List<String> users = getRandomUsers(2)
        String user1 = users[0]
        String user2 = users[1]

        // user 1
        skillsService.addSkill(p1Skills[0], user1, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user1, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user1)

        // user 2
        skillsService.addSkill(p1Skills[3], user2, new Date() - 2) // group skill
        skillsService.addSkill(p1Skills[4], user2, new Date() - 1) // group skill
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
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId)
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[0].skillId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[1].skillId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1g1.skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)

        projUser1_t0.skillsLevel == 1
        projUser1_t0.points == 300
        projUser1_t0.subjects.skillsLevel == [1, 0]
        projUser1_t0.subjects.points == [300, 0]

        projUser1_t1.skillsLevel == 1
        projUser1_t1.points == 300
        projUser1_t1.subjects.skillsLevel == [1, 0]
        projUser1_t1.subjects.points == [300, 0]

        // user 2
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 900

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId).points == 900
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId)

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[0].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[1].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[2].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[3].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[4].skillId).points == 100
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1g1.skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2, 3]

        projUser2_t0.skillsLevel == 3
        projUser2_t0.points == 900
        projUser2_t0.subjects.skillsLevel == [3, 0]
        projUser2_t0.subjects.points == [900, 0]

        projUser2_t1.skillsLevel == 3
        projUser2_t1.points == 900
        projUser2_t1.subjects.skillsLevel == [3, 0]
        projUser2_t1.subjects.points == [900, 0]
    }

    def "move skill from a group into another group under the same subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100, 3)

        def p1subj1g0 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g0])
        p1Skills[0..2].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g0.skillId, it)
        }

        def p1subj1g1 = createSkillsGroup(1, 1, 9)
        skillsService.createSkill(p1subj1g1)
        p1Skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        List<String> users = getRandomUsers(2)
        String user1 = users[0]
        String user2 = users[1]

        // user 1
        skillsService.addSkill(p1Skills[0], user1, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user1, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user1)

        // user 2
        skillsService.addSkill(p1Skills[3], user2, new Date() - 2) // group skill
        skillsService.addSkill(p1Skills[4], user2, new Date() - 1) // group skill
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
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj1g1.skillId)
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[0].skillId).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1Skills[1].skillId)
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 300
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1g1.skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)

        projUser1_t0.skillsLevel == 1
        projUser1_t0.points == 300
        projUser1_t0.subjects.skillsLevel == [1, 0]
        projUser1_t0.subjects.points == [300, 0]

        projUser1_t1.skillsLevel == 1
        projUser1_t1.points == 300
        projUser1_t1.subjects.skillsLevel == [1, 0]
        projUser1_t1.subjects.points == [300, 0]

        // user 2
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 900

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId).points == 900
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId)

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[0].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[1].skillId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[2].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[3].skillId).points == 100
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1Skills[4].skillId).points == 100
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1g1.skillId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId)

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2, 3]

        projUser2_t0.skillsLevel == 3
        projUser2_t0.points == 900
        projUser2_t0.subjects.skillsLevel == [3, 0]
        projUser2_t0.subjects.points == [900, 0]

        projUser2_t1.skillsLevel == 3
        projUser2_t1.points == 900
        projUser2_t1.subjects.skillsLevel == [3, 0]
        projUser2_t1.subjects.points == [900, 0]
    }

    def "all user group achievement are removed if group is empty after the move"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills[3..4]) // last 2 skills

        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createSkill(p1subj1g1)
        p1Skills[0..2].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        List<String> users = getRandomUsers(2)
        String user1 = users[0]
        String user2 = users[1]

        // user 1 - completed the group
        skillsService.addSkill(p1Skills[0], user1)
        skillsService.addSkill(p1Skills[1], user1)
        skillsService.addSkill(p1Skills[2], user1)

        // user 2 - didn't complete the group yet
        skillsService.addSkill(p1Skills[0], user1)
        skillsService.addSkill(p1Skills[1], user1)

        when:
        def user1_t0 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1g1.skillId)
        def user2_t0 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1g1.skillId)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId], p1subj1.subjectId)
        def user1_t1 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1g1.skillId)
        def user2_t1 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1g1.skillId)

        then:
        user1_t0
        !user1_t1
        !user2_t0
        !user2_t1

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1, 2, 3]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[2].skillId)
    }

    def "when skills are moved into an empty group - group achievement is awarded to users that achieved all of those skills"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills) // last 2 skills

        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createSkill(p1subj1g1)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        List<String> users = getRandomUsers(2)
        String user1 = users[0]
        String user2 = users[1]

        // user 1 - completed the group
        skillsService.addSkill(p1Skills[0], user1)
        skillsService.addSkill(p1Skills[1], user1)
        skillsService.addSkill(p1Skills[2], user1)

        // user 2 - didn't complete the group yet
        skillsService.addSkill(p1Skills[0], user1)
        skillsService.addSkill(p1Skills[1], user1)

        when:
        def user1_t0 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1g1.skillId)
        def user2_t0 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1g1.skillId)
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[1].skillId], p1subj1.subjectId, p1subj1g1.skillId)
        def user1_t1 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1g1.skillId)
        def user2_t1 = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1g1.skillId)

        then:
        !user1_t0
        user1_t1

        !user2_t0
        !user2_t1

        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1, 2, 3]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[0].skillId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[1].skillId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1Skills[2].skillId)
    }

    def "move group from subject to another subject with user achievement and points recalculation"() {
        def p1 = createProject(1)

        // Create first subject with skills and groups
        def p1subj1 = createSubject(1, 1)
        def p1SkillsSubj1 = createSkills(4, 1, 1, 100, 3, )
        p1SkillsSubj1.each { it.pointIncrementInterval = 0  } // disable time window
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1SkillsSubj1[0], p1SkillsSubj1[1], p1subj1g1])
        skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, p1SkillsSubj1[2])
        skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, p1SkillsSubj1[3])

        // Create second subject with skills and groups
        def p1subj2 = createSubject(1, 2)
        def p1SkillsSubj2 = createSkills(3, 1, 2, 100, 3)
        p1SkillsSubj2.each { it.pointIncrementInterval = 0  } // disable time window
        def p1subj2g1 = createSkillsGroup(1, 2, 10)
        
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, [p1SkillsSubj2[0], p1SkillsSubj2[1], p1subj2g1])
        skillsService.assignSkillToSkillsGroup(p1subj2g1.skillId, p1SkillsSubj2[2])

        List<String> users = getRandomUsers(3)
        String user1 = users[0]
        String user2 = users[1]
        String user3 = users[2]

        // User 1: Progress in subject 1 - individual skills and group 1
        skillsService.addSkill(p1SkillsSubj1[0], user1, new Date() - 3)
        skillsService.addSkill(p1SkillsSubj1[0], user1, new Date() - 2)
        skillsService.addSkill(p1SkillsSubj1[0], user1, new Date() - 1)
        skillsService.addSkill(p1SkillsSubj1[1], user1, new Date() - 2)
        skillsService.addSkill(p1SkillsSubj1[1], user1, new Date() - 1)
        skillsService.addSkill(p1SkillsSubj1[2], user1, new Date() - 2) // group 1 skill
        skillsService.addSkill(p1SkillsSubj1[3], user1, new Date() - 1) // group 1 skill

        // User 2: Progress in both subjects - individual skills and groups
        skillsService.addSkill(p1SkillsSubj1[0], user2, new Date() - 2)
        skillsService.addSkill(p1SkillsSubj1[0], user2, new Date() - 1)
        skillsService.addSkill(p1SkillsSubj1[1], user2, new Date() - 1)
        skillsService.addSkill(p1SkillsSubj1[2], user2, new Date() - 2) // group 1 skill
        skillsService.addSkill(p1SkillsSubj1[3], user2, new Date() - 1) // group 1 skill
        skillsService.addSkill(p1SkillsSubj2[0], user2, new Date() - 2)
        skillsService.addSkill(p1SkillsSubj2[0], user2, new Date() - 1)
        skillsService.addSkill(p1SkillsSubj2[1], user2, new Date() - 1)
        skillsService.addSkill(p1SkillsSubj2[2], user2, new Date() - 1) // subject 2 group skill

        // User 3: Progress only in subject 2
        skillsService.addSkill(p1SkillsSubj2[0], user3, new Date() - 2)
        skillsService.addSkill(p1SkillsSubj2[0], user3, new Date() - 1)
        skillsService.addSkill(p1SkillsSubj2[1], user3, new Date() - 1)

        when:
        def projUser1_t0 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t0 = skillsService.getSkillSummary(user2, p1.projectId)
        def projUser3_t0 = skillsService.getSkillSummary(user3, p1.projectId)
        
        // Move group 1 from subject 1 to subject 2
        skillsService.moveSkills(p1.projectId, [p1subj1g1.skillId], p1subj2.subjectId)
        
        def projUser1_t1 = skillsService.getSkillSummary(user1, p1.projectId)
        def projUser2_t1 = skillsService.getSkillSummary(user2, p1.projectId)
        def projUser3_t1 = skillsService.getSkillSummary(user3, p1.projectId)

        then:
        // Level thresholds: Level 1 = 10%, Level 2 = 25%, Level 3 = 45%, Level 4 = 67%
        // Subject 1: 4 skills × 100 points × 3 = 1200 total points
        //   Level 1: 120 points (10%), Level 2: 300 points (25%), Level 3: 540 points (45%), Level 4: 804 points (67%)
        //   After move: 2 skills × 100 points × 3 = 600 total points (group removed)
        //   After move - Level 1: 60 points (10%), Level 2: 150 points (25%), Level 3: 270 points (45%), Level 4: 402 points (67%)
        // Subject 2: 3 skills × 100 points × 3 = 900 total points (before move), 1500 total points (after move)
        //   Before move - Level 1: 90 points (10%), Level 2: 225 points (25%), Level 3: 405 points (45%), Level 4: 603 points (67%)
        //   After move  - Level 1: 150 points (10%), Level 2: 375 points (25%), Level 3: 675 points (45%), Level 4: 1005 points (67%)
        // Project: 7 skills × 100 points × 3 = 2100 total points
        //   Level 1: 210 points (10%), Level 2: 525 points (25%), Level 3: 945 points (45%), Level 4: 1407 points (67%)
        
        // User 1 validation - lost points and achievements from subject 1, gained them in subject 2
        projUser1_t0.skillsLevel == 2
        projUser1_t0.points == 700
        projUser1_t0.subjects.skillsLevel == [3, 0]
        projUser1_t0.subjects.points == [700, 0]

        projUser1_t1.skillsLevel == 2
        projUser1_t1.points == 700
        projUser1_t1.subjects.skillsLevel == [4, 1]
        projUser1_t1.subjects.points == [500, 200]

        // Subject 1 should only have individual skills now
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1.subjectId).points == 500
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj1g1.skillId)
        
        // Subject 2 should have the moved group points
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, p1subj2.subjectId).points == 200
        
        // Total project points should remain the same
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user1, null).points == 700
        
        // Achievements should be recalculated - subject 1 increases to level 4 (83.3% of total points)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3, 4]
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj1g1.skillId)
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, p1subj2.subjectId).level.sort() == [1]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user1, p1.projectId, null).level.sort() == [1, 2]

        // User 2 validation - had progress in both subjects, should see points/achievements moved

        projUser2_t0.skillsLevel == 2
        projUser2_t0.points == 900
        projUser2_t0.subjects.skillsLevel == [2, 2]
        projUser2_t0.subjects.points == [500, 400]

        projUser2_t1.skillsLevel == 2
        projUser2_t1.points == 900
        projUser2_t1.subjects.skillsLevel == [3, 2]
        projUser2_t1.subjects.points == [300, 600]

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1.subjectId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj2.subjectId).points == 600
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, p1subj1g1.skillId)
        
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user2, null).points == 900
        
        // Subject 1 increases from level 2 to level 3, subject 2 gains points and stays at level 2
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj1.subjectId).level.sort() == [1, 2, 3]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, p1subj2.subjectId).level.sort() == [1, 2]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user2, p1.projectId, null).level.sort() == [1, 2]

        // User 3 validation - level drops from 2 to 1 in subject 2 due to percentage change from 33.3% to 20%
        projUser3_t0.skillsLevel == 1
        projUser3_t0.points == 300
        projUser3_t0.subjects.skillsLevel == [0, 2]
        projUser3_t0.subjects.points == [0, 300]

        projUser3_t1.skillsLevel == 1
        projUser3_t1.points == 300
        projUser3_t1.subjects.skillsLevel == [0, 1]
        projUser3_t1.subjects.points == [0, 300]

        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user3, p1subj2.subjectId).points == 300
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(p1.projectId, user3, null).points == 300
        
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user3, p1.projectId, p1subj2.subjectId).level.sort() == [1]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user3, p1.projectId, null).level.sort() == [1]
    }

}
