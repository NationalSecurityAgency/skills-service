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
package skills.intTests.clientDisplay

import skills.controller.UserInfoController
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.skillLoading.RankingLoader

class ClientDisplayRankSpecs extends DefaultIntSpec {

    def "get rank - only 1 very lonely user"(){
        String userId = getRandomUsers(1).first()

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getRank(userId, proj1.projectId)
        def summarySubj = skillsService.getRank(userId, proj1.projectId, proj1_subj.subjectId)
        then:
        summary.numUsers == 1
        summary.position == 1

        summarySubj.numUsers == 1
        summarySubj.position == 1
    }

    def "get rank - just 2 users with same points"(){
        List<String> users = getRandomUsers(2)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(0), new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), new Date())

        when:
        def summaryUsr1 = skillsService.getRank(users.get(0), proj1.projectId)
        def summaryUsr2 = skillsService.getRank(users.get(1), proj1.projectId)

        def summaryUsr1Subj = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj.subjectId)
        def summaryUsr2Subj = skillsService.getRank(users.get(1), proj1.projectId, proj1_subj.subjectId)
        then:
        summaryUsr1.numUsers == 2
        summaryUsr1.position == 1

        summaryUsr2.numUsers == 2
        summaryUsr2.position == 1

        summaryUsr1Subj.numUsers == 2
        summaryUsr1Subj.position == 1

        summaryUsr2Subj.numUsers == 2
        summaryUsr2Subj.position == 1
    }

    def "get rank - users with various ranks"(){
        List<String> users = getRandomUsers(5)
        List<Date> days = (0..5).collect { new Date() - it }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(0), days.get(0))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(1))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(2))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(2))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(3))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(2))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(3))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(4))

        when:
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
    }

    def "get rank - tie for first"(){
        List<String> users = getRandomUsers(5)
        List<Date> days = (0..5).collect { new Date() - it }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(0), days.get(0))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(1))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(2))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(2))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(3))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(2))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(3))

        when:
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
        then:
        usr1.numUsers == 5
        usr1.position == 5

        usr2.numUsers == 5
        usr2.position == 4

        usr3.numUsers == 5
        usr3.position == 3

        usr4.numUsers == 5
        usr4.position == 1

        usr5.numUsers == 5
        usr5.position == 1

        usr1Subj.numUsers == 5
        usr1Subj.position == 5

        usr2Subj.numUsers == 5
        usr2Subj.position == 4

        usr3Subj.numUsers == 5
        usr3Subj.position == 3

        usr4Subj.numUsers == 5
        usr4Subj.position == 1

        usr5Subj.numUsers == 5
        usr5Subj.position == 1
    }

    def "users from other projects should not affect each other"(){
        List<String> users = getRandomUsers(3)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj2_skills = SkillsFactory.createSkills(3, 2, 1)
        proj2_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(0), new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users.get(1), new Date())

        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(0).skillId], users.get(1), new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: proj2_skills.get(1).skillId], users.get(2), new Date())

        when:
        def summaryUsr1 = skillsService.getRank(users.get(0), proj1.projectId)
        def summaryUsr2 = skillsService.getRank(users.get(1), proj1.projectId)
        def summaryUsr3 = skillsService.getRank(users.get(2), proj1.projectId)

        def summaryUsr1Subj = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj.subjectId)
        def summaryUsr2Subj = skillsService.getRank(users.get(1), proj1.projectId, proj1_subj.subjectId)
        def summaryUsr3Subj = skillsService.getRank(users.get(2), proj1.projectId, proj1_subj.subjectId)

        def summaryUsr1Proj2 = skillsService.getRank(users.get(0), proj2.projectId)
        def summaryUsr2Proj2 = skillsService.getRank(users.get(1), proj2.projectId)
        def summaryUsr3Proj2 = skillsService.getRank(users.get(2), proj2.projectId)

        def summaryUsr1Proj2Subj = skillsService.getRank(users.get(0), proj2.projectId, proj2_subj.subjectId)
        def summaryUsr2Proj2Subj = skillsService.getRank(users.get(1), proj2.projectId, proj2_subj.subjectId)
        def summaryUsr3Proj2Subj = skillsService.getRank(users.get(2), proj2.projectId, proj2_subj.subjectId)

        then:
        summaryUsr1.numUsers == 2
        summaryUsr1.position == 2

        summaryUsr2.numUsers == 2
        summaryUsr2.position == 1

        // since the user never visited this project then he/she is placed at the very end
        summaryUsr3.numUsers == 3
        summaryUsr3.position == 3

        summaryUsr1Subj.numUsers == 2
        summaryUsr1Subj.position == 2

        summaryUsr2Subj.numUsers == 2
        summaryUsr2Subj.position == 1

        summaryUsr3Subj.numUsers == 3
        summaryUsr3Subj.position == 3

        summaryUsr1Proj2.numUsers == 3
        summaryUsr1Proj2.position == 3

        summaryUsr2Proj2.numUsers == 2
        summaryUsr2Proj2.position == 1

        summaryUsr3Proj2.numUsers == 2
        summaryUsr3Proj2.position == 1

        summaryUsr1Proj2Subj.numUsers == 3
        summaryUsr1Proj2Subj.position == 3

        summaryUsr2Proj2Subj.numUsers == 2
        summaryUsr2Proj2Subj.position == 1

        summaryUsr3Proj2Subj.numUsers == 2
        summaryUsr3Proj2Subj.position == 1
    }

    def "get rank - only 1 very lonely user opted out"(){
        List<String> users = getRandomUsers(2)
        String userId = users.get(0)
        String otherUserId = users.get(1)
        SkillsService user1SkillService = createService(userId)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        // user opt-out
        user1SkillService.addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')

        when:
        def summary = skillsService.getRank(userId, proj1.projectId)
        def summarySubj = skillsService.getRank(userId, proj1.projectId, proj1_subj.subjectId)

        def summary1 = skillsService.getRank(otherUserId, proj1.projectId)
        def summarySubj1 = skillsService.getRank(otherUserId, proj1.projectId, proj1_subj.subjectId)

        then:
        summary.numUsers == 1
        summary.position == 1
        summary.optedOut

        summarySubj.numUsers == 1
        summarySubj.position == 1
        summarySubj.optedOut

        summary1.numUsers == 1
        summary1.position == 1
        !summary1.optedOut

        summarySubj1.numUsers == 1
        summarySubj1.position == 1
        !summarySubj1.optedOut
    }

    def "opted out user should not be ranked"() {
        List<String> users = getRandomUsers(5)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(5, 1, 1, 100)

        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)


        users.eachWithIndex { String user, int index ->
           ((4-index)..0).each { Integer skillCount ->
                skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(skillCount).skillId], user, new Date())
            }
        }

        // user opt-out
        createService(users[2]).addOrUpdateUserSetting(UserInfoController.RANK_AND_LEADERBOARD_OPT_OUT_PREF, 'true')

        when:
        List subjectRanks = users.collect {skillsService.getRank(it, proj1.projectId, proj1_subj.subjectId) }
        List ranks = users.collect {skillsService.getRank(it, proj1.projectId) }

        then:
        ranks.collect { it.optedOut } == [false, false, true, false, false]
        ranks[0].position == 1
        ranks[1].position == 2
        ranks[2].position == 3 // this one opted-out
        ranks[3].position == 3 // should skip opted-out user
        ranks[4].position == 4

        subjectRanks.collect { it.optedOut } == [false, false, true, false, false]
        subjectRanks[0].position == 1
        subjectRanks[1].position == 2
        subjectRanks[2].position == 3 // this one opted-out
        subjectRanks[3].position == 3 // should skip opted-out user
        subjectRanks[4].position == 4
    }

    def "ability to opt-out all project admins from being ranked"() {
        List<String> users = getRandomUsers(7)

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(7, 1, 1, 100)

        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)


        users.eachWithIndex { String user, int index ->
            ((6-index)..0).each { Integer skillCount ->
                skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(skillCount).skillId], user, new Date())
            }
        }

        // make couple users admins
        createService(users[2])
        skillsService.addProjectAdmin(proj1.projectId, users[2])

        createService(users[5])
        skillsService.addProjectAdmin(proj1.projectId, users[5])
        skillsService.addOrUpdateProjectSetting(proj1.projectId, RankingLoader.PROJ_ADMINS_RANK_AND_LEADERBOARD_OPT_OUT_PREF, true.toString())

        when:
        List subjectRanks = users.collect {skillsService.getRank(it, proj1.projectId, proj1_subj.subjectId) }
        List ranks = users.collect {skillsService.getRank(it, proj1.projectId) }

        then:
        ranks.collect { it.optedOut } == [false, false, true, false, false, true, false]
        ranks[0].position == 1
        ranks[1].position == 2
        ranks[2].position == 3 // this one opted-out
        ranks[3].position == 3 // should skip opted-out user
        ranks[4].position == 4
        ranks[5].position == 5 // this one opted-out
        ranks[6].position == 5 // should skip opted-out user

        subjectRanks.collect { it.optedOut } == [false, false, true, false, false, true, false]
        subjectRanks[0].position == 1
        subjectRanks[1].position == 2
        subjectRanks[2].position == 3 // this one opted-out
        subjectRanks[3].position == 3 // should skip opted-out user
        subjectRanks[4].position == 4
        subjectRanks[5].position == 5 // this one opted-out
        subjectRanks[6].position == 5 // should skip opted-out user
    }
}
