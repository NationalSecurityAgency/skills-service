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
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserAttrs

import java.nio.file.Files

import static skills.intTests.utils.SkillsFactory.*

class QuizSlideDeckConfigSpecs extends DefaultIntSpec {

    def "save and get slide deck settings" () {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        skillsService.saveQuizSlidesAttributes(quiz.quizId, [ url: "http://some.url"])

        when:
        def attributes = skillsService.getQuizSlidesAttributes(quiz.quizId)
        then:
        attributes.url == "http://some.url"
    }

    def "upload slides" () {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")

        SkillsService rootService = createRootSkillService()
        UserAttrs skillsServiceUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def allActions = rootService.getUserActionsForEverything()
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides,])
        def allActions_t1 = rootService.getUserActionsForEverything()

        def attributes = skillsService.getQuizSlidesAttributes(quiz.quizId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"


        Closure findSlidesCreated = { it -> it.item == DashboardItem.SlidesSettings.toString() && it.action == DashboardAction.Create.toString() }

        !allActions.data.findAll(findSlidesCreated)

        def settingsCreated = allActions_t1.data.findAll(findSlidesCreated)
        settingsCreated.itemId == [quiz.quizId]
        settingsCreated.userId == [skillsServiceUserAttrs.userId]
        settingsCreated.userIdForDisplay == [skillsServiceUserAttrs.userIdForDisplay]
        settingsCreated.projectId == [null]
        settingsCreated.quizId == [quiz.quizId]
    }

    def "override existing uploaded slides" () {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides,])

        when:
        def attributes = skillsService.getQuizSlidesAttributes(quiz.quizId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides2,])

        def attributes1 = skillsService.getQuizSlidesAttributes(quiz.quizId)
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
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides,])

        when:
        def attributes = skillsService.getQuizSlidesAttributes(quiz.quizId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        skillsService.saveQuizSlidesAttributes(quiz.quizId, [
                isAlreadyHosted: true,
                width: 900
        ])

        def attributes1 = skillsService.getQuizSlidesAttributes(quiz.quizId)
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
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides,])

        when:
        def attributes = skillsService.getQuizSlidesAttributes(quiz.quizId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)

        skillsService.saveQuizSlidesAttributes(quiz.quizId, [
                url: "http://some.url",
        ])

        def attributes1 = skillsService.getQuizSlidesAttributes(quiz.quizId)

        then:
        attributes.url.toString().startsWith('/api/download/')
        downloaded.file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        downloaded.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"

        attributes.url != attributes1.url

        attributes1.url == "http://some.url"
    }

    def "external pdf url is replaced with uploaded slides" () {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        skillsService.saveQuizSlidesAttributes(quiz.quizId, [ url: "http://some.url"])

        when:
        def attributes = skillsService.getQuizSlidesAttributes(quiz.quizId)

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides2,])

        def attributes1 = skillsService.getQuizSlidesAttributes(quiz.quizId)
        SkillsService.FileAndHeaders downloaded2 = skillsService.downloadAttachment(attributes1.url)

        then:
        attributes.url == "http://some.url"

        attributes.url != attributes1.url

        attributes1.url.toString().startsWith('/api/download/')
        downloaded2.file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        downloaded2.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
    }

    def "delete slides" () {
        SkillsService rootService = createRootSkillService()

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides,])

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveQuizSlidesAttributes(quiz2.quizId, [file: pdfSlides2,])

        UserAttrs skillsServiceUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)

        when:
        def attributes = skillsService.getQuizSlidesAttributes(quiz.quizId)
        SkillsService.FileAndHeaders downloaded = skillsService.downloadAttachment(attributes.url)
        def attributes1 = skillsService.getQuizSlidesAttributes(quiz2.quizId)
        SkillsService.FileAndHeaders downloaded2 = skillsService.downloadAttachment(attributes1.url)

        def allActions = rootService.getUserActionsForEverything()
        skillsService.deleteQuizSlidesAttributes(quiz.quizId)
        def allActions_t1 =rootService.getUserActionsForEverything()

        def attributes_t1 = skillsService.getQuizSlidesAttributes(quiz.quizId)
        def attributes1_t1 = skillsService.getQuizSlidesAttributes(quiz2.quizId)
        SkillsService.FileAndHeaders downloaded2_t1 = skillsService.downloadAttachment(attributes1_t1.url)

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
        settingsDeleted.itemId == [quiz.quizId]
        settingsDeleted.userId == [skillsServiceUserAttrs.userId]
        settingsDeleted.userIdForDisplay == [skillsServiceUserAttrs.userIdForDisplay]
        settingsDeleted.projectId == [null]
        settingsDeleted.quizId == [quiz.quizId]
    }

}
