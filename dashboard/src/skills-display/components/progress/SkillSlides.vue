/*
Copyright 2025 SkillTree

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
import {computed, ref} from "vue";
import SlideDeck from "@/components/slides/SlideDeck.vue";
import {useStorage} from "@vueuse/core";
import {useElementSizeUtil} from "@/common-components/utilities/UseElementSizeUtil.js";

const props = defineProps({
  skill: Object,
})

const slidesContainer = ref(null)
const slidesContainerSize = useElementSizeUtil(slidesContainer)

const url = computed(() => props.skill.slidesSummary?.url)
const hasSlides = computed(() => url.value != null)

const slidesId = computed(() => `${props.skill.projectId}-${props.skill.skillId}-slides`)

const widthInLocalStorageAsString = useStorage(`${slidesId.value}-slidesWidth`, null)
const widthInLocalStorage = computed(() => widthInLocalStorageAsString.value ? parseInt(widthInLocalStorageAsString.value) : null)
const defaultWidth = computed(() => widthInLocalStorage.value || props.skill.slidesSummary?.width)

const onResize = (newWidth) => {
  widthInLocalStorageAsString.value = newWidth
}
</script>

<template>
  <div ref="slidesContainer" v-if="hasSlides">
    <slide-deck
        :slides-id="slidesId"
        :pdf-url="url"
        :default-width="defaultWidth"
        :max-width="slidesContainerSize.width.value"
        @on-resize="onResize"
    />
  </div>

</template>

<style scoped>

</style>