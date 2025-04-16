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
import { computed } from 'vue'
import dayjs from 'dayjs'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import PlacementBadge from '@/skills-display/components/badges/PlacementBadge.vue'
import BadgeHeaderIcons from '@/skills-display/components/badges/BadgeHeaderIcons.vue'
import ExtraBadgeAward from '@/skills-display/components/badges/ExtraBadgeAward.vue'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'

const props = defineProps({
  badge: {
    type: Object
  },
  iconColor: {
    type: String,
    default: 'text-cyan-300'
  },
  displayProjectName: {
    type: Boolean,
    required: false,
    default: false
  },
  viewDetailsBtnTo: {
    type: Object,
    default: null
  },
  searchString: {
    type: String,
    default: ''
  },
})

const timeUtils = useTimeUtils()
const iconCss = computed(() => `${props.badge.iconClass} ${props.iconColor}`)
const percent = computed(() => {
  if (props.badge.numTotalSkills === 0) {
    return 0
  }
  return Math.trunc((props.badge.numSkillsAchieved / props.badge.numTotalSkills) * 100)
})

const showHeader = computed(() => props.badge.gem || props.badge.global)
const iconCardPt = computed(() => {
  return {
    root: { class: '!border' },
    content:
      {
        class: showHeader.value ? 'p-0' : ''
      }
  }
})

const currentTime = computed(() => {
  return dayjs().utc().valueOf()
})

const positionNames = ['first', 'second', 'third']
const showBadgeBonusDetails = computed(() => {
  return (props.badge && !props.badge.global) && (bonusAwardAchieved.value || bonusAwardTimerActive.value || props.badge.numberOfUsersAchieved >= 0)
})
const bonusAwardAchieved = computed(() => {
  return props.badge.badgeAchieved && props.badge.achievedWithinExpiration
})
const bonusAwardTimerActive = computed(() => {
  return props.badge.firstPerformedSkill && !props.badge.badgeAchieved && !props.badge.hasExpired && props.badge.expirationDate && currentTime.value
})
const achievementOrder = computed(() => {
  return props.badge.achievementPosition > 0 && props.badge.achievementPosition < 4 ? positionNames[props.badge.achievementPosition - 1] : ''
})
const usersAchieved = computed(() => {
  return props.badge.numberOfUsersAchieved === 1 ? 'person has' : 'people have'
})
const otherUsersAchieved = computed(() => {
  return (props.badge.numberOfUsersAchieved - 1) === 1 ? 'person has' : 'people have'
})
</script>

<template>
  <div  :data-cy="`badge_${badge.badgeId}`" class="badge-catalog-item">
    <div class="md:flex gap-4">
      <Card class="w-min-10rem mb-4 md:mb-0" :pt="iconCardPt" :data-cy="`badge_${badge.badgeId}`">
        <template #header v-if="showHeader">
          <div class="pt-2 px-2">
            <badge-header-icons :badge="badge" />
          </div>
        </template>
        <template #content>
          <div class="text-center">
            <i :class="iconCss" style="font-size: 5rem;" />
            <placement-badge :badge="badge" class="mt-2" />
            <div v-if="badge.gem" class="text-muted text-orange-800" :data-cy="`badge_${badge.badgeId}_gem`">
              <small aria-label="`This is a gem badge and it ${timeUtils.isInThePast(badge.endDate) ? 'expired' : 'expires'} ${timeUtils.relativeTime(badge.endDate)}`">{{timeUtils.isInThePast(badge.endDate) ? 'Expired' : 'Expires'}} {{ timeUtils.relativeTime(badge.endDate) }}</small>
            </div>
            <div v-if="badge.global" class="text-muted">
              <small><b>Global Badge</b></small>
            </div>
            <div v-else-if="displayProjectName" class="text-muted text-center text-truncate" data-cy="badgeProjectName">
              <small>Proj<span class="d-md-none d-xl-inline">ect</span>: {{ badge.projectName }}</small>
            </div>

            <extra-badge-award v-if="badge.achievedWithinExpiration"
                               :icon-class="badge.awardAttrs.iconClass"
                               :name="badge.awardAttrs.name"
                               class="my-4"/>
          </div>
        </template>
      </Card>
      <div class="flex-1">
        <div class="flex content-end">
          <div class="flex-1 text-2xl font-medium" data-cy="badgeTitle">
            <div v-if="badge.projectName" class="text-muted-color text-base" data-cy="badgeProjectName">
              <span class="italic">Project:</span> {{ badge.projectName}}
            </div>
            <highlighted-value :value="badge.badge" :filter="searchString" />
          </div>
          <div class="content-end">
            <div class="float-right text-navy" :class="{ 'text-success': percent === 100 }" data-cy="badgePercentCompleted">
              <i v-if="percent === 100" class="fa fa-check" /> {{ percent }}% Complete
            </div>
          </div>
        </div>

        <vertical-progress-bar :total-progress="percent" :bar-size="6" class="mt-1" />

        <div v-if="showBadgeBonusDetails" style="font-size: 1.2em;">
          <Message v-if="badge.numberOfUsersAchieved > 0"
                   :closable="false"
                   icon="fas fa-trophy">
            <span v-if="!badge.badgeAchieved">{{ badge.numberOfUsersAchieved }} {{ usersAchieved }} achieved this badge so far - <span
              class="time-style">you could be next!</span></span>
            <span
              v-else-if="badge.badgeAchieved && badge.numberOfUsersAchieved > 1">{{ badge.numberOfUsersAchieved - 1 }} other {{ otherUsersAchieved
              }} achieved this badge so far</span>
            <span v-else>You've achieved this badge</span>
            <span v-if="achievementOrder !== ''"> - <span
              class="time-style">and you were the {{ achievementOrder }}!</span></span>
          </Message>
          <Message v-else-if="badge.numberOfUsersAchieved === 0"
                   :closable="false"
                   icon="fas fa-car-side">
            No one
            has achieved this badge yet - <span class="font-bold">you could be the first!</span>
          </Message>

          <Message v-if="bonusAwardTimerActive"
                   :closable="false"
                   data-cy="achieveThisBadgeMsg"
                   icon="fas fa-clock">
            Achieve this badge in
            <span class="time-style">
                          {{ timeUtils.formatDurationDiff(currentTime, badge.expirationDate, true) }}
                        </span>
            for the <i :class="badge.awardAttrs.iconClass"></i> <span class="time-style">{{ badge.awardAttrs.name
            }}</span> bonus!
          </Message>

          <Message v-if="bonusAwardAchieved"
               :closable="false"
               icon="fas fa-clock">
            <i :class="badge.awardAttrs.iconClass" class="award-info-icon"></i>You've earned the <span
            class="time-style">{{ badge.awardAttrs.name }}</span> bonus!
          </Message>
        </div>

        <p v-if="badge && badge.description">
          <markdown-text :text="badge.description" :instance-id="badge.badgeId" />
        </p>

        <div v-if="viewDetailsBtnTo" class="text-center md:text-left mt-4">
          <router-link
            :to="viewDetailsBtnTo"
            class="skills-theme-btn"
            :data-cy="`badgeDetailsLink_${badge.badgeId}`">
            <Button
              label="View Details"
              icon="fas fa-eye"
              outlined
              class="w-full md:w-auto"
              size="small"
            />
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>