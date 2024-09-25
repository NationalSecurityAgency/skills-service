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
import { ref, computed, onMounted, watch } from 'vue';
import NoContent2 from "@/components/utils/NoContent2.vue";
import MetricsService from "@/components/metrics/MetricsService.js";
import SupervisorService from "@/components/utils/SupervisorService.js";
import AutoComplete from "primevue/autocomplete";
import NumberFormatter from "../../utils/NumberFormatter.js";
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import ColumnGroup from 'primevue/columngroup';
import Row from 'primevue/row';
import LevelBadge from "@/components/metrics/multipleProjects/LevelBadge.vue";

const props = defineProps(['availableProjects']);

const fields = ref(['name', 'numSubjects', 'numBadges', 'numSkills', 'totalPoints', 'minLevel']);
const projects = ref({
  loading: true,
  available: [],
  selected: [],
});
const selectProjects = ref([]);
const results = ref([]);
const resultsLoaded = ref(false);
const resultTableOptions = ref({
  busy: false,
  sortBy: 'userId',
  sortOrder: 1,
  bordered: true,
  outlined: true,
  rowDetailsControls: false,
  stacked: 'md',
  fields: [
    {
      key: 'userId',
      label: 'User',
      sortable: true,
    },
  ],
  pagination: {
    server: true,
    currentPage: 1,
    totalRows: 1,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20, 50],
  },
});

watch(() => projects.value.selected, () => {
  rebuildFields();
})

const atLeast1Proj = computed(() => {
  return projects.value.selected && projects.value.selected.length > 0;
});

const atLeast2Proj = computed(() => {
  return projects.value.selected && projects.value.selected.length > 1;
});

const hasResults = computed(() => {
  return results.value && results.value.length > 0;
});

const enoughOverallProjects = computed(() => {
  return props.availableProjects && props.availableProjects.length >= 2;
});

const beforeListSlotText = computed(() => {
  if (projects.value.selected.length >= 5) {
    return 'Maximum of 5 options selected. First remove a selected option to select another.';
  }
  return '';
});

onMounted(() => {
  loadProjects();
});

const pageChanged = (pagingInfo) => {
  resultTableOptions.value.pagination.pageSize = pagingInfo.rows
  resultTableOptions.value.pagination.currentPage = pagingInfo.page + 1
  locateUsers()
}
const sortTable = (sortContext) => {
  resultTableOptions.value.sortOrder = sortContext.sortOrder;
  resultTableOptions.value.sortBy = sortContext.sortField;
  // set to the first page
  resultTableOptions.value.pagination.currentPage = 1;
  locateUsers();
};

const projAdded = (addedItem) => {
  const addedProject = addedItem.value;
  clearRes();

  SupervisorService.getProjectLevels(addedProject.projectId)
      .then((res) => {
        addedProject.availableLevels = res.map((r) => r.level);
      }).finally(() => {
    addedProject.loadingLevels = false;
    loadProjects();
  });
};

const projRemoved = () => {
  clearRes();
  loadProjects();
};

const rebuildFields = () => {
  resultTableOptions.value.fields = projects.value.selected.map((projItem, index) => ({
    projectId: projItem.projectId,
    key: `${index}`,
    label: projItem.name,
    sortable: false,
  }));
  resultTableOptions.value.fields.splice(0, 0, {
    key: 'userId',
    label: 'User',
    sortable: true,
  });
  loadProjects();
  projects.value.available = projects.value.available.filter((el) => !projects.value.selected.some((sel) => sel.projectId === el.projectId));
};

const clearRes = () => {
  results.value = [];
  resultsLoaded.value = false;
};

const locateUsers = () => {
  resultTableOptions.value.busy = true;
  resultsLoaded.value = true;

  const params = {
    pageSize: resultTableOptions.value.pagination.pageSize,
    currentPage: resultTableOptions.value.pagination.currentPage,
    sortDesc: resultTableOptions.value.sortOrder === -1,
    projIdsAndLevel: projects.value.selected.map((item) => `${item.projectId}AndLevel${item.minLevel}`).join(','),
  };
  MetricsService.loadGlobalMetrics('findExpertsForMultipleProjectsChartBuilder', params)
      .then((dataFromServer) => {
        resultTableOptions.value.busy = false;
        resultTableOptions.value.pagination.totalRows = dataFromServer.totalNum;
        results.value = dataFromServer.data.map((item) => {
          const res = { userId: item.userId };
          item.levels.forEach((level) => {
            const keyForLevel = resultTableOptions.value.fields.find((p) => p.projectId === level.projectId);
            res[`${keyForLevel.projectId}`] = level.level;
          });
          return res;
        });
      });
};

const loadProjects = (filter) => {
  if( projects.value.selected.length < 5 ) {
    projects.value.available = props.availableProjects.map((proj) => ({
      loadingLevels: true,
      minLevel: 1,
      ...proj,
    }));

    projects.value.available = projects.value.available.filter((el) => !projects.value.selected.some((sel) => sel.projectId === el.projectId));

    if (filter) {
      projects.value.available = projects.value.available.filter((el) => el.name.toLowerCase().includes(filter));
    }
  } else {
    projects.value.available = [];
  }

  projects.value.loading = false;
};

const syncOtherLevels = (level) => {
  for (let i = 0; i < projects.value.selected.length; i += 1) {
    const maxLevel = Math.max(...projects.value.selected[i].availableLevels);
    projects.value.selected[i].minLevel = level > maxLevel ? maxLevel : level;
  }
};

const filterProjects = (event) => {
  loadProjects(event.query.toLowerCase());
}
</script>

<template>
  <Card data-cy="multiProjectUsersInCommon" class="mb-4">
    <template #header>
      <SkillsCardHeader title="Find users across multiple projects"></SkillsCardHeader>
    </template>
    <template #content>
      <skills-spinner :is-loading="projects.loading" class="mb-5"/>

      <div v-if="enoughOverallProjects && !projects.loading">
        <div class="flex">
          <AutoComplete
              v-model="projects.selected"
              :suggestions="projects.available"
              :loading="projects.loading"
              :delay="500"
              :completeOnFocus="true"
              dropdown
              @item-unselect="projRemoved"
              @item-select="projAdded"
              multiple
              optionLabel="name"
              inputClass="w-full"
              class="w-full mb-4"
              @complete="filterProjects"
              data-cy="projectSelector"
              placeholder="Select option">
            <template #empty>
              <div v-if="projects.selected.length === 5" class="ml-4" data-cy="projectSelectorMaximumReached">
                Maximum of 5 options selected. First remove a selected option to select another.
              </div>
              <div v-else class="ml-4">
                No results found
              </div>
            </template>
          </AutoComplete>
        </div>
        <div class="flex mb-4">
          <no-content2 v-if="!atLeast1Proj" title="No Projects Selected" class="w-full"
                       message="Please select at least 2 projects using search above then click 'Find Users' button below"></no-content2>

          <SkillsDataTable :value="projects.selected"
                           v-if="atLeast1Proj"
                           class="w-full"
                           show-gridlines
                           striped-rows
                           aria-label="Selected Projects"
                           data-cy="multiProjectUsersInCommon-inputProjs"
                           table-stored-state-id="multiProjectUsersInCommon-inputProjs">
            <Column field="name" header="Name"></Column>
            <Column field="numSubjects" header="# of Subjects"></Column>
            <Column field="numBadges" header="# of Badges"></Column>
            <Column field="numSkills" header="# of Skills">
              <template #body="slotProps">
                {{ NumberFormatter.format(slotProps.data.numSkills )}}
              </template>
            </Column>
            <Column field="totalPoints" header="Total Points">
              <template #body="slotProps">
                {{ NumberFormatter.format(slotProps.data.totalPoints )}}
              </template>
            </Column>
            <Column field="minLevel" header="Min Level">
              <template #body="slotProps">
                <div class="flex gap-4">
                  <Dropdown :options="slotProps.data.availableLevels"
                            v-if="!slotProps.data.loadingLevels"
                            v-model="slotProps.data.minLevel"
                            data-cy="minLevelSelector">
                  </Dropdown>
                  <SkillsButton variant="outline-info"
                                aria-label="Sync other levels"
                                @click="syncOtherLevels(slotProps.data.minLevel)"
                                data-cy="syncLevelButton"
                                size="small"
                                icon="fas fa-sync"
                                label="Sync Levels">
                  </SkillsButton>
                </div>
              </template>
            </Column>
          </SkillsDataTable>
        </div>
        <div class="flex justify-content-center">
          <SkillsButton :disabled="!atLeast2Proj"
                        @click="locateUsers"
                        label="Find Users"
                        icon="fas fa-search-plus"
                        data-cy="findUsersBtn">
          </SkillsButton>
        </div>
        <div class="flex mt-4">
          <SkillsDataTable v-if="hasResults || resultsLoaded"
                           :value="results"
                           class="w-full"
                           @page="pageChanged"
                           @sort="sortTable"
                           :totalRecords="resultTableOptions.
                           pagination.totalRows"
                           :rows="resultTableOptions.pagination.pageSize"
                           v-model:sort-field="resultTableOptions.sortBy"
                           v-model:sort-order="resultTableOptions.sortOrder"
                           :rowsPerPageOptions="resultTableOptions.pagination.possiblePageSizes"
                           paginator
                           lazy
                           striped-rows
                           show-gridlines
                           aria-label="Users in Common"
                           tableStoredStateId="usersInCommonResultTable"
                           data-cy="usersInCommonResultTable">

            <Column field="userId" header="User" sortable></Column>
            <Column v-for="project in projects.selected" v-bind:key="project.projectId" :header="project.name" field="levels">
              <template #body="slotProps">
                <level-badge :level="slotProps.data[project.projectId]"></level-badge>
              </template>
            </Column>

            <template #paginatorstart>
              <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ resultTableOptions.pagination.totalRows }}</span>
            </template>

            <template #empty>
              <span class="flex align-items-center justify-content-center">There are no records to show</span>
            </template>
          </SkillsDataTable>
        </div>
      </div>

      <no-content2 v-if="!enoughOverallProjects"
                   class="my-5"
                   title="Feature is disabled"
                   icon="fas fa-poo"
                   message="At least 2 projects must exist for this feature to work. Please create more projects to enable this feature."/>
    </template>
  </Card>
</template>

<style scoped>

</style>