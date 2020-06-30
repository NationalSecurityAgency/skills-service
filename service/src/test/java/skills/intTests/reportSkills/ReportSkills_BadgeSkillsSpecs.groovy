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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class ReportSkills_BadgeSkillsSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    def setup(){
        skillsService.deleteProjectIfExist(projId)
    }

    def "give credit if all dependencies were fulfilled"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]


        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId]).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId]).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId]).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}
    }

    def "give credit if all dependencies were fulfilled, but the badge/gem is active"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Date tomorrow = new Date()+1
        Date twoWeeksAgo = new Date()-14
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: twoWeeksAgo, endDate: tomorrow]
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }


        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId]).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId]).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId]).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}
    }

    def "do not give credit if all dependencies were fulfilled, but the badge/gem is not active"(){
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1, dependentSkillsIds: [skill1.skillId, skill2.skillId, skill3.skillId]]

        Date oneWeekAgo = new Date()-7
        Date twoWeeksAgo = new Date()-14
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: twoWeeksAgo, endDate: oneWeekAgo]

        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId]).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId]).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId]).body

        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]
        requiredSkillsIds.each { String skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        resSkill4.skillApplied && !resSkill4.completed.find { it.id == 'badge1'}
    }

    def 'validate that if one gem date is provided both dates need to be provided - start provided'() {
        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: new Date()]
        skillsService.createBadge(badge)

        then:
        SkillsClientException e = thrown()
        e.message.contains("explanation:If one date is provided then both start and end dates must be provided")
        e.message.contains("errorCode:BadParam")
    }

    def 'validate that if one gem date is provided both dates need to be provided - end provided'() {
        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1', startDate: new Date()]
        skillsService.createBadge(badge)

        then:
        SkillsClientException e = thrown()
        e.message.contains("explanation:If one date is provided then both start and end dates must be provided")
        e.message.contains("errorCode:BadParam")
    }

    def 'badge not awarded if inactive'() {
        String subj = "testSubj"

        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        badge.enabled = false
        List<String> requiredSkillsIds = [skill1.skillId]


        when:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId]).body

        then:
        resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
    }

    def "badge awarded to users with requirements after enabling"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(20)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        badge.enabled = false
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date())
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date())

        when:
        def user1SummaryBeforeEnable = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)
        skillsService.updateBadge([projectId: proj.projectId, badgeId: badge.badgeId, enabled: true, name: badge.name], badge.badgeId)

        def user1Summary = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)
        def user2Summary = skillsService.getBadgeSummary("user2", proj.projectId, badge.badgeId)

        then:
        !user1SummaryBeforeEnable.badgeAchieved
        user1Summary.badgeAchieved
        !user2Summary.badgeAchieved
    }

    def "gem awarded to users with requirements after enabling"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(20)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date twoWeeksAgo = new Date() - 14
        Date nextWeek = new Date() + 7

        badge.enabled = false
        badge.startDate = twoWeeksAgo
        badge.endDate = nextWeek

        //add start/end dates
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(1).skillId])

        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user1", new Date())
        skillsService.addSkill([skillId: skills.get(0).skillId, projectId: proj.projectId], "user2", new Date()-60)
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user1", new Date()-7)
        skillsService.addSkill([skillId: skills.get(1).skillId, projectId: proj.projectId], "user2", new Date()-35)

        when:
        def user1SummaryBeforeEnable = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)

        skillsService.updateBadge([projectId: proj.projectId,
                                   badgeId: badge.badgeId,
                                   enabled: true,
                                   name: badge.name,
                                   startDate: twoWeeksAgo,
                                   endDate: nextWeek], badge.badgeId)

        def user1Summary = skillsService.getBadgeSummary("user1", proj.projectId, badge.badgeId)
        def user2Summary = skillsService.getBadgeSummary("user2", proj.projectId, badge.badgeId)

        then:
        !user1SummaryBeforeEnable.badgeAchieved
        user1Summary.badgeAchieved
        !user2Summary.badgeAchieved
    }

    def "changes to skill occurrence causes badge to be awarded"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 3, 90, 100)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 1, 0, 100)

        skillsService.createSkills([skill1, skill2])

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill2.skillId])

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], "u123", new Date())

        when:
        //get history for user123 and assert that badge is not awarded
        def u123SummaryBeforeEdit = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)


        skillsService.updateSkill([projectId: proj1.projectId,
                                   subjectId: subj.subjectId,
                                   skillId: skill1.skillId,
                                   numPerformToCompletion: 1,
                                   pointIncrement: skill1.pointIncrement,
                                   pointIncrementInterval: skill1.pointIncrementInterval,
                                   numMaxOccurrencesIncrementInterval: skill1.numMaxOccurrencesIncrementInterval,
                                   version: skill1.version,
                                   name: skill1.name], skill1.skillId)

        def u123SummaryAfterEditOccurrences = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)

        then:
        !u123SummaryBeforeEdit.badgeAchieved
        u123SummaryAfterEditOccurrences.badgeAchieved
    }

    def "deletion of a skill causes badge to be awarded"() {
        def proj1 = SkillsFactory.createProject(1)
        skillsService.createProject(proj1)
        def subj = SkillsFactory.createSubject(1)
        skillsService.createSubject(subj)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 0, 1, 90, 100)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 1, 0, 100)

        skillsService.createSkills([skill1, skill2])

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill2.skillId])

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], "u123", new Date())

        when:
        //get history for user123 and assert that badge is not awarded
        def u123SummaryBeforeEdit = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)

        skillsService.deleteSkill([projectId: proj1.projectId, subjectId: subj.subjectId, skillId: skill2.skillId])

        def u123SummaryAfterSkillDeletion = skillsService.getBadgeSummary("u123", proj1.projectId, badge.badgeId)

        then:
        !u123SummaryBeforeEdit.badgeAchieved
        u123SummaryAfterSkillDeletion.badgeAchieved
    }
}
