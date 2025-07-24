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
import skills.intTests.utils.SkillsService

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
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [
                file: pdfSlides,
        ])

        when:
        def attributes = skillsService.getSlidesAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
    }
}
