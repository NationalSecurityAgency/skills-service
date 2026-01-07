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
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import TableNoRes from "@/components/utils/table/TableNoRes.vue";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import {useStorage} from "@vueuse/core";

const props = defineProps({
  tagChart: Object
})
const route = useRoute();
const numberFormat = useNumberFormat()

onMounted(() => {
  loadData();
});

const announcer = useSkillsAnnouncer()
const timeUtils = useTimeUtils();
const isLoading = ref(true);
const titleInternal = ref(props.tagChart.title);
const filters = ref({
  tag: '',
});
const table = ref({
  items: [],
  options: {
    busy: false,
    sortBy: 'count',
    sortOrder: -1,
    pagination: {
      server: true,
      currentPage: 1,
      totalRows: 1,
      removePerPage: true,
    },
    tableDescription: `${props.tagChart.title} Users`,
  },
});
const pageSize = useStorage('userTagTable-tablePageSize', 10)

const projectId = computed(() => {
  return route.params.projectId;
});

const tagKey = computed(() => {
  return props.tagChart.key;
});

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
  table.value.options.pagination.currentPage = pagingInfo.page + 1
  loadData()
}
const sortTable = (sortContext) => {
  table.value.options.sordOrder = sortContext.sortOrder;
  table.value.options.sortBy = sortContext.sortField;
  // set to the first page
  table.value.options.pagination.currentPage = 1;
  loadData();
};

const filter = () => {
  filters.value.tag = filters.value.tag.trim();
  loadData(true);
};

const clearFilter = () => {
  filters.value.tag = '';
  filterRange.value = [];
  loadData();
};

const loadData = (shouldHighlight = false) => {
  table.value.options.busy = true;
  const dateRange = timeUtils.prepareDateRange(filterRange.value)
  const params = {
    tagKey: props.tagChart.key,
    currentPage: table.value.options.pagination.currentPage,
    pageSize: pageSize.value,
    sortDesc: table.value.options.sortOrder === -1,
    tagFilter: filters.value.tag,
    sortBy: table.value.options.sortBy === 'count' ? 'numUsers' : 'tag',
    fromDayFilter: dateRange.startDate,
    toDayFilter: dateRange.endDate,
  };
  MetricsService.loadChart(route.params.projectId, 'numUsersPerTagBuilder', params)
      .then((dataFromServer) => {
        let { items } = dataFromServer;
        if (shouldHighlight && filters.value.tag && filters.value.tag.length > 0) {
          items = items.map((item) => {
            const searchStringNorm = filters.value.tag.trim().toLowerCase();
            const index = item.value.toLowerCase().indexOf(searchStringNorm);
            const htmlValue = `${item.value.substring(0, index)}<mark>${item.value.substring(index, index + searchStringNorm.length)}</mark>${item.value.substring(index + searchStringNorm.length)}`;
            return { htmlValue, ...item };
          });
        }
        table.value.items = items;
        table.value.options.pagination.totalRows = dataFromServer.totalNumItems;
        isLoading.value = false;
        table.value.options.busy = false;
      });
};

const filterRange = ref([]);
</script>

<template>
  <Card data-cy="userTagTableCard">
    <template #header>
      <SkillsCardHeader :title="titleInternal"></SkillsCardHeader>
    </template>
    <template #content>
      <div>
        <div class="flex mb-3 gap-4">
          <div class="flex flex-3 flex-col">
            <SkillsTextInput label="Filter by Tag" v-model="filters.tag" v-on:keydown.enter="filter" :disabled="isLoading" id="userTagTable-tagFilter" :name="`userTagTable-${tagKey}-tagFilter`"/>
            <div class="flex gap-2">
              <SkillsButton @click="filter" icon="fa-solid fa-search" label="Filter" :disabled="isLoading" :data-cy="`userTagTable-${tagKey}-filterBtn`" />
              <SkillsButton severity="danger" icon="fa-solid fa-eraser" label="Clear" @click="clearFilter" :disabled="isLoading" :data-cy="`userTagTable-${tagKey}-clearBtn`" />
            </div>
          </div>
          <div class="flex flex-wrap flex-col gap-2">
            Filter by Date(s):
            <SkillsCalendarInput selectionMode="range" name="filterRange" v-model="filterRange" :maxDate="new Date()" :disabled="isLoading" placeholder="Select a date range" :data-cy="`${tagKey}-metricsDateFilter`" />
          </div>
        </div>
        <SkillsDataTable :value="table.items"
                         :loading="table.options.busy || isLoading"
                         show-gridlines
                         striped-rows
                         paginator
                         lazy
                         @page="pageChanged"
                         @sort="sortTable"
                         v-model:sort-field="table.options.sortBy"
                         v-model:sort-order="table.options.sortOrder"
                         :rows="pageSize"
                         :rowsPerPageOptions="table.options.pagination.possiblePageSizes"
                         :total-records="table.options.pagination.totalRows"
                         tableStoredStateId="userTagsTable"
                         aria-label="User Tags"
                         :data-cy="`userTagsTable-${tagKey}`">
          <Column field="value" :header="tagChart.tagLabel ? tagChart.tagLabel : 'Tag'" sortable>
            <template #body="slotProps">
              <span :data-cy="`cell_tagValue-${slotProps.data.value}`">
                <router-link :to="{ name: 'UserTagMetrics', params: { projectId: projectId, tagKey: tagKey, tagFilter: slotProps.data.value } }" :data-cy="`userTagTable-${tagKey}_viewMetricsLink`">
                  <span v-if="slotProps.data.htmlValue" v-html="slotProps.data.htmlValue"></span><span v-else>{{ slotProps.data.value }}</span>
                </router-link>
              </span>
            </template>
          </Column>
          <Column field="count" header="# Users" sortable></Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(table.options.pagination.totalRows) }}</span>
          </template>

          <template #empty>
            <table-no-res :is-loading="isLoading"/>
          </template>
        </SkillsDataTable>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>