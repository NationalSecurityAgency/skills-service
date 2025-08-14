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
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import PlacementBadge from '@/skills-display/components/badges/PlacementBadge.vue'
import { computed } from 'vue'
import BadgeHeaderIcons from '@/skills-display/components/badges/BadgeHeaderIcons.vue'
import ExtraBadgeAward from '@/skills-display/components/badges/ExtraBadgeAward.vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  badges: {
    type: Array,
    required: true
  },
  displayBadgeProject: {
    type: Boolean,
    required: false,
    default: false
  }
})
const skillsDisplayInfo = useSkillsDisplayInfo()
const colors = useColors()
const timeUtils = useTimeUtils()
const route = useRoute()

const badgeAriaLabel = (badge) => {
  let res = `You earned badge ${badge.badge}.`
  if (badge.global) {
    res += ' This is a global badge.'
  }
  if (badge.gem) {
    res += ' This is a gem badge.'
  }
  return res
}

const buildBadgeLink = (badge) => {
  let globalBadgeUnderProjectId = null
  if (!route.params.projectId) {
    const hasData = badge.projectLevelsAndSkillsSummaries && badge.projectLevelsAndSkillsSummaries.length > 0
    if (!hasData) {
      throw new Error(`Expected [${badge.badgeId}] to be a global badge with data in projectLevelsAndSkillsSummaries variable`)
    }
    globalBadgeUnderProjectId = badge.projectLevelsAndSkillsSummaries[0].projectId
  }
  return skillsDisplayInfo.createToBadgeLink(badge, globalBadgeUnderProjectId)
}
</script>

<template>
  <Card class="card" data-cy="myBadges">
    <template #header>
      <div class="flex p-4">
        <h2 class="flex-1 text-xl uppercase">My Earned Badges</h2>
        <div v-if="badges && badges.length > 0" class="text-muted float-right">
          <Tag severity="info">{{ badges.length }}</Tag> Badge<span v-if="badges.length !== 1">s</span> Earned
        </div>
      </div>
    </template>
    <template #content>
      <no-content2 v-if="!badges || badges.length === 0" title="No badges earned yet."
                   message="Take a peak at the catalog below to get started!" />

      <div v-if="badges && badges.length > 0" class="flex-col md:flex-row flex flex-wrap justify-center gap-4 ">
          <Card class="skills-card-theme-border skills-earned-badge"
                v-for="(badge, index) in badges" v-bind:key="badge.badgeId"
                :pt="{ root: { class: 'border!' }, content: { class: 'h-full!' }, body: { class: 'h-full!' } }" :data-cy="`achievedBadge-${badge.badgeId}`">
            <template #header>
              <div class="pt-4 px-4 flex">
                <div class="flex-1">
                  <i class="fa fa-check-circle position-absolute text-success"
                     v-if="badge.achievementPosition > 3"
                     style="right: 10px; top: 10px;" />
                  <badge-header-icons :badge="badge"/>
                </div>
                <placement-badge :badge="badge"  />
              </div>
            </template>
            <template #content>
              <div class="earned-badge text-center flex flex-col h-full pb-4">
                <div class="flex-1">
                  <i :class="`${badge.iconClass} ${colors.getTextClass(index)}`" style="font-size: 4em;" />
                  <div class="mb-0 font-bold text-xl"
                       data-cy="badgeName"
                        :aria-label="badgeAriaLabel(badge)">
                    {{ badge.badge }}
                  </div>
                  <div v-if="badge.projectName" class="text-center text-muted-color mb-2"
                       data-cy="badgeProjectName">
                    <small>Project: {{ badge.projectName }}</small>
                  </div>
                  <div data-cy="dateBadgeAchieved" class="text-muted mb-2">
                    <i class="far fa-clock text-secondary" aria-hidden="true"></i>
                    {{ timeUtils.relativeTime(badge.dateAchieved) }}
                  </div>

                  <extra-badge-award v-if="badge.achievedWithinExpiration"
                                     :icon-class="badge.awardAttrs.iconClass"
                                     :name="badge.awardAttrs.name"
                                      class="my-4"/>
                </div>
                <div>
                  <router-link
                    :to="buildBadgeLink(badge)">
                    <Button
                      label="View"
                      icon="far fa-eye"
                      :data-cy="`earnedBadgeLink_${badge.badgeId}`"
                      outlined class="w-full" size="small" />
                  </router-link>
                </div>
              </div>
            </template>
          </Card>
      </div>
    </template>
  </Card>
</template>

<style scoped>
@media only screen and (min-width: 740px) {
  .skills-earned-badge {
    min-width: 18rem !important;
  }
}
</style>