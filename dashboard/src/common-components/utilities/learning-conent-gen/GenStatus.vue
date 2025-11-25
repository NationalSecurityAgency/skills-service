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
import {ref, onMounted, onBeforeUnmount, computed} from 'vue'
import ThinkingIndicator from "@/common-components/utilities/learning-conent-gen/ThinkingIndicator.vue";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useLog} from "@/components/utils/misc/useLog.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const props = defineProps({
  id: String,
  welcomeMsg: {
    type: String,
    default: 'Got it! I\'ll get started right away!'
  },
  isGenerating: {
    type: Boolean,
    default: false
  },
  isGenerateValueEmpty: {
    type: Boolean,
    default: true
  }
})

const log = useLog()
const appConfig = useAppConfig()

const statusMessages = computed(() => appConfig.openaiTakingLongerThanExpectedMessages)
const thinkingWords = [
  'Putting it together...',
  'Crunching data...',
  'Working...',
  'Processing...',
  'Thinking...'
]
const currentThinkingWord = ref('Thinking...')
const getThinkingWord = (index) => {
  return thinkingWords[Math.abs(index) % thinkingWords.length]
}

const statusMsg = ref(null)
const checkThatProgressWasMade = () => {
  const timeoutMs = appConfig.openaiTakingLongerThanExpectedTimeoutPerMsg;
  const maxAttempts = statusMessages.value.length
  let numAttempts = 0;

  const checkProgress = () => {
    setTimeout(() => {
      if (props.isGenerating && numAttempts < maxAttempts && props.isGenerateValueEmpty) {
        statusMsg.value = statusMessages.value[numAttempts]
        currentThinkingWord.value = getThinkingWord(numAttempts)
        numAttempts += 1
        log.debug(`GenerateDescriptionDialog: Checking progress attempt=[${numAttempts}]`)
        checkProgress()
      }
    }, timeoutMs)
  }
  checkProgress()
}

onMounted(() => {
  checkThatProgressWasMade()
})

</script>

<template>
  <div class="mt-2 text-gray-900">
    <div>
      <markdown-text :text="welcomeMsg" :instanceId="id"/>
      <div v-if="isGenerateValueEmpty">
        <transition
            name="fade"
            mode="out-in"
            enter-active-class="transition-opacity duration-500 ease-out"
            leave-active-class="transition-opacity duration-500 ease-in"
            enter-from-class="opacity-0"
            enter-to-class="opacity-100"
            leave-from-class="opacity-100"
            leave-to-class="opacity-0"
        >
          <div class="flex gap-2">
            <div v-if="statusMsg" :key="statusMsg" class="flex items-center min-h-6">
              {{ statusMsg }}
            </div>
            <thinking-indicator v-if="isGenerating" :value="currentThinkingWord"/>
          </div>
        </transition>
      </div>

    </div>
  </div>
</template>

<style scoped>
</style>
