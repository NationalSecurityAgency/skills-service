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
package skills.intTests.reportSkills

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import spock.lang.Requires

@Slf4j
class BulkReportSkillsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "bulk report skill for multiple users"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], sampleUserIds, new Date())

        then:
        res.body.userIdsAppliedCount
        res.body.userIdsAppliedCount == sampleUserIds.size()
        res.body.userIdsNotAppliedCount == 0
        res.body.userIdsErrored.size() == 0
    }

    @Requires({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "bulk report skill for multiple users, one invalid user "() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        List<String> userIds = []
        userIds.addAll(sampleUserIds)
        userIds.add('doesNotExist')
        def res = skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], userIds, new Date())

        then:
        res.body.userIdsAppliedCount
        res.body.userIdsAppliedCount == sampleUserIds.size()
        res.body.userIdsNotAppliedCount == 0
        res.body.userIdsErrored.size() == 1
        res.body.userIdsErrored[0] == 'doesNotExist'
    }


    def "bulk report skill for multiple users, one skill not applied"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res1 = skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], sampleUserIds.take(1), new Date())
        def res = skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], sampleUserIds, new Date())

        then:

        res1.body.userIdsAppliedCount
        res1.body.userIdsAppliedCount == 1
        res1.body.userIdsNotAppliedCount == 0
        res1.body.userIdsErrored.size() == 0

        res.body.userIdsAppliedCount
        res.body.userIdsAppliedCount == sampleUserIds.size()-1
        res.body.userIdsNotAppliedCount == 1
        res.body.userIdsErrored.size() == 0
    }

    def "attempt to bulk report skill events without specifying a timestamp"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], sampleUserIds, (Long) null)
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("timestamp was not provided., errorCode:BadParam, success:false, projectId:TestProject1, skillId:skill1")
    }

    def "attempt to bulk report skill events without specifying a negative timestamp"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], sampleUserIds, -1L)
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("timestamp must be greater than 0, errorCode:BadParam, success:false, projectId:TestProject1, skillId:skill1")
    }

    def "attempt to bulk report skill events without specifying a future timestamp"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], sampleUserIds, System.currentTimeMillis() + 40000)
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("Skill Events may not be in the future, errorCode:BadParam, success:false, projectId:TestProject1, skillId:skill1")
    }

    def "attempt to bulk report skill events without specifying any userIds"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], [], System.currentTimeMillis())
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("userIds must contain at least 1 item., errorCode:BadParam, success:false, projectId:TestProject1, skillId:skill1")
    }

    def "attempt to bulk report skill events specifying blank userIds"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], [' '], System.currentTimeMillis())
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("userIds must contain at least 1 item., errorCode:BadParam, success:false, projectId:TestProject1, skillId:skill1")
    }

    def "attempt to bulk report skill events for more than the max allowable userIds"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )
        List<String> userIds = (0..1000).collect { "user${it}"}

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        skillsService.bulkAddSkill([projectId: projId, skillId: skills[0].skillId], userIds, System.currentTimeMillis())
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.message.contains("number of userIds cannot exceed 1000, errorCode:BadParam, success:false, projectId:TestProject1, skillId:skill1")
    }
}
