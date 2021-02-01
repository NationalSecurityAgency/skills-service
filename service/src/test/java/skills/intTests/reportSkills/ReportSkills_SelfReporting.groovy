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
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo

@Slf4j
class ReportSkills_SelfReporting extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillDefRepo skillDefRepo

    def "self report skill with approval"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user0", date, "Please approve this!")
        then:
        !res.body.skillApplied
        res.body.pointsEarned == 0
        res.body.explanation == "Skill was submitted for approval"
        !res.body.completed
        res.body.skillId == skills[0].skillId

        List<SkillApproval> approvals = skillApprovalRepo.findAll().collect {it}
        approvals.size() == 1

        approvals.get(0).requestMsg == "Please approve this!"
        !approvals.get(0).rejectedOn
        !approvals.get(0).rejectionMsg

        approvals.get(0).projectId == proj.projectId
        approvals.get(0).skillRefId == skillDefRepo.findAll().find({it.skillId == skills[0].skillId}).id
        approvals.get(0).userId == "user0"
        approvals.get(0).requestedOn == date
    }

    def "self report skill with honor system"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user0", date, "Please approve this!")
        then:
        res.body.skillApplied
        res.body.pointsEarned == 200
        res.body.explanation == "Skill event was applied"

        !skillApprovalRepo.findAll().collect {it}
    }

}
