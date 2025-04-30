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
    <div class="skills-theme-filter-menu d-inline-block" data-cy="filterMenu">
      <b-dropdown variant="outline-info" toggle-class="skills-theme-btn" class="ml-1" dropup>
        <template slot="button-content">
          <i class="fas fa-filter" style="font-size: 1.1rem" aria-hidden="true" data-cy="filterBtn"/>
          <span class="sr-only">Skills Filter</span>
        </template>
        <b-dropdown-group v-for="filterGroup in filters" :key="filterGroup.groupId" :id="filterGroup.groupId">
          <template slot="header">
            <span class="skills-theme-menu-secondary-color" style="font-size: 0.9rem">{{ filterGroup.groupLabel }}</span>
            <span class="sr-only">{{ filterGroup.groupLabel }}</span>
          </template>
          <b-dropdown-item href="#" v-for="(filter, index) in filterGroup.filterItems"
                           :key="filter.id"
                           @click="filterSelected(filter.id)"
                           :disabled="filter.count === 0"
                           :data-cy="`filter_${filter.id}`"
                           class="text-primary m-0"
                           :class="{ 'skills-theme-menu-primary-color' : filter.count > 0, 'skills-theme-menu-secondary-color' : filter.count === 0, '' : index > 0  }">
            <div class="row no-gutters skills-dropdown-item">
              <div class="col-auto">
                <i class="text-center border rounded-sm p-1" :class="filter.icon" :aria-hidden="true" />
              </div>
              <div class="col">
                <span class="ml-2 align-middle" v-html="filter.html"></span>
              </div>
              <div class="col-auto">
                <span class="badge badge-info float-right" data-cy="filterCount">{{ filter.count }}</span>
              </div>
            </div>
          </b-dropdown-item>
        </b-dropdown-group>
      </b-dropdown>
      <b-badge v-if="selectedFilter"
               variant="light"
               class="mx-1 py-1 border-info border selected-filter"
               data-cy="selectedFilter">
        <i :class="selectedFilter.icon" class="ml-1"></i> <span v-html="selectedFilter.html"></span>
        <button type="button" class="btn btn-link p-0" @click="clearSelection" data-cy="clearSelectedFilter">
          <i class="fas fa-times-circle ml-1"></i>
          <span class="sr-only">clear filter</span>
        </button>
      </b-badge>
    </div>
</template>

<script>
  export default {
    name: 'ListFilterMenu',
    props: ['filters', 'counts'],
    data() {
      return {
        selectedFilter: null,
        flattenedFilters: [],
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
        this.flattenedFilters = this.filters.map((group) => group.filterItems).flat();
        keys.map((key) => {
          const filter = this.flattenedFilters.find((item) => item.id === key);
          // some of the filters are optionally configured based on the page
          if (filter) {
            filter.count = this.counts[key];
          }
          return key;
        });
      },
      filterSelected(filterId) {
        const filter = this.flattenedFilters.find((item) => item.id === filterId);
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

<style scoped>
    .selected-filter {
      font-size: 0.82rem;
    }
    .skills-dropdown-item {
      min-width: 18rem !important;
      font-size: 0.95rem !important;
    }
    .skills-dropdown-item i {
      min-width: 2rem;
      font-size: 1.1rem;
    }
    .skills-dropdown-item .badge {
      font-size: 0.85rem;
    }
</style>
