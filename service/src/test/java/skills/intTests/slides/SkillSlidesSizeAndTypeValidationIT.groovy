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
package skills.intTests.slides

import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
@SpringBootTest(properties = [
        'skills.config.ui.maxSlidesUploadSize=125KB',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8202/status',
        'skills.authorization.userInfoUri=https://localhost:8202/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8202/userQuery?query={query}'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class SlidesSizeAndTypeValidationIT extends DefaultIntSpec {
    def "cannot exceed max slide size for a skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource slides44k = new ClassPathResource("/testSlides/test-slides-1.pdf")
        Resource slides160k = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [file: slides44k])

        when:
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [file: slides160k])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("File size [157 KB] exceeds maximum file size [125 KB]")
    }

    def "cannot exceed max slide size for a quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        Resource slides44k = new ClassPathResource("/testSlides/test-slides-1.pdf")
        Resource slides160k = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: slides44k])

        when:
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: slides160k])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("File size [157 KB] exceeds maximum file size [125 KB]")
    }

    def "validate supported mime types for skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource mp4Type = new ClassPathResource("/testVideos/empty-quiz.mp4")
        Resource pdfType = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [file: pdfType])

        when:
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [file: mp4Type])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Invalid media type [video/mp4]")
    }

    def "validate supported mime types for quiz"() {
            def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
            skillsService.createQuizDef(quiz)

            Resource mp4Type = new ClassPathResource("/testVideos/empty-quiz.mp4")
            Resource pdfType = new ClassPathResource("/testSlides/test-slides-1.pdf")
            skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfType])

            when:
            skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: mp4Type])

            then:
            SkillsClientException skillsClientException = thrown()
            skillsClientException.message.contains("Invalid media type [video/mp4]")
        }
}