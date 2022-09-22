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
  <metrics-card :title="titleInternal" data-cy="userTagTableCard" :noPadding="true">
    <skills-spinner :is-loading="isLoading" class="mb-5"/>
    <div v-if="!isLoading">
      <div class="p-3 row no-gutters">
        <div class="col px-1">
            <b-input v-model="filters.tag" v-on:keydown.enter="filter" data-cy="userTagTable-tagFilter" aria-label="tag filter"/>
        </div>
        <div class="col-auto px-1">
          <b-button-group class="float-right">
            <b-button variant="outline-info" class="text-secondary" @click="filter" data-cy="userTagTable-filterBtn" title="search by tag">
              <i class="fa fa-search"/><span class="sr-only">filter tags</span>
            </b-button>
            <b-button variant="outline-info" class="text-danger" @click="clearFilter" data-cy="userTagTable-clearBtn" title="clear filter">
              <i class="fas fa-eraser"></i><span class="sr-only">clear filter</span>
            </b-button>
          </b-button-group>
        </div>
      </div>
      <skills-b-table :options="table.options"
                      :items="table.items"
                      tableId="userTagsTable"
                      @page-changed="pageChanged"
                      @sort-changed="sortTable"
                      data-cy="userTagsTable">
        <template v-slot:cell(value)="data">
          <span :data-cy="`cell_tagValue-${data.item.value}`">
          <span v-if="data.item.htmlValue" v-html="data.item.htmlValue"></span><span v-else>{{ data.item.value }}</span>
          <b-button-group class="float-right">
            <b-button :to="{ name: 'UserTagMetrics', params: { projectId: projectId, tagKey: tagKey, tagFilter: data.item.value } }"
                      variant="outline-info" size="sm" class="text-secondary"
                      v-b-tooltip.hover="'View Metrics'"
                      :aria-label="`View metrics for ${data.item.value}`"
                      data-cy="userTagTable_viewMetricsBtn"><i class="fa fa-chart-bar"/><span class="sr-only">view user tag metrics</span>
            </b-button>
          </b-button-group>
          </span>
        </template>
      </skills-b-table>
    </div>
  </metrics-card>
</template>

<script>
  import MetricsCard from '../utils/MetricsCard';
  import SkillsSpinner from '../../utils/SkillsSpinner';
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import MetricsService from '../MetricsService';

  export default {
    name: 'UserTagTable',
    components: { SkillsBTable, SkillsSpinner, MetricsCard },
    props: {
      tagChart: Object,
    },
    data() {
      return {
        isLoading: true,
        titleInternal: this.tagChart.title,
        filters: {
          tag: '',
        },
        table: {
          items: [],
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'count',
            sortDesc: true,
            fields: [
              {
                key: 'value',
                label: this.tagChart.tagLabel ? this.tagChart.tagLabel : 'Tag',
                sortable: true,
              },
              {
                key: 'count',
                label: '# Users',
                sortable: true,
              },
            ],
            pagination: {
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 10,
              removePerPage: true,
            },
            tableDescription: `${this.tagChart.title} Users`,
          },
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      projectId() {
        return this.$route.params.projectId;
      },
      tagKey() {
        return this.tagChart.key;
      },
    },
    methods: {
      sortTable(sortContext) {
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      filter() {
        this.filters.tag = this.filters.tag.trim();
        this.loadData(true);
      },
      clearFilter() {
        this.filters.tag = '';
        this.loadData();
      },
      loadData(shouldHighlight = false) {
        this.table.options.busy = true;
        const params = {
          tagKey: this.tagChart.key,
          currentPage: this.table.options.pagination.currentPage,
          pageSize: this.table.options.pagination.pageSize,
          sortDesc: this.table.options.sortDesc,
          tagFilter: this.filters.tag,
          sortBy: this.table.options.sortBy === 'count' ? 'numUsers' : 'tag',
        };
        MetricsService.loadChart(this.$route.params.projectId, 'numUsersPerTagBuilder', params)
          .then((dataFromServer) => {
            let { items } = dataFromServer;
            if (shouldHighlight && this.filters.tag && this.filters.tag.length > 0) {
              items = items.map((item) => {
                const searchStringNorm = this.filters.tag.trim().toLowerCase();
                const index = item.value.toLowerCase().indexOf(searchStringNorm);
                const htmlValue = `${item.value.substring(0, index)}<mark>${item.value.substring(index, index + searchStringNorm.length)}</mark>${item.value.substring(index + searchStringNorm.length)}`;
                return { htmlValue, ...item };
              });
            }
            this.table.items = items;
            this.table.options.pagination.totalRows = dataFromServer.totalNumItems;
            this.isLoading = false;
            this.table.options.busy = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
