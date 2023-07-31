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
package skills.intTests.video

import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject


@Slf4j
@SpringBootTest(properties = ['skills.config.ui.allowedVideoUploadMimeTypes:video/webm',
        'skills.config.ui.maxVideoUploadSize:500KB'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class VideoSizeAndTypeValidationIT  extends DefaultIntSpec {

    def "cannot exceed max video size" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video440k = new ClassPathResource("/testVideos/create-project.webm")
        Resource video540k = new ClassPathResource("/testVideos/create-subject.webm")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video440k,
        ])

        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video540k,
        ])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("File size [534 KB] exceeds maximum file size [500 KB]")
    }

    def "validate supported mime types" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource webmType = new ClassPathResource("/testVideos/create-project.webm")
        Resource mp4Type = new ClassPathResource("/testVideos/empty-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: webmType,
        ])

        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: mp4Type,
        ])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Invalid media type [video/mp4]")
    }
}
