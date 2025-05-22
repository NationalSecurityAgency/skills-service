/**
 * Copyright 2025 SkillTree
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
package skills.intTests.clientDisplay


import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.skillLoading.model.LeaderboardRes
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo

class SkillsDisplayArchivedUsersSpec extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillDefRepo skillDefRepo

    def "archived users are filtered from ranking"() {
        List<String> users = getRandomUsers(5)
        List<Date> days = (0..5).collect { new Date() - it }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(3, 2, 1)
        proj2_skills.each { it.numPerformToCompletion = 10 }
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        Closure addSkills = { def theProj ->
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(0), days.get(0))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(1))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(1))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(2))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(1))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(2))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(3))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(1))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(2))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(3))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(4))
        }
        addSkills(proj1)
        addSkills(proj2)

        def usr1 = skillsService.getRank(users.get(0), proj1.projectId)
        def usr2 = skillsService.getRank(users.get(1), proj1.projectId)
        def usr3 = skillsService.getRank(users.get(2), proj1.projectId)
        def usr4 = skillsService.getRank(users.get(3), proj1.projectId)
        def usr5 = skillsService.getRank(users.get(4), proj1.projectId)

        def usr1Subj = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj.subjectId)
        def usr2Subj = skillsService.getRank(users.get(1), proj1.projectId, proj1_subj.subjectId)
        def usr3Subj = skillsService.getRank(users.get(2), proj1.projectId, proj1_subj.subjectId)
        def usr4Subj = skillsService.getRank(users.get(3), proj1.projectId, proj1_subj.subjectId)
        def usr5Subj = skillsService.getRank(users.get(4), proj1.projectId, proj1_subj.subjectId)

        when:
        skillsService.archiveUsers([users.get(4)], proj1.projectId)

        def usr1_afterArchive = skillsService.getRank(users.get(0), proj1.projectId)
        def usr2_afterArchive = skillsService.getRank(users.get(1), proj1.projectId)
        def usr3_afterArchive = skillsService.getRank(users.get(2), proj1.projectId)
        def usr4_afterArchive = skillsService.getRank(users.get(3), proj1.projectId)
        def usr5_afterArchive = skillsService.getRank(users.get(4), proj1.projectId)

        def usr2Subj_afterArchive = skillsService.getRank(users.get(1), proj1.projectId, proj1_subj.subjectId)
        def usr3Subj_afterArchive = skillsService.getRank(users.get(2), proj1.projectId, proj1_subj.subjectId)
        def usr4Subj_afterArchive = skillsService.getRank(users.get(3), proj1.projectId, proj1_subj.subjectId)
        def usr5Subj_afterArchive = skillsService.getRank(users.get(4), proj1.projectId, proj1_subj.subjectId)
        def usr1Subj_afterArchive = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj.subjectId)

        def usr1P2_afterArchive = skillsService.getRank(users.get(0), proj2.projectId)
        def usr2P2_afterArchive = skillsService.getRank(users.get(1), proj2.projectId)
        def usr3P2_afterArchive = skillsService.getRank(users.get(2), proj2.projectId)
        def usr4P2_afterArchive = skillsService.getRank(users.get(3), proj2.projectId)
        def usr5P2_afterArchive = skillsService.getRank(users.get(4), proj2.projectId)

        def usr2SubjP2_afterArchive = skillsService.getRank(users.get(1), proj2.projectId, proj2_subj.subjectId)
        def usr3SubjP2_afterArchive = skillsService.getRank(users.get(2), proj2.projectId, proj2_subj.subjectId)
        def usr4SubjP2_afterArchive = skillsService.getRank(users.get(3), proj2.projectId, proj2_subj.subjectId)
        def usr5SubjP2_afterArchive = skillsService.getRank(users.get(4), proj2.projectId, proj2_subj.subjectId)
        def usr1SubjP2_afterArchive = skillsService.getRank(users.get(0), proj2.projectId, proj2_subj.subjectId)

        then:
        usr1.numUsers == 5
        usr1.position == 5

        usr2.numUsers == 5
        usr2.position == 4

        usr3.numUsers == 5
        usr3.position == 3

        usr4.numUsers == 5
        usr4.position == 2

        usr5.numUsers == 5
        usr5.position == 1

        usr1Subj.numUsers == 5
        usr1Subj.position == 5

        usr2Subj.numUsers == 5
        usr2Subj.position == 4

        usr3Subj.numUsers == 5
        usr3Subj.position == 3

        usr4Subj.numUsers == 5
        usr4Subj.position == 2

        usr5Subj.numUsers == 5
        usr5Subj.position == 1

        usr1_afterArchive.numUsers == 4
        usr1_afterArchive.position == 4
        usr1_afterArchive.archivedUser == false

        usr2_afterArchive.numUsers == 4
        usr2_afterArchive.position == 3
        usr2_afterArchive.archivedUser == false

        usr3_afterArchive.numUsers == 4
        usr3_afterArchive.position == 2
        usr3_afterArchive.archivedUser == false

        usr4_afterArchive.numUsers == 4
        usr4_afterArchive.position == 1
        usr4_afterArchive.archivedUser == false

        usr5_afterArchive.numUsers == 4
        usr5_afterArchive.position == -1
        usr5_afterArchive.archivedUser == true

        usr1Subj_afterArchive.numUsers == 4
        usr1Subj_afterArchive.position == 4
        usr1Subj_afterArchive.archivedUser == false

        usr2Subj_afterArchive.numUsers == 4
        usr2Subj_afterArchive.position == 3
        usr2Subj_afterArchive.archivedUser == false

        usr3Subj_afterArchive.numUsers == 4
        usr3Subj_afterArchive.position == 2
        usr3Subj_afterArchive.archivedUser == false

        usr4Subj_afterArchive.numUsers == 4
        usr4Subj_afterArchive.position == 1
        usr4Subj_afterArchive.archivedUser == false

        usr5Subj_afterArchive.numUsers == 4
        usr5Subj_afterArchive.position == -1
        usr5Subj_afterArchive.archivedUser == true

        // p2
        usr1P2_afterArchive.numUsers == 5
        usr1P2_afterArchive.position == 5

        usr2P2_afterArchive.numUsers == 5
        usr2P2_afterArchive.position == 4

        usr3P2_afterArchive.numUsers == 5
        usr3P2_afterArchive.position == 3

        usr4P2_afterArchive.numUsers == 5
        usr4P2_afterArchive.position == 2

        usr5P2_afterArchive.numUsers == 5
        usr5P2_afterArchive.position == 1

        usr1SubjP2_afterArchive.numUsers == 5
        usr1SubjP2_afterArchive.position == 5

        usr2SubjP2_afterArchive.numUsers == 5
        usr2SubjP2_afterArchive.position == 4

        usr3SubjP2_afterArchive.numUsers == 5
        usr3SubjP2_afterArchive.position == 3

        usr4SubjP2_afterArchive.numUsers == 5
        usr4SubjP2_afterArchive.position == 2

        usr5SubjP2_afterArchive.numUsers == 5
        usr5SubjP2_afterArchive.position == 1
    }

    def "archived users are filtered from leaderboard - top 10"() {
        List<String> users = getRandomUsers(5)
        List<Date> days = (0..5).collect { new Date() - it }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(3, 2, 1)
        proj2_skills.each { it.numPerformToCompletion = 10 }
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        Closure addSkills = { def theProj ->
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(0), days.get(0))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(1))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(1))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(2))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(1))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(2))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(3))

            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(0))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(1))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(2))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(3))
            skillsService.addSkill([projectId: theProj.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(4))
        }
        addSkills(proj1)
        addSkills(proj2)

        def usr1 = skillsService.getLeaderboard(users.get(0), proj1.projectId)
        def usr2 = skillsService.getLeaderboard(users.get(1), proj1.projectId)
        def usr3 = skillsService.getLeaderboard(users.get(2), proj1.projectId)
        def usr4 = skillsService.getLeaderboard(users.get(3), proj1.projectId)
        def usr5 = skillsService.getLeaderboard(users.get(4), proj1.projectId)

        def usr1Subj = skillsService.getLeaderboard(users.get(0), proj1.projectId, proj1_subj.subjectId)
        def usr2Subj = skillsService.getLeaderboard(users.get(1), proj1.projectId, proj1_subj.subjectId)
        def usr3Subj = skillsService.getLeaderboard(users.get(2), proj1.projectId, proj1_subj.subjectId)
        def usr4Subj = skillsService.getLeaderboard(users.get(3), proj1.projectId, proj1_subj.subjectId)
        def usr5Subj = skillsService.getLeaderboard(users.get(4), proj1.projectId, proj1_subj.subjectId)

        when:
        skillsService.archiveUsers([users.get(4)], proj1.projectId)

        def usr1_afterArchive = skillsService.getLeaderboard(users.get(0), proj1.projectId)
        def usr2_afterArchive = skillsService.getLeaderboard(users.get(1), proj1.projectId)
        def usr3_afterArchive = skillsService.getLeaderboard(users.get(2), proj1.projectId)
        def usr4_afterArchive = skillsService.getLeaderboard(users.get(3), proj1.projectId)
        def usr5_afterArchive = skillsService.getLeaderboard(users.get(4), proj1.projectId)

        def usr2Subj_afterArchive = skillsService.getLeaderboard(users.get(1), proj1.projectId, proj1_subj.subjectId)
        def usr3Subj_afterArchive = skillsService.getLeaderboard(users.get(2), proj1.projectId, proj1_subj.subjectId)
        def usr4Subj_afterArchive = skillsService.getLeaderboard(users.get(3), proj1.projectId, proj1_subj.subjectId)
        def usr5Subj_afterArchive = skillsService.getLeaderboard(users.get(4), proj1.projectId, proj1_subj.subjectId)
        def usr1Subj_afterArchive = skillsService.getLeaderboard(users.get(0), proj1.projectId, proj1_subj.subjectId)

        def usr1P2_afterArchive = skillsService.getLeaderboard(users.get(0), proj2.projectId)
        def usr2P2_afterArchive = skillsService.getLeaderboard(users.get(1), proj2.projectId)
        def usr3P2_afterArchive = skillsService.getLeaderboard(users.get(2), proj2.projectId)
        def usr4P2_afterArchive = skillsService.getLeaderboard(users.get(3), proj2.projectId)
        def usr5P2_afterArchive = skillsService.getLeaderboard(users.get(4), proj2.projectId)

        def usr1SubjP2_afterArchive = skillsService.getLeaderboard(users.get(0), proj2.projectId, proj2_subj.subjectId)
        def usr2SubjP2_afterArchive = skillsService.getLeaderboard(users.get(1), proj2.projectId, proj2_subj.subjectId)
        def usr3SubjP2_afterArchive = skillsService.getLeaderboard(users.get(2), proj2.projectId, proj2_subj.subjectId)
        def usr4SubjP2_afterArchive = skillsService.getLeaderboard(users.get(3), proj2.projectId, proj2_subj.subjectId)
        def usr5SubjP2_afterArchive = skillsService.getLeaderboard(users.get(4), proj2.projectId, proj2_subj.subjectId)

        then:
        usr1.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr1.archivedUser == false
        usr2.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr2.archivedUser == false
        usr3.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr3.archivedUser == false
        usr4.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr4.archivedUser == false
        usr5.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr5.archivedUser == false

        usr1Subj.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr1Subj.archivedUser == false
        usr2Subj.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr2Subj.archivedUser == false
        usr3Subj.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr3Subj.archivedUser == false
        usr4Subj.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr4Subj.archivedUser == false
        usr5Subj.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr5Subj.archivedUser == false

        usr1_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr1_afterArchive.archivedUser == false
        usr2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr2_afterArchive.archivedUser == false
        usr3_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr3_afterArchive.archivedUser == false
        usr4_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr4_afterArchive.archivedUser == false
        !usr5_afterArchive.rankedUsers
        usr5_afterArchive.archivedUser == true

        usr1Subj_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr1Subj_afterArchive.archivedUser == false
        usr2Subj_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr2Subj_afterArchive.archivedUser == false
        usr3Subj_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr3Subj_afterArchive.archivedUser == false
        usr4Subj_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(3), users.get(2), users.get(1), users.get(0)])
        usr4Subj_afterArchive.archivedUser == false
        !usr5Subj_afterArchive.rankedUsers
        usr5Subj_afterArchive.archivedUser == true

        // p2
        usr1P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr1P2_afterArchive.archivedUser == false
        usr2P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr2P2_afterArchive.archivedUser == false
        usr3P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr3P2_afterArchive.archivedUser == false
        usr4P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr4P2_afterArchive.archivedUser == false
        usr5P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr5P2_afterArchive.archivedUser == false

        usr1SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr1SubjP2_afterArchive.archivedUser == false
        usr2SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr2SubjP2_afterArchive.archivedUser == false
        usr3SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr3SubjP2_afterArchive.archivedUser == false
        usr4SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr4SubjP2_afterArchive.archivedUser == false
        usr5SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay([users.get(4), users.get(3), users.get(2), users.get(1), users.get(0)])
        usr5SubjP2_afterArchive.archivedUser == false
    }

    def "archived users are filtered from leaderboard - 10 around me"() {
        List<String> users = getRandomUsers(25)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(25, 1, 1)
        proj1_skills.eachWithIndex { it, int index ->
            it.pointIncrement = ((index+1) * 10)
        }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(25, 2, 1)
        proj2_skills.eachWithIndex { it, int index ->
            it.pointIncrement = ((index+1) * 10)
        }
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        Closure addSkills = { def theProj, List<Map> theSkills ->
            25.times { Integer index ->
                skillsService.addSkill([projectId: theProj.projectId, skillId: theSkills.get(index).skillId], users.get(index), new Date())
            }
        }
        addSkills(proj1, proj1_skills)
        addSkills(proj2, proj2_skills)

        def usr1 = skillsService.getLeaderboard(users.get(0), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr2 = skillsService.getLeaderboard(users.get(5), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr3 = skillsService.getLeaderboard(users.get(15), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr4 = skillsService.getLeaderboard(users.get(4), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())

        def usr1Subj = skillsService.getLeaderboard(users.get(0), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr2Subj = skillsService.getLeaderboard(users.get(5), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr3Subj = skillsService.getLeaderboard(users.get(15), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr4Subj = skillsService.getLeaderboard(users.get(4), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())

        when:
        skillsService.archiveUsers([users.get(4)], proj1.projectId)

        def usr1_afterArchive = skillsService.getLeaderboard(users.get(0), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr2_afterArchive = skillsService.getLeaderboard(users.get(5), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr3_afterArchive = skillsService.getLeaderboard(users.get(15), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr4_afterArchive = skillsService.getLeaderboard(users.get(4), proj1.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())

        def usr1Subj_afterArchive = skillsService.getLeaderboard(users.get(0), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr2Subj_afterArchive = skillsService.getLeaderboard(users.get(5), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr3Subj_afterArchive = skillsService.getLeaderboard(users.get(15), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr4Subj_afterArchive = skillsService.getLeaderboard(users.get(4), proj1.projectId, proj1_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())

        def usr1P2_afterArchive = skillsService.getLeaderboard(users.get(0), proj2.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr2P2_afterArchive = skillsService.getLeaderboard(users.get(5), proj2.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr3P2_afterArchive = skillsService.getLeaderboard(users.get(15), proj2.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())
        def usr4P2_afterArchive = skillsService.getLeaderboard(users.get(4), proj2.projectId, null, LeaderboardRes.Type.tenAroundMe.toString())

        def usr1SubjP2_afterArchive = skillsService.getLeaderboard(users.get(0), proj2.projectId, proj2_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr2SubjP2_afterArchive = skillsService.getLeaderboard(users.get(5), proj2.projectId, proj2_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr3SubjP2_afterArchive = skillsService.getLeaderboard(users.get(15), proj2.projectId, proj2_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())
        def usr4SubjP2_afterArchive = skillsService.getLeaderboard(users.get(4), proj2.projectId, proj2_subj.subjectId, LeaderboardRes.Type.tenAroundMe.toString())


        then:
        usr1.rankedUsers.collect{ it.userId } == forDisplay(users[5..0])
        usr1.archivedUser == false
        usr2.rankedUsers.collect{ it.userId } == forDisplay(users[10..0])
        usr2.archivedUser == false
        usr3.rankedUsers.collect{ it.userId } == forDisplay(users[20..10])
        usr3.archivedUser == false
        usr4.rankedUsers.collect{ it.userId } == forDisplay(users[9..0])
        usr4.archivedUser == false

        usr1Subj.rankedUsers.collect{ it.userId } == forDisplay(users[5..0])
        usr1Subj.archivedUser == false
        usr2Subj.rankedUsers.collect{ it.userId } == forDisplay(users[10..0])
        usr2Subj.archivedUser == false
        usr3Subj.rankedUsers.collect{ it.userId } == forDisplay(users[20..10])
        usr3Subj.archivedUser == false
        usr4Subj.rankedUsers.collect{ it.userId } == forDisplay(users[9..0])
        usr4Subj.archivedUser == false

        usr1_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users.findAll { it != users.get(4) }[5..0])
        usr1_afterArchive.archivedUser == false
        usr2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users.findAll { it != users.get(4) }[9..0])
        usr2_afterArchive.archivedUser == false
        usr3_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users.findAll { it != users.get(4) }[19..9])
        usr3_afterArchive.archivedUser == false
        !usr4_afterArchive.rankedUsers
        usr4_afterArchive.archivedUser == true

        usr1Subj_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users.findAll { it != users.get(4) }[5..0])
        usr1Subj_afterArchive.archivedUser == false
        usr2Subj_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users.findAll { it != users.get(4) }[9..0])
        usr2Subj_afterArchive.archivedUser == false
        usr3Subj_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users.findAll { it != users.get(4) }[19..9])
        usr3Subj_afterArchive.archivedUser == false
        !usr4Subj_afterArchive.rankedUsers
        usr4Subj_afterArchive.archivedUser == true

        // p2
        usr1P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[5..0])
        usr1P2_afterArchive.archivedUser == false
        usr2P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[10..0])
        usr2P2_afterArchive.archivedUser == false
        usr3P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[20..10])
        usr3P2_afterArchive.archivedUser == false
        usr4P2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[9..0])
        usr4P2_afterArchive.archivedUser == false

        usr1SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[5..0])
        usr1SubjP2_afterArchive.archivedUser == false
        usr2SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[10..0])
        usr2SubjP2_afterArchive.archivedUser == false
        usr3SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[20..10])
        usr3SubjP2_afterArchive.archivedUser == false
        usr4SubjP2_afterArchive.rankedUsers.collect{ it.userId } == forDisplay(users[9..0])
        usr4SubjP2_afterArchive.archivedUser == false
    }

    List<String> forDisplay(List<String> users) {
        return users.collect {
            userAttrsRepo.findByUserIdIgnoreCase(it)?.userIdForDisplay
        }
    }
}
