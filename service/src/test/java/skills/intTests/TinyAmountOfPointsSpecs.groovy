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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

/**
 * Make sure systems behaves well when ruleset definitions don't contain a lot of points
 */
class TinyAmountOfPointsSpecs extends DefaultIntSpec {

    def "if there are not enough to properly calculate levels then skills events cannot be added"() {
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill1".toString(),
                                   name     : "Test Skill 1".toString(),
                                   type     : "Skill", pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])

        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"])

        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient project points, skill achievement is disallowed, errorCode:InternalError, success:false, projectId:${proj1.projectId}, skillId:null")
        skillsClientException.message.contains("${skillsService.userName}")
    }

    def "user level should be zero if project has insufficient points"(){
        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill1".toString(),
                                   name     : "Test Skill 1".toString(),
                                   type     : "Skill", pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill2".toString(),
                                   name     : "Test Skill 2".toString(),
                                   type     : "Skill", pointIncrement: 120, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])

        skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"])

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: proj1_subj.subjectId, skillId: "skill2"])
        def userLevel = skillsService.getSkillSummary(user, proj1.projectId).skillsLevel

        when:
        def res1 = skillsService.addSkill([projectId: proj1.projectId, skillId: "skill1"])

        then:
        userLevel == 0
    }

    def "skills may not be achieved if subject has insufficient points (even if project does)"(){

        String user = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        def proj1_subj1_skills = SkillsFactory.createSkills(25, 1, 1, )
        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        proj1_subj1_skills.each{
            skillsService.createSkill(it)
        }
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkill([projectId: proj1.projectId, subjectId: proj1_subj2.subjectId, skillId: "skill111".toString(),
                                   name     : "Test Skill 11111111111111".toString(),
                                   type     : "Skill", pointIncrement: 10, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
                                   version  : 0])
        when:
        def result = skillsService.addSkill([projectId:proj1.projectId, skillId:"skill111"])

        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        skillsClientException.message.contains("Insufficient Subject points, skill achievement is disallowed, errorCode:InternalError, success:false, projectId:${proj1.projectId}, skillId:null".toString())
        skillsClientException.message.contains("${skillsService.userName}")
    }
}
