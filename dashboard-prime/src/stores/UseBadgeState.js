import { ref } from 'vue'
import { defineStore } from 'pinia'
import BadgesService from "@/components/badges/BadgesService.js";
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";

export const useBadgeState = defineStore('badgeState', () => {
    const badge = ref({})

    function loadBadgeDetailsState(projectId, badgeId) {
        return new Promise((resolve, reject) => {
            BadgesService.getBadge(projectId, badgeId)
                .then((response) => {
                    badge.value = response;
                    resolve(response)
                })
                .catch((error) => reject(error))
        })
    }

    function loadGlobalBadgeDetailsState(badgeId) {
        return new Promise((resolve, reject) => {
            GlobalBadgeService.getBadge(badgeId)
                .then((response) => {
                    badge.value = response;
                    resolve(response)
                })
                .catch((error) => reject(error))
        })
    }

    return {
        badge,
        loadBadgeDetailsState,
        loadGlobalBadgeDetailsState,
    }

})