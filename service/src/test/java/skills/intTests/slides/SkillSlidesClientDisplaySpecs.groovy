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

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef

import java.nio.file.Files

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class SkillSlidesClientDisplaySpecs extends DefaultIntSpec {

    def "get slides attributes for a single skill" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides, width: 111])

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[1].skillId, [ file: pdfSlides2, width: 222])

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[2].skillId, [ url: "http://some.url", width: 333])
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[3].skillId, [ url: "http://some1.url"])
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[4].skillId, [ url: "http://some2.url"])

        def user = getRandomUsers(1).first()
        when:
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[2].skillId)
        def skill3 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[3].skillId)
        def skill4 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[4].skillId)
        def skill5 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[5].skillId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(skill))
        then:
        skillsService.downloadAttachment(skill.slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        skill.slidesSummary.width == 111.0
        skill.slidesSummary.type == "application/pdf"

        skillsService.downloadAttachment(skill1.slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        skill1.slidesSummary.width == 222.0
        skill1.slidesSummary.type == "application/pdf"

        skill2.slidesSummary.url == "http://some.url"
        skill2.slidesSummary.width == 333.0
        skill2.slidesSummary.type == null

        skill3.slidesSummary.url == "http://some1.url"
        skill3.slidesSummary.width == null
        skill3.slidesSummary.type  == null

        skill4.slidesSummary.url == "http://some2.url"
        skill4.slidesSummary.width == null
        skill4.slidesSummary.type  == null

        skill5.slidesSummary == null
    }

    def "get slides attributes in descriptions endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides, width: 111])

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[1].skillId, [ file: pdfSlides2, width: 222])

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[2].skillId, [ url: "http://some.url", width: 333])
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[3].skillId, [ url: "http://some1.url"])
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[4].skillId, [ url: "http://some2.url"])

        def user = getRandomUsers(1).first()
        when:
        def descriptions = skillsService.getSubjectDescriptions(p1.projectId, p1subj1.subjectId, user)
        def slidesSummary = descriptions.find { it.skillId == p1Skills[0].skillId }.slidesSummary
        def slidesSummary1 = descriptions.find { it.skillId == p1Skills[1].skillId }.slidesSummary
        def slidesSummary2 = descriptions.find { it.skillId == p1Skills[2].skillId }.slidesSummary
        def slidesSummary3 = descriptions.find { it.skillId == p1Skills[3].skillId }.slidesSummary
        def slidesSummary4 = descriptions.find { it.skillId == p1Skills[4].skillId }.slidesSummary
        def slidesSummary5 = descriptions.find { it.skillId == p1Skills[5].skillId }.slidesSummary
        then:
        skillsService.downloadAttachment(slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        slidesSummary.width == 111.0
        slidesSummary.type == "application/pdf"

        skillsService.downloadAttachment(slidesSummary1.url).file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        slidesSummary1.width == 222.0
        slidesSummary1.type == "application/pdf"

        slidesSummary2.url == "http://some.url"
        slidesSummary2.width == 333.0
        slidesSummary2.type == null

        slidesSummary3.url == "http://some1.url"
        slidesSummary3.width == null
        slidesSummary3.type  == null

        slidesSummary4.url == "http://some2.url"
        slidesSummary4.width == null
        slidesSummary4.type  == null

        slidesSummary5 == null
    }

    def "get slide attributes in badge descriptions endpoint" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides, width: 111])

        Resource pdfSlides2 = new ClassPathResource("/testSlides/test-slides-2.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[1].skillId, [ file: pdfSlides2, width: 222])

        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[2].skillId, [ url: "http://some.url", width: 333])
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[3].skillId, [ url: "http://some1.url"])
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[4].skillId, [ url: "http://some2.url"])

        def badge = createBadge(1, 20)
        skillsService.createBadge(badge)
        p1Skills.each {
            skillsService.assignSkillToBadge(projectId: p1.projectId, badgeId: badge.badgeId, skillId: it.skillId)
        }
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def user = getRandomUsers(1).first()
        when:
        def descriptions = skillsService.getBadgeDescriptions(p1.projectId, badge.badgeId, false, user)
        def slidesSummary = descriptions.find { it.skillId == p1Skills[0].skillId }.slidesSummary
        def slidesSummary1 = descriptions.find { it.skillId == p1Skills[1].skillId }.slidesSummary
        def slidesSummary2 = descriptions.find { it.skillId == p1Skills[2].skillId }.slidesSummary
        def slidesSummary3 = descriptions.find { it.skillId == p1Skills[3].skillId }.slidesSummary
        def slidesSummary4 = descriptions.find { it.skillId == p1Skills[4].skillId }.slidesSummary
        def slidesSummary5 = descriptions.find { it.skillId == p1Skills[5].skillId }.slidesSummary
        then:
        skillsService.downloadAttachment(slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        slidesSummary.width == 111.0
        slidesSummary.type == "application/pdf"

        skillsService.downloadAttachment(slidesSummary1.url).file.bytes == Files.readAllBytes(pdfSlides2.getFile().toPath())
        slidesSummary1.width == 222.0
        slidesSummary1.type == "application/pdf"

        slidesSummary2.url == "http://some.url"
        slidesSummary2.width == 333.0
        slidesSummary2.type == null

        slidesSummary3.url == "http://some1.url"
        slidesSummary3.width == null
        slidesSummary3.type  == null

        slidesSummary4.url == "http://some2.url"
        slidesSummary4.width == null
        slidesSummary4.type  == null

        slidesSummary5 == null
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

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveSlidesAttributes(p1.projectId, p1Skills[0].skillId, [ file: pdfSlides, width: 111])

        def user = getRandomUsers(1).first()
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def slidesUrl = skill.slidesSummary.url

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
