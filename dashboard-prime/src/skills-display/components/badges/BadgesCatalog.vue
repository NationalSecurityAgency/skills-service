<script setup>
import { computed, ref } from 'vue'
import BadgeCatalogItem from '@/skills-display/components/badges/BadgeCatalogItem.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'


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

const searchString = ref('')
const shownBadges = computed(() => props.badges)
const skillsDisplayInfo = useSkillsDisplayInfo()
const colors = useColors()
</script>

<template>
  <Card class="card" data-cy="myBadges">
    <template #header>
      <div class="flex p-3" v-if="badges && badges.length > 0">
        <div class="">
          <InputGroup>
            <InputText
              v-model="searchString"
              placeholder="Search Available Badges"
              aria-label="Search badges"
              data-cy="badgeSearchInput" />
            <InputGroupAddon class="p-0 m-0">
              <SkillsButton
                icon="fas fa-times"
                text
                outlined
                @click="searchString = ''"
                class="position-absolute skills-theme-btn" variant="outline-info" style="right: 0rem;"
                data-cy="clearSkillsSearchInput" />
            </InputGroupAddon>
          </InputGroup>


<!--          <div class="">-->
<!--            <b-form-input @input="searchBadges" style="padding-right: 2.3rem;"-->
<!--                          v-model="searchString"-->
<!--                          placeholder="Search Available Badges"-->
<!--                          aria-label="Search badges"-->
<!--                          data-cy="badgeSearchInput"></b-form-input>-->
<!--            <b-button v-if="searchString && searchString.length > 0" @click="clearSearch"-->
<!--                      class="position-absolute skills-theme-btn" variant="outline-info" style="right: 0rem;"-->
<!--                      data-cy="clearBadgesSearchInput">-->
<!--              <i class="fas fa-times"></i>-->
<!--              <span class="sr-only">clear search</span>-->
<!--            </b-button>-->
<!--          </div>-->
        </div>
        <div class="">
<!--          <badges-filter :counts="metaCounts" :filters="filters" @filter-selected="filterSelected" @clear-filter="clearFilters"/>-->
        </div>
      </div>
    </template>

    <template #content>
      <div class="">
        <div class="mb-5" v-for="(badge, index) in shownBadges" v-bind:key="badge.badgeId">
<!--          :badgeRouterLinkGenerator="badgeRouterLinkGenerator"-->
          <badge-catalog-item
            :display-project-name="displayBadgeProject"
            :badge="badge"
            :view-details-btn-to="skillsDisplayInfo.createToBadgeLink(badge)"
            :icon-color="colors.getTextClass(index)"
            ></badge-catalog-item>
        </div>

        <no-content2 v-if="!(shownBadges && shownBadges.length > 0) && searchString.length > 0" class="my-5"
                     icon="fas fa-search-minus fa-5x"
                     title="No results" :message="`Please refine [${searchString}] search${(this.filter) ? ' and/or clear the selected filter' : ''}`"/>

        <no-content2 v-if="!(badges && badges.length > 0) && searchString.length === 0" class="my-5"
                     data-cy="badge-catalog_no-badges"
                     :message="noBadgesMessage"/>

      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>