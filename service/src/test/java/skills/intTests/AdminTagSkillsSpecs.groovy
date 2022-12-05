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

        tagsForProject && tagsForProject.size() == 1 && tagsForProject[0].tagValue == 'New Tag'
        tagsForSkills && tagsForSkills.size() == 1 && tagsForSkills[0].tagValue == 'New Tag'
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
        tagsForSkills && tagsForSkills.size() == 2 && tagsForSkills.find { it.tagValue == tagValue1 } && tagsForSkills.find { it.tagValue == tagValue2 }
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

        // verify the tag itself is removed since now other skills are tagged/associated with it
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
}
