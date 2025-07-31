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
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.SkillDef

import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

class CopySkillsToAnotherProjGroupAttributesSpecs extends CopyIntSpec {

    def "skill attributes are properly copied - skills were under a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        skill1.description = "blah blah blah"
        skill1.helpUrl = "/ok/that/is/good"
        skill1.selfReportingType = SkillDef.SelfReportingType.Approval
        skill1.justificationRequired = true
        skill1.iconClass = "fa fa-icon-test"

        def skill2 = createSkill(1, 1, 23, 0, 13, 458, 55,)
        skill2.description = "something else"
        skill2.helpUrl = "http://www.djleaje.org"
        skill2.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill2.justificationRequired = false
        skill2.iconClass = "fa fa-icon-test"
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [skill1, skill2])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, [skill1.skillId, skill2.skillId], p2.projectId, p2subj1.subjectId, destGroup.skillId)

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
        copiedSkill1.groupName == destGroup.name
        copiedSkill1.groupId == destGroup.skillId
        copiedSkill1.iconClass == 'fa fa-icon-test'
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
        copiedSkill2.groupName == destGroup.name
        copiedSkill2.groupId == destGroup.skillId
        copiedSkill2.iconClass == 'fa fa-icon-test'
        copiedSkill2.iconClass == originalSkill2.iconClass
        !copiedSkill2.readOnly
        !copiedSkill2.reusedSkill
        !copiedSkill2.thisSkillWasReusedElsewhere
    }

    def "skill attributes are properly copied - skills were under a group"() {
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
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, [skill1.skillId, skill2.skillId, skill3.skillId], p2.projectId, p2subj1.subjectId, destGroup.skillId)

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
        copiedSkill1.displayOrder == 1
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
        copiedSkill1.groupName == destGroup.name
        copiedSkill1.groupId == destGroup.skillId
        copiedSkill1.iconClass == 'fa fa-icon-test'
        copiedSkill1.iconClass == originalSkill1.iconClass
        !copiedSkill1.readOnly
        !copiedSkill1.reusedSkill
        !copiedSkill1.thisSkillWasReusedElsewhere

        copiedSkill2.skillId == originalSkill2.skillId
        copiedSkill2.projectId == p2.projectId
        copiedSkill2.name == originalSkill2.name
        copiedSkill2.version == originalSkill2.version
        copiedSkill2.displayOrder == 2
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
        copiedSkill2.groupName == destGroup.name
        copiedSkill2.groupId == destGroup.skillId
        copiedSkill2.iconClass == 'fa fa-icon-test'
        copiedSkill2.iconClass == originalSkill2.iconClass
        !copiedSkill2.readOnly
        !copiedSkill2.reusedSkill
        !copiedSkill2.thisSkillWasReusedElsewhere

        copiedSkill3.skillId == originalSkill3.skillId
        copiedSkill3.projectId == p2.projectId
        copiedSkill3.name == originalSkill3.name
        copiedSkill3.version == originalSkill3.version
        copiedSkill3.displayOrder == 3
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
        copiedSkill3.groupName == destGroup.name
        copiedSkill3.groupId == destGroup.skillId
        copiedSkill3.iconClass == 'fa fa-icon-test'
        copiedSkill3.iconClass == originalSkill3.iconClass
        !copiedSkill3.readOnly
        !copiedSkill3.reusedSkill
        !copiedSkill3.thisSkillWasReusedElsewhere
    }

    def "skill display order is handled - from same subject"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.moveSkillDown(p1Skills[0])

        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, [p1Skills[1].skillId, p1Skills[0].skillId, p1Skills[2].skillId], p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def copiedSkills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId).sort { it.displayOrder }

        then:
        originalSkills.skillId == [p1Skills[1].skillId, p1Skills[0].skillId, p1Skills[2].skillId]
        copiedSkills.skillId == [p1Skills[1].skillId, p1Skills[0].skillId, p1Skills[2].skillId]
        copiedSkills.displayOrder == [1, 2, 3]
    }

    def "skill display order is handled - already has existing skills"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])
        def p2Skills = createSkills(10, 2, 2, 100)
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Skills[5])
        skillsService.assignSkillToSkillsGroup(destGroup.skillId, p2Skills[6])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def copiedSkills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId).sort { it.displayOrder }

        then:
        copiedSkills.skillId == [p2Skills[5].skillId, p2Skills[6].skillId, p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId]
        copiedSkills.displayOrder == [1, 2, 3, 4, 5]
    }

    def "skill display order is handled - from multiple subjects"() {
        def p1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.moveSkillDown(p1Skills[0])

        def p1subj2 = createSubject(1, 2)
        def p1SkillsSubj2 = createSkills(2, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1SkillsSubj2)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        List<String> skillIdsToCopy = p1Skills.collect { it.skillId as String } + p1SkillsSubj2.collect { it.skillId as String }
        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, skillIdsToCopy, p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def copiedSkills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId).sort { it.displayOrder }

        then:
        copiedSkills.skillId == skillIdsToCopy
        copiedSkills.displayOrder == [1, 2, 3, 4 , 5]
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
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, p1Skills.collect { it.skillId as String }, p2.projectId, p2subj1.subjectId, destGroup.skillId)

        def copiedSkills = skillsService.getSkillsForGroup(p2.projectId, destGroup.skillId).sort { it.displayOrder }

        then:
        originalSkills.version == [0, 1, 2]
        copiedSkills.version == [0, 0, 0]
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
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, [skill1.skillId, skill2.skillId], p2.projectId, p2subj1.subjectId, destGroup.skillId)

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
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 6)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        skillsService.copySkillDefsIntoAnotherProjectSkillGroup(p1.projectId, [skill1.skillId, skill2.skillId], p2.projectId, p2subj1.subjectId, destGroup.skillId)

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

}
