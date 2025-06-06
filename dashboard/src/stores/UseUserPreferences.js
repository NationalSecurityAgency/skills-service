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
import {ref} from "vue";
import SettingsService from "@/components/settings/SettingsService.js";

export const useUserPreferences = defineStore('userPreferences', () => {
    const userPreferences = ref(null)
    const loadingUserPreferences = ref(true)

    const loadUserPreferences = () => {
        loadingUserPreferences.value = true
        return SettingsService.getUserSettings().then((response) => {
            if (response) {
                userPreferences.value = response.reduce((map, obj) => {
                    // eslint-disable-next-line no-param-reassign
                    map[obj.setting] = obj.value
                    return map
                }, {})
            }
            return userPreferences.value
        }).finally(() => {
            loadingUserPreferences.value = false
        })
    }

    function afterUserPreferencesLoaded() {
        return new Promise((resolve) => {
            (function waitForUserPreferences() {
                if (!loadingUserPreferences.value) return resolve(userPreferences.value)
                setTimeout(waitForUserPreferences, 100)
                return userPreferences.value
            }())
        })
    }

    return {
        loadUserPreferences,
        loadingUserPreferences,
        afterUserPreferencesLoaded
    }
})