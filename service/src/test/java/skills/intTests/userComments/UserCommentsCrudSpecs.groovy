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
package skills.intTests.userComments


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs

import static skills.intTests.utils.SkillsFactory.*

class UserCommentsCrudSpecs extends DefaultIntSpec {

    def "create user comment"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        SkillsService otherUser = createService(getRandomUsers(1).first())
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)
        when:
        def comment = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment")?.body
        then:
        comment.comment == "comment"
        comment.userIdForDisplay == userAttrs.userIdForDisplay
        comment.commentedOn
    }

    def "respond to an existing user comment"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        SkillsService otherUser = createService(getRandomUsers(1).first())
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)
        def originalComment = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment")?.body
        when:
        def comment = otherUser.respondToUserComment(p1.projectId, originalComment.threadId, "response",)?.body
        then:
        comment.comment == "response"
        comment.userIdForDisplay == userAttrs.userIdForDisplay
        comment.commentedOn
    }

    def "get flat list of comments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        List<String> users = getRandomUsers(2)
        SkillsService otherUser = createService(users[0])
        def comment1 = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment1")?.body
        def comment2 = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment2")?.body
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)

        SkillsService otherUser1 = createService(users[1])
        def comment3 = otherUser1.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment3")?.body
        def comment4 = otherUser1.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment4")?.body
        UserAttrs userAttrs1 = userAttrsRepo.findByUserIdIgnoreCase(otherUser1.userName)

        when:
        def res = otherUser.getUserComments(p1.projectId, p1Skills[0].skillId)
        def res1 = otherUser1.getUserComments(p1.projectId, p1Skills[0].skillId)
        then:
        res.comments.comment == ["comment2", "comment1"]
        res.comments.userIdForDisplay == [userAttrs.userIdForDisplay, userAttrs.userIdForDisplay]
        res.comments.replies == [[], []]

        res1.comments.comment == ["comment4", "comment3"]
        res1.comments.userIdForDisplay == [userAttrs1.userIdForDisplay, userAttrs1.userIdForDisplay]
        res1.comments.replies == [[], []]
    }

    def "get list with replies from the same user"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        List<String> users = getRandomUsers(2)
        SkillsService otherUser = createService(users[0])
        def comment1 = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment1")?.body
        def comment2 = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment2")?.body
        otherUser.respondToUserComment(p1.projectId, comment2.threadId, "response1")
        otherUser.respondToUserComment(p1.projectId, comment2.threadId, "response2")
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)

        SkillsService otherUser1 = createService(users[1])
        def comment3 = otherUser1.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment3")?.body
        otherUser1.respondToUserComment(p1.projectId, comment3.threadId, "response3")
        otherUser1.respondToUserComment(p1.projectId, comment3.threadId, "response4")
        def comment4 = otherUser1.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment4")?.body
        otherUser1.respondToUserComment(p1.projectId, comment4.threadId, "response5")
        otherUser1.respondToUserComment(p1.projectId, comment4.threadId, "response6")
        otherUser1.respondToUserComment(p1.projectId, comment4.threadId, "response7")
        UserAttrs userAttrs1 = userAttrsRepo.findByUserIdIgnoreCase(otherUser1.userName)

        when:
        def res = otherUser.getUserComments(p1.projectId, p1Skills[0].skillId)
        def res1 = otherUser1.getUserComments(p1.projectId, p1Skills[0].skillId)
        then:
        res.comments.comment == ["comment2", "comment1"]
        res.comments.userIdForDisplay == [userAttrs.userIdForDisplay, userAttrs.userIdForDisplay]
        res.comments.replies[0].comment == ["response2", "response1"]
        res.comments.replies[0].userIdForDisplay == [userAttrs.userIdForDisplay, userAttrs.userIdForDisplay]
        res.comments.replies[1].size() == 0

        res1.comments.comment == ["comment4", "comment3"]
        res1.comments.userIdForDisplay == [userAttrs1.userIdForDisplay, userAttrs1.userIdForDisplay]
        res1.comments.replies[0].comment == ["response7", "response6", "response5"]
        res1.comments.replies[0].userIdForDisplay == [userAttrs1.userIdForDisplay, userAttrs1.userIdForDisplay, userAttrs1.userIdForDisplay]
        res1.comments.replies[1].comment == ["response4", "response3"]
        res1.comments.replies[1].userIdForDisplay == [userAttrs1.userIdForDisplay, userAttrs1.userIdForDisplay]
    }

    def "get list with replies from the admin user"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        List<String> users = getRandomUsers(2)
        SkillsService otherUser = createService(users[0])
        def comment1 = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment1")?.body
        def comment2 = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment2")?.body
        skillsService.respondToUserComment(p1.projectId, comment2.threadId, "response1", otherUser.userName)
        otherUser.respondToUserComment(p1.projectId, comment2.threadId, "response2")
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)

        SkillsService otherUser1 = createService(users[1])
        def comment3 = otherUser1.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment3")?.body
        otherUser1.respondToUserComment(p1.projectId, comment3.threadId, "response3")
        otherUser1.respondToUserComment(p1.projectId, comment3.threadId, "response4")
        def comment4 = otherUser1.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment4")?.body
        skillsService.respondToUserComment(p1.projectId, comment4.threadId, "response5", otherUser1.userName)
        otherUser1.respondToUserComment(p1.projectId, comment4.threadId, "response6")
        skillsService.respondToUserComment(p1.projectId, comment4.threadId, "response7", otherUser1.userName)
        UserAttrs userAttrs1 = userAttrsRepo.findByUserIdIgnoreCase(otherUser1.userName)
        UserAttrs adminAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        when:
        def res = otherUser.getUserComments(p1.projectId, p1Skills[0].skillId)
        def res1 = otherUser1.getUserComments(p1.projectId, p1Skills[0].skillId)
        then:
        res.comments.comment == ["comment2", "comment1"]
        res.comments.userIdForDisplay == [userAttrs.userIdForDisplay, userAttrs.userIdForDisplay]
        res.comments.replies[0].comment == ["response2", "response1"]
        res.comments.replies[0].userIdForDisplay == [userAttrs.userIdForDisplay, adminAttrs.userIdForDisplay]
        res.comments.replies[1].size() == 0

        res1.comments.comment == ["comment4", "comment3"]
        res1.comments.userIdForDisplay == [userAttrs1.userIdForDisplay, userAttrs1.userIdForDisplay]
        res1.comments.replies[0].comment == ["response7", "response6", "response5"]
        res1.comments.replies[0].userIdForDisplay == [adminAttrs.userIdForDisplay, userAttrs1.userIdForDisplay, adminAttrs.userIdForDisplay]
        res1.comments.replies[1].comment == ["response4", "response3"]
        res1.comments.replies[1].userIdForDisplay == [userAttrs1.userIdForDisplay, userAttrs1.userIdForDisplay]
    }

    def "other non-admin uses are not allowed to respond to comments"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        List<String> users = getRandomUsers(2)
        SkillsService otherUser = createService(users[0])
        SkillsService otherUser1 = createService(users[1])
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName)
        def originalComment = otherUser.saveUserComment(p1.projectId, p1Skills[0].skillId, "comment")?.body
        when:
        otherUser1.respondToUserComment(p1.projectId, originalComment.threadId, "response",)?.body
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Only an admin or the original comment creator can respond to a thread")
    }

}
