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

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillVideoClientDisplay_ReusedSkillsSpecs extends DefaultIntSpec {

    def "get video attributes for a single skill" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                videoType: "video",
                transcript: "transcript",
                captions: "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl: "http://some.url",
                videoType: "video",
                transcript: "transcript",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[2].skillId, [
                videoUrl: "http://some.url",
                videoType: "video",
        ])
        p1Skills[2].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[2])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[3].skillId, [
                videoUrl: "http://some.url",
        ])
        p1Skills[3].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[3])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[4].skillId, [
                videoUrl: "http://some.url",
        ])

        def user = getRandomUsers(1).first()

        when:
        skillsService.reuseSkills(p1.projectId, p1Skills.collect { it.skillId }, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0))
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[2].skillId, 0))
        def skill3 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[3].skillId, 0))
        def skill4 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[4].skillId, 0))
        def skill5 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[5].skillId, 0))
        println JsonOutput.prettyPrint(JsonOutput.toJson(skill1))
        then:
        skill.videoSummary.videoUrl == "http://some.url"
        skill.videoSummary.videoType == "video"
        skill.videoSummary.hasCaptions == true
        skill.videoSummary.hasTranscript == true
        skill.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill1.videoSummary.videoUrl == "http://some.url"
        skill1.videoSummary.videoType == "video"
        skill1.videoSummary.hasCaptions == false
        skill1.videoSummary.hasTranscript == true
        skill1.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill2.videoSummary.videoUrl == "http://some.url"
        skill2.videoSummary.videoType == "video"
        skill2.videoSummary.hasCaptions == false
        skill2.videoSummary.hasTranscript == false
        skill2.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill3.videoSummary.videoUrl == "http://some.url"
        !skill3.videoSummary.videoType
        skill3.videoSummary.hasCaptions == false
        skill3.videoSummary.hasTranscript == false
        skill3.selfReporting.type == SkillDef.SelfReportingType.Video.toString()

        skill4.videoSummary.videoUrl == "http://some.url"
        !skill4.videoSummary.videoType
        skill4.videoSummary.hasCaptions == false
        skill4.videoSummary.hasTranscript == false
        !skill4.selfReporting?.type

        !skill5.videoSummary
        !skill4.selfReporting?.type
    }

    def "get video attributes in descriptions endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl  : "http://some.url",
                videoType : "video",
                transcript: "transcript",
                captions  : "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl  : "http://some.url",
                videoType : "video",
                transcript: "transcript",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[2].skillId, [
                videoUrl : "http://some.url",
                videoType: "video",
        ])
        p1Skills[2].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[2])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[3].skillId, [
                videoUrl: "http://some.url",
        ])
        p1Skills[3].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[3])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[4].skillId, [
                videoUrl: "http://some.url",
        ])

        def user = getRandomUsers(1).first()
        when:
        skillsService.reuseSkills(p1.projectId, p1Skills.collect { it.skillId }, p1subj2.subjectId)

        def descriptions = skillsService.getSubjectDescriptions(p1.projectId, p1subj2.subjectId, user)
        println JsonOutput.prettyPrint(JsonOutput.toJson(descriptions))
        def skill1VidSummary = descriptions.find { it.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0) }.videoSummary
        def skill2VidSummary = descriptions.find { it.skillId == SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0) }.videoSummary
        def skill3VidSummary = descriptions.find { it.skillId == SkillReuseIdUtil.addTag(p1Skills[2].skillId, 0) }.videoSummary
        def skill4VidSummary = descriptions.find { it.skillId == SkillReuseIdUtil.addTag(p1Skills[3].skillId, 0) }.videoSummary
        def skill5VidSummary = descriptions.find { it.skillId == SkillReuseIdUtil.addTag(p1Skills[4].skillId, 0) }.videoSummary
        def skill6VidSummary = descriptions.find { it.skillId == SkillReuseIdUtil.addTag(p1Skills[5].skillId, 0) }.videoSummary
        then:
        skill1VidSummary.videoUrl == "http://some.url"
        skill1VidSummary.videoType == "video"
        skill1VidSummary.hasCaptions == true
        skill1VidSummary.hasTranscript == true

        skill2VidSummary.videoUrl == "http://some.url"
        skill2VidSummary.videoType == "video"
        skill2VidSummary.hasCaptions == false
        skill2VidSummary.hasTranscript == true

        skill3VidSummary.videoUrl == "http://some.url"
        skill3VidSummary.videoType == "video"
        skill3VidSummary.hasCaptions == false
        skill3VidSummary.hasTranscript == false

        skill4VidSummary.videoUrl == "http://some.url"
        !skill4VidSummary.videoType
        skill4VidSummary.hasCaptions == false
        skill4VidSummary.hasTranscript == false

        skill5VidSummary.videoUrl == "http://some.url"
        !skill5VidSummary.videoType
        skill5VidSummary.hasCaptions == false
        skill5VidSummary.hasTranscript == false

        !skill6VidSummary
    }

    def "get captions endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl  : "http://some.url",
                videoType : "video",
                transcript: "transcript",
                captions  : "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl  : "http://some.url",
                videoType : "video",
                transcript: "transcript",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        when:
        skillsService.reuseSkills(p1.projectId, p1Skills.collect { it.skillId }, p1subj2.subjectId)

        def skill1Captions = skillsService.getVideoCaptions(p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def skill2Captions = skillsService.getVideoCaptions(p1.projectId, SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0))

        then:
        skill1Captions == "captions"
        !skill2Captions
    }

    def "get transcript endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl  : "http://some.url",
                videoType : "video",
                transcript: "transcript",
                captions  : "captions",
        ])
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[0])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl  : "http://some.url",
                videoType : "video",
        ])
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.Video
        skillsService.createSkill(p1Skills[1])

        when:
        skillsService.reuseSkills(p1.projectId, p1Skills.collect { it.skillId }, p1subj2.subjectId)
        def skill1T = skillsService.getVideoTranscript(p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def skill2T = skillsService.getVideoTranscript(p1.projectId, SkillReuseIdUtil.addTag(p1Skills[1].skillId, 0))

        then:
        skill1T == "transcript"
        !skill2T
    }
}
