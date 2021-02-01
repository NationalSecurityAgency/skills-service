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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo

class SkillApprovalSpecs extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    void "reject approval requests"() {
        String user = "user0"

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
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        List approvalsEndpointRes = skillsService.getApprovals(proj.projectId)
        List<Integer> ids = approvalsEndpointRes.collect { it.id }
        skillsService.rejectSkillApprovals(proj.projectId, ids, 'Just felt like it')

        List<SkillApproval> approvalsAfterRejection = skillApprovalRepo.findAll()
        println approvalsAfterRejection

        List approvalsEndpointResAfter = skillsService.getApprovals(proj.projectId)
        def skillEvents = skillsService.getPerformedSkills(user, proj.projectId)



        then:
        !res.body.skillApplied
        approvalsEndpointRes.size() == 1
        !skillEvents.data
        approvalsEndpointResAfter.size() == 0

        approvalsAfterRejection.size() == 1
        approvalsAfterRejection.get(0).requestMsg == "Please approve this!"
        approvalsAfterRejection.get(0).rejectedOn.format("yyyy-MM-dd") == new Date().format("yyyy-MM-dd")
        approvalsAfterRejection.get(0).rejectionMsg == "Just felt like it"

        approvalsAfterRejection.get(0).projectId == proj.projectId
        approvalsAfterRejection.get(0).skillRefId == skillDefRepo.findAll().find({it.skillId == skills[0].skillId}).id
        approvalsAfterRejection.get(0).userId == "user0"
        approvalsAfterRejection.get(0).requestedOn == date
    }
}
