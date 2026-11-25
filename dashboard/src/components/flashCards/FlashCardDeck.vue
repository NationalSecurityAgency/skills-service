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
import { computed, ref, watch } from 'vue'

const props = defineProps({
  cards: {
    type: Array,
    required: true,
  },
  deckId: {
    type: String,
    required: true,
  },
})

const index = ref(0)
const showBack = ref(false)
const showHint = ref(false)

const numCards = computed(() => props.cards.length)
const card = computed(() => props.cards[Math.min(index.value, numCards.value - 1)])
const cardText = computed(() => (showBack.value ? card.value?.back : card.value?.front))
const sideLabel = computed(() => (showBack.value ? 'Back' : 'Front'))

watch(numCards, (newNum) => {
  if (index.value >= newNum) {
    index.value = Math.max(newNum - 1, 0)
  }
})

const goTo = (newIndex) => {
  if (newIndex < 0 || newIndex >= numCards.value) {
    return
  }
  index.value = newIndex
  showBack.value = false
  showHint.value = false
}
const flip = () => {
  showBack.value = !showBack.value
}
const onKeydown = (event) => {
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    goTo(index.value - 1)
  } else if (event.key === 'ArrowRight') {
    event.preventDefault()
    goTo(index.value + 1)
  }
}
</script>

<template>
  <div v-if="card" class="flex flex-col gap-2" :data-cy="`flashCardDeck-${deckId}`" @keydown="onKeydown">
    <button type="button"
            class="w-full min-h-40 p-6 border border-surface rounded-md bg-surface-0 dark:bg-surface-900 text-center cursor-pointer"
            :class="{ 'border-primary': showBack }"
            :aria-label="`Card ${index + 1} of ${numCards}, ${sideLabel}: ${cardText}. Press to flip`"
            @click="flip"
            data-cy="flashCard">
      <span class="block text-sm text-muted-color mb-2" data-cy="flashCardSide" aria-hidden="true">{{ sideLabel }}</span>
      <span class="block text-lg whitespace-pre-wrap break-words" data-cy="flashCardText" aria-hidden="true">{{ cardText }}</span>
    </button>
    <div v-if="card.hint" class="flex flex-col gap-1">
      <div>
        <Button v-if="!showHint"
                link
                size="small"
                class="p-0"
                label="Show Hint"
                icon="fas fa-lightbulb"
                :aria-label="`Show hint for card ${index + 1}`"
                @click="showHint = true"
                data-cy="showFlashCardHintBtn" />
      </div>
      <div v-if="showHint" class="text-sm whitespace-pre-wrap break-words" data-cy="flashCardHint">
        <i class="fas fa-lightbulb mr-1" aria-hidden="true" />{{ card.hint }}
      </div>
    </div>
    <div class="flex flex-wrap gap-2 items-center justify-between">
      <Button outlined
              size="small"
              icon="fas fa-arrow-left"
              label="Previous"
              :disabled="index === 0"
              @click="goTo(index - 1)"
              data-cy="prevFlashCardBtn" />
      <span data-cy="flashCardPosition">Card {{ index + 1 }} of {{ numCards }}</span>
      <Button outlined
              size="small"
              icon="fas fa-arrow-right"
              iconPos="right"
              label="Next"
              :disabled="index === numCards - 1"
              @click="goTo(index + 1)"
              data-cy="nextFlashCardBtn" />
    </div>
    <span class="sr-only" aria-live="polite" aria-atomic="true">Card {{ index + 1 }} of {{ numCards }}, {{ sideLabel }}: {{ cardText }}</span>
  </div>
</template>
