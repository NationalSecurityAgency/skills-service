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
import {useAiModelsState} from "@/common-components/utilities/learning-conent-gen/UseAiModelsState";
import Fieldset from "primevue/fieldset";
import Slider from "primevue/slider";
import {computed, ref} from "vue";

const aiModelsState = useAiModelsState()
const showModelSettings = ref(false)
const failedToLoad = computed(() => aiModelsState.failedToLoad)
const showSelector = computed(() => !aiModelsState.loadingModels && !failedToLoad.value)
</script>

<template>
  <div data-cy="aiModelsSelector">
    <Message v-if="failedToLoad" severity="error" :closable="false" data-cy="failedToLoad">
      <div class="font-semibold">
        Unfortunately, the AI assistant is not available at this time. Please try again later.
      </div>
      <div>
        If the issue persists, please reach out to the support team.
      </div>
    </Message>
    <div v-if="showSelector">
      <div class="flex gap-2 items-center justify-end mb-3">
        <label class="italic" id="selectedModelLabel">AI Model:</label>
        <div class="font-semibold" data-cy="selectedModel" aria-labelledby="selectedModelLabel">{{ aiModelsState.selectedModel?.model }}</div>
        <skills-button icon="fa-solid fa-gear"
                       size="small"
                       aria-label="Configure the model and its attributes"
                       data-cy="modelSettingsButton"
                       @click="showModelSettings = !showModelSettings" />
      </div>
      <div v-if="showModelSettings" class="mb-5">
        <Fieldset legend="Settings" data-cy="modelSettings">
          <div class="flex flex-col gap-5">
            <div class="flex flex-col gap-2">
              <label id="modelLabel">AI Model:</label>
              <Select v-model="aiModelsState.selectedModel"
                      :options="aiModelsState.availableModels"
                      optionLabel="model"
                      aria-labelledby="modelLabel"
                      data-cy="modelSelector"
                      class="w-full"/>
            </div>
            <div class="flex flex-col gap-3 mt-3">
              <div class="flex gap-2 text-grey-700">
                <div><i class="fa-solid fa-robot text-blue-500" aria-hidden="true"></i> Analytical</div>
                <div class="flex-1 text-center"><i class="fa-solid fa-circle-half-stroke text-gray-400" aria-hidden="true"></i> Neutral</div>
                <div><i class="fa-solid fa-palette text-amber-600" aria-hidden="true"></i> Creative</div>
              </div>

              <div class="flex items-center gap-3">
                <div class="flex-1">
                  <Slider v-model="aiModelsState.modelTemperature"
                          :min="0"
                          :max="1"
                          :step="0.01"
                          :style="{ height: '5px' }"
                          data-cy="modelTempSlider" aria-label="configure model's temperature"/>
                </div>
              </div>
            </div>
          </div>
        </Fieldset>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>