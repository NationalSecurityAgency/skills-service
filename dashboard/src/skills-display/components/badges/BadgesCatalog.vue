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
import { computed, ref } from 'vue'
import BadgeCatalogItem from '@/skills-display/components/badges/BadgeCatalogItem.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import BadgeTypeFilter from '@/skills-display/components/badges/BadgeTypeFilter.vue'
import { useRoute } from 'vue-router'


// badgeRouterLinkGenerator: {
//   type: Function,
//     required: true,
// },
const props = defineProps({
  badges: {
    type: Array,
    required: true,
  },
  noBadgesMessage: {
    type: String,
    required: false,
    default: 'No Badges left to earn!',
  },
  displayBadgeProject: {
    type: Boolean,
    required: false,
    default: false,
  },
})

const route = useRoute()

const searchString = ref('')
const badgesWithTypes = computed(() => {
  return props.badges.map((badge) => {
    const badgeTypes = []
    if (badge.global) {
      badgeTypes.push('globalBadges')
    } else if (badge.projectId) {
      badgeTypes.push('projectBadges')
      if (badge.startDate && badge.endDate) {
        badgeTypes.push('gems')
      }
    }
    return {...badge, badgeTypes }
  })
})
const shownBadges = computed(() => {
  return badgesWithTypes.value.filter((badge) => {
    if(filterId.value && !badge.badgeTypes.includes(filterId.value)) {
      return false
    }
    if (searchString.value && !badge.badge.toLowerCase().includes(searchString.value.toLowerCase())) {
      return false
    }
    return true
  })
})
const skillsDisplayInfo = useSkillsDisplayInfo()
const colors = useColors()

const filterId = ref('')
const setFilterId = (newFilterId) => {
  filterId.value = newFilterId
}

const buildBadgeLink = (badge) => {
  let globalBadgeUnderProjectId = null
  if (!route.params.projectId) {
    globalBadgeUnderProjectId = badgesWithTypes.value.find((b) => b.projectId).projectId
  }
  return skillsDisplayInfo.createToBadgeLink(badge, globalBadgeUnderProjectId)
}

</script>

<template>
  <Card class="card" data-cy="myBadges">
    <template #header>
      <div class="flex p-4" v-if="badges && badges.length > 0">
        <div class="">
          <InputGroup>
            <InputText
              v-model="searchString"
              placeholder="Search Available Badges"
              aria-label="Search badges"
              data-cy="badgeSearchInput" />
            <InputGroupAddon class="p-0 m-0">
              <SkillsButton :pt="{ root: { class: '!border-0' } }"
                icon="fas fa-times"
                text
                outlined
                @click="searchString = ''"
                class="position-absolute skills-theme-btn m-0 h-full"
                aria-label="clear search input"
                data-cy="clearSkillsSearchInput" />
            </InputGroupAddon>
          </InputGroup>

        </div>
        <div class="">
          <badge-type-filter
            :badges="badgesWithTypes"
            @filter-selected="setFilterId"
            @clear-filter="filterId = ''"
            class="ml-2"
          />
        </div>
      </div>
    </template>

    <template #content>
      <div class="">
        <div class="mb-8" v-for="(badge, index) in shownBadges" v-bind:key="badge.badgeId">
          <badge-catalog-item
            :display-project-name="displayBadgeProject"
            :badge="badge"
            :search-string="searchString"
            :view-details-btn-to="buildBadgeLink(badge)"
            :icon-color="colors.getTextClass(index)"
            ></badge-catalog-item>
        </div>

        <no-content2 v-if="!(shownBadges && shownBadges.length > 0) && searchString.length > 0" class="my-8"
                     icon="fas fa-search-minus"
                     title="No results" :message="`Please refine [${searchString}] search${(filterId) ? ' and/or clear the selected filter' : ''}`"/>

        <no-content2 v-if="!(badges && badges.length > 0) && searchString.length === 0" class="my-8"
                     data-cy="badge-catalog_no-badges"
                     :message="noBadgesMessage"/>

      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>