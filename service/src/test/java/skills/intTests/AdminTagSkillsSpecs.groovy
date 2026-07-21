/**
 * Copyright 2026 SkillTree
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
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.repos.SkillDefWithExtraRepo

class AdminTagSkillsSpecs extends DefaultIntSpec {

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    void "add tag to all skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"

        when:
        def res = skillsService.addTagToSkills(proj.projectId, skillIds, tagValue)
        List skillsAfterTagging = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List tagsForSkills = skillsService.getTagsForSkills(proj.projectId, skillIds)
        List tagsForProject = skillsService.getTagsForProject(proj.projectId)

        then:
        res
        res.success

        skillsAfterTagging
        skillsAfterTagging.size() == 4
        skillsAfterTagging.sort { it.skillId }
        skillsAfterTagging[0].tags && skillsAfterTagging[0].tags.size() == 1 && skillsAfterTagging[0].tags[0].tagValue == 'New Tag'
        skillsAfterTagging[1].tags && skillsAfterTagging[1].tags.size() == 1 && skillsAfterTagging[1].tags[0].tagValue == 'New Tag'
        skillsAfterTagging[2].tags && skillsAfterTagging[2].tags.size() == 1 && skillsAfterTagging[2].tags[0].tagValue == 'New Tag'
        skillsAfterTagging[3].tags && skillsAfterTagging[3].tags.size() == 1 && skillsAfterTagging[3].tags[0].tagValue == 'New Tag'

        tagsForProject && tagsForProject.size() == 1
        tagsForProject[0].tagValue == 'New Tag'
        tagsForProject[0].createdOn
        tagsForSkills
        tagsForSkills.size() == 1
        tagsForSkills[0].tagValue == 'New Tag'
    }

    void "retrieve tags for project ensures isolation between projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj = SkillsFactory.createSubject(1, 1)
        def proj1Skills = SkillsFactory.createSkills(4, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1Subj)
        skillsService.createSkills(proj1Skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2Subj = SkillsFactory.createSubject(2, 1)
        def proj2Skills = SkillsFactory.createSkills(4, 2, 1)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2Subj)
        skillsService.createSkills(proj2Skills)

        List<String> proj1SkillIds = proj1Skills.collect { it.skillId }
        List<String> proj2SkillIds = proj2Skills.collect { it.skillId }
        String proj1Tag1 = "Project 1 Tag 1"
        String proj1Tag2 = "Project 1 Tag 2"
        String proj2Tag1 = "Project 2 Tag 1"
        String proj2Tag2 = "Project 2 Tag 2"

        when:
        skillsService.addTagToSkills(proj1.projectId, [proj1SkillIds[0], proj1SkillIds[1]], proj1Tag1)
        skillsService.addTagToSkills(proj1.projectId, [proj1SkillIds[2], proj1SkillIds[3]], proj1Tag2)
        skillsService.addTagToSkills(proj2.projectId, [proj2SkillIds[0], proj2SkillIds[1]], proj2Tag1)
        skillsService.addTagToSkills(proj2.projectId, [proj2SkillIds[2], proj2SkillIds[3]], proj2Tag2)

        List tagsForProject1 = skillsService.getTagsForProject(proj1.projectId)
        List tagsForProject2 = skillsService.getTagsForProject(proj2.projectId)

        then:
        tagsForProject1.size() == 2
        tagsForProject1.find { it.tagValue == proj1Tag1 }
        tagsForProject1.find { it.tagValue == proj1Tag2 }
        !tagsForProject1.find { it.tagValue == proj2Tag1 }
        !tagsForProject1.find { it.tagValue == proj2Tag2 }

        tagsForProject2.size() == 2
        tagsForProject2.find { it.tagValue == proj2Tag1 }
        tagsForProject2.find { it.tagValue == proj2Tag2 }
        !tagsForProject2.find { it.tagValue == proj1Tag1 }
        !tagsForProject2.find { it.tagValue == proj1Tag2 }
    }

    void "retrieve tags for skills ensures isolation between projects"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj = SkillsFactory.createSubject(1, 1)
        def proj1Skills = SkillsFactory.createSkills(4, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1Subj)
        skillsService.createSkills(proj1Skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2Subj = SkillsFactory.createSubject(2, 1)
        def proj2Skills = SkillsFactory.createSkills(4, 2, 1)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2Subj)
        skillsService.createSkills(proj2Skills)

        List<String> proj1SkillIds = proj1Skills.collect { it.skillId }
        List<String> proj2SkillIds = proj2Skills.collect { it.skillId }
        String proj1Tag1 = "Project 1 Tag 1"
        String proj1Tag2 = "Project 1 Tag 2"
        String proj2Tag1 = "Project 2 Tag 1"
        String proj2Tag2 = "Project 2 Tag 2"

        when:
        skillsService.addTagToSkills(proj1.projectId, [proj1SkillIds[0], proj1SkillIds[1]], proj1Tag1)
        skillsService.addTagToSkills(proj1.projectId, [proj1SkillIds[2], proj1SkillIds[3]], proj1Tag2)
        skillsService.addTagToSkills(proj2.projectId, [proj2SkillIds[0], proj2SkillIds[1]], proj2Tag1)
        skillsService.addTagToSkills(proj2.projectId, [proj2SkillIds[2], proj2SkillIds[3]], proj2Tag2)

        List tagsForProject1Skills = skillsService.getTagsForSkills(proj1.projectId, proj1SkillIds)
        List tagsForProject2Skills = skillsService.getTagsForSkills(proj2.projectId, proj2SkillIds)

        then:
        tagsForProject1Skills.size() == 2
        tagsForProject1Skills.find { it.tagValue == proj1Tag1 }
        tagsForProject1Skills.find { it.tagValue == proj1Tag2 }
        !tagsForProject1Skills.find { it.tagValue == proj2Tag1 }
        !tagsForProject1Skills.find { it.tagValue == proj2Tag2 }

        tagsForProject2Skills.size() == 2
        tagsForProject2Skills.find { it.tagValue == proj2Tag1 }
        tagsForProject2Skills.find { it.tagValue == proj2Tag2 }
        !tagsForProject2Skills.find { it.tagValue == proj1Tag1 }
        !tagsForProject2Skills.find { it.tagValue == proj1Tag2 }
    }

    void "update tag value and verify it was updated"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagId1 = "tag1"
        String origTagValue = "Orig Tag"
        String newTagValue  = "New Value"

        String tagId2 = "tag2"
        String origTagValue1 = "Tag 2 Value"

        when:
        def res = skillsService.addTagToSkills(proj.projectId, skillIds, origTagValue, tagId1)
        def res2 = skillsService.addTagToSkills(proj.projectId, skillIds, origTagValue1, tagId2)
        List skillsRes = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List tagsForSkills = skillsService.getTagsForSkills(proj.projectId, skillIds)
        List tagsForProject = skillsService.getTagsForProject(proj.projectId)
        def res_t1 = skillsService.addTagToSkills(proj.projectId, [], newTagValue, tagId1)
        List skillsRes_t1 = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List tagsForSkills_t1 = skillsService.getTagsForSkills(proj.projectId, skillIds)
        List tagsForProject_t1 = skillsService.getTagsForProject(proj.projectId)

        then:
        res.success
        res2.success
        res_t1.success

        // before
        skillsRes[0].tags.findAll { it.tagId == tagId1 }.tagValue  == [origTagValue]
        skillsRes[1].tags.findAll { it.tagId == tagId1 }.tagValue  == [origTagValue]
        skillsRes[2].tags.findAll { it.tagId == tagId1 }.tagValue  == [origTagValue]
        skillsRes[3].tags.findAll { it.tagId == tagId1 }.tagValue  == [origTagValue]
        tagsForProject.findAll { it.tagId == tagId1 }.tagValue == [origTagValue]
        tagsForSkills.findAll { it.tagId == tagId1 }.tagValue == [origTagValue]

        skillsRes[0].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        skillsRes[1].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        skillsRes[2].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        skillsRes[3].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        tagsForProject.findAll { it.tagId == tagId2 }.tagValue == [origTagValue1]
        tagsForSkills.findAll { it.tagId == tagId2 }.tagValue == [origTagValue1]

        // after
        skillsRes_t1[0].tags.findAll { it.tagId == tagId1 }.tagValue  == [newTagValue]
        skillsRes_t1[1].tags.findAll { it.tagId == tagId1 }.tagValue  == [newTagValue]
        skillsRes_t1[2].tags.findAll { it.tagId == tagId1 }.tagValue  == [newTagValue]
        skillsRes_t1[3].tags.findAll { it.tagId == tagId1 }.tagValue  == [newTagValue]
        tagsForProject_t1.findAll { it.tagId == tagId1 }.tagValue == [newTagValue]
        tagsForSkills_t1.findAll { it.tagId == tagId1 }.tagValue == [newTagValue]

        skillsRes_t1[0].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        skillsRes_t1[1].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        skillsRes_t1[2].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        skillsRes_t1[3].tags.findAll { it.tagId == tagId2 }.tagValue  == [origTagValue1]
        tagsForProject_t1.findAll { it.tagId == tagId2 }.tagValue == [origTagValue1]
        tagsForSkills_t1.findAll { it.tagId == tagId2 }.tagValue == [origTagValue1]
    }

    void "add tag to some skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        List<String> taggedSkillIds = skillIds[1..2]
        String tagValue = "New Tag"

        when:
        def res = skillsService.addTagToSkills(proj.projectId, taggedSkillIds, tagValue)
        List skillsAfterTagging = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List tagsForSkills = skillsService.getTagsForSkills(proj.projectId, skillIds)
        List tagsForProject = skillsService.getTagsForProject(proj.projectId)

        def skillWithTag = skillsService.getSingleSkillSummary("user1", proj.projectId, skillIds[1])
        def skillWithSubjWithTag = skillsService.getSingleSkillSummaryWithSubject("user1", proj.projectId, subj.subjectId, skillIds[1])
        def skillWithoutTag = skillsService.getSingleSkillSummary("user1", proj.projectId, skillIds[0])
        def skillWithSubjWithoutTag = skillsService.getSingleSkillSummaryWithSubject("user1", proj.projectId, subj.subjectId, skillIds[0])

        then:
        res
        res.success

        skillsAfterTagging
        skillsAfterTagging.size() == 4
        skillsAfterTagging.sort { it.skillId }
        !skillsAfterTagging[0].tags
        skillsAfterTagging[1].tags && skillsAfterTagging[1].tags.size() == 1 && skillsAfterTagging[1].tags[0].tagValue == 'New Tag'
        skillsAfterTagging[2].tags && skillsAfterTagging[2].tags.size() == 1 && skillsAfterTagging[2].tags[0].tagValue == 'New Tag'
        !skillsAfterTagging[3].tags

        tagsForProject && tagsForProject.size() == 1 && tagsForProject[0].tagValue == 'New Tag'
        tagsForSkills && tagsForSkills.size() == 1 && tagsForSkills[0].tagValue == 'New Tag'

        skillWithTag.tags.tagValue == [tagValue]
        skillWithSubjWithTag.tags.tagValue == [tagValue]
        !skillWithoutTag.tags.tagValue
        !skillWithSubjWithoutTag.tags.tagValue
    }

    void "add multiple tags to some skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        List<String> tagged1SkillIds = skillIds[1..2]
        List<String> tagged2SkillIds = skillIds[1..3]
        String tagValue1 = "New Tag 1"
        String tagValue2 = "New Tag 2"

        when:
        def res1 = skillsService.addTagToSkills(proj.projectId, tagged1SkillIds, tagValue1)
        def res2 = skillsService.addTagToSkills(proj.projectId, tagged2SkillIds, tagValue2)
        List skillsAfterTagging = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List tagsForSkills = skillsService.getTagsForSkills(proj.projectId, skillIds)
        List tagsForProject = skillsService.getTagsForProject(proj.projectId)

        then:
        res1
        res1.success

        skillsAfterTagging
        skillsAfterTagging.size() == 4
        skillsAfterTagging.sort { it.skillId }
        !skillsAfterTagging[0].tags
        skillsAfterTagging[1].tags && skillsAfterTagging[1].tags.size() == 2 && skillsAfterTagging[1].tags.find { it.tagValue == tagValue1 } && skillsAfterTagging[1].tags.find { it.tagValue == tagValue2 }
        skillsAfterTagging[2].tags && skillsAfterTagging[2].tags.size() == 2 && skillsAfterTagging[2].tags.find { it.tagValue == tagValue1 } && skillsAfterTagging[2].tags.find { it.tagValue == tagValue2 }
        skillsAfterTagging[3].tags && skillsAfterTagging[3].tags.size() == 1 && skillsAfterTagging[3].tags[0].tagValue == tagValue2

        tagsForProject && tagsForProject.size() == 2 && tagsForProject.find { it.tagValue == tagValue1 } && tagsForSkills.find { it.tagValue == tagValue2 }
        tagsForProject.find { it.tagValue == tagValue1 }.numSkills == 2
        tagsForProject.find { it.tagValue == tagValue2 }.numSkills == 3
        tagsForSkills && tagsForSkills.size() == 2 && tagsForSkills.find { it.tagValue == tagValue1 } && tagsForSkills.find { it.tagValue == tagValue2 }
        tagsForSkills.find { it.tagValue == tagValue1 }.numSkills == 2
        tagsForSkills.find { it.tagValue == tagValue2 }.numSkills == 3
    }

    void "delete tag from some skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"
        String tagId = 'newtag'

        when:
        def res = skillsService.addTagToSkills(proj.projectId, skillIds, tagValue)

        skillsService.deleteTagForSkills(proj.projectId, [skillIds[0]], tagId)
        List skillsAfterDeletingOne = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        SkillDefWithExtra tagAfterDeletingOne = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(proj.projectId, tagId, SkillDef.ContainerType.Tag)

        then:
        res
        res.success

        !skillsAfterDeletingOne[0].tags
        skillsAfterDeletingOne[1].tags && skillsAfterDeletingOne[1].tags.size() == 1 && skillsAfterDeletingOne[1].tags[0].tagValue == 'New Tag'
        skillsAfterDeletingOne[2].tags && skillsAfterDeletingOne[2].tags.size() == 1 && skillsAfterDeletingOne[2].tags[0].tagValue == 'New Tag'
        skillsAfterDeletingOne[3].tags && skillsAfterDeletingOne[3].tags.size() == 1 && skillsAfterDeletingOne[3].tags[0].tagValue == 'New Tag'
        tagAfterDeletingOne
    }

    void "delete the last skill from a tag removes tag fully"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String tagValue = "New Tag"
        String tagId = 'newtag'

        when:
        def res = skillsService.addTagToSkills(proj.projectId, [skills[0].skillId], tagValue)

        skillsService.deleteTagForSkills(proj.projectId, [skills[0].skillId], tagId)
        List skillsAfterDeletingOne = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        SkillDefWithExtra tagAfterDeletingOne = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(proj.projectId, tagId, SkillDef.ContainerType.Tag)

        then:
        res
        res.success

        !skillsAfterDeletingOne[0].tags
        !tagAfterDeletingOne
    }

    void "delete the last skill from a tag keeps the tag when retainTag=true param is supplied"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String tagValue = "New Tag"
        String tagId = 'newtag'

        when:
        def res = skillsService.addTagToSkills(proj.projectId, [skills[0].skillId], tagValue)

        skillsService.deleteTagForSkills(proj.projectId, [skills[0].skillId], tagId, null, true)
        List skillsAfterDeletingOne = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        SkillDefWithExtra tagAfterDeletingOne = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(proj.projectId, tagId, SkillDef.ContainerType.Tag)

        then:
        res
        res.success

        !skillsAfterDeletingOne[0].tags
        tagAfterDeletingOne
    }

    void "delete tag fully"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"
        String tagId = 'newtag'

        when:
        def res = skillsService.addTagToSkills(proj.projectId, skillIds, tagValue)

        skillsService.deleteTagForSkills(proj.projectId, [], tagId, true)
        List skillsAfterDeletingOne = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        SkillDefWithExtra tagAfterDeletingOne = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(proj.projectId, tagId, SkillDef.ContainerType.Tag)

        then:
        res
        res.success

        !skillsAfterDeletingOne[0].tags
        !skillsAfterDeletingOne[1].tags
        !skillsAfterDeletingOne[2].tags
        !skillsAfterDeletingOne[3].tags
        !tagAfterDeletingOne
    }

    void "delete tag fully is not allowed if skill ids are provided"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"
        String tagId = 'newtag'
        skillsService.addTagToSkills(proj.projectId, skillIds, tagValue)
        when:
        skillsService.deleteTagForSkills(proj.projectId, skillIds, tagId, true)
        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("Skill ids must not be provided when removeTagFully parameter is true")
    }

    void "skill ids must be provided if removeTagFully==false"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"
        String tagId = 'newtag'
        skillsService.addTagToSkills(proj.projectId, skillIds, tagValue)
        when:
        skillsService.deleteTagForSkills(proj.projectId, [], tagId, false)
        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("skillsTagRequest.skillIds must contain at least 1 item")
    }

    void "when deleting removeTagFully==true and retainTag==true is not a valid request"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"
        String tagId = 'newtag'
        skillsService.addTagToSkills(proj.projectId, skillIds, tagValue)
        when:
        skillsService.deleteTagForSkills(proj.projectId, [], tagId, true, true)
        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("removeTagFully==true and retainTag==true is not a valid request")
    }

    void "delete tag from all skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"
        String tagId = 'newtag'

        when:
        def res = skillsService.addTagToSkills(proj.projectId, skillIds, tagValue)

        skillsService.deleteTagForSkills(proj.projectId, skillIds, tagId)
        List skillsAfterDeletingAll = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def tagAfterDeletingAll = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(proj.projectId, tagId, SkillDef.ContainerType.Tag)

        then:
        res
        res.success

        !skillsAfterDeletingAll[0].tags
        !skillsAfterDeletingAll[1].tags
        !skillsAfterDeletingAll[2].tags
        !skillsAfterDeletingAll[3].tags

        // verify the tag itself is removed since no other skills are tagged/associated with it
        !tagAfterDeletingAll
    }

    def "tag values cannot exceed maxSkillTagLength"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String invalidTagValue = (1..51).collect{"A"}.join()

        when:
        skillsService.addTagToSkills(proj.projectId, skillIds, invalidTagValue)

        then:
        def ex = thrown(SkillsClientException)
        ex.message.contains("[Tag Value] must not exceed [50]")
    }

    def "validate tag id's are case insensitive on tag creation and deletion"(){
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        String tagValue = "New Tag"
        String tagId = 'neWtaG'

        when:
        def res = skillsService.addTagToSkills(proj.projectId, skillIds, tagValue, tagId)
        List tagsForSkills = skillsService.getTagsForSkills(proj.projectId, skillIds)

        skillsService.deleteTagForSkills(proj.projectId, skillIds, tagId)
        List skillsAfterDeletingAll = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        def tagAfterDeletingAll = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(proj.projectId, tagId, SkillDef.ContainerType.Tag)

        then:
        res
        res.success
        tagsForSkills && tagsForSkills.size() == 1 && tagsForSkills[0].tagValue == 'New Tag' && tagsForSkills[0].tagId == tagId.toLowerCase()

        skillsAfterDeletingAll && skillsAfterDeletingAll.size() == 1 && !skillsAfterDeletingAll[0].tags

        // verify the tag itself is removed since no other skills are tagged/associated with it
        !tagAfterDeletingAll
    }

    void "add multiple tags to some skills, exclude disabled"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        skills[1].enabled = false
        skills[2].enabled = false

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> skillIds = skills.collect {it.skillId}
        List<String> tagged1SkillIds = skillIds[1..2]
        List<String> tagged2SkillIds = skillIds[1..3]
        String tagValue1 = "New Tag 1"
        String tagValue2 = "New Tag 2"

        when:
        def res1 = skillsService.addTagToSkills(proj.projectId, tagged1SkillIds, tagValue1)
        def res2 = skillsService.addTagToSkills(proj.projectId, tagged2SkillIds, tagValue2)
        List skillsAfterTagging = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List tagsForSkills = skillsService.getTagsForSkills(proj.projectId, skillIds)
        List tagsForProject = skillsService.getTagsForProject(proj.projectId, false)
        List tagsForProjectWithDisabled = skillsService.getTagsForProject(proj.projectId, true)

        then:
        res1
        res1.success

        skillsAfterTagging
        skillsAfterTagging.size() == 4
        skillsAfterTagging.sort { it.skillId }
        !skillsAfterTagging[0].tags
        skillsAfterTagging[1].tags && skillsAfterTagging[1].tags.size() == 2 && skillsAfterTagging[1].tags.find { it.tagValue == tagValue1 } && skillsAfterTagging[1].tags.find { it.tagValue == tagValue2 }
        skillsAfterTagging[2].tags && skillsAfterTagging[2].tags.size() == 2 && skillsAfterTagging[2].tags.find { it.tagValue == tagValue1 } && skillsAfterTagging[2].tags.find { it.tagValue == tagValue2 }
        skillsAfterTagging[3].tags && skillsAfterTagging[3].tags.size() == 1 && skillsAfterTagging[3].tags[0].tagValue == tagValue2

        tagsForProject.find { it.tagValue == tagValue1 }.numSkills == 0
        tagsForSkills.find { it.tagValue == tagValue2 }.numSkills == 3

        tagsForProjectWithDisabled.find { it.tagValue == tagValue1 }.numSkills == 2
        tagsForProjectWithDisabled.find { it.tagValue == tagValue2 }.numSkills == 3

        tagsForSkills && tagsForSkills.size() == 2 && tagsForSkills.find { it.tagValue == tagValue1 } && tagsForSkills.find { it.tagValue == tagValue2 }
    }

    def "get single tag info"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(8, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills[0..3])
        def proj1Subj1Group = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createSkill(proj1Subj1Group)
        proj1Subj1Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj1Subj1Group.skillId, it)
        }
        def proj1Subj2 = SkillsFactory.createSubject(1, 2)
        def proj1Subj2Skills = SkillsFactory.createSkills(8, 1, 2)
        skillsService.createProjectAndSubjectAndSkills(null, proj1Subj2, proj1Subj2Skills[0..3])
        def proj1Subj2Group = SkillsFactory.createSkillsGroup(1, 2, 12)
        skillsService.createSkill(proj1Subj2Group)
        proj1Subj2Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj1Subj2Group.skillId, it)
        }

        def proj2 = SkillsFactory.createProject(2)
        def proj2Subj1 = SkillsFactory.createSubject(2, 1)
        def proj2Subj1Skills = SkillsFactory.createSkills(8, 2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2Subj1, proj2Subj1Skills[0..3])
        def proj2Subj1Group = SkillsFactory.createSkillsGroup(2, 1, 11)
        skillsService.createSkill(proj2Subj1Group)
        proj2Subj1Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj2Subj1Group.skillId, it)
        }
        def proj2Subj2 = SkillsFactory.createSubject(2, 2)
        def proj2Subj2Skills = SkillsFactory.createSkills(8, 2, 2)
        skillsService.createProjectAndSubjectAndSkills(null, proj2Subj2, proj2Subj2Skills[0..3])
        def proj2Subj2Group = SkillsFactory.createSkillsGroup(2, 2, 12)
        skillsService.createSkill(proj2Subj2Group)
        proj2Subj2Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj2Subj2Group.skillId, it)
        }

        skillsService.addTagToSkills(proj2.projectId, [proj2Subj1Skills[0].skillId], tagValue)
        skillsService.addTagToSkills(proj2.projectId, [proj2Subj2Skills[0].skillId], tagValue)
        skillsService.addTagToSkills(proj2.projectId, [proj2Subj1Skills[4].skillId], tagValue)
        skillsService.addTagToSkills(proj2.projectId, [proj2Subj2Skills[4].skillId], tagValue)

        skillsService.addTagToSkills(proj1.projectId, [proj1Subj1Skills[0].skillId], tagValue)
        when:
        def res_t0 = skillsService.getTagInfo(proj1.projectId, tagId)
        skillsService.addTagToSkills(proj1.projectId, [proj1Subj2Skills[0].skillId], tagValue)
        def res_t1 = skillsService.getTagInfo(proj1.projectId, tagId)
        skillsService.addTagToSkills(proj1.projectId, [proj1Subj1Skills[4].skillId], tagValue)
        def res_t2 = skillsService.getTagInfo(proj1.projectId, tagId)
        skillsService.addTagToSkills(proj1.projectId, [proj1Subj2Skills[4].skillId], tagValue)
        def res_t3 = skillsService.getTagInfo(proj1.projectId, tagId)
        def proj2Res_t3 = skillsService.getTagInfo(proj2.projectId, tagId)

        then:
        res_t0.tagId == tagId
        res_t0.tagValue == tagValue
        res_t0.skills.size() == 1
        res_t0.skills[0].skillId == proj1Subj1Skills[0].skillId
        res_t0.skills[0].skillName == proj1Subj1Skills[0].name
        res_t0.skills[0].subjectName == proj1Subj1.name
        res_t0.skills[0].subjectId == proj1Subj1.subjectId
        !res_t0.skills[0].groupName
        !res_t0.skills[0].groupId
        res_t0.skills[0].taggedOn

        res_t1.tagId == tagId
        res_t1.tagValue == tagValue
        res_t1.skills.skillId == [proj1Subj2Skills[0].skillId, proj1Subj1Skills[0].skillId]
        res_t1.skills.skillName == [proj1Subj2Skills[0].name, proj1Subj1Skills[0].name]
        res_t1.skills.subjectName == [proj1Subj2.name, proj1Subj1.name]
        res_t1.skills.subjectId == [proj1Subj2.subjectId, proj1Subj1.subjectId]
        res_t1.skills.groupName == [null, null]
        res_t1.skills.groupId == [null, null]

        res_t2.tagId == tagId
        res_t2.tagValue == tagValue
        res_t2.skills.skillId == [proj1Subj1Skills[4].skillId, proj1Subj2Skills[0].skillId, proj1Subj1Skills[0].skillId]
        res_t2.skills.skillName == [proj1Subj1Skills[4].name, proj1Subj2Skills[0].name, proj1Subj1Skills[0].name]
        res_t2.skills.subjectName == [proj1Subj1.name, proj1Subj2.name, proj1Subj1.name]
        res_t2.skills.subjectId == [proj1Subj1.subjectId, proj1Subj2.subjectId, proj1Subj1.subjectId]
        res_t2.skills.groupName == [proj1Subj1Group.name, null, null]
        res_t2.skills.groupId == [proj1Subj1Group.skillId, null, null]

        res_t3.tagId == tagId
        res_t3.tagValue == tagValue
        res_t3.skills.skillId == [proj1Subj2Skills[4].skillId, proj1Subj1Skills[4].skillId, proj1Subj2Skills[0].skillId, proj1Subj1Skills[0].skillId]
        res_t3.skills.skillName == [proj1Subj2Skills[4].name, proj1Subj1Skills[4].name, proj1Subj2Skills[0].name, proj1Subj1Skills[0].name]
        res_t3.skills.subjectName == [proj1Subj2.name, proj1Subj1.name, proj1Subj2.name, proj1Subj1.name]
        res_t3.skills.subjectId == [proj1Subj2.subjectId, proj1Subj1.subjectId, proj1Subj2.subjectId, proj1Subj1.subjectId]
        res_t3.skills.groupName == [proj1Subj2Group.name, proj1Subj1Group.name, null, null]
        res_t3.skills.groupId == [proj1Subj2Group.skillId, proj1Subj1Group.skillId, null, null]

        proj2Res_t3.tagId == tagId
        proj2Res_t3.tagValue == tagValue
        proj2Res_t3.skills.skillId == [proj1Subj2Skills[4].skillId, proj1Subj1Skills[4].skillId, proj1Subj2Skills[0].skillId, proj1Subj1Skills[0].skillId]
        proj2Res_t3.skills.skillName == [proj1Subj2Skills[4].name, proj1Subj1Skills[4].name, proj1Subj2Skills[0].name, proj1Subj1Skills[0].name]
        proj2Res_t3.skills.subjectName == [proj1Subj2.name, proj1Subj1.name, proj1Subj2.name, proj1Subj1.name]
        proj2Res_t3.skills.subjectId == [proj1Subj2.subjectId, proj1Subj1.subjectId, proj1Subj2.subjectId, proj1Subj1.subjectId]
        proj2Res_t3.skills.groupName == [proj1Subj2Group.name, proj1Subj1Group.name, null, null]
        proj2Res_t3.skills.groupId == [proj1Subj2Group.skillId, proj1Subj1Group.skillId, null, null]

    }

    def "get single tag info - tags are retrieved for the correct projectId"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(8, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills[0..3])

        def proj2 = SkillsFactory.createProject(2)
        def proj2Subj1 = SkillsFactory.createSubject(2, 1)
        def proj2Subj1Skills = SkillsFactory.createSkills(8, 2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2Subj1, proj2Subj1Skills[0..3])

        skillsService.addTagToSkills(proj2.projectId, [proj2Subj1Skills[0].skillId], tagValue)
        def proj2Res = skillsService.getTagInfo(proj2.projectId, tagId)

        when:
        skillsService.getTagInfo(proj1.projectId, tagId)

        then:
        def ex = thrown(SkillsClientException)
        ex.toString().contains("Tag with id [${tagId}] does not exist.")

        proj2Res.tagId == tagId
        proj2Res.tagValue == tagValue
        proj2Res.skills.skillId == [proj2Subj1Skills[0].skillId]
    }
}