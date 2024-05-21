<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";

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
    bordered: true,
    outlined: true,
    stacked: 'md',
    sortBy: 'count',
    sortDesc: true,
    fields: [
      {
        key: 'value',
        label: props.tagChart.tagLabel ? props.tagChart.tagLabel : 'Tag',
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
    tableDescription: `${props.tagChart.title} Users`,
  },
});

const projectId = computed(() => {
  return route.params.projectId;
});

const tagKey = computed(() => {
  return props.tagChart.key;
});


const sortTable = (sortContext) => {
  table.value.options.sortDesc = sortContext.sortDesc;

  // set to the first page
  table.value.options.pagination.currentPage = 1;
  loadData();
};

const pageChanged = (pageNum) => {
  table.value.options.pagination.currentPage = pageNum;
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
    sortDesc: table.value.options.sortDesc,
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
      test
    </template>
  </Card>
</template>

<style scoped>

</style>