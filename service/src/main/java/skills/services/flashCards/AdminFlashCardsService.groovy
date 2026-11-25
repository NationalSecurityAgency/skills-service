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
package skills.services.flashCards

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.PublicProps
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.services.LockingService
import skills.services.attributes.FlashCardsAttrs
import skills.services.attributes.SkillAttributeService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.repos.SkillDefRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class AdminFlashCardsService {

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Autowired
    PublicProps publicProps

    @Autowired
    LockingService lockingService

    @Transactional
    FlashCardsAttrs saveFlashCards(String projectId, String skillId, FlashCardsAttrs flashCards) {
        lockingService.lockProject(projectId)
        boolean isReadOnly = skillDefRepo.isImportedFromCatalog(projectId, skillId)
        SkillsValidator.isTrue(!isReadOnly, "Cannot set flash cards of read-only skill", projectId, skillId)

        List<FlashCardsAttrs.FlashCard> cards = flashCards?.cards ?: []
        int maxCards = publicProps.getInt(PublicProps.UiProp.maxFlashCardsPerSkill)
        SkillsValidator.isTrue(cards.size() <= maxCards, "A skill can have at most [${maxCards}] flash cards", projectId, skillId)
        cards.eachWithIndex { FlashCardsAttrs.FlashCard card, int index ->
            SkillsValidator.isNotBlank(card?.front, "Flash card ${index + 1} front", projectId, skillId)
            SkillsValidator.isNotBlank(card?.back, "Flash card ${index + 1} back", projectId, skillId)
            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxFlashCardTextLength, "Flash card ${index + 1} front", card.front)
            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxFlashCardTextLength, "Flash card ${index + 1} back", card.back)
            propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxFlashCardHintLength, "Flash card ${index + 1} hint", card.hint)
        }

        FlashCardsAttrs existing = skillAttributeService.getFlashCardsAttrs(projectId, skillId)
        int existingVersion = existing.version ?: 0
        if ((flashCards?.version ?: 0) != existingVersion) {
            throw new SkillException("Flash cards were changed after they were loaded; reload them and apply your changes again", projectId, skillId, ErrorCode.BadParam)
        }
        boolean isEdit = existing.cards

        FlashCardsAttrs toSave = new FlashCardsAttrs(
                version: existingVersion + 1,
                cards: cards.collect {
                    new FlashCardsAttrs.FlashCard(
                            front: InputSanitizer.sanitize(it.front.trim()),
                            back: InputSanitizer.sanitize(it.back.trim()),
                            hint: StringUtils.isNotBlank(it.hint) ? InputSanitizer.sanitize(it.hint.trim()) : null)
                })
        skillAttributeService.saveFlashCardsAttrs(projectId, skillId, toSave)

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: !cards ? DashboardAction.Delete : (isEdit ? DashboardAction.Edit : DashboardAction.Create),
                item: DashboardItem.FlashCards,
                itemId: skillId,
                projectId: projectId,
                actionAttributes: [numCards: cards.size()]
        ))
        return getFlashCards(projectId, skillId)
    }

    FlashCardsAttrs getFlashCards(String projectId, String skillId) {
        return unsanitize(skillAttributeService.getFlashCardsAttrs(projectId, skillId))
    }

    static FlashCardsAttrs unsanitize(FlashCardsAttrs flashCards) {
        flashCards.cards = flashCards.cards.collect {
            new FlashCardsAttrs.FlashCard(
                    front: InputSanitizer.unsanitizeEscapedHtml(it.front),
                    back: InputSanitizer.unsanitizeEscapedHtml(it.back),
                    hint: it.hint ? InputSanitizer.unsanitizeEscapedHtml(it.hint) : null)
        }
        return flashCards
    }
}
