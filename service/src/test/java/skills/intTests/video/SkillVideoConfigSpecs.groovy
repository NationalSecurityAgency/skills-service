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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef
import skills.storage.repos.AttachmentRepo

import java.nio.file.Files

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
    
    def "save and get video settings" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ])

        when:
        def attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.videoUrl == "http://some.url"
        attributes.captions == "captions"
        attributes.transcript == "transcript"
    }

    def "upload video" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video,
                transcript: "transcript",
                captions: "captions",
        ])

        when:
        def attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.videoUrl.toString().startsWith('/api/download/')
        attributes.captions == "captions"
        attributes.transcript == "transcript"
    }

    @Autowired
    AttachmentRepo attachmentRepo

    def "override existing uploaded video" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video1 = new ClassPathResource("/testVideos/create-project.webm")
        Resource video2 = new ClassPathResource("/testVideos/create-quiz.mp4")
        Resource video3 = new ClassPathResource("/testVideos/create-skill.webm")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video1,
                transcript: "transcript",
                captions: "captions",
        ])
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                file: video2,
                transcript: "transcript",
                captions: "captions",
        ])
        def user = getRandomUsers(1).first()

        when:
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1Download = skillsService.downloadAttachment(skill1.videoSummary.videoUrl)
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2Download = skillsService.downloadAttachment(skill2.videoSummary.videoUrl)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video3,
                transcript: "transcript",
                captions: "captions",
        ])
        def skill1After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1DownloadAfter = skillsService.downloadAttachment(skill1After.videoSummary.videoUrl)
        def skill2After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2DownloadAfter = skillsService.downloadAttachment(skill2After.videoSummary.videoUrl)

        then:
        attachmentRepo.count() == 2
        skill1.videoSummary.videoType == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-project.webm"'
        skill1Download.file.bytes == Files.readAllBytes(video1.getFile().toPath())

        skill2.videoSummary.videoType == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2Download.file.bytes == Files.readAllBytes(video2.getFile().toPath())

        skill1After.videoSummary.videoType == "video/webm"
        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-skill.webm"'
        skill1DownloadAfter.file.bytes == Files.readAllBytes(video3.getFile().toPath())

        skill2After.videoSummary.videoType == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2DownloadAfter.file.bytes == Files.readAllBytes(video2.getFile().toPath())
    }

    def "update captions and transcript while keeping uploaded video unchanged" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video1 = new ClassPathResource("/testVideos/create-project.webm")
        Resource video2 = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video1,
                transcript: "transcript",
                captions: "captions",
        ])
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                file: video2,
                transcript: "transcript",
                captions: "captions",
        ])
        def user = getRandomUsers(1).first()

        when:
        def skill1 = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1Download = skillsService.downloadAttachment(skill1.videoUrl)
        def skill2 = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2Download = skillsService.downloadAttachment(skill2.videoUrl)
        assert attachmentRepo.count() == 2

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                isAlreadyHosted: true,
                transcript: "transcript new",
                captions: "captions new",
        ])
        def skill1After = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1DownloadAfter = skillsService.downloadAttachment(skill1After.videoUrl)
        def skill2After = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2DownloadAfter = skillsService.downloadAttachment(skill2After.videoUrl)

        then:
        attachmentRepo.count() == 2
        skill1.videoUrl.startsWith('/api/download/')
        skill1.videoType == "video/webm"
        skill1.captions == "captions"
        skill1.transcript == "transcript"

        skill2.videoUrl.startsWith('/api/download/')
        skill2.videoType == "video/mp4"
        skill2.captions == "captions"
        skill2.transcript == "transcript"

        skill1Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-project.webm"'
        skill1Download.file.bytes == Files.readAllBytes(video1.getFile().toPath())

        skill2Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2Download.file.bytes == Files.readAllBytes(video2.getFile().toPath())

        skill1After.videoUrl.startsWith('/api/download/')
        skill1After.videoType == "video/webm"
        skill1After.captions == "captions new"
        skill1After.transcript == "transcript new"

        skill2After.videoUrl.startsWith('/api/download/')
        skill2After.videoType == "video/mp4"
        skill2After.captions == "captions"
        skill2After.transcript == "transcript"

        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-project.webm"'
        skill1DownloadAfter.file.bytes == Files.readAllBytes(video1.getFile().toPath())

        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2DownloadAfter.file.bytes == Files.readAllBytes(video2.getFile().toPath())
    }

    def "uploaded video is replaced with an external URL" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video1 = new ClassPathResource("/testVideos/create-project.webm")
        Resource video2 = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video1,
                transcript: "transcript",
                captions: "captions",
        ])
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                file: video2,
                transcript: "transcript",
                captions: "captions",
        ])
        def user = getRandomUsers(1).first()

        when:
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1Download = skillsService.downloadAttachment(skill1.videoSummary.videoUrl)
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2Download = skillsService.downloadAttachment(skill2.videoSummary.videoUrl)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ])
        def skill1After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skill2After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2DownloadAfter = skillsService.downloadAttachment(skill2After.videoSummary.videoUrl)

        then:
        attachmentRepo.count() == 1
        skill1.videoSummary.videoType == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-project.webm"'
        skill1Download.file.bytes == Files.readAllBytes(video1.getFile().toPath())

        skill2.videoSummary.videoType == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2Download.file.bytes == Files.readAllBytes(video2.getFile().toPath())

        !skill1After.videoSummary.videoType
        skill1After.videoSummary.videoUrl == "http://some.url"

        skill2After.videoSummary.videoType == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2DownloadAfter.file.bytes == Files.readAllBytes(video2.getFile().toPath())
    }

    def "uploaded video overrides existing external video url" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video1 = new ClassPathResource("/testVideos/create-project.webm")
        Resource video2 = new ClassPathResource("/testVideos/create-quiz.mp4")
        Resource video3 = new ClassPathResource("/testVideos/create-skill.webm")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ])
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                file: video2,
                transcript: "transcript",
                captions: "captions",
        ])
        def user = getRandomUsers(1).first()

        when:
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2Download = skillsService.downloadAttachment(skill2.videoSummary.videoUrl)
        assert attachmentRepo.count() == 1

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video3,
                transcript: "transcript",
                captions: "captions",
        ])
        def skill1After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1DownloadAfter = skillsService.downloadAttachment(skill1After.videoSummary.videoUrl)
        def skill2After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2DownloadAfter = skillsService.downloadAttachment(skill2After.videoSummary.videoUrl)

        then:
        attachmentRepo.count() == 2
        !skill1.videoSummary.videoType
        skill1.videoSummary.videoUrl == "http://some.url"

        skill2.videoSummary.videoType == "video/mp4"
        skill2.videoSummary.videoUrl.startsWith('/api/download')
        skill2Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2Download.file.bytes == Files.readAllBytes(video2.getFile().toPath())

        skill1After.videoSummary.videoType == "video/webm"
        skill1After.videoSummary.videoUrl.startsWith('/api/download')
        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-skill.webm"'
        skill1DownloadAfter.file.bytes == Files.readAllBytes(video3.getFile().toPath())

        skill2After.videoSummary.videoType == "video/mp4"
        skill2After.videoSummary.videoUrl.startsWith('/api/download')
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2DownloadAfter.file.bytes == Files.readAllBytes(video2.getFile().toPath())
    }

    def "delete video attributes" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some1.url",
                transcript: "transcript1",
                captions: "captions1",
                height: 300,
                width: 600
        ])

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                videoUrl: "http://some2.url",
                transcript: "transcript2",
                captions: "captions2",
                height: 300,
                width: 600
        ])

        when:
        def skill1Attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        def skill2Attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        def skill1AttributesAfter = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        def skill2AttributesAfter = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        then:
        skill1Attributes.videoUrl == "http://some1.url"
        skill1Attributes.captions == "captions1"
        skill1Attributes.transcript == "transcript1"

        skill2Attributes.videoUrl == "http://some2.url"
        skill2Attributes.captions == "captions2"
        skill2Attributes.transcript == "transcript2"

        skill1AttributesAfter.videoUrl == "http://some1.url"
        skill1AttributesAfter.captions == "captions1"
        skill1AttributesAfter.transcript == "transcript1"

        !skill2AttributesAfter.videoUrl
        !skill2AttributesAfter.height
        !skill2AttributesAfter.width
    }

    def "delete video attributes unsets self-report=video" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem.toString()
        p1Skills[2].selfReportingType = SkillDef.SelfReportingType.Approval.toString()
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        p1Skills.each {
            skillsService.saveSkillVideoAttributes(p1.projectId, it.skillId, [
                    videoUrl: "http://some1.url",
            ])
        }
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Video.toString()
        skillsService.createSkill(p1Skills[0])

        when:
        def skill = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def skill1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def skill2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[2].skillId])
        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[1].skillId)
        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[2].skillId)
        def skillAfter = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])
        def skill1After = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[1].skillId])
        def skill2After = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[2].skillId])
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Video.toString()
        !skillAfter.selfReportingType

        skill1.selfReportingType == SkillDef.SelfReportingType.HonorSystem.toString()
        skill1After.selfReportingType == SkillDef.SelfReportingType.HonorSystem.toString()

        skill2.selfReportingType == SkillDef.SelfReportingType.Approval.toString()
        skill2After.selfReportingType == SkillDef.SelfReportingType.Approval.toString()
    }

    def "delete video attributes with an uploaded video" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video1 = new ClassPathResource("/testVideos/create-project.webm")
        Resource video2 = new ClassPathResource("/testVideos/create-quiz.mp4")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video1,
                transcript: "transcript",
                captions: "captions",
        ])
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[1].skillId, [
                file: video2,
                transcript: "transcript",
                captions: "captions",
        ])
        def user = getRandomUsers(1).first()

        when:
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1Download = skillsService.downloadAttachment(skill1.videoSummary.videoUrl)
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2Download = skillsService.downloadAttachment(skill2.videoSummary.videoUrl)
        assert attachmentRepo.count() == 2

        skillsService.deleteSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)

        def skill1After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skill2After = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        SkillsService.FileAndHeaders skill2DownloadAfter = skillsService.downloadAttachment(skill2After.videoSummary.videoUrl)

        then:
        attachmentRepo.count() == 1
        skill1.videoSummary.videoType == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-project.webm"'
        skill1Download.file.bytes == Files.readAllBytes(video1.getFile().toPath())

        skill2.videoSummary.videoType == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2Download.file.bytes == Files.readAllBytes(video2.getFile().toPath())

        !skill1After.videoSummary

        skill2After.videoSummary.videoType == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/mp4"
        skill2DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-quiz.mp4"'
        skill2DownloadAfter.file.bytes == Files.readAllBytes(video2.getFile().toPath())
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
                transcript: "transcript",
                captions: "captions",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Either videoUrl or file must be supplied")
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
                transcript: "transcript",
                captions: "captions",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Failed to find skillId")
    }

    def "captions max length"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        String captionsJustRight = (1..5000).collect { "0" }.join("")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                captions: captionsJustRight.toString(),
        ])
        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                captions: captionsJustRight.toString() + "1",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Captions] must not exceed [5000]")
    }

    def "transcript max length"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        String transcriptJustRight = (1..20000).collect { "0" }.join("")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: transcriptJustRight.toString(),
        ])
        when:
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: transcriptJustRight.toString() + "1",
        ])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Transcript] must not exceed [20000]")
    }

    def "save and get video size" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
                height: 300,
                width: 600
        ])

        when:
        def attributes = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        then:
        attributes.height == 300
        attributes.width == 600
    }

    def "update video dimensions while keeping uploaded video unchanged" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource video1 = new ClassPathResource("/testVideos/create-project.webm")
        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                file: video1,
                transcript: "transcript",
                captions: "captions",
                width: 600,
                height: 300
        ])
        def user = getRandomUsers(1).first()

        when:
        def skill1 = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1Download = skillsService.downloadAttachment(skill1.videoUrl)

        assert attachmentRepo.count() == 1

        skillsService.saveSkillVideoAttributes(p1.projectId, p1Skills[0].skillId, [
                isAlreadyHosted: true,
                transcript: "transcript new",
                captions: "captions new",
                width: 900,
                height: 600,
        ])
        def skill1After = skillsService.getSkillVideoAttributes(p1.projectId, p1Skills[0].skillId)
        SkillsService.FileAndHeaders skill1DownloadAfter = skillsService.downloadAttachment(skill1After.videoUrl)

        then:
        attachmentRepo.count() == 1
        skill1.videoUrl.startsWith('/api/download/')
        skill1.videoType == "video/webm"
        skill1.captions == "captions"
        skill1.transcript == "transcript"
        skill1.width == 600
        skill1.height == 300

        skill1Download.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1Download.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-project.webm"'
        skill1Download.file.bytes == Files.readAllBytes(video1.getFile().toPath())

        skill1After.videoUrl.startsWith('/api/download/')
        skill1After.videoType == "video/webm"
        skill1After.captions == "captions new"
        skill1After.transcript == "transcript new"
        skill1After.width == 900
        skill1After.height == 600

        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_TYPE)[0] == "video/webm"
        skill1DownloadAfter.headers.get(HttpHeaders.CONTENT_DISPOSITION)[0] == 'inline; filename="create-project.webm"'
        skill1DownloadAfter.file.bytes == Files.readAllBytes(video1.getFile().toPath())
    }
}
