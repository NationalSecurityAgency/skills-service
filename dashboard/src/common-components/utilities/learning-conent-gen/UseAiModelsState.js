/*
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
import {defineStore} from "pinia";
import {ref, watch} from "vue";
import {useOpenaiService} from "@/common-components/utilities/learning-conent-gen/UseOpenaiService.js";
import {useStorage} from "@vueuse/core";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

export const useAiModelsState = defineStore('aiModelsStore', () => {

    const appConfig = useAppConfig()

    const availableModels = ref([])
    const loadingModels = ref(true)
    const selectedModel = ref(null)
    const failedToLoad = ref(false)
    const modelTemperature = ref(appConfig.openaiModelDefaultTemperature)

    const openaiService = useOpenaiService()
    const selectedModelLocalStorage = useStorage('selectedAiModel', null)
    const modelTemperatureStorage = useStorage('selectedAiModelTemperature', null)

    watch(selectedModel, (newVal, oldValue) => {
        if (oldValue !== undefined) {
            selectedModelLocalStorage.value = newVal.model
        }
    })
    watch(modelTemperature, (newVal, oldValue) => {
        if (oldValue !== undefined) {
            modelTemperatureStorage.value = newVal
        }
    })

    const loadModels = () => {
        loadingModels.value = true
        failedToLoad.value = false
        return openaiService.getAvailableModels()
            .then((data) => {
                const defaultModel = data.models.find((model) => model.model === appConfig.openaiDefaultModel) ?? data.models[0]
                selectedModel.value = data.models.find((model) => model.model === selectedModelLocalStorage.value) ?? defaultModel
                availableModels.value = data.models
                if (modelTemperatureStorage.value !== null) {
                    modelTemperature.value = Number(modelTemperatureStorage.value)
                }
            }).finally(() => {
                loadingModels.value = false
            }).catch(() => {
                loadingModels.value = false
                failedToLoad.value = true
            })
    }

    const afterModelsLoaded = () => {
        return new Promise((resolve) => {
            (function waitForLoading() {
                if (!loadingModels.value) return resolve(availableModels.value);
                setTimeout(waitForLoading, 100);
                return loadingModels.value;
            }());
        });
    }

    return {
        availableModels,
        loadingModels,
        selectedModel,
        modelTemperature,
        loadModels,
        failedToLoad,
        afterModelsLoaded
    }
})