<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";

const props = defineProps({
  tagChart: Object
})
const route = useRoute();

onMounted(() => {
  loadData();
});

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
      pageSize: 10,
      removePerPage: true,
    },
    tableDescription: `${props.tagChart.title} Users`,
  },
});

const projectId = computed(() => {
  return route.params.projectId;
});

const tagKey = computed(() => {
  return props.tagChart.key;
});

const pageChanged = (pagingInfo) => {
  table.value.options.pagination.pageSize = pagingInfo.rows
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
  loadData();
};

const loadData = (shouldHighlight = false) => {
  table.value.options.busy = true;
  const params = {
    tagKey: props.tagChart.key,
    currentPage: table.value.options.pagination.currentPage,
    pageSize: table.value.options.pagination.pageSize,
    sortDesc: table.value.options.sortOrder === -1,
    tagFilter: filters.value.tag,
    sortBy: table.value.options.sortBy === 'count' ? 'numUsers' : 'tag',
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
</script>

<template>
  <Card data-cy="userTagTableCard">
    <template #header>
      <SkillsCardHeader :title="titleInternal"></SkillsCardHeader>
    </template>
    <template #content>
      <skills-spinner :is-loading="isLoading" class="mb-5"/>
      <div v-if="!isLoading">
        <div class="flex gap-2">
          <div class="flex">
            <SkillsTextInput label="Filter" v-model="filters.tag" v-on:keydown.enter="filter" id="userTagTable-tagFilter" name="userTagTable-tagFilter"/>
          </div>
          <div class="mt-5">
            <SkillsButton size="small" @click="filter" data-cy="userTagTable-filterBtn" title="search by tag">
              <i class="fa fa-search"/><span class="sr-only">filter tags</span>
            </SkillsButton>
            <SkillsButton size="small" severity="danger" class="ml-2" @click="clearFilter" data-cy="userTagTable-clearBtn" title="clear filter">
              <i class="fas fa-eraser"></i><span class="sr-only">clear filter</span>
            </SkillsButton>
          </div>
        </div>
        <SkillsDataTable :value="table.items"
                         :loading="table.options.busy"
                         show-gridlines
                         striped-rows
                         paginator
                         lazy
                         @page="pageChanged"
                         @sort="sortTable"
                         v-model:sort-field="table.options.sortBy"
                         v-model:sort-order="table.options.sortOrder"
                         :rows="table.options.pagination.pageSize"
                         :rowsPerPageOptions="table.options.pagination.possiblePageSizes"
                         :total-records="table.options.pagination.totalRows"
                         tableStoredStateId="userTagsTable"
                         data-cy="userTagsTable">
          <Column field="value" :header="tagChart.tagLabel ? tagChart.tagLabel : 'Tag'" sortable>
            <template #body="slotProps">
              <span :data-cy="`cell_tagValue-${slotProps.data.value}`">
                <router-link :to="{ name: 'UserTagMetrics', params: { projectId: projectId, tagKey: tagKey, tagFilter: slotProps.data.value } }" data-cy="userTagTable_viewMetricsLink">
                  <span v-if="slotProps.data.htmlValue" v-html="slotProps.data.htmlValue"></span><span v-else>{{ slotProps.data.value }}</span>
                </router-link>
              </span>
            </template>
          </Column>
          <Column field="count" header="# Users" sortable></Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ table.options.pagination.totalRows }}</span>
          </template>

          <template #empty>
            <div class="flex justify-content-center flex-wrap" data-cy="emptyTable">
              <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle" aria-hidden="true"></i>
              <span class="flex align-items-center justify-content-center">There are no records to show</span>
            </div>
          </template>
        </SkillsDataTable>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>