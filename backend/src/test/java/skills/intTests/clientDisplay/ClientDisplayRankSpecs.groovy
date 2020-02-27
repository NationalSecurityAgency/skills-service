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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ClientDisplayRankSpecs extends DefaultIntSpec {

    def "get rank - only 1 very lonely user"(){
        String userId = "user1"

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
        List<String> users = (1..2).collect({ "user${it}".toString() })

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
        List<String> users = (1..5).collect({ "user${it}".toString() })
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
        List<String> users = (1..5).collect({ "user${it}".toString() })
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
        List<String> users = (1..3).collect({ "user${it}".toString() })

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
}
