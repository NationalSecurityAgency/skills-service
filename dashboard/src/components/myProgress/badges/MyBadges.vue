/*
Copyright 2024 SkillTree

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
import MyProgressService from '@/components/myProgress/MyProgressService.js'
import { computed, onMounted, ref } from 'vue'
import MyBadgesDetails from '@/skills-display/components/badges/MyBadgesDetails.vue'
import BadgesCatalog from '@/skills-display/components/badges/BadgesCatalog.vue'
import MyProgressTitle from '@/components/myProgress/MyProgressTitle.vue'
import IconManagerService from '@/components/utils/iconPicker/IconManagerService.js'

const loading = ref(true)
const badges = ref([])

onMounted(() => {
  loadBadges()
})

const unachievedBadges = computed(() => badges.value.filter((badge) => badge.badgeAchieved === false))
const achievedBadges = computed(() => badges.value.filter((badge) => badge.badgeAchieved === true))


const loadBadges = () => {
  loading.value = true
  MyProgressService.loadMyBadges().then((res) => {
    badges.value = res
    const filterWithCustomIcons = (badge) => badge.iconClass &&
      (
        (badge.projectId && badge.iconClass.startsWith(`${badge.projectId}-`)) ||
        (!badge.projectId && badge.iconClass.startsWith(`${badge.badgeId}-`))
      )
    const projectIds = res.filter(filterWithCustomIcons).filter(badge => badge.projectId).map((badge) => badge.projectId)
    const globalBadgeIds = res.filter(filterWithCustomIcons).filter(badge => !badge.projectId).map((badge) => badge.badgeId)
    const refreshProjectIcons = [...new Set(projectIds)].map((projId) => {
      return IconManagerService.refreshCustomIconCss(projId, null)
    })
    const refreshGlobalBadgeIcons = [...new Set(globalBadgeIds)].map((badgeId) => {
      return IconManagerService.refreshCustomIconCss(null, badgeId)
    })
    return Promise.all([...refreshProjectIcons, ...refreshGlobalBadgeIcons])
  }).finally(() => {
    loading.value = false
  })
}
</script>

<template>
<div>
  <my-progress-title title="My Badges" />

  <my-badges-details
    data-cy="achievedBadges"
    :badges="achievedBadges"
    class="mt-4"
  />

  <badges-catalog class="mt-4"
                  :badges="unachievedBadges"
                  data-cy="availableBadges">
  </badges-catalog>
</div>
</template>

<style scoped>

</style>