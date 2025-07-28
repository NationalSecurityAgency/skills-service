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
package skills.intTests.copyProject

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class CopyProjectsSkillsSpecs extends DefaultIntSpec {

    def "skill attributes are properly copied - skill under a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill1 = createSkill(1, 1, 22, 0, 12, 512, 18,)
        skill1.description = "blah blah blah"
        skill1.helpUrl = "/ok/that/is/good"
        skill1.selfReportingType = SkillDef.SelfReportingType.Approval
        skill1.justificationRequired = true
        skill1.iconClass ='fa fa-icon-test'

        def skill2 = createSkill(1, 1, 23, 0, 13, 458, 55,)
        skill2.description = "something else"
        skill2.helpUrl = "http://www.djleaje.org"
        skill2.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skill2.justificationRequired = false
        skill2.iconClass = 'fa fa-icon-test'
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [skill1, skill2])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def originalSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def originalSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])

        def copiedSkill1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def copiedSkill2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        then:
        copiedSkill1.skillId == originalSkill1.skillId
        copiedSkill1.projectId == projToCopy.projectId
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
        copiedSkill1.iconClass == originalSkill1.iconClass
        copiedSkill1.iconClass == 'fa fa-icon-test'
        !copiedSkill1.groupName
        !copiedSkill1.groupId
        !copiedSkill1.readOnly
        !copiedSkill1.reusedSkill
        !copiedSkill1.thisSkillWasReusedElsewhere

        copiedSkill2.skillId == originalSkill2.skillId
        copiedSkill2.projectId == projToCopy.projectId
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
        copiedSkill2.iconClass == originalSkill2.iconClass
        copiedSkill2.iconClass == 'fa fa-icon-test'
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


        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def originalSkill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def originalSkill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        def originalSkill3 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: skill3.skillId])

        def copiedSkill1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill1.skillId])
        def copiedSkill2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill2.skillId])
        def copiedSkill3 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: skill3.skillId])
        then:
        copiedSkill1.skillId == originalSkill1.skillId
        copiedSkill1.projectId == projToCopy.projectId
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
        copiedSkill1.iconClass == 'fa fa-icon-test'
        !copiedSkill1.readOnly
        !copiedSkill1.reusedSkill
        !copiedSkill1.thisSkillWasReusedElsewhere

        copiedSkill2.skillId == originalSkill2.skillId
        copiedSkill2.projectId == projToCopy.projectId
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
        copiedSkill2.iconClass == 'fa fa-icon-test'
        !copiedSkill2.readOnly
        !copiedSkill2.reusedSkill
        !copiedSkill2.thisSkillWasReusedElsewhere

        copiedSkill3.skillId == originalSkill3.skillId
        copiedSkill3.projectId == projToCopy.projectId
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
        copiedSkill3.iconClass == 'fa fa-icon-test'
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

        when:
        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSkills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj1.subjectId).sort { it.displayOrder }

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

        when:
        def originalSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId).sort { it.displayOrder }
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedSkills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj1.subjectId).sort { it.displayOrder }

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

        when:
        def projToCopy = createProject(3)
        skillsService.copyProject(p2.projectId, projToCopy)

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

        when:
        def projToCopy = createProject(3)
        skillsService.copyProject(p2.projectId, projToCopy)

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

    def "validate reused skills were copied"() {
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

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)

        def copiedGroup1Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group1.skillId)
        def originalReusedSkillInGroup1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def copiedReusedSkillInGroup1 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        def copiedSubj2Skills = skillsService.getSkillsForSubject(projToCopy.projectId, p1subj2.subjectId)
        def copiedGroup3Skills = skillsService.getSkillsForGroup(projToCopy.projectId, group3.skillId)

        def originalReusedSkillInSubj2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def copiedReusedSkillInSubj2 = skillsService.getSkill([projectId: projToCopy.projectId, subjectId: p1subj1.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)])

        println JsonOutput.prettyPrint(JsonOutput.toJson(copiedReusedSkillInGroup1))
        then:
        copiedGroup1Skills.skillId == [p1Skills[4..9].skillId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)].flatten()

        copiedReusedSkillInGroup1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        copiedReusedSkillInGroup1.name == p1Skills[0].name
        copiedReusedSkillInGroup1.reusedSkill == true
        copiedReusedSkillInGroup1.groupId == group1.skillId
        copiedReusedSkillInGroup1.pointIncrement == originalReusedSkillInGroup1.pointIncrement
        copiedReusedSkillInGroup1.pointIncrementInterval == originalReusedSkillInGroup1.pointIncrementInterval
        copiedReusedSkillInGroup1.numMaxOccurrencesIncrementInterval == originalReusedSkillInGroup1.numMaxOccurrencesIncrementInterval
        copiedReusedSkillInGroup1.numPerformToCompletion == originalReusedSkillInGroup1.numPerformToCompletion
        copiedReusedSkillInGroup1.type == originalReusedSkillInGroup1.type
        copiedReusedSkillInGroup1.enabled == true
        copiedReusedSkillInGroup1.description == originalReusedSkillInGroup1.description
        copiedReusedSkillInGroup1.helpUrl == originalReusedSkillInGroup1.helpUrl
        copiedReusedSkillInGroup1.selfReportingType == originalReusedSkillInGroup1.selfReportingType
        copiedReusedSkillInGroup1.justificationRequired == originalReusedSkillInGroup1.justificationRequired

        copiedSubj2Skills.skillId == [group3.skillId, SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)]
        copiedGroup3Skills.skillId == [SkillReuseIdUtil.addTag(p1Skills[7].skillId, 0)]

        copiedReusedSkillInSubj2.skillId == SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0)
        copiedReusedSkillInSubj2.name == p1Skills[1].name
        copiedReusedSkillInSubj2.reusedSkill == true
        !copiedReusedSkillInSubj2.groupId
        copiedReusedSkillInSubj2.pointIncrement == originalReusedSkillInSubj2.pointIncrement
        copiedReusedSkillInSubj2.pointIncrementInterval == originalReusedSkillInSubj2.pointIncrementInterval
        copiedReusedSkillInSubj2.numMaxOccurrencesIncrementInterval == originalReusedSkillInSubj2.numMaxOccurrencesIncrementInterval
        copiedReusedSkillInSubj2.numPerformToCompletion == originalReusedSkillInSubj2.numPerformToCompletion
        copiedReusedSkillInSubj2.type == originalReusedSkillInSubj2.type
        copiedReusedSkillInSubj2.enabled == true
        copiedReusedSkillInSubj2.description == originalReusedSkillInSubj2.description
        copiedReusedSkillInSubj2.helpUrl == originalReusedSkillInSubj2.helpUrl
        copiedReusedSkillInSubj2.selfReportingType == originalReusedSkillInSubj2.selfReportingType
        copiedReusedSkillInSubj2.justificationRequired == originalReusedSkillInSubj2.justificationRequired
    }

}