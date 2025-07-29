/**
 * Copyright 2025 SkillTree
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
package skills.intTests.slides

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserAttrs

import java.nio.file.Files

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class SlideDeckConfigSpecs extends DefaultIntSpec {


    def "save and get slide deck settings" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                url: "http://some.url",
        ])

        when:
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.url == "http://some.url"
    }

    def "upload slides" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")

        SkillsService rootService = createRootSkillService()
        UserAttrs skillsServiceUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def allActions = rootService.getUserActionsForEverything()
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides,
        ])
        def allActions_t1 = rootService.getUserActionsForEverything()

        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"


        Closure findSlidesCreated = { it -> it.item == DashboardItem.SlidesSettings.toString() && it.action == DashboardAction.Create.toString() }

        !allActions.data.findAll(findSlidesCreated)

        def settingsCreated = allActions_t1.data.findAll(findSlidesCreated)
        settingsCreated.itemId == [p1Skills[0].skillId]
        settingsCreated.userId == [skillsServiceUserAttrs.userId]
        settingsCreated.userIdForDisplay == [skillsServiceUserAttrs.userIdForDisplay]
        settingsCreated.projectId == [p1.projectId]
        settingsCreated.quizId == [null]
    }

    def "override existing uploaded slides" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides,
        ])

        when:
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides2,
        ])

        def attributes1 = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded2 = skillsService.downloadAttachment(attributes1.url)

        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes.url != attributes1.url

        attributes1.url.toString().startsWith('/api/download/')
        downloaded2.file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        downloaded2.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
    }

    def "update width while keeping uploaded slides unchanged" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides,
        ])

        when:
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                isAlreadyHosted: true,
                width: 900
        ])

        def attributes1 = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded2 = skillsService.downloadAttachment(attributes1.url)

        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes.url == attributes1.url

        attributes1.url.toString().startsWith('/api/download/')
        downloaded2.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded2.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes.width == null
        attributes1.width == 900
    }

    def "uploaded slides are replaced wth an external pdf url" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides,
        ])

        when:
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                url: "http://some.url",
        ])

        def attributes1 = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)

        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes.url != attributes1.url

        attributes1.url == "http://some.url"
    }

    def "external pdf url is replaced with uploaded slides" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                url: "http://some.url",
        ])

        when:
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides2,
        ])

        def attributes1 = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded2 = skillsService.downloadAttachment(attributes1.url)

        then:
        attributes.url == "http://some.url"

        attributes.url != attributes1.url

        attributes1.url.toString().startsWith('/api/download/')
        downloaded2.file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        downloaded2.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
    }

    def "do not allow to set slides attributes on an imported skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.importSkillFromCatalog(p2.projectId, p2subj1.subjectId, p1.projectId, p1Skills[0].skillId)

        when:
        skillsService.saveSlidesAttributes(p2.projectId, p1Skills[0].skillId, [
                file: new ClassPathResource("/testSlides/test-slides-1.pdf"),
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot set slide attributes of read-only skill")
    }

    def "do not allow to set slides attributes on reused skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, p1Skills.collect { it.skillId }, p1subj2.subjectId)
        String reusedSkillId = SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        when:
        skillsService.saveSlidesAttributes(p1.projectId, reusedSkillId, [
                file: new ClassPathResource("/testSlides/test-slides-1.pdf"),
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot set slide attributes of read-only skill")
    }

    def "can only set slide attributes for a skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 50)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1skillsGroup])
        def p1Skills = createSkills(6, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1skillsGroup.skillId, it)
        }
        when:
        skillsService.saveSlidesAttributes(p1.projectId, p1skillsGroup.skillId, [
                file: new ClassPathResource("/testSlides/test-slides-1.pdf"),
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Failed to find skillId")
    }

    def "delete slides" () {
        SkillsService rootService = createRootSkillService()

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides,
        ])

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[1].skillId, [
                file: pdfSlides2,
        ])

        UserAttrs skillsServiceUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)

        when:
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        def attributes1 = skillsService.getSlidesAttributes(p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders downloaded2 = skillsService.downloadAttachment(attributes1.url)

        def allActions = rootService.getUserActionsForEverything()
        skillsService.deleteSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        def allActions_t1 =rootService.getUserActionsForEverything()

        def attributes_t1 = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        def attributes1_t1 = skillsService.getSlidesAttributes(p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders downloaded2_t1 = skillsService.downloadAttachment(attributes1.url)

        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes.url != attributes1.url

        attributes1.url.toString().startsWith('/api/download/')
        downloaded2.file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        downloaded2.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes_t1.url == null
        attributes_t1.type == null
        attributes_t1.isInternallyHosted == null
        attributes_t1.internallyHostedFileName == null
        attributes_t1.internallyHostedAttachmentUuid == null
        attributes1_t1.url.toString().startsWith('/api/download/')
        downloaded2_t1.file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        downloaded2_t1.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        Closure findSlideDeleted = { it -> it.item == DashboardItem.SlidesSettings.toString() && it.action == DashboardAction.Delete.toString() }

        !allActions.data.findAll(findSlideDeleted)

        def settingsDeleted = allActions_t1.data.findAll(findSlideDeleted)
        settingsDeleted.itemId == [p1Skills[0].skillId]
        settingsDeleted.userId == [skillsServiceUserAttrs.userId]
        settingsDeleted.userIdForDisplay == [skillsServiceUserAttrs.userIdForDisplay]
        settingsDeleted.projectId == [p1.projectId]
        settingsDeleted.quizId == [null]
    }

}
