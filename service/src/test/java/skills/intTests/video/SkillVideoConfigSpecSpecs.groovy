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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillVideoConfigSpecSpecs extends DefaultIntSpec {

    def "not allowed to save self-report=video when creating a skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        when:
        skillsService.createSkill(p1Skills[0])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("selfReportingType=Video is not allowed when creating a new skill")
    }

    def "not allowed to save self-report=video if video is not configured"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        when:
        skillsService.createSkill(p1Skills[0])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Video URL must be configured prior to attempting to set selfReportingType=Video")
    }

    def "not allowed to save self-report=video if numPerformToCompletion > 1"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [videoUrl: "http://some.url"])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        p1Skills[0].numPerformToCompletion = 2
        when:
        skillsService.createSkill(p1Skills[0])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("When selfReportingType=Video numPerformToCompletion must equal to 1 but [2] was provided")
    }

    def "save and get video settings" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                videoType: "video",
                transcript: "transcript",
                captions: "captions",
        ])

        when:
        def attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.videoUrl == "http://some.url"
        attributes.videoType == "video"
        attributes.captions == "captions"
        attributes.transcript == "transcript"
    }

    def "sanitize captions and transcript"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "<a href='http://skillcooolexample.com/' onclick='evilDoing()'>transcript</a>",
                captions: "<a href='http://skillcooolexample.com/' onclick='evilDoing()'>captions</a>",
        ])

        when:
        def attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.captions == "<a href=\"http://skillcooolexample.com/\">captions</a>"
        attributes.transcript == "<a href=\"http://skillcooolexample.com/\">transcript</a>"
    }

    def "video attributes must provide videoUrl"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [videoUrl: "http://some.url"])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoType: "video",
                transcript: "transcript",
                captions: "captions",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("videoUrl was not provided")
    }
}
