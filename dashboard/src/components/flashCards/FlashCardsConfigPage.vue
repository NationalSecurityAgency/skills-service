/*
Copyright 2026 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useFieldArray, useForm } from 'vee-validate'
import { array, object, string } from 'yup'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue'
import SkillsTextarea from '@/components/utils/inputForm/SkillsTextarea.vue'
import SkillsTextInput from '@/components/utils/inputForm/SkillsTextInput.vue'
import FlashCardsService from '@/components/flashCards/FlashCardsService.js'
import FlashCardDeck from '@/components/flashCards/FlashCardDeck.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useSkillsState } from '@/stores/UseSkillsState.js'

const route = useRoute()
const appConfig = useAppConfig()
const announcer = useSkillsAnnouncer()
const skillsState = useSkillsState()

const projectId = route.params.projectId
const skillId = route.params.skillId

const maxCards = computed(() => Number(appConfig.maxFlashCardsPerSkill || 100))
const maxTextLength = computed(() => Number(appConfig.maxFlashCardTextLength || 2000))
const maxHintLength = computed(() => Number(appConfig.maxFlashCardHintLength || 500))

const schema = object({
  cards: array().of(object({
    front: string().trim().required().max(maxTextLength.value).label('Front'),
    back: string().trim().required().max(maxTextLength.value).label('Back'),
    hint: string().nullable().max(maxHintLength.value).label('Hint'),
  })).max(maxCards.value),
})
const { values, meta, resetForm, handleSubmit } = useForm({ validationSchema: schema, initialValues: { cards: [] } })
const { fields, push, remove, swap } = useFieldArray('cards')

const loading = ref(true)
const isSaving = ref(false)
const showSavedMsg = ref(false)
const errMsg = ref(null)
const version = ref(0)

const isImported = computed(() => skillsState.skill && skillsState.skill.copiedFromProjectId && skillsState.skill.copiedFromProjectId.length > 0 && !skillsState.skill.reusedSkill)
const isReused = computed(() => skillsState.skill && skillsState.skill.reusedSkill)
const isReadOnly = computed(() => isImported.value || isReused.value)
const atMaxCards = computed(() => fields.value.length >= maxCards.value)
const previewCards = computed(() => (values.cards || []).filter((card) => card?.front?.trim() && card?.back?.trim()))

const toFormValues = (res) => ({
  cards: (res.cards || []).map((card) => ({ front: card.front, back: card.back, hint: card.hint || '' })),
})
const load = () => {
  loading.value = true
  errMsg.value = null
  return FlashCardsService.getSkillFlashCards(projectId, skillId)
    .then((res) => {
      version.value = res.version || 0
      resetForm({ values: toFormValues(res) })
    }).finally(() => {
      loading.value = false
    })
}
onMounted(() => {
  load()
})

const addCard = () => {
  push({ front: '', back: '', hint: '' })
}
const removeCard = (index) => {
  remove(index)
  announcer.polite(`Card ${index + 1} removed`)
}
const moveCard = (index, newIndex) => {
  swap(index, newIndex)
  announcer.polite(`Card ${index + 1} moved to position ${newIndex + 1}`)
}

const save = handleSubmit((formValues) => {
  isSaving.value = true
  errMsg.value = null
  const toSave = {
    version: version.value,
    cards: formValues.cards.map((card) => ({ front: card.front, back: card.back, hint: card.hint?.trim() ? card.hint : null })),
  }
  FlashCardsService.saveSkillFlashCards(projectId, skillId, toSave)
    .then((res) => {
      version.value = res.version
      resetForm({ values: toFormValues(res) })
      showSavedMsg.value = true
      setTimeout(() => {
        showSavedMsg.value = false
      }, 3500)
      announcer.polite('Flash cards were saved')
    }).catch((err) => {
      errMsg.value = err?.response?.data?.explanation || 'Flash cards were not saved'
      announcer.assertive(`Flash cards were not saved. ${errMsg.value}`)
    }).finally(() => {
      isSaving.value = false
    })
})
</script>

<template>
  <div>
    <SubPageHeader title="Configure Flash Cards"/>
    <Card>
      <template #content>
        <skills-spinner v-if="loading" :is-loading="loading" />
        <div v-else class="flex flex-col gap-4">
          <Message v-if="isReadOnly" severity="info" icon="fas fa-exclamation-triangle" data-cy="readOnlyAlert" :closable="false">
            Flash cards of
            <span v-if="isImported"><Tag severity="success"><i class="fas fa-book mr-1" aria-hidden="true"/> Imported</Tag></span>
            <span v-if="isReused"><Tag severity="success"><i class="fas fa-recycle mr-1" aria-hidden="true"/> Reused</Tag></span>
            skills are read-only.
          </Message>
          <div v-if="fields.length === 0" class="text-muted-color" data-cy="noFlashCards">
            No flash cards yet. Trainees review them on the skill page, flipping each card from front to back.
          </div>

          <div v-for="(field, index) in fields"
               :key="field.key"
               class="flex flex-col gap-2 p-3 border border-surface rounded-md"
               :data-cy="`flashCard-${index}`">
            <div class="flex flex-wrap gap-2 items-center">
              <div class="flex-1 font-semibold">Card {{ index + 1 }}</div>
              <SkillsButton v-if="!isReadOnly"
                            icon="fas fa-arrow-up"
                            size="small"
                            outlined
                            :disabled="index === 0"
                            :aria-label="`Move card ${index + 1} up`"
                            @click="moveCard(index, index - 1)"
                            :data-cy="`moveFlashCardUp-${index}`" />
              <SkillsButton v-if="!isReadOnly"
                            icon="fas fa-arrow-down"
                            size="small"
                            outlined
                            :disabled="index === fields.length - 1"
                            :aria-label="`Move card ${index + 1} down`"
                            @click="moveCard(index, index + 1)"
                            :data-cy="`moveFlashCardDown-${index}`" />
              <SkillsButton v-if="!isReadOnly"
                            icon="fas fa-trash"
                            size="small"
                            severity="warn"
                            outlined
                            :aria-label="`Remove card ${index + 1}`"
                            @click="removeCard(index)"
                            :data-cy="`removeFlashCard-${index}`" />
            </div>
            <SkillsTextarea :name="`cards[${index}].front`"
                            label="Front"
                            :is-required="true"
                            rows="2"
                            :submit-on-enter="false"
                            :disabled="isReadOnly"
                            :max-num-chars="maxTextLength"
                            :data-cy="`flashCardFront-${index}`" />
            <SkillsTextarea :name="`cards[${index}].back`"
                            label="Back"
                            :is-required="true"
                            rows="2"
                            :submit-on-enter="false"
                            :disabled="isReadOnly"
                            :max-num-chars="maxTextLength"
                            :data-cy="`flashCardBack-${index}`" />
            <SkillsTextInput :name="`cards[${index}].hint`"
                             label="Hint (optional)"
                             :disabled="isReadOnly"
                             :data-cy="`flashCardHint-${index}`" />
          </div>

          <div v-if="!isReadOnly" class="flex flex-wrap gap-2 items-center">
            <SkillsButton label="Add Card"
                          icon="fas fa-plus-circle"
                          outlined
                          :disabled="atMaxCards"
                          @click="addCard"
                          data-cy="addFlashCardBtn" />
            <span v-if="atMaxCards" class="text-sm" data-cy="maxFlashCardsMsg">A skill can have at most {{ maxCards }} flash cards</span>
          </div>

          <Message v-if="errMsg" severity="error" :closable="false" data-cy="flashCardsSaveError">
            {{ errMsg }}
            <SkillsButton label="Reload"
                          size="small"
                          class="ml-2"
                          icon="fas fa-sync"
                          @click="load"
                          data-cy="reloadFlashCardsBtn" />
          </Message>

          <div v-if="!isReadOnly" class="flex flex-wrap gap-2 items-center">
            <SkillsButton label="Save"
                          icon="fas fa-arrow-circle-right"
                          :disabled="!meta.valid || !meta.dirty || isSaving"
                          :loading="isSaving"
                          @click="save"
                          data-cy="saveFlashCardsBtn" />
            <InlineMessage v-if="meta.dirty" severity="warn" data-cy="unsavedChangesAlert">Unsaved Changes</InlineMessage>
            <InlineMessage v-if="!meta.dirty && showSavedMsg" severity="success" data-cy="savedMsg">Saved</InlineMessage>
          </div>

          <div v-if="previewCards.length > 0" class="flex flex-col gap-2" data-cy="flashCardsPreview">
            <div class="font-semibold">Preview</div>
            <flash-card-deck :cards="previewCards" :deck-id="`preview-${projectId}-${skillId}`" />
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>
