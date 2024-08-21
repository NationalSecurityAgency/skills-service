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
package skills.intTests.attachments

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import org.springframework.core.io.Resource
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.Attachment
import skills.utils.GroovyToJavaByteUtils

class DataCleanupSpecs extends DefaultIntSpec {

    @PersistenceContext
    EntityManager entityManager

    SkillsService supervisorService

    def setup() {
        supervisorService = createSupervisor()
    }

    def "make sure there are no orphan attachments in db when project is removed"() {
        Map proj = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill()
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 2)
        def badge = SkillsFactory.createBadge()
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        skillsService.createProject(proj)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)
        skillsService.createSkill(skillsGroup)
        skillsService.createBadge(badge)
        skillsService.uploadAttachment(resource, proj.projectId)
        skillsService.uploadAttachment(resource, proj.projectId, subject.subjectId)
        skillsService.uploadAttachment(resource, proj.projectId, skill.skillId)
        skillsService.uploadAttachment(resource, proj.projectId, skillsGroup.skillId)
        skillsService.uploadAttachment(resource, proj.projectId, badge.badgeId)

        long attachmentCountBefore = countAllAttachments()
        skillsService.deleteProject(proj.projectId)
        long attachmentCountAfter = countAllAttachments()

        then:
        attachmentCountBefore == 5
        attachmentCountAfter == 0
    }

    def "make sure there are no orphan attachments in db when global badge is removed"() {
        def badge = SkillsFactory.createBadge()
        badge.enabled = false
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        supervisorService.createGlobalBadge(badge)
        def result = supervisorService.uploadAttachment(resource, null, badge.badgeId)

        boolean attachmentExistBefore = doesAttachmentExist(result.uuid)
        supervisorService.deleteGlobalBadge(badge.badgeId)
        boolean attachmentExistAfter = doesAttachmentExist(result.uuid)

        then:
        attachmentExistBefore
        !attachmentExistAfter
    }

    def "make sure there are no orphan attachments in db when quiz is removed"() {
        def quiz = QuizDefFactory.createQuizSurvey()
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        when:
        skillsService.createQuizDef(quiz)
        def result = skillsService.uploadAttachment(resource, null, null, quiz.quizId)

        boolean attachmentExistBefore = doesAttachmentExist(result.uuid)
        skillsService.removeQuizDef(quiz.quizId)
        boolean attachmentExistAfter = doesAttachmentExist(result.uuid)

        then:
        attachmentExistBefore
        !attachmentExistAfter
    }

    private Boolean doesAttachmentExist(String uuid) {
        String query = "SELECT count(a) from Attachment a where a.uuid = '${uuid}'"
        Query getAttachmentsCount = entityManager.createQuery(query, Long)
        return getAttachmentsCount.getSingleResult() > 0
    }

    private Long countAllAttachments() {
        String query = "SELECT count(a) from Attachment a"
        Query getAttachmentsCount = entityManager.createQuery(query, Long)
        return  getAttachmentsCount.getSingleResult()
    }
}
