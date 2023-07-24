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
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillVideoConfigSpecs extends DefaultIntSpec {

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

    def "delete video attributs" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some1.url",
                videoType: "video1",
                transcript: "transcript1",
                captions: "captions1",
        ])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl: "http://some2.url",
                videoType: "video2",
                transcript: "transcript2",
                captions: "captions2",
        ])

        when:
        def skill1Attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        def skill2Attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        def skill1AttributesAfter = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        def skill2AttributesAfter = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        then:
        skill1Attributes.videoUrl == "http://some1.url"
        skill1Attributes.videoType == "video1"
        skill1Attributes.captions == "captions1"
        skill1Attributes.transcript == "transcript1"

        skill2Attributes.videoUrl == "http://some2.url"
        skill2Attributes.videoType == "video2"
        skill2Attributes.captions == "captions2"
        skill2Attributes.transcript == "transcript2"

        skill1AttributesAfter.videoUrl == "http://some1.url"
        skill1AttributesAfter.videoType == "video1"
        skill1AttributesAfter.captions == "captions1"
        skill1AttributesAfter.transcript == "transcript1"

        !skill2AttributesAfter.videoUrl
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

    def "keep --> in captions"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        String sampleCaption = '''WEBVTT

    1
    00:00:00.500 --> 00:00:04.000
    This is the very first caption!'''
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "<a href='http://skillcooolexample.com/' onclick='evilDoing()'>transcript</a>",
                captions: sampleCaption.toString(),
        ])

        when:
        def attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        String caption = skillsService.getVideoCaptions(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.captions == sampleCaption
        caption == sampleCaption
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

    def "do not allow to set video attributes on an imported skill"() {
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
        skillsService.saveSkillVideoAttributes(p2.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                videoType: "video",
                transcript: "transcript",
                captions: "captions",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot set video attributes of read-only skill")
    }

    def "do not allow to set video attributes on reused skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, p1Skills.collect { it.skillId }, p1subj2.subjectId)
        String reusedSkillId = SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, reusedSkillId, [
                videoUrl: "http://some.url",
                videoType: "video",
                transcript: "transcript",
                captions: "captions",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot set video attributes of read-only skill")
    }

    def "can only set video attributes for a skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 50)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1skillsGroup])
        def p1Skills = createSkills(6, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1skillsGroup.skillId, it)
        }
        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1skillsGroup.skillId, [
                videoUrl: "http://some.url",
                videoType: "video",
                transcript: "transcript",
                captions: "captions",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Failed to find skillId")
    }
}
