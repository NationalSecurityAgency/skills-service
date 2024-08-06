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
import { computed, onMounted, ref } from 'vue'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import MyBadgesDetails from '@/skills-display/components/badges/MyBadgesDetails.vue'
import BadgesCatalog from '@/skills-display/components/badges/BadgesCatalog.vue'

const skillsDisplayService = useSkillsDisplayService()

const loading = ref(true)
const badges = ref([])
const unachievedBadges = computed(() => badges.value.filter((badge) => badge.badgeAchieved === false))

const achievedBadges = computed(() => badges.value.filter((badge) => badge.badgeAchieved === true))

onMounted(() => {
  loadBadges()
})
const loadBadges = () => {
  loading.value = true
  skillsDisplayService.getBadgeSummaries().then((res) => {
    badges.value = res
  }).finally(() => {
    loading.value = false
  })
}

</script>

<template>
  <div>
    <skills-spinner :is-loading="loading" class="mt-8" />

    <div v-if="!loading">
      <skills-title>My Badges</skills-title>

<!--      :badgeRouterLinkGenerator="genLink"-->
      <my-badges-details
        data-cy="achievedBadges"
        :badges="achievedBadges"
        class="mt-3"
        />

      <!--      :badgeRouterLinkGenerator="genLink"-->
      <!--      :noBadgesMessage="noCatalogMsg"-->
      <badges-catalog class="mt-3"
                      :badges="unachievedBadges"
                      data-cy="availableBadges">
      </badges-catalog>
    </div>
  </div>
</template>

<style scoped>

</style>