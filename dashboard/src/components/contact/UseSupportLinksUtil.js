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
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {computed, ref} from "vue";
import {useRouter} from "vue-router";
import {usePagePath} from "@/components/utils/UsePageLocation.js";
import {defineStore} from "pinia";
import {useMyProgressState} from "@/stores/UseMyProgressState.js";
import {useAppInfoState} from "@/stores/UseAppInfoState.js";

export const useSupportLinksUtil = defineStore('supportLinksStore', () => {

    const appConfig = useAppConfig()
    const router = useRouter()
    const pagePath = usePagePath()
    const myProgressState = useMyProgressState()
    const appInfoState = useAppInfoState()

    const showContactProjectAdminsDialog = ref(false)
    const showContactDialog = () => {
        if (!appInfoState.emailEnabled) {
            router.push('/support')
            return
        }

        if (pagePath.isOnProgressAndRankingHomePage.value) {
            myProgressState.afterMyProjectsLoaded().then((myProjects) => {
                if (myProjects && myProjects.length > 0) {
                    showContactProjectAdminsDialog.value = true
                } else {
                    router.push('/support')
                }
            })
        } else if (pagePath.isProgressAndRankingPage.value) {
            showContactProjectAdminsDialog.value = true
        } else {
            router.push('/support')
        }
    }

    const supportLinks = computed(() => {
        const res = []
        if (appConfig.contactSupportEnabled) {
            res.push({
                label: 'Contact',
                icon: 'fa-solid fa-headset',
                command: () => {
                    showContactDialog()
                }
            })
        }

        const keyLookup = 'supportLink'
        const configs = appConfig.getConfigsThatStartsWith(keyLookup)
        const dupKeys = Object.keys(configs).map((conf) => conf.substring(0, 12))
        const keys = dupKeys.filter((v, i, a) => a.indexOf(v) === i)
        if (keys && keys.length > 0) {
            keys.forEach((key) => {
                res.push({
                    label: configs[`${key}Label`],
                    icon: configs[`${key}Icon`],
                    url: configs[key]
                })
            })
        }
        return res
    })

    return {
        supportLinks,
        showContactProjectAdminsDialog
    }
})