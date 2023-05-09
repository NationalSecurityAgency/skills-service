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
package skills.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.controller.result.model.TableResult
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.ProjectError
import skills.storage.repos.ProjectErrorRepo

class ProjectErrorServiceSpec extends DefaultIntSpec {

    String projectPointsSetting = "level.points.enabled"

    @Autowired
    ProjectErrorService projectErrorService

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ProjectErrorRepo projectErrorRepo

    def "generates project issue if project level is unachievable"() {
        def proj = SkillsFactory.createProject(5)
        def subject = SkillsFactory.createSubject(5, 1)
        def skill = SkillsFactory.createSkill(5, 1, 1)
        skill.pointIncrement = 500

        def proj2 = SkillsFactory.createProject(6)
        def subject1 = SkillsFactory.createSubject(6, 1)
        subject1.name = "SubjectName"
        def p2s1Skill = SkillsFactory.createSkill(6, 1)
        p2s1Skill.pointIncrement = 100
        def subject2 = SkillsFactory.createSubject(6, 2)
        def p2s2Skill = SkillsFactory.createSkill(6, 2)
        p2s2Skill.pointIncrement = 100

        def proj3 = SkillsFactory.createProject(7)
        def p3sub1 = SkillsFactory.createSubject(7, 1)
        def p3sub1Skill = SkillsFactory.createSkill(7, 1)
        p3sub1Skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        skillsService.createProject(proj2)
        skillsService.createSubject(subject1)
        skillsService.createSkill(p2s1Skill)
        skillsService.createSubject(subject2)
        skillsService.createSkill(p2s2Skill)

        skillsService.createProject(proj3)
        skillsService.createSubject(p3sub1)
        skillsService.createSkill(p3sub1Skill)

        skillsService.changeSetting(proj.projectId, projectPointsSetting, [projectId: proj.projectId, setting: projectPointsSetting, value: "true"])
        def props = [:]
        props.projectId = proj.projectId
        props.level = 5
        props.pointsFrom = 50000
        props.pointsTo = null
        skillsService.editLevel(proj.projectId, null, "${props.level}", props)

        skillsService.changeSetting(proj2.projectId, projectPointsSetting, [projectId: proj2.projectId, setting: projectPointsSetting, value: "true"])
        props = [:]
        props.projectId = proj2.projectId
        props.level = 5
        props.pointsFrom = 50000
        props.pointsTo = null
        skillsService.editLevel(proj2.projectId, subject1.subjectId, "${props.level}", props)

        when:
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        transactionTemplate.execute() {
            projectErrorService.generateIssuesForUnachievableLevels()
        }

        Iterable<ProjectError> allErrs = projectErrorRepo.findAll()

        then:
        allErrs.size() == 2
        allErrs[0].projectId == proj.projectId
        allErrs[0].count == 1
        allErrs[0].errorType == ProjectError.ErrorType.UnachievableProjectLevel
        allErrs[0].error == "Level 5 cannot be achieved based on the points available in the project"

        allErrs[1].projectId == proj2.projectId
        allErrs[1].count == 1
        allErrs[1].errorType == ProjectError.ErrorType.UnachievableSubjectLevel
        allErrs[1].error == "Level 5 in SubjectName cannot be achieved based on the points available in the subject"
    }

    def "unachievable levels are not generated for percent based project"() {
        def proj = SkillsFactory.createProject(5)
        def subject = SkillsFactory.createSubject(5, 1)
        def skill = SkillsFactory.createSkill(5, 1, 1)
        skill.pointIncrement = 1000
        skillsService.createProjectAndSubjectAndSkills(proj, subject, [skill])

        skillsService.changeSetting(proj.projectId, projectPointsSetting, [projectId: proj.projectId, setting: projectPointsSetting, value: "true"])
        skill.pointIncrement = 100
        skillsService.createSkill(skill)

        skillsService.changeSetting(proj.projectId, projectPointsSetting, [projectId: proj.projectId, setting: projectPointsSetting, value: "false"])

        when:
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        transactionTemplate.execute() {
            projectErrorService.generateIssuesForUnachievableLevels()
        }
        then:
        TableResult tableResult = projectErrorService.getAllErrorsForProject(proj.projectId, PageRequest.of(0, 10))
        tableResult.count == 0
        !tableResult.data
    }

    def "existing unachievable level issues are updated as opposed to being recreated"() {
        def proj = SkillsFactory.createProject(5)
        def subject = SkillsFactory.createSubject(5, 1)
        def skill = SkillsFactory.createSkill(5, 1, 1)
        skill.pointIncrement = 500

        def proj2 = SkillsFactory.createProject(6)
        def subject1 = SkillsFactory.createSubject(6, 1)
        subject1.name = "SubjectName"
        def p2s1Skill = SkillsFactory.createSkill(6, 1)
        p2s1Skill.pointIncrement = 100
        def subject2 = SkillsFactory.createSubject(6, 2)
        def p2s2Skill = SkillsFactory.createSkill(6, 2)
        p2s2Skill.pointIncrement = 100

        def proj3 = SkillsFactory.createProject(7)
        def p3sub1 = SkillsFactory.createSubject(7, 1)
        def p3sub1Skill = SkillsFactory.createSkill(7, 1)
        p3sub1Skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)

        skillsService.createProject(proj2)
        skillsService.createSubject(subject1)
        skillsService.createSkill(p2s1Skill)
        skillsService.createSubject(subject2)
        skillsService.createSkill(p2s2Skill)

        skillsService.createProject(proj3)
        skillsService.createSubject(p3sub1)
        skillsService.createSkill(p3sub1Skill)

        skillsService.changeSetting(proj.projectId, projectPointsSetting, [projectId: proj.projectId, setting: projectPointsSetting, value: "true"])
        def props = [:]
        props.projectId = proj.projectId
        props.level = 5
        props.pointsFrom = 50000
        props.pointsTo = null
        skillsService.editLevel(proj.projectId, null, "${props.level}", props)

        skillsService.changeSetting(proj2.projectId, projectPointsSetting, [projectId: proj2.projectId, setting: projectPointsSetting, value: "true"])
        props = [:]
        props.projectId = proj2.projectId
        props.level = 5
        props.pointsFrom = 50000
        props.pointsTo = null
        skillsService.editLevel(proj2.projectId, subject1.subjectId, "${props.level}", props)

        when:
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        transactionTemplate.execute() {
            projectErrorService.generateIssuesForUnachievableLevels()
        }

        transactionTemplate.execute() {
            projectErrorService.generateIssuesForUnachievableLevels()
        }

        Iterable<ProjectError> allErrs = projectErrorRepo.findAll()

        then:
        allErrs.size() == 2
        allErrs[0].projectId == proj.projectId
        allErrs[0].count == 2
        allErrs[0].created < allErrs[0].lastSeen
        allErrs[0].errorType == ProjectError.ErrorType.UnachievableProjectLevel
        allErrs[0].error == "Level 5 cannot be achieved based on the points available in the project"

        allErrs[1].projectId == proj2.projectId
        allErrs[1].count == 2
        allErrs[1].created < allErrs[1].lastSeen
        allErrs[1].errorType == ProjectError.ErrorType.UnachievableSubjectLevel
        allErrs[1].error == "Level 5 in SubjectName cannot be achieved based on the points available in the subject"
    }
}
