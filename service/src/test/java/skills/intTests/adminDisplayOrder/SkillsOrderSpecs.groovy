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
package skills.intTests.adminDisplayOrder

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class SkillsOrderSpecs extends DefaultIntSpec {
    def proj
    def subj
    List skills
    def setup(){
        proj = SkillsFactory.createProject()
        subj = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        int numSkills = 5
        skills = (1..numSkills).collect {
            def skill = SkillsFactory.createSkill(1, 1, it)
            skillsService.createSkill(skill)
            return skill
        }
    }

    def "move skill down"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillDown(skills.first())
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        beforeMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
        afterMove.collect({it.skillId}) == ["skill2", "skill1", "skill3", "skill4", "skill5"]
        afterMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
    }

    def "should not be able to move down the last skill"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillDown(skills.last())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
    }

    def "move skill up"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillUp(skills.get(1))
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        beforeMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
        afterMove.collect({it.skillId}) == ["skill2", "skill1", "skill3", "skill4", "skill5"]
        afterMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
    }

    def "should not be able to move the first skill up"() {
        when:
        def beforeMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.moveSkillUp(skills.first())
        then:
        thrown(SkillsClientException)
        beforeMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        def afterMove = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        afterMove.collect({it.skillId}) == ["skill1", "skill2", "skill3", "skill4", "skill5"]
        afterMove.collect({it.displayOrder}) == [1, 2, 3, 4, 5]
    }

    def "attempt to move skill that doesn't exist"(){
        when:
        skillsService.moveSkillUp(projectId: skills.first().projectId, subjectId: skills.first().subjectId, skillId: "doesntexist")
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Failed to find skillId")
    }

    def "display order is correct when skill is deleted from the middle"() {
        when:
        skillsService.deleteSkill(skills.get(2))
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4]
    }

    def "display order is correct when skill is deleted from the beginning"() {
        when:
        skillsService.deleteSkill(skills.get(0))
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4]
    }

    def "display order is correct when skill is deleted from the end"() {
        when:
        skillsService.deleteSkill(skills.get(4))
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4]
    }

    def "display order is correct when skill is added"() {
        when:
        def newSkill = SkillsFactory.createSkill(1, 1, 6)
        skillsService.createSkill(newSkill)
        then:
        skillsService.getSkillsForSubject(proj.projectId, subj.subjectId).collect({it.displayOrder}) == [1, 2, 3, 4, 5, 6]
    }

    def "display order is correct when skill is added and other skill is deleted"() {
        when:
        def newSkill = SkillsFactory.createSkill(1, 1, 6)
        skillsService.createSkill(newSkill)
        skillsService.deleteSkill(skills.get(0))
        skillsService.deleteSkill(skills.get(3))
        then:
        def modifiedSkills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        modifiedSkills.collect({it.displayOrder}) == [1, 2, 3, 4]
        modifiedSkills.collect({it.skillId}) == ['skill2', 'skill3', 'skill5', 'skill6']
    }


    def "loading the first skill sets nextSkillId but not prevSkillId"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 2, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def skill1 = skillsService.getSkill(proj1_skills[0])

        then:
        skill1.prevSkillId == null
        skill1.nextSkillId == 'skill2'
        skill1.orderInGroup == 1
        skill1.totalSkills == 3

    }

    def "loading a skill in the middle appropriately sets prev and next skill Ids"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 2, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSkill(proj1_skills[1])

        then:
        summary.prevSkillId == 'skill1'
        summary.nextSkillId == 'skill3'
        summary.orderInGroup == 2
        summary.totalSkills == 3
    }

    def "loading the last skill sets prevSkillId but not nextSkillId"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 2, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSkill(proj1_skills[2])

        then:
        summary.prevSkillId == 'skill2'
        summary.nextSkillId == null
        summary.orderInGroup == 3
        summary.totalSkills == 3
    }

    def "loading a skill with a broken next display works correctly"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 2, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillDefRepo.setSkillDisplayOrder(proj1_skills[1].skillId, 3)
        skillDefRepo.setSkillDisplayOrder(proj1_skills[2].skillId, 5)

        when:
        def firstSkill = skillsService.getSkill(proj1_skills[0])
        def secondSkill = skillsService.getSkill(proj1_skills[1])
        def lastSkill = skillsService.getSkill(proj1_skills[2])

        then:
        firstSkill.prevSkillId == null
        firstSkill.nextSkillId == 'skill2'
        firstSkill.orderInGroup == 1
        firstSkill.totalSkills == 3
        secondSkill.prevSkillId == 'skill1'
        secondSkill.nextSkillId == 'skill3'
        secondSkill.orderInGroup == 2
        secondSkill.totalSkills == 3
        lastSkill.prevSkillId == 'skill2'
        lastSkill.nextSkillId == null
        lastSkill.orderInGroup == 3
        lastSkill.totalSkills == 3
    }

    def "skills with groups - loading prev/last skillIds with broken displayOrder"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 2, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(2, 1, 22)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        skillsService.createSkills(proj1_skills[5..9])

        skillDefRepo.setSkillDisplayOrder(p1subj1g1.skillId, 10)
        proj1_skills[0..9].eachWithIndex { it, index ->
            def newIndex = (index + 1) * 2
            skillDefRepo.setSkillDisplayOrder(it.skillId, newIndex)
        }

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSkill(it)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills with groups - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 2, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(2, 1, 22)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        skillsService.createSkills(proj1_skills[5..9])

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSkill(it)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills with multiple groups (adjacent) - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 2, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(2, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(2, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(p1subj1g2)
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        proj1_skills[5..7].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }
        skillsService.createSkills(proj1_skills[8..9])

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSkill(it)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills with multiple groups (not adjacent) - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 2, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(2, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(2, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(proj1_skills[5])
        skillsService.createSkill(p1subj1g2)
        skillsService.createSkill(proj1_skills[9])
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        proj1_skills[6..8].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSkill(it)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills are all in groups - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 2, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(2, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(2, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(p1subj1g2)
        proj1_skills[0..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        proj1_skills[5..9].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSkill(it)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills organized correctly after display orders changed - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(2)
        def proj1_subj = SkillsFactory.createSubject(2, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 2, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(2, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(2, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(proj1_skills[5])
        skillsService.createSkill(p1subj1g2)
        skillsService.createSkill(proj1_skills[9])
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        proj1_skills[6..8].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }

        skillsService.moveSkillDown(proj1_skills[0])
        skillsService.moveSkillDown(proj1_skills[0])
        skillsService.moveSkillDown(proj1_skills[0])
        skillsService.moveSkillUp(proj1_skills[8])
        skillsService.moveSkillUp(proj1_skills[8])

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSkill(it)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                proj1_skills[4].skillId,
                null,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[0].skillId,
                proj1_skills[8].skillId,
                proj1_skills[6].skillId,
                proj1_skills[5].skillId,
                proj1_skills[7].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[5].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[0].skillId,
                proj1_skills[8].skillId,
                proj1_skills[7].skillId,
                proj1_skills[9].skillId,
                proj1_skills[6].skillId,
                null
        ]
        summaries.orderInGroup == [
                5,
                1,
                2,
                3,
                4,
                6,
                8,
                9,
                7,
                10
        ]
    }
}
