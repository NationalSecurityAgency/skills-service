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
import {computed, ref} from 'vue'
import {defineStore} from 'pinia'
import AccessService from '@/components/access/AccessService.js'
import {useRoute, useRouter} from "vue-router";
import {useAuthState} from "@/stores/UseAuthState.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {useLog} from "@/components/utils/misc/useLog.js";
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";
import {useSkillsDisplayInfo} from "@/skills-display/UseSkillsDisplayInfo.js";

export const useMatomoSupport = defineStore('matomoSupport', () => {

    const router = useRouter()
    const authState = useAuthState()
    const appConfig = useAppConfig()
    const skillsDisplayAttributesState = useSkillsDisplayAttributesState()
    const skillDisplayInfo = useSkillsDisplayInfo()
    const log = useLog()

    const isEnabled = computed(() => appConfig.matomoUrl && appConfig.matomoUrl.length > 0)

    const initialized = ref(false)
    const isSkillsClient = ref(false)
    const skillsClientProjectId = ref(null)

    const userId = computed(() => {
        const userId = appConfig.isPkiAuthenticated ? authState.userInfo.dn : authState.userInfo.userId
        if (appConfig.matomoProcessUserIdRegex) {
            const regex = new RegExp(appConfig.matomoProcessUserIdRegex)
            log.info(`Matomo support: processing userId [${userId}] with regex [${regex}]`)
            const match = userId.match(regex);
            if (match && match[1]) {
                return match[1]; // Return the first capture group
            } else if (match) {
                return match[0]; // If no capture groups, return the full match
            }
        }
        return userId
    })

    const buildLink = (path) => {
        if (log.isTraceEnabled()) {
            log.trace(`UseMatomoSupport.buildLink: isSkillsClient: ${isSkillsClient.value}, skillsClientProjectId: ${skillsClientProjectId.value}`)
        }
        if (isSkillsClient.value) {
            return `/skills-client/${skillsClientProjectId.value}${path}`
        }

        return path
    }

    const init = (conf = {}) => {
        if (isEnabled.value && !initialized.value) {
            if (appConfig.matomoSiteId === null || appConfig.matomoSiteId === undefined) {
                throw new Error('matomoSiteId is required when matomoUrl is set')
            }
            initialized.value = true
            if (conf.isSkillsClient) {
                if (!conf.projectId || conf.projectId.length === 0) {
                    throw new Error(`projectId is required when isSkillsClient is set to true, conf=conf=${JSON.stringify(conf)}`)
                }
                isSkillsClient.value = true
                skillsClientProjectId.value = conf.projectId
            }
            if (log.isTraceEnabled()) {
                log.trace(`Matomo support enabled with matomoUrl=${appConfig.matomoUrl} and matomoSiteId=${appConfig.matomoSiteId}, conf=${JSON.stringify(conf)}`)
            }
            const _paq = window._paq = window._paq || [];
            _paq.push(['trackPageView']);
            _paq.push(['enableLinkTracking']);
            (function () {
                const url = appConfig.matomoUrl
                _paq.push(['setTrackerUrl', `${url}/matomo.php`]);
                _paq.push(['setSiteId', appConfig.matomoSiteId]);

                _paq.push(['setUserId', userId.value]);
                const d = document, g = d.createElement('script'), s = d.getElementsByTagName('script')[0];
                g.type = 'text/javascript';
                g.async = true;
                g.src = `${url}/matomo.js`;
                s.parentNode.insertBefore(g, s);
            })();

            router.afterEach((to, from) => {
                const linkToReport = buildLink(to.path)
                if (log.isTraceEnabled()) {
                    log.trace(`UseMatomoSupport.js.router.afterEach: Navigated from ${from.path} to ${to.path}, linkToReport=${linkToReport}`)
                }
                const _paq = window._paq = window._paq || [];
                _paq.push(['trackLink', linkToReport, 'link']);
            })
        } else {
            log.debug('Matomo support disabled')
        }
    }

    return {
        init,
    }
})