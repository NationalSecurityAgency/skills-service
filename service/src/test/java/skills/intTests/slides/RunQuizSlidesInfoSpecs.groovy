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
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService

import java.nio.file.Files

@Slf4j
class RunQuizSlidesInfoSpecs extends DefaultIntSpec {

    def "get quiz slides attributes"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def quiz2 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        def quiz3 = QuizDefFactory.createQuiz(3, "Fancy Description")
        skillsService.createQuizDef(quiz3)
        def quiz4 = QuizDefFactory.createQuiz(4, "Fancy Description")
        skillsService.createQuizDef(quiz4)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides, width: 111])
        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveQuizSlidesAttributes(quiz2.quizId, [file: pdfSlides2])

        skillsService.saveQuizSlidesAttributes(quiz3.quizId, [ url: "http://some.url", width: 333])
        skillsService.saveQuizSlidesAttributes(quiz4.quizId, [ url: "http://some1.url"])

        when:
        def quizInfo =  skillsService.getQuizInfo(quiz.quizId)
        def quizInfo2 =  skillsService.getQuizInfo(quiz2.quizId)
        def quizInfo3 =  skillsService.getQuizInfo(quiz3.quizId)
        def quizInfo4 =  skillsService.getQuizInfo(quiz4.quizId)
        then:
        skillsService.downloadAttachment(quizInfo.slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        quizInfo.slidesSummary.width == 111.0
        quizInfo.slidesSummary.type == "application/pdf"

        skillsService.downloadAttachment(quizInfo2.slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        !quizInfo2.slidesSummary.width
        quizInfo2.slidesSummary.type == "application/pdf"

        quizInfo3.slidesSummary.url == "http://some.url"
        quizInfo3.slidesSummary.width == 333.0

        quizInfo4.slidesSummary.url == "http://some1.url"
        !quizInfo4.slidesSummary.width
    }

    def "UC protection applies for slides download" () {
        SkillsService rootSkillsService = createRootSkillService()
        skillsService.getCurrentUser() // initialize skillsService user_attrs

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon']);

        List<String> userNames = getRandomUsers(2)
        SkillsService nonUcUser = createService(userNames[0])

        SkillsService otherUcUser = createService(userNames[1])
        rootSkillsService.saveUserTag(otherUcUser.userName, 'dragons', ['DivineDragon']);

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        quiz.enableProtectedUserCommunity = true
        skillsService.createQuizDef(quiz)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides, width: 111])

        def quizInfo =  otherUcUser.getQuizInfo(quiz.quizId)
        def slidesUrl = quizInfo.slidesSummary.url

        File resource = pdfSlides.getFile();
        byte[] expectedContentAsBytes = Files.readAllBytes(resource.toPath())

        when:
        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment(slidesUrl)
        SkillsService.FileAndHeaders fileAndHeaders1 = otherUcUser.downloadAttachment(slidesUrl)

        nonUcUser.downloadAttachment(slidesUrl)
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN

        fileAndHeaders.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
        fileAndHeaders.file.bytes == expectedContentAsBytes

        fileAndHeaders1.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "application/pdf"
        fileAndHeaders1.file.bytes == expectedContentAsBytes
    }
}
