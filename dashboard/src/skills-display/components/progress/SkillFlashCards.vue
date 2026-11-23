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
import { computed } from 'vue'
import FlashCardDeck from '@/components/flashCards/FlashCardDeck.vue'

const props = defineProps({
  skill: Object,
})

const cards = computed(() => props.skill?.flashCards || [])
const deckId = computed(() => `${props.skill.projectId}-${props.skill.skillId}`)
</script>

<template>
  <div v-if="cards.length > 0" class="flex flex-col gap-2" data-cy="skillFlashCards">
    <div class="font-semibold"><i class="fas fa-clone mr-1" aria-hidden="true" />Flash Cards</div>
    <flash-card-deck :cards="cards" :deck-id="deckId" />
  </div>
</template>
