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
import { ref, computed, watch } from 'vue';
import { useDebounceFn } from '@vueuse/core'
import SettingsService from '@/components/settings/SettingsService.js';
import ProjectService from '@/components/projects/ProjectService';
import DateCell from "@/components/utils/table/DateCell.vue";
import OptionalDateCell from "@/components/utils/table/OptionalDateCell.vue";
import Column from "primevue/column";
import InputGroup from "primevue/inputgroup";
import InputText from "primevue/inputtext";
import InputGroupAddon from "primevue/inputgroupaddon";

const emit = defineEmits(['done']);

const model = defineModel();

const isLoading = ref(false);
const searchValue = ref('');
const tableStoredStateId = 'PinProjects-table';
const result = ref({
  values: [],
    paging: {
      totalRows: 1,
      currentPage: 1,
      perPage: 5,
      pageOptions: [5, 10, 15],
    },
    fields: [{
      key: 'name',
      sortable: true,
    },
    {
      key: 'numSkills',
      label: 'Skills',
      sortable: true,
    },
    {
      key: 'lastReportedSkill',
      label: 'Last Reported Skill',
      sortable: true,
    },
    {
      key: 'created',
      label: 'Created',
      sortable: true,
    }],
});

const hasResults = computed(() => {
  return result.value.values && result.value.values.length > 0;
});

const hasSearch = computed(() => {
  return searchValue.value && searchValue.value.length > 0;
});

const done = () => {
  emit('done');
  searchValue.value = '';
  result.value.values = [];
};

const searchData = (searchValue) => {
  if (!searchValue) {
    result.value.values = [];
  } else {
    isLoading.value = true;
    ProjectService.searchProjects(searchValue)
        .then((response) => {
          result.value.values = response;
        })
        .finally(() => {
          isLoading.value = false;
        });
  }
};

const loadAll = () => {
  searchValue.value = '';
  isLoading.value = true;
  ProjectService.loadAllProjects()
      .then((response) => {
        result.value.values = response;
      })
      .finally(() => {
        isLoading.value = false;
      });
};

const pinProject = (item) => {
  const itemRef = item;
  SettingsService.pinProject(item.projectId)
      .then(() => {
        itemRef.pinned = true;
      });
};

const unpinProject = (item) => {
  const itemRef = item;
  SettingsService.unpinProject(item.projectId)
      .then(() => {
        itemRef.pinned = false;
      });
};

watch(() => searchValue.value, useDebounceFn((newValue) => {
  searchData(newValue);
}, 250))
</script>

<template>
  <SkillsDialog v-model="model"
                :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }"
                header="Pin Projects"
                :show-ok-button="false"
                @on-cancel="done"
                footer-class="p-3"
                cancel-button-label="Done"
                cancel-button-icon=""
                cancel-button-severity="success">
    <div data-cy="pinProjects">
      <div class="flex gap-4 mb-4 p-3 align-items-center">
        <InputGroup class="flex-1">
          <InputText v-model="searchValue"
                     placeholder="Search projects to pin"
                     data-cy="pinProjectsSearchInput"
                     aria-label="search for projects to pin" />
          <InputGroupAddon @click="searchValue=''" data-cy="pinProjectsClearSearch">
            <i class="fas fa-times" aria-hidden="true" />
          </InputGroupAddon>
        </InputGroup>
        <span class="text-secondary">OR</span>
        <SkillsButton label="Load All" size="small" @click="loadAll" data-cy="pinProjectsLoadAllButton" icon="fas fa-weight-hanging"/>
      </div>
      <div>
        <SkillsDataTable
          :value="result.values"
          :rowsPerPageOptions="[5, 10, 15, 20]"
          data-cy="pinProjectsSearchResults"
          aria-label="Projects"
          :loading="isLoading"
          striped-rows
          paginator
          :nullSortOrder="-1"
          :rows="5"
          :table-stored-state-id="tableStoredStateId">
          <Column field="name" header="Name" style="width: 50%;" sortable>
            <template #body="slotProps">
              <div class="flex">
                <div class="flex flex-1">
                  {{ slotProps.data.name }}
                </div>
                <div class="flex flex-1 gap-2 justify-content-end">
                  <SkillsButton v-if="!slotProps.data.pinned" @click="pinProject(slotProps.data)" variant="outline-primary"
                                size="small"
                                data-cy="pinButton"
                                icon="fas fa-thumbtack"
                                label="Pin"
                                :aria-label="`pin project ${slotProps.data.projectId}`">
                  </SkillsButton>
                  <SkillsButton v-if="slotProps.data.pinned" variant="outline-warning" @click="unpinProject(slotProps.data)"
                                size="small"
                                data-cy="unpinButton" icon="fas fa-ban" label="Unpin"
                                :aria-label="`remove pin from project ${slotProps.data.projectId}`">
                  </SkillsButton>
                  <router-link :to="{ name:'Subjects', params: { projectId: slotProps.data.projectId }}" tabindex="-1">
                    <SkillsButton variant="outline-primary"
                                  target="_blank"
                                  size="small"
                                  icon="fas fa-eye"
                                  label="View"
                                  data-cy="viewProjectButton"
                                  :aria-label="`view project ${slotProps.data.projectId}`">
                    </SkillsButton>
                  </router-link>
                </div>
              </div>
            </template>
          </Column>
          <Column field="numSkills" header="Skills" sortable></Column>
          <Column field="lastReportedSkill" header="Last Reported Skill" sortable>
            <template #body="slotProps">
              <optional-date-cell :value="slotProps.data.lastReportedSkill" />
            </template>
          </Column>
          <Column field="created" header="Created" sortable>
            <template #body="slotProps">
              <date-cell :value="slotProps.data.created" />
            </template>
          </Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy="skillsBTableTotalRows">{{ result.values.length }}</span>
          </template>

          <template #empty>
            <div v-if="!hasResults && !hasSearch" class="text-center">
              <i class="fas fa-2x fa-th-list text-secondary"></i>
              <div class="h4 mt-2 text-secondary">
                Search Project Catalog
              </div>
              <p class="small">
                Search and browse projects to pin and unpin for the default view.
              </p>
            </div>
            <div v-if="!hasResults && hasSearch" class="text-center">
              <i class="fas fa-2x fa-dragon text-secondary"></i>
              <div class="h4 mt-2 text-secondary">
                No Results
              </div>
              <p class="small">
                Modify your search string or use the 'Load All' feature.
              </p>
            </div>
          </template>
        </SkillsDataTable>
      </div>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>