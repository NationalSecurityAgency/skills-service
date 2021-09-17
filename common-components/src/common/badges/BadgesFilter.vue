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
  <div class="row" data-cy="skillsFilter">
    <div class="col-sm-auto pl-0 pl-md-3 pr-sm-0">
      <b-dropdown variant="link" class="dropdown-no-outline" dropup>
        <template slot="button-content"  class="noOutline">
          <i class="fas fa-filter" style="font-size: 1.1rem" aria-hidden="true" data-cy="badgesFilterBtn"/>
          <span class="sr-only">clear filter</span>
        </template>
        <b-dropdown-item href="#" v-for="filter in filters" :key="filter.id" @click="filterSelected(filter.id)" :disabled="filter.count === 0"
                         :data-cy="`badgesFilter_${filter.id}`" class="skills-theme-link">
          <div style="min-width: 16rem;">
            <i class="text-center" :class="filter.icon" style="min-width: 1.2rem;"></i> <span v-html="filter.html"></span> <span class="badge badge-info float-right" data-cy="filterCount">{{ filter.count }}</span>
          </div>
        </b-dropdown-item>
      </b-dropdown>
    </div>
    <div class="col-sm px-sm-0">
      <span v-if="selectedFilter" class="border skills-card-theme-border rounded py-1 px-2 border-info" data-cy="selectedFilter">
        <i class="text-center" :class="selectedFilter.icon"></i> <span style="font-size: 0.8rem;" v-html="selectedFilter.html"></span>
        <button type="button" class="btn btn-link px-0 text-info" @click="clearSelection" data-cy="clearSelectedFilter">
          <i class="fas fa-times-circle ml-1"></i>
          <span class="sr-only">clear filter</span>
        </button>
      </span>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'BadgesFilter',
    props: ['counts'],
    data() {
      return {
        selectedFilter: null,
        filters: [
          {
            icon: 'fas fa-list-alt',
            id: 'projectBadges',
            html: 'Project Badges',
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
      };
    },
    mounted() {
      this.updateFilters();
    },
    watch: {
      counts: {
        deep: true,
        handler() {
          this.updateFilters();
        },
      },
    },
    methods: {
      updateFilters() {
        const keys = Object.keys(this.counts);
        keys.map((key) => {
          const filter = this.filters.find((item) => item.id === key);
          filter.count = this.counts[key];
          return key;
        });
      },
      filterSelected(filterId) {
        const filter = this.filters.find((item) => item.id === filterId);
        this.selectedFilter = filter;
        this.$emit('filter-selected', filter);
      },
      clearSelection() {
        this.selectedFilter = null;
        this.$emit('clear-filter');
      },
    },
  };
</script>

<style>
/* by default dropdown button will keep outline when :focus is applied */
.dropdown-no-outline button {
  outline: none 0 !important;
  box-shadow: none !important;
  -moz-box-shadow: none !important;
  -webkit-box-shadow: none !important;
}
</style>
