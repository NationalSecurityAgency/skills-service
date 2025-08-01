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
package skills.intTests.copySubject

import groovy.json.JsonOutput
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import skills.intTests.copyProject.CopyIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.Attachment
import skills.storage.model.SkillDef

import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

class CopySubjectSkillsSpecs extends CopyIntSpec {

    def "skill attributes are properly copied - skill under a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        skill1.description = "blah blah blah"
        skill1.helpUrl = "/ok/that/is/good"
        skill1.selfReportingType = SkillDef.SelfReportingType.Approval
        skill1.justificationRequired = true
        skill1.iconClass = 'fa fa-icon-test'

        def skill2 = createSkill(1, 1, 23, 0, 13, 458, 55,)
        skill2.description = "something else"
        skill2.helpUrl = "http://www.djleaje.org"
        skill2.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill2.justificationRequired = false
        skill2.iconClass ='fa fa-icon-test2'
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [skill1, skill2])

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def originalSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def originalSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])

        def copiedSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def copiedSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        then:
        copiedSkill1.skillId == originalSkill1.skillId
        copiedSkill1.projectId == p2.projectId
        copiedSkill1.name == originalSkill1.name
        copiedSkill1.version == originalSkill1.version
        copiedSkill1.displayOrder == originalSkill1.displayOrder
        copiedSkill1.totalPoints == originalSkill1.totalPoints
        copiedSkill1.pointIncrement == originalSkill1.pointIncrement
        copiedSkill1.pointIncrementInterval == originalSkill1.pointIncrementInterval
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.numPerformToCompletion == originalSkill1.numPerformToCompletion
        copiedSkill1.type == originalSkill1.type
        copiedSkill1.selfReportingType == originalSkill1.selfReportingType
        copiedSkill1.enabled == originalSkill1.enabled
        copiedSkill1.justificationRequired == originalSkill1.justificationRequired
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.description == originalSkill1.description
        copiedSkill1.helpUrl == originalSkill1.helpUrl
        copiedSkill1.iconClass == 'fa fa-icon-test'
        copiedSkill1.iconClass == originalSkill1.iconClass
        !copiedSkill1.groupName
        !copiedSkill1.groupId
        !copiedSkill1.readOnly
        !copiedSkill1.reusedSkill
        !copiedSkill1.thisSkillWasReusedElsewhere

        copiedSkill2.skillId == originalSkill2.skillId
        copiedSkill2.projectId == p2.projectId
        copiedSkill2.name == originalSkill2.name
        copiedSkill2.version == originalSkill2.version
        copiedSkill2.displayOrder == originalSkill2.displayOrder
        copiedSkill2.totalPoints == originalSkill2.totalPoints
        copiedSkill2.pointIncrement == originalSkill2.pointIncrement
        copiedSkill2.pointIncrementInterval == originalSkill2.pointIncrementInterval
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.numPerformToCompletion == originalSkill2.numPerformToCompletion
        copiedSkill2.type == originalSkill2.type
        copiedSkill2.selfReportingType == originalSkill2.selfReportingType
        copiedSkill2.enabled == originalSkill2.enabled
        copiedSkill2.justificationRequired == originalSkill2.justificationRequired
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.description == originalSkill2.description
        copiedSkill2.helpUrl == originalSkill2.helpUrl
        copiedSkill2.iconClass == 'fa fa-icon-test2'
        copiedSkill2.iconClass == originalSkill2.iconClass
        !copiedSkill2.groupName
        !copiedSkill2.groupId
        !copiedSkill2.readOnly
        !copiedSkill2.reusedSkill
        !copiedSkill2.thisSkillWasReusedElsewhere
    }

    def "skill attributes are properly copied - skill under a group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        skill1.description = "blah blah blah"
        skill1.helpUrl = "/ok/that/is/good"
        skill1.selfReportingType = SkillDef.SelfReportingType.Approval
        skill1.justificationRequired = true
        skill1.iconClass = 'fa fa-icon-test'

        def skill2 = createSkill(1, 1, 23, 0, 13, 458, 55,)
        skill2.description = "something else"
        skill2.helpUrl = "http://www.djleaje.org"
        skill2.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill2.justificationRequired = false
        skill2.iconClass = 'fa fa-icon-test'

        def group = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [group])
        skillsService.assignSkillToSkillsGroup(group.skillId, skill1)
        skillsService.assignSkillToSkillsGroup(group.skillId, skill2)

        def group2 = createSkillsGroup(1, 1, 5)
        def skill3 = createSkill(1, 1, 24, 0, 13, 458, 55,)
        skill3.description = "something else"
        skill3.helpUrl = "http://www.djleaje.org"
        skill3.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill3.justificationRequired = false
        skill3.iconClass = 'fa fa-icon-test'
        skillsService.createSkill(group2)
        skillsService.assignSkillToSkillsGroup(group2.skillId, skill3)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def originalSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def originalSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        def originalSkill3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill3.skillId])

        def copiedSkill1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def copiedSkill2 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        def copiedSkill3 = skillsService.getSkill([projectId: p2.projectId, subjectId: p1subj1.subjectId, skillId: skill3.skillId])
        then:
        copiedSkill1.skillId == originalSkill1.skillId
        copiedSkill1.projectId == p2.projectId
        copiedSkill1.name == originalSkill1.name
        copiedSkill1.version == originalSkill1.version
        copiedSkill1.displayOrder == originalSkill1.displayOrder
        copiedSkill1.totalPoints == originalSkill1.totalPoints
        copiedSkill1.pointIncrement == originalSkill1.pointIncrement
        copiedSkill1.pointIncrementInterval == originalSkill1.pointIncrementInterval
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.numPerformToCompletion == originalSkill1.numPerformToCompletion
        copiedSkill1.type == originalSkill1.type
        copiedSkill1.selfReportingType == originalSkill1.selfReportingType
        copiedSkill1.enabled == originalSkill1.enabled
        copiedSkill1.justificationRequired == originalSkill1.justificationRequired
        copiedSkill1.numMaxOccurrencesIncrementInterval == originalSkill1.numMaxOccurrencesIncrementInterval
        copiedSkill1.description == originalSkill1.description
        copiedSkill1.helpUrl == originalSkill1.helpUrl
        copiedSkill1.groupName == group.name
        copiedSkill1.groupId == group.skillId
        copiedSkill1.iconClass == originalSkill1.iconClass
        !copiedSkill1.readOnly
        !copiedSkill1.reusedSkill
        !copiedSkill1.thisSkillWasReusedElsewhere

        copiedSkill2.skillId == originalSkill2.skillId
        copiedSkill2.projectId == p2.projectId
        copiedSkill2.name == originalSkill2.name
        copiedSkill2.version == originalSkill2.version
        copiedSkill2.displayOrder == originalSkill2.displayOrder
        copiedSkill2.totalPoints == originalSkill2.totalPoints
        copiedSkill2.pointIncrement == originalSkill2.pointIncrement
        copiedSkill2.pointIncrementInterval == originalSkill2.pointIncrementInterval
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.numPerformToCompletion == originalSkill2.numPerformToCompletion
        copiedSkill2.type == originalSkill2.type
        copiedSkill2.selfReportingType == originalSkill2.selfReportingType
        copiedSkill2.enabled == originalSkill2.enabled
        copiedSkill2.justificationRequired == originalSkill2.justificationRequired
        copiedSkill2.numMaxOccurrencesIncrementInterval == originalSkill2.numMaxOccurrencesIncrementInterval
        copiedSkill2.description == originalSkill2.description
        copiedSkill2.helpUrl == originalSkill2.helpUrl
        copiedSkill2.groupName == group.name
        copiedSkill2.groupId == group.skillId
        copiedSkill2.iconClass == originalSkill2.iconClass
        !copiedSkill2.readOnly
        !copiedSkill2.reusedSkill
        !copiedSkill2.thisSkillWasReusedElsewhere

        copiedSkill3.skillId == originalSkill3.skillId
        copiedSkill3.projectId == p2.projectId
        copiedSkill3.name == originalSkill3.name
        copiedSkill3.version == originalSkill3.version
        copiedSkill3.displayOrder == originalSkill3.displayOrder
        copiedSkill3.totalPoints == originalSkill3.totalPoints
        copiedSkill3.pointIncrement == originalSkill3.pointIncrement
        copiedSkill3.pointIncrementInterval == originalSkill3.pointIncrementInterval
        copiedSkill3.numMaxOccurrencesIncrementInterval == originalSkill3.numMaxOccurrencesIncrementInterval
        copiedSkill3.numPerformToCompletion == originalSkill3.numPerformToCompletion
        copiedSkill3.type == originalSkill3.type
        copiedSkill3.selfReportingType == originalSkill3.selfReportingType
        copiedSkill3.enabled == originalSkill3.enabled
        copiedSkill3.justificationRequired == originalSkill3.justificationRequired
        copiedSkill3.numMaxOccurrencesIncrementInterval == originalSkill3.numMaxOccurrencesIncrementInterval
        copiedSkill3.description == originalSkill3.description
        copiedSkill3.helpUrl == originalSkill3.helpUrl
        copiedSkill3.groupName == group2.name
        copiedSkill3.groupId == group2.skillId
        copiedSkill3.iconClass == originalSkill3.iconClass
        !copiedSkill3.readOnly
        !copiedSkill3.reusedSkill
        !copiedSkill3.thisSkillWasReusedElsewhere
    }

    def "skill display order is copied"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.moveSkillDown(p1Skills[0])

        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSkills = skillsService.getSkillsForSubject(p2.projectId, p1subj1.subjectId).sort { it.displayOrder }

        then:
        originalSkills.skillId == [p1Skills[1].skillId, p1Skills[0].skillId, p1Skills[2].skillId]
        copiedSkills.skillId == [p1Skills[1].skillId, p1Skills[0].skillId, p1Skills[2].skillId]
        copiedSkills.displayOrder == [1, 2, 3]
    }

    def "copied skill version must be reset to 0"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].version = 0
        p1Skills[1].version = 1
        p1Skills[2].version = 2
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSkills = skillsService.getSkillsForSubject(p2.projectId, p1subj1.subjectId).sort { it.displayOrder }

        then:
        originalSkills.version == [0, 1, 2]
        copiedSkills.version == [0, 0, 0]
    }

    def "do not copy disabled imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])
        skillsService.bulkImportSkillsFromCatalog(p2.projectId, p2subj1.subjectId,
                [[projectId: p1.projectId, skillId: p1Skills[1].skillId]])

        def projToCopy = createProject(3)
        skillsService.createProject(projToCopy)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p2.projectId, p2subj1.subjectId, projToCopy.projectId)

        def origProjStats = skillsService.getProject(p2.projectId)
        def copiedProjStats = skillsService.getProject(projToCopy.projectId)

        def originalSubj = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def copiedSubj = skillsService.getSkillsForSubject(projToCopy.projectId, p2subj1.subjectId)

        def originalGroup = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def copiedGroup = skillsService.getSkillsForGroup(projToCopy.projectId, p2skillsGroup.skillId)

        then:
        origProjStats.numSkills == 2
        origProjStats.numSkillsDisabled == 2

        copiedProjStats.numSkills == 2
        copiedProjStats.numSkillsDisabled == 0

        originalSubj.skillId == [p2skillsGroup.skillId, p1Skills[1].skillId]
        copiedSubj.skillId == [p2skillsGroup.skillId]

        originalGroup.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        copiedGroup.skillId == [gSkill1.skillId, gSkill2.skillId]
    }

    def "do not copy finalized imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId,
                [[projectId: p1.projectId, skillId: p1Skills[1].skillId]])

        def projToCopy = createProject(3)
        skillsService.createProject(projToCopy)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p2.projectId, p2subj1.subjectId, projToCopy.projectId)

        def origProjStats = skillsService.getProject(p2.projectId)
        def copiedProjStats = skillsService.getProject(projToCopy.projectId)

        def originalSubj = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def copiedSubj = skillsService.getSkillsForSubject(projToCopy.projectId, p2subj1.subjectId)

        def originalGroup = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        def copiedGroup = skillsService.getSkillsForGroup(projToCopy.projectId, p2skillsGroup.skillId)

        then:
        origProjStats.numSkills == 4
        origProjStats.numSkillsDisabled == 0

        copiedProjStats.numSkills == 2
        copiedProjStats.numSkillsDisabled == 0

        originalSubj.skillId == [p2skillsGroup.skillId, p1Skills[1].skillId]
        copiedSubj.skillId == [p2skillsGroup.skillId]

        originalGroup.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        copiedGroup.skillId == [gSkill1.skillId, gSkill2.skillId]
    }

    def "do not copy reused skills"() {
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
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj2.subjectId, p2.projectId)

        def p1Subj1Summary = skillsService.getSkillSummary(skillsService.userName, p1.projectId, p1subj1.subjectId, -1, true)
        def p2Subj1Summary = skillsService.getSkillSummary(skillsService.userName, p2.projectId, p1subj1.subjectId, -1, true)

        def p1Subj2Summary = skillsService.getSkillSummary(skillsService.userName, p1.projectId, p1subj2.subjectId, -1, true)
        def p2Subj2Summary = skillsService.getSkillSummary(skillsService.userName, p2.projectId, p1subj2.subjectId, -1, true)
        then:
        (p2Subj1Summary.skills.skillId + p2Subj1Summary.skills.children.skillId.flatten()).sort() == (p1Skills.skillId + [group1.skillId]).sort()
        (p1Subj1Summary.skills.skillId + p1Subj1Summary.skills.children.skillId.flatten()).sort() == (p1Skills.skillId + [group1.skillId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)]).sort()

        (p1Subj2Summary.skills.skillId + p1Subj2Summary.skills.children.skillId.flatten()).sort() == [
                group3.skillId,
                SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0),
                SkillReuseIdUtil.addTag(p1Skills[7].skillId, 0),
        ].sort()
        (p2Subj2Summary.skills.skillId + p2Subj2Summary.skills.children.skillId.flatten()) == []
    }

    def "skill expiration attributes are copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        def skill2 = createSkill(1, 1, 23, 0, 13, 458, 55,)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [skill1, skill2])

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(p1.projectId, skill1.skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])
        skillsService.saveSkillExpirationAttributes(p1.projectId, skill2.skillId, [
                expirationType: ExpirationAttrs.DAILY,
                every: 10,
        ])

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def p1Skill1ExpirationAttrs = skillsService.getSkillExpirationAttributes(p1.projectId, skill1.skillId)
        def p2Skill1ExpirationAttrs = skillsService.getSkillExpirationAttributes(p2.projectId, skill1.skillId)

        def p1Skill2ExpirationAttrs = skillsService.getSkillExpirationAttributes(p1.projectId, skill2.skillId)
        def p2Skill2ExpirationAttrs = skillsService.getSkillExpirationAttributes(p2.projectId, skill2.skillId)

        then:
        p2Skill1ExpirationAttrs.expirationType == p1Skill1ExpirationAttrs.expirationType
        p2Skill1ExpirationAttrs.every == p1Skill1ExpirationAttrs.every
        p2Skill1ExpirationAttrs.monthlyDay == p1Skill1ExpirationAttrs.monthlyDay
        p2Skill1ExpirationAttrs.nextExpirationDate == p1Skill1ExpirationAttrs.nextExpirationDate

        p2Skill2ExpirationAttrs.expirationType == p1Skill2ExpirationAttrs.expirationType
        p2Skill2ExpirationAttrs.every == p1Skill2ExpirationAttrs.every
        p2Skill2ExpirationAttrs.monthlyDay == p1Skill2ExpirationAttrs.monthlyDay
        p2Skill2ExpirationAttrs.nextExpirationDate == p1Skill2ExpirationAttrs.nextExpirationDate
    }

    def "skill video attributes are copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 1, 512, 18,)
        def skill2 = createSkill(1, 1, 23, 0, 1, 458, 55,)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [skill1, skill2])

        skillsService.saveSkillVideoAttributes(p1.projectId, skill1.skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ])
        skill1.selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(skill1)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def p1Skill1VideoAttributes = skillsService.getSkillVideoAttributes(p1.projectId, skill1.skillId)
        def p2Skill1VideoAttributes = skillsService.getSkillVideoAttributes(p2.projectId, skill1.skillId)

        def p1Skill2VideoAttributes = skillsService.getSkillVideoAttributes(p1.projectId, skill2.skillId)
        def p2Skill2VideoAttributes = skillsService.getSkillVideoAttributes(p2.projectId, skill2.skillId)

        def p1Skill1 = skillsService.getSingleSkillSummary(skillsService.userName, p1.projectId, skill1.skillId)
        def p2Skill1 = skillsService.getSingleSkillSummary(skillsService.userName, p2.projectId, skill1.skillId)

        then:
        p2Skill1VideoAttributes.videoUrl == p1Skill1VideoAttributes.videoUrl
        p2Skill1VideoAttributes.videoType == p1Skill1VideoAttributes.videoType
        p2Skill1VideoAttributes.captions == p1Skill1VideoAttributes.captions
        p2Skill1VideoAttributes.transcript == p1Skill1VideoAttributes.transcript

        p1Skill1.videoSummary.videoUrl == "http://some.url"
        p1Skill1.videoSummary.hasCaptions == true
        p1Skill1.videoSummary.hasTranscript == true
        p1Skill1.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        p2Skill1.videoSummary.videoUrl == "http://some.url"
        p2Skill1.videoSummary.hasCaptions == true
        p2Skill1.videoSummary.hasTranscript == true
        p2Skill1.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        !p1Skill2VideoAttributes.videoUrl
        !p1Skill2VideoAttributes.videoType
        !p1Skill2VideoAttributes.captions
        !p1Skill2VideoAttributes.transcript

        !p2Skill2VideoAttributes.videoUrl
        !p2Skill2VideoAttributes.videoType
        !p2Skill2VideoAttributes.captions
        !p2Skill2VideoAttributes.transcript
    }

    def "copy a subject with skills that have internally hosted videos - videos should be copied"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video,
                transcript: "transcript",
                captions: "captions",
                width: 600,
                height: 400
        ])

        Resource video1 = new ClassPathResource("/testVideos/create-project.webm")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                file: video1,
                transcript: "transcript1",
                captions: "captions1",
                width: 601,
                height: 401
        ])

        List<Attachment> attachments_t1 = attachmentRepo.findAll()

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        List<Attachment> attachments_t2 = attachmentRepo.findAll()

        def p1Skill1VideoAttributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        def p1Skill2VideoAttributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)

        def p2Skill1VideoAttributes = skillsService.getSkillVideoAttributes(p2.projectId, p1Skills[0].skillId)
        def p2Skill2VideoAttributes = skillsService.getSkillVideoAttributes(p2.projectId, p1Skills[1].skillId)
        then:
        attachments_t1.size() == 2
        Attachment originalCreateQuizVideo = attachments_t1.find { it.filename == 'create-quiz.mp4' }
        Attachment originalCreateProjectVideo = attachments_t1.find { it.filename == 'create-project.webm' }

        originalCreateQuizVideo.projectId == p1.projectId
        originalCreateQuizVideo.skillId == p1Skills[0].skillId

        originalCreateProjectVideo.projectId == p1.projectId
        originalCreateProjectVideo.skillId == p1Skills[1].skillId

        attachments_t2.size() == 4
        Attachment originalCreateQuizVideo_t1 = attachments_t1.find { it.filename == 'create-quiz.mp4' && it.uuid == originalCreateQuizVideo.uuid }
        Attachment originalCreateProjectVideo_t1 = attachments_t1.find { it.filename == 'create-project.webm' && it.uuid == originalCreateProjectVideo.uuid }

        originalCreateQuizVideo_t1.projectId == p1.projectId
        originalCreateQuizVideo_t1.skillId == p1Skills[0].skillId

        originalCreateProjectVideo_t1.projectId == p1.projectId
        originalCreateProjectVideo_t1.skillId == p1Skills[1].skillId

        Attachment newCreateQuizVideo_t1 = attachments_t2.find { it.filename == 'create-quiz.mp4' && it.uuid != originalCreateQuizVideo.uuid }
        Attachment newCreateProjectVideo_t1 = attachments_t2.find { it.filename == 'create-project.webm' && it.uuid != originalCreateProjectVideo.uuid }

        newCreateQuizVideo_t1.projectId == p2.projectId
        newCreateQuizVideo_t1.skillId == p1Skills[0].skillId

        newCreateProjectVideo_t1.projectId == p2.projectId
        newCreateProjectVideo_t1.skillId == p1Skills[1].skillId

        p1Skill1VideoAttributes.videoUrl.toString().startsWith('/api/download/' + originalCreateQuizVideo.uuid)
        p1Skill1VideoAttributes.internallyHostedAttachmentUuid == originalCreateQuizVideo.uuid
        p1Skill2VideoAttributes.videoUrl.toString().startsWith('/api/download/' + originalCreateProjectVideo.uuid)
        p1Skill2VideoAttributes.internallyHostedAttachmentUuid == originalCreateProjectVideo.uuid

        p2Skill1VideoAttributes.videoUrl.toString().startsWith('/api/download/' + newCreateQuizVideo_t1.uuid)
        p2Skill1VideoAttributes.internallyHostedAttachmentUuid == newCreateQuizVideo_t1.uuid
        p2Skill2VideoAttributes.videoUrl.toString().startsWith('/api/download/' + newCreateProjectVideo_t1.uuid)
        p2Skill2VideoAttributes.internallyHostedAttachmentUuid == newCreateProjectVideo_t1.uuid

        p2Skill1VideoAttributes.transcript == p1Skill1VideoAttributes.transcript
        p2Skill1VideoAttributes.captions == p1Skill1VideoAttributes.captions
        p2Skill1VideoAttributes.width == p1Skill1VideoAttributes.width
        p2Skill1VideoAttributes.height == p1Skill1VideoAttributes.height
        p2Skill1VideoAttributes.isInternallyHosted == p1Skill1VideoAttributes.isInternallyHosted
        p2Skill1VideoAttributes.internallyHostedFileName == p1Skill1VideoAttributes.internallyHostedFileName
        p2Skill1VideoAttributes.videoType == p1Skill1VideoAttributes.videoType
    }

    def "copy a subject with skills that have externally hosted videos"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
                width: 600,
                height: 400
        ])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl: "http://some1.url",
                transcript: "transcript1",
                captions: "captions1",
                width: 601,
                height: 401
        ])

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def p1Skill1VideoAttributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        def p1Skill2VideoAttributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)

        def p2Skill1VideoAttributes = skillsService.getSkillVideoAttributes(p2.projectId, p1Skills[0].skillId)
        def p2Skill2VideoAttributes = skillsService.getSkillVideoAttributes(p2.projectId, p1Skills[1].skillId)
        then:
        attachmentRepo.findAll().size() == 0

        p1Skill1VideoAttributes.videoUrl.toString().startsWith('http://some.url')
        !p1Skill1VideoAttributes.internallyHostedAttachmentUuid
        p1Skill2VideoAttributes.videoUrl.toString().startsWith('http://some1.url')
        !p1Skill2VideoAttributes.internallyHostedAttachmentUuid

        p2Skill1VideoAttributes.videoUrl.toString().startsWith('http://some.url')
        !p2Skill1VideoAttributes.internallyHostedAttachmentUuid
        p2Skill2VideoAttributes.videoUrl.toString().startsWith('http://some1.url')
        !p2Skill2VideoAttributes.internallyHostedAttachmentUuid

        p2Skill1VideoAttributes.transcript == p1Skill1VideoAttributes.transcript
        p2Skill1VideoAttributes.captions == p1Skill1VideoAttributes.captions
        p2Skill1VideoAttributes.width == p1Skill1VideoAttributes.width
        p2Skill1VideoAttributes.height == p1Skill1VideoAttributes.height
        p2Skill1VideoAttributes.isInternallyHosted == p1Skill1VideoAttributes.isInternallyHosted
        p2Skill1VideoAttributes.internallyHostedFileName == p1Skill1VideoAttributes.internallyHostedFileName
        p2Skill1VideoAttributes.videoType == p1Skill1VideoAttributes.videoType
    }
}
