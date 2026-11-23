/**
 * Copyright 2026 SkillTree
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
package skills.intTests.flashCards

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem

import static skills.intTests.utils.SkillsFactory.*

class SkillFlashCardsSpecs extends DefaultIntSpec {

    static List<Map> cards(int num) {
        (1..num).collect { [front: "Front ${it}".toString(), back: "Back ${it}".toString(), hint: it % 2 ? "Hint ${it}".toString() : null] }
    }

    def "no flash cards by default"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        def res = skillsService.getFlashCards(p1.projectId, p1Skills[0].skillId)
        def summary = skillsService.getSingleSkillSummary(getRandomUsers(1).first(), p1.projectId, p1Skills[0].skillId)

        then:
        res.version == 0
        res.cards == []
        summary.flashCards == null
    }

    def "save, edit and remove flash cards"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        def saved = skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(3)])
        def edited = skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 1, cards: [[front: "  New front  ", back: "New back"]]])
        def afterEdit = skillsService.getFlashCards(p1.projectId, p1Skills[0].skillId)
        def otherSkill = skillsService.getFlashCards(p1.projectId, p1Skills[1].skillId)
        def removed = skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 2, cards: []])

        then:
        saved.version == 1
        saved.cards.front == ["Front 1", "Front 2", "Front 3"]
        saved.cards.back == ["Back 1", "Back 2", "Back 3"]
        saved.cards.hint == ["Hint 1", null, "Hint 3"]
        edited.version == 2
        afterEdit.cards == [[front: "New front", back: "New back", hint: null]]
        otherSkill.cards == []
        removed.version == 3
        removed.cards == []
    }

    def "special characters are kept as entered"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        String text = "a < b && c > d \"quoted\" it's"

        when:
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: [[front: text, back: text, hint: text]]])
        def res = skillsService.getFlashCards(p1.projectId, p1Skills[0].skillId)
        def summary = skillsService.getSingleSkillSummary(getRandomUsers(1).first(), p1.projectId, p1Skills[0].skillId)

        then:
        res.cards == [[front: text, back: text, hint: text]]
        summary.flashCards == [[front: text, back: text, hint: text]]
    }

    def "a save based on an outdated version is rejected"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(2)])

        when:
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(1)])

        then:
        SkillsClientException ex = thrown()
        ex.message.contains("Flash cards were changed after they were loaded")
        skillsService.getFlashCards(p1.projectId, p1Skills[0].skillId).cards.size() == 2
    }

    def "front and back are required; lengths and number of cards are limited"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        String skillId = p1Skills[0].skillId

        when:
        String msg = null
        try {
            skillsService.saveFlashCards(p1.projectId, skillId, [version: 0, cards: cardsToSave])
        } catch (SkillsClientException e) {
            msg = e.message
        }

        then:
        msg.contains(expected)
        skillsService.getFlashCards(p1.projectId, skillId).cards == []

        where:
        cardsToSave                                          | expected
        [[front: "f", back: "b"], [front: " ", back: "b"]]   | "Flash card 2 front"
        [[front: "f", back: null]]                           | "Flash card 1 back"
        [[front: "f" * 2001, back: "b"]]                     | "[Flash card 1 front] must not exceed [2000] chars."
        [[front: "f", back: "b" * 2001]]                     | "[Flash card 1 back] must not exceed [2000] chars."
        [[front: "f", back: "b", hint: "h" * 501]]           | "[Flash card 1 hint] must not exceed [500] chars."
        cards(101)                                           | "A skill can have at most [100] flash cards"
    }

    def "maximum lengths and number of cards are accepted"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        List<Map> toSave = cards(100)
        toSave[0] = [front: "f" * 2000, back: "b" * 2000, hint: "h" * 500]

        when:
        def res = skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: toSave])

        then:
        res.cards.size() == 100
        res.cards[0].front.size() == 2000
    }

    def "trainee skill summary includes the flash cards"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(2)])
        String user = getRandomUsers(1).first()

        when:
        def summary = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def withSubject = skillsService.getSingleSkillSummaryWithSubject(user, p1.projectId, p1subj1.subjectId, p1Skills[0].skillId)
        def noCards = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[1].skillId)

        then:
        summary.flashCards == [[front: "Front 1", back: "Back 1", hint: "Hint 1"], [front: "Front 2", back: "Back 2", hint: null]]
        withSubject.flashCards == summary.flashCards
        noCards.flashCards == null
    }

    def "reused skills show the original skill's flash cards and cannot be edited"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(2)])
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        String reusedSkillId = SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)

        when:
        def summary = skillsService.getSingleSkillSummary(getRandomUsers(1).first(), p1.projectId, reusedSkillId)
        skillsService.saveFlashCards(p1.projectId, reusedSkillId, [version: 0, cards: cards(1)])

        then:
        summary.flashCards.front == ["Front 1", "Front 2"]
        SkillsClientException ex = thrown()
        ex.message.contains("Cannot set flash cards of read-only skill")
    }

    def "imported skills show the exported skill's flash cards and cannot be edited"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(2)])
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])

        when:
        def summary = skillsService.getSingleSkillSummary(getRandomUsers(1).first(), p2.projectId, p1Skills[0].skillId)
        skillsService.saveFlashCards(p2.projectId, p1Skills[0].skillId, [version: 0, cards: cards(1)])

        then:
        summary.flashCards.front == ["Front 1", "Front 2"]
        SkillsClientException ex = thrown()
        ex.message.contains("Cannot set flash cards of read-only skill")
    }

    def "flash cards are copied with the project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(2)])

        when:
        def projToCopy = createProject(2)
        skillsService.copyProject(p1.projectId, projToCopy)
        def copied = skillsService.getFlashCards(projToCopy.projectId, p1Skills[0].skillId)

        then:
        copied.cards.front == ["Front 1", "Front 2"]
        copied.version == 1
    }

    def "only project admins can read or change flash cards"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 0, cards: cards(1)])
        SkillsService otherUser = createService(getRandomUsers(1, true).first())

        when:
        otherUser.saveFlashCards(p1.projectId, p1Skills[0].skillId, [version: 1, cards: cards(2)])

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus.value() == 403
        skillsService.getFlashCards(p1.projectId, p1Skills[0].skillId).cards.size() == 1

        when:
        otherUser.getFlashCards(p1.projectId, p1Skills[0].skillId)

        then:
        SkillsClientException ex2 = thrown()
        ex2.httpStatus.value() == 403
    }

    def "changes are recorded in the project's history"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        String skillId = p1Skills[0].skillId

        when:
        skillsService.saveFlashCards(p1.projectId, skillId, [version: 0, cards: cards(2)])
        skillsService.saveFlashCards(p1.projectId, skillId, [version: 1, cards: cards(1)])
        skillsService.saveFlashCards(p1.projectId, skillId, [version: 2, cards: []])
        def actions = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "created", true, DashboardItem.FlashCards)

        then:
        actions.data.action == [DashboardAction.Create.toString(), DashboardAction.Edit.toString(), DashboardAction.Delete.toString()]
        actions.data.itemId == [skillId, skillId, skillId]
    }
}
