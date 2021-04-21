<template>
  <div class="row" data-cy="skillsFilter">
    <div class="col-sm-auto pl-0 pl-md-3 pr-sm-0">
      <b-dropdown variant="link" class="dropdown-no-outline" dropup>
        <template slot="button-content"  class="noOutline">
          <i class="fas fa-filter" style="font-size: 1.1rem" aria-hidden="true" data-cy="skillsFilterBtn"/>
          <span class="sr-only">skills filter</span>
        </template>
        <b-dropdown-item href="#" v-for="filter in filters" :key="filter.id" @click="filterSelected(filter.id)" :disabled="filter.count === 0"
                         :data-cy="`skillsFilter_${filter.id}`">
          <div style="min-width: 16rem;">
            <i class="text-center" :class="filter.icon" style="min-width: 1.2rem;"></i> <span v-html="filter.html"></span> <span class="badge badge-info float-right" data-cy="filterCount">{{ filter.count }}</span>
          </div>
        </b-dropdown-item>
      </b-dropdown>
    </div>
    <div class="col-sm px-sm-0">
      <span v-if="selectedFilter" class="border rounded py-1 px-2 border-info" data-cy="selectedFilter">
        <i class="text-center" :class="selectedFilter.icon"></i> <span style="font-size: 0.8rem;" v-html="selectedFilter.html"></span>
        <button type="button" class="btn btn-link px-0" @click="clearSelection" data-cy="clearSelectedFilter"><i
            class="fas fa-times-circle text-info ml-1"></i></button>
      </span>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'SkillsFilter',
    props: ['counts'],
    data() {
      return {
        selectedFilter: null,
        filters: [
          {
            icon: 'fas fa-battery-empty text-danger',
            id: 'withoutProgress',
            html: 'Skills <b>without</b> progress',
            count: 0,
          },
          {
            icon: 'far fa-calendar-check text-muted',
            id: 'withPointsToday',
            html: 'Skills with points earned <b>today</b>',
            count: 0,
          },
          {
            icon: 'far fa-check-circle text-success',
            id: 'complete',
            html: '<b>Completed</b> skills',
            count: 0,
          },
          {
            icon: 'fas fa-laptop text-primary',
            id: 'selfReported',
            html: '<b>Self</b> Reported Skills',
            count: 0,
          },
          {
            icon: 'fas fa-running text-warning',
            id: 'inProgress',
            html: 'Skills <b>in progress</b>',
            count: 0,
          },
        ],
      };
    },
    mounted() {
      const keys = Object.keys(this.counts);
      keys.map((key) => {
        const filter = this.filters.find((item) => item.id === key);
        filter.count = this.counts[key];
        return key;
      });
    },
    methods: {
      filterSelected(filterId) {
        const filter = this.filters.find((item) => item.id === filterId);
        this.selectedFilter = filter;
        this.$emit('filter-selected', filter.id);
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
