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

import groovy.util.logging.Slf4j
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef

import java.nio.file.Files

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillSlidesFeaturesSpecs extends DefaultIntSpec {

    def "copy project with skill that has slides configured" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides])

        when:
        def copyProj = SkillsFactory.createProject(50)
        skillsService.copyProject(p1.projectId, copyProj)
        def attributes = skillsService.getSlidesAttributes(copyProj.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
    }

    def "imported skills return slides attributes" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides])

        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])

        when:
        skillsService.importSkillFromCatalog(p2.projectId, p2subj1.subjectId, p1.projectId, p1Skills[0].skillId)

        def attributes = skillsService.getSlidesAttributes(p2.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
    }

    def "changes to the slides attributes are propagated to the imported skills" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides])

        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.importSkillFromCatalog(p2.projectId, p2subj1.subjectId, p1.projectId, p1Skills[0].skillId)

        when:
        def attributes = skillsService.getSlidesAttributes(p2.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides2])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def attributes_t1 = skillsService.getSlidesAttributes(p2.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded_t1 = skillsService.downloadAttachment(attributes_t1.url)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        downloaded_t1.file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
    }

    def "changes to the slides attributes are propagated to the imported skills - slides were removed" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides])

        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.importSkillFromCatalog(p2.projectId, p2subj1.subjectId, p1.projectId, p1Skills[0].skillId)

        when:
        def attributes = skillsService.getSlidesAttributes(p2.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        skillsService.deleteSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def attributes_t1 = skillsService.getSlidesAttributes(p2.projectId, p1Skills[0].skillId)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes_t1.url == null
    }

    def "cannot set slides on an imported skill" () {
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
        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p2.projectId, p1Skills[0].skillId, [ file: pdfSlides])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Cannot set slide attributes of read-only skill")
    }

    def "moved skills return video attributes" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides])

        when:
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
    }

}
