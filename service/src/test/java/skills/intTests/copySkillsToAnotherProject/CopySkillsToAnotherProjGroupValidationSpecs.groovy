/**
 * Copyright 2024 SkillTree
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
package skills.intTests.copySkillsToAnotherProject


import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.*

class CopySkillsToAnotherProjGroupValidationSpecs extends CopyIntSpec {


    def "validate that there is no skill id collisions with skill group destination"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].skillId = p1Subj1Skills[2].skillId
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Subj1Skills[2])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("ID [${p1Subj1Skills[2].skillId}] already exists in the project [${p2.projectId}]")
    }

    def "validate that all provided skill ids exist"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        List<String> skillIds = p1Subj1Skills.collect { it.skillId as String } + ["skill-does-not-exist"]
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, skillIds, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Not all provided skill were loaded, missing skill ids are: [skill-does-not-exist]")
    }

    def "validate that there is no skill name collisions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        p2Subj1Skills[1].name = p1Subj1Skills[0].name
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Subj1Skills[0])
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Subj1Skills[1])
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Subj1Skills[2])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with name [${p2Subj1Skills[1].name}] already exists in the project [${p2.projectId}]")
    }

    def "validate dest proj exist"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        String badProjectId = p2.projectId + "a"
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, badProjectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Project with id [${badProjectId}] does not exist")
    }

    def "validate destination group exist"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2Subj1Skills = createSkills(3, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2Subj1Skills)

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, p2Subj1Skills[0].skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Group with id [${p2Subj1Skills[0].skillId}] does not exist.")
    }

    def "validate subject id is provided"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, destGroup.skillId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Provided id [${destGroup.skillId}] is not for a subject")
    }

    def "user with approver role for the destination project does not have permission to copy"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        SkillsService otherUser = createService(getRandomUsers(1).first())
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        otherUser.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])
        otherUser.addUserRole(skillsService.userName, p2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Subj1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("User [${skillsService.userName}] is not an admin for destination project [${p2.projectId}]")
    }

    def "must be an admin of destination project to copy"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        SkillsService otherUser = createService(getRandomUsers(1).first())
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        otherUser.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("User [${skillsService.userName}] is not an admin for destination project [${p2.projectId}]")
    }

    def "do not allow to copy from community protected project to a non-community protected project"() {
        SkillsService rootSkillsService = createRootSkillService()
        SkillsService dragonUser = createService(getRandomUsers(1).first())
        rootSkillsService.saveUserTag(dragonUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        dragonUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        dragonUser.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        dragonUser.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Subjects from Divine Dragon projects cannot be copied to All Dragons projects")
    }

    def "do allow to copy imported skills that are not finalized"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.bulkImportSkillsFromCatalog(p2.projectId, p2subj1.subjectId, [
                [projectId: p1.projectId, skillId: p1Skills[0].skillId],
                [projectId: p1.projectId, skillId: p1Skills[1].skillId],
        ])

        def projToCopy = createProject(3)
        def p3subj1 = createSubject(3, 1)
        def destGroup = createSkillsGroup(3, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(projToCopy, p3subj1, [destGroup])

        when:
        List<String> skillIdsToCopy = [p1Skills[0].skillId, p1Skills[1].skillId]
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p2.projectId, skillIdsToCopy, projToCopy.projectId, p3subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Can't copy imported skills, following skills were imported: [${p1Skills[0].skillId}, ${p1Skills[1].skillId}]")
    }

    def "do allow to copy imported skills that are finalized"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, [
                [projectId: p1.projectId, skillId: p1Skills[0].skillId],
                [projectId: p1.projectId, skillId: p1Skills[1].skillId],
        ])

        def projToCopy = createProject(3)
        def p3subj1 = createSubject(3, 1)
        def destGroup = createSkillsGroup(3, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(projToCopy, p3subj1, [destGroup])

        when:
        List<String> skillIdsToCopy = [p1Skills[0].skillId, p1Skills[1].skillId].sort()
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p2.projectId, skillIdsToCopy, projToCopy.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Can't copy imported skills, following skills were imported: [${skillIdsToCopy[0]}, ${skillIdsToCopy[1]}]")
    }

    def "do not allow to copy reused skills"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        p1Skills[0].description = "blah blah blah"
        p1Skills[0].helpUrl = "/ok/that/is/good"
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        p1Skills[0].justificationRequired = true

        p1Skills[1].description = "something else"
        p1Skills[1].helpUrl = "http://www.djleaje.org"
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        p1Skills[1].justificationRequired = false

        def group1 = createSkillsGroup(1, 1, 22)
        skillsService.createSubject(p1subj1)
        skillsService.createSkills([p1Skills[0..3], group1].flatten())
        p1Skills[4..9].each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def group3 = createSkillsGroup(1, 2, 33)
        skillsService.createSubject(p1subj2)
        skillsService.createSkills([group3])

        // group in the same subject
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, group1.skillId)
        // different subject
        skillsService.reuseSkills(p1.projectId, [p1Skills[1].skillId], p1subj2.subjectId)
        // group in a different subject
        skillsService.reuseSkills(p1.projectId, [p1Skills[7].skillId], p1subj2.subjectId, group3.skillId)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def destGroup = createSkillsGroup(2, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        List<String> skillIdsToCopy = [SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0), SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)]
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, skillIdsToCopy, p2.projectId, p2subj1.subjectId, destGroup.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains(":Can't copy imported skills, following skills were imported: [${SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)}, ${SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)}]")
    }

}
