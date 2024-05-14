<script setup>
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import PlacementBadge from '@/skills-display/components/badges/PlacementBadge.vue'
import { computed } from 'vue'
import BadgeHeaderIcons from '@/skills-display/components/badges/BadgeHeaderIcons.vue'

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
</script>

<template>
  <Card class="card" data-cy="myBadges">
    <template #header>
      <div class="flex p-3">
        <div class="flex-1 text-xl uppercase">
          My Earned Badges
        </div>
        <div v-if="badges && badges.length > 0" class="text-muted float-right">
          <Tag severity="info">{{ badges.length }}</Tag> Badge<span v-if="badges.length !== 1">s</span> Earned
        </div>
      </div>
    </template>
    <template #content>
      <no-content2 v-if="!badges || badges.length === 0" title="No badges earned yet."
                   message="Take a peak at the catalog below to get started!" />

      <div v-if="badges && badges.length > 0" class="flex justify-content-left">
        <div v-for="(badge, index) in badges" v-bind:key="badge.badgeId" class="w-full md:w-auto skills-earned-badge">
          <Card class="skills-card-theme-border" :pt="{ content: { class: 'py-0' } }">
            <template #header>
              <div class="pt-3 px-3 flex">
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
              <!--                :style="{ 'margin-top': !badge.achievedWithinExpiration ? '30px' : '' }"-->
              <div class="earned-badge text-center">
                <i :class="`${badge.iconClass} ${colors.getTextClass(index)}`" style="font-size: 5em;" />
                <div class="mb-0 font-bold text-xl"
                     data-cy="badgeName"
                      :aria-label="badgeAriaLabel(badge)">
                  {{ badge.badge }}
                </div>
                <div v-if="displayBadgeProject && badge.projectName" class="text-muted text-center text-truncate"
                     data-cy="badgeProjectName">
                  <small>Proj<span class="d-md-none d-xl-inline">ect</span>: {{ badge.projectName }}</small>
                </div>
                <div data-cy="dateBadgeAchieved" class="text-muted mb-2">
                  <i class="far fa-clock text-secondary" aria-hidden="true"></i>
                  {{ timeUtils.relativeTime(badge.dateAchieved) }}
                </div>

                <div v-if="badge.achievedWithinExpiration" class="bonus-award mt-2 border-top">
                  <div class="award-icon"><i :class="badge.awardAttrs.iconClass + ' skills-color-orange'"></i></div>
                  <span class="sr-only">You got the </span>
                  <div style="font-size: .4em;">{{ badge.awardAttrs.name }}</div>
                  <span class="sr-only"> bonus</span>
                </div>
              </div>
            </template>
            <template #footer>
              <router-link
                :to="skillsDisplayInfo.createToBadgeLink(badge)">
                <Button
                  label="View"
                  icon="far fa-eye"
                  :data-cy="`earnedBadgeLink_${badge.badgeId}`"
                  outlined class="w-full" size="small" />
              </router-link>
            </template>
          </Card>
        </div>
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