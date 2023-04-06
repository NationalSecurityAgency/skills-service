/*
Copyright 2020 SkillTree

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
<template>
    <div class="flex-fill card">
        <div class="card-header">
            <div class="row" v-if="badgesInternalOrig && badgesInternalOrig.length > 0">
                <div class="col-md-auto text-left pr-md-0">
                    <div class="d-flex">
                        <b-form-input @input="searchBadges" style="padding-right: 2.3rem;"
                                      v-model="searchString"
                                      placeholder="Search Available Badges"
                                      aria-label="Search badges"
                                      data-cy="badgeSearchInput"></b-form-input>
                        <b-button v-if="searchString && searchString.length > 0" @click="clearSearch"
                                  class="position-absolute skills-theme-btn" variant="outline-info" style="right: 0rem;"
                                  data-cy="clearBadgesSearchInput">
                            <i class="fas fa-times"></i>
                            <span class="sr-only">clear search</span>
                        </b-button>
                    </div>
                </div>
                <div class="col-md text-left my-2 my-md-0 ml-md-0 pl-md-0">
                    <badges-filter :counts="metaCounts" :filters="filters" @filter-selected="filterSelected" @clear-filter="clearFilters"/>
                </div>
            </div>
        </div>
        <div class="card-body">
            <div class="" v-for="(badge, index) in badgesInternal" v-bind:key="badge.badgeId">
                <badge-catalog-item
                        :display-project-name="displayBadgeProject"
                        :badge="badge" class="pb-3"
                        :badgeRouterLinkGenerator="badgeRouterLinkGenerator"></badge-catalog-item>
                <div v-if="index !== badges.length - 1">
                    <hr/>
                </div>
            </div>

            <no-data-yet v-if="!(badgesInternal && badgesInternal.length > 0) && searchString.length > 0" class="my-5"
                         icon="fas fa-search-minus fa-5x"
                         title="No results" :sub-title="`Please refine [${searchString}] search${(this.filter) ? ' and/or clear the selected filter' : ''}`"/>

            <no-data-yet v-if="!(badgesInternal && badgesInternal.length > 0) && searchString.length === 0" class="my-5"
                         data-cy="badge-catalog_no-badges"
                         :title="noBadgesMessage"/>

        </div>
    </div>
</template>

<script>
  import debounce from 'lodash.debounce';
  import NoDataYet from '@/common-components/utilities/NoDataYet';
  import BadgesFilter from '@/common-components/utilities/ListFilterMenu';
  import BadgeCatalogItem from './BadgeCatalogItem';
  import store from '../../store/store';

  export default {
    name: 'BadgesCatalog',
    components: { NoDataYet, BadgeCatalogItem, BadgesFilter },
    props: {
      badges: {
        type: Array,
        required: true,
      },
      badgeRouterLinkGenerator: {
        type: Function,
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
    },
    mounted() {
      this.badgesInternal = this.badges;
      this.badgesInternalOrig = [...this.badges];
      this.updateCounts(this.badgesInternal);
    },
    data() {
      return {
        colors: ['text-success', 'text-warning', 'text-danger', 'text-info'],
        badgesInternal: [],
        badgesInternalOrig: [],
        searchString: '',
        filter: null,
        metaCounts: {
          projectBadges: 0,
          globalBadges: 0,
          gems: 0,
        },
        filters: [
          {
            groupId: 'progressGroup',
            groupLabel: 'Badge Filters',
            filterItems: [
              {
                icon: 'fas fa-list-alt',
                id: 'projectBadges',
                html: `${(store.getters.projectDisplayName ? store.getters.projectDisplayName : 'Project')} Badges`,
                count: 0,
                filter: (badge) => badge.projectId,
              },
              {
                icon: 'fas fa-gem',
                id: 'gems',
                html: 'Gems',
                count: 0,
                filter: (badge) => badge.startDate && badge.endDate,
              },
              {
                icon: 'fas fa-globe',
                id: 'globalBadges',
                html: 'Global Badges',
                count: 0,
                filter: (badge) => badge.global === true,
              },
            ],
          },
        ],
      };
    },
    methods: {
      updateCounts(badges) {
        const counts = {
          globalBadges: 0,
          projectBadges: 0,
          gems: 0,
        };

        badges.forEach((badge) => {
          if (badge.global) {
            counts.globalBadges += 1;
          } else if (badge.projectId) {
            counts.projectBadges += 1;
            if (badge.startDate && badge.endDate) {
              counts.gems += 1;
            }
          }
        });

        this.$set(this.metaCounts, 'globalBadges', counts.globalBadges);
        this.$set(this.metaCounts, 'projectBadges', counts.projectBadges);
        this.$set(this.metaCounts, 'gems', counts.gems);
      },
      filterSelected(filter) {
        this.filter = filter;
        this.filterAndSearch(false);
      },
      clearFilters() {
        this.filter = null;
        this.filterAndSearch(false);
      },
      searchBadges: debounce(function search() {
        this.filterAndSearch(true);
      }, 200),
      clearSearch() {
        this.searchString = '';
        this.filterAndSearch(true);
      },
      filterAndSearch(updateCounts) {
        let filteredResult = this.badgesInternalOrig.map((item) => ({ ...item }));
        if (this.searchString && this.searchString.trim().length > 0) {
          const searchStrNormalized = this.searchString.trim().toLowerCase();
          filteredResult = filteredResult.reduce((result, item) => {
            const name = item.badge;
            const nameLc = name?.toLowerCase();
            if (nameLc?.trim()?.includes(searchStrNormalized)) {
              const index = nameLc.indexOf(searchStrNormalized);
              const badgeHtml = `${name.substring(0, index)}<mark>${name.substring(index, index + searchStrNormalized.length)}</mark>${name.substring(index + searchStrNormalized.length)}`;
              return result.concat({ badgeHtml, ...item });
            }
            return result;
          }, []);
        }

        if (updateCounts) {
          this.updateCounts(filteredResult);
        }

        if (this.filter) {
          filteredResult = filteredResult.filter(this.filter.filter);
        }
        this.badgesInternal = filteredResult;
      },
      getIconColor(index) {
        const colorIndex = index % this.colors.length;
        const color = this.colors[colorIndex];
        return color;
      },
    },
  };
</script>

<style scoped>

</style>
