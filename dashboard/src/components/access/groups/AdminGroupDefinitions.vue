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
import { computed, onMounted, ref } from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { FilterMatchMode } from 'primevue/api'
import AdminGroupsService from '@/components/access/groups/AdminGroupsService.js';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import Column from 'primevue/column'
import DateCell from '@/components/utils/table/DateCell.vue'
import EditAdminGroup from '@/components/access/groups/EditAdminGroup.vue';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import { useAdminGroupState } from '@/stores/UseAdminGroupState.js';
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js';
import Avatar from 'primevue/avatar';

const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()
const adminGroupState = useAdminGroupState()
const communityLabels = useCommunityLabels()

const loading = ref(false);
const adminGroups = ref([]);
const sortInfo = ref({ sortOrder: 1, sortBy: 'created' })
const options = ref({
  busy: false,
  fields: [
    {
      key: 'name',
      label: 'Name',
      sortable: true,
      imageClass: 'fas fa-spell-check skills-color-subjects',
    },
    // {
    //   key: 'numberOfProjects',
    //   label: '# Of Projects',
    //   sortable: true,
    //   imageClass: 'fas fa-users text-success',
    // },
    // {
    //   key: 'numberOfQuizzesAndSurveys',
    //   label: '# Of Quizzes And Surveys',
    //   sortable: true,
    //   imageClass: 'fas fa-users text-success',
    // },
    {
      key: 'created',
      label: 'Created On',
      sortable: true,
      imageClass: 'fas fa-clock text-warning',
    },
  ],
  pagination: {
    server: false,
    currentPage: 1,
    totalRows: 0,
    pageSize: 5,
    possiblePageSizes: [5, 10, 15, 20],
  },
});
const deleteAdminGroupInfo = ref( {
  showDialog: false,
  adminGroupDef: {},
});
const editAdminGroupInfo = ref({
  showDialog: false,
  isEdit: false,
  adminGroupDef: {},
});

const totalRows = ref(0)

const hasData = computed(() => {
  return adminGroups.value && adminGroups.value.length > 0;
});

onMounted(() => {
  loadData()
})

function loadData() {
  loading.value = true;
  AdminGroupsService.getAdminGroupDefs()
      .then((res) => {
        adminGroups.value = res?.map((q) => ({ ...q }));
        options.value.pagination.totalRows = adminGroups.value.length;
        totalRows.value = adminGroups.value.length;
      })
      .finally(() => {
        options.value.busy = false;
        loading.value = false;
      });
}

const filters = ref({
  global: {value: null, matchMode: FilterMatchMode.CONTAINS},
})

const clearFilter = () => {
  filters.value.global.value = null
}
const onFilter = (filterEvent) => {
  totalRows.value = filterEvent.filteredValue.length
}
function updateAdminGroupDef(adminGroupDef) {
  // adminGroupState.adminGroup.name = adminGroupDef.name
  const existingIndex = adminGroups.value.findIndex((item) => item.name === adminGroupDef.originalAdminGroupName)
  if (existingIndex >= 0) {
    adminGroups.value.splice(existingIndex, 1, adminGroupDef)
  } else {
    adminGroups.value.push(adminGroupDef)
  }
  announcer.polite(`Admin Group named ${adminGroupDef.name} was saved`);
}
function showDeleteWarningModal(adminGroupDef) {
  deleteAdminGroupInfo.value.adminGroupDef = adminGroupDef;
  deleteAdminGroupInfo.value.showDialog = true;
}
function deleteAdminGroup() {
  options.value.busy = true;
  const { adminGroupDef } = deleteAdminGroupInfo.value;
  deleteAdminGroupInfo.value.adminGroupDef = {};
  AdminGroupsService.deleteAdminGroupId(adminGroupDef.adminGroupId)
      .then(() => {
        adminGroups.value = adminGroups.value.filter((q) => q.adminGroupId !== adminGroupDef.adminGroupId);
      })
      .finally(() => {
        options.value.busy = false;
        announcer.polite(`$Admin Group ${adminGroupDef.name} was removed.`);
      });
}
const showUpdateModal = (adminGroupDef, isEdit = true) => {
  editAdminGroupInfo.value.adminGroupDef = adminGroupDef;
  editAdminGroupInfo.value.isEdit = isEdit;
  editAdminGroupInfo.value.showDialog = true;
};

defineExpose({
  showUpdateModal,
})
</script>

<template>
  <div style="min-height: 20rem;">
    <SkillsSpinner :is-loading="loading" class="my-5" />
    <NoContent2 v-if="!loading && !hasData"
                title="No Admin Group Definitions"
                class="py-8 px-4"
                data-cy="noAdminGroupsYet">
      <div>
        <p>
          Create an Admin Group that can be assigned to SkillTree quizzes and projects.
        </p>
        <p>
          When a group is assigned to a project or a quiz, group's members automatically gain administrative privileges of that project, streamlining management.
        </p>
      </div>
    </NoContent2>
    <div v-if="!loading && hasData">
      <SkillsDataTable
        tableStoredStateId="adminGroupDefinitionsTable"
        aria-label="Admin Groups table"
        :value="adminGroups"
        data-cy="adminGroupDefinitionsTable"
        v-model:filters="filters"
        :globalFilterFields="['name']"
        @filter="onFilter"
        v-model:sort-field="sortInfo.sortBy"
        v-model:sort-order="sortInfo.sortOrder"
        paginator :rows="5" :rowsPerPageOptions="[5, 10, 15, 20]"
        show-gridlines
        striped-rows>
        <template #header>
          <div class="flex gap-1">
            <InputGroup>
              <InputGroupAddon>
                <i class="fas fa-search" aria-hidden="true"/>
              </InputGroupAddon>
              <InputText class="flex flex-grow-1"
                         v-model="filters['global'].value"
                         data-cy="adminGroupNameFilter"
                         placeholder="Admin Group Search"
                         aria-label="Admin Group Name Filter"/>
              <InputGroupAddon class="p-0 m-0">
                <SkillsButton
                              icon="fa fa-times"
                              text
                              outlined
                              @click="clearFilter"
                              aria-label="Reset admin groups filter"
                              data-cy="filterResetBtn"/>
              </InputGroupAddon>
            </InputGroup>
          </div>
        </template>

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
        </template>

        <template #empty>
          <div class="flex justify-content-center flex-wrap">
            <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle" aria-hidden="true"></i>
            <span class="flex align-items-center justify-content-center">No Admin Groups found.  Click
            <SkillsButton class="flex flex align-items-center justify-content-center px-1"
                          label="Reset"
                          link
                          size="small"
                          @click="clearFilter"
                          aria-label="Reset admin groups filter"
                          data-cy="emptyResultsFilterResetBtn"/> to clear the existing filter.
              </span>
          </div>
        </template>
        <Column v-for="col of options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
                :class="{'flex': responsive.md.value }">
          <template #header>
            <span><i :class="col.imageClass" aria-hidden="true"></i> {{ col.label }}</span>
          </template>
          <template #body="slotProps">
            <div v-if="slotProps.field === 'name'" class="flex w-full flex-wrap flex-column sm:flex-row gap-2">
              <div class="flex align-items-start justify-content-start w-min-10rem">
                <div>
                  <router-link :data-cy="`managesAdminGroupLink_${slotProps.data.adminGroupId}`"
                               :to="{ name:'AdminGroupMembers', params: { adminGroupId: slotProps.data.adminGroupId }}"
                               :aria-label="`Manage Admin Group ${slotProps.data.name}`">
                    <highlighted-value :value="slotProps.data.name" :filter="filters.global.value" />
                  </router-link>
                <div v-if="slotProps.data.userCommunity" class="my-2" data-cy="userCommunity">
                  <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
                  <span class="text-color-secondary font-italic ml-1">{{ communityLabels.beforeCommunityLabel.value }}</span> <span
                      class="font-bold text-primary">{{ slotProps.data.userCommunity }}</span> <span
                      class="text-color-secondary font-italic">{{ communityLabels.afterCommunityLabel.value }}</span>
                </div>
                </div>
              </div>
              <div class="flex flex-1 flex-wrap align-items-start justify-content-end gap-2">
                <router-link :data-cy="`managesAdminGroupBtn_${slotProps.data.adminGroupId}`"
                             :to="{ name:'AdminGroupMembers', params: { adminGroupId: slotProps.data.adminGroupId }}"
                             :aria-label="`Manage Admin Group ${slotProps.data.name}`" tabindex="-1">
                  <SkillsButton label="Manage"
                                icon="fas fa-arrow-circle-right"
                                  class="flex-shrink-1"
                                outlined
                                size="small"/>
                </router-link>
                <ButtonGroup class="flex flex-nowrap">
                  <SkillsButton @click="showUpdateModal(slotProps.data)"
                                icon="fas fa-edit"
                                outlined
                                :data-cy="`editAdminGroupButton_${slotProps.data.adminGroupId}`"
                                :aria-label="`Edit Admin Group ${slotProps.data.name}`"
                                :ref="`edit_${slotProps.data.adminGroupId}`"
                                :id="`edit_${slotProps.data.adminGroupId}`"
                                :track-for-focus="true"
                                title="Edit Admin Group">
                  </SkillsButton>
                  <SkillsButton @click="showDeleteWarningModal(slotProps.data)"
                                icon="text-warning fas fa-trash"
                                outlined
                                :data-cy="`deleteAdminGroupButton_${slotProps.data.adminGroupId}`"
                                :aria-label="'delete Admin Group '+slotProps.data.name"
                                :ref="`delete_${slotProps.data.adminGroupId}`"
                                :id="`delete_${slotProps.data.adminGroupId}`"
                                :track-for-focus="true"
                                title="Delete Admin Group">
                  </SkillsButton>
                </ButtonGroup>
              </div>
            </div>
            <div v-else-if="slotProps.field === 'created'">
              <DateCell :value="slotProps.data[col.key]" />
            </div>
            <div v-else>
              {{ slotProps.data[col.key] }}
            </div>
          </template>
        </Column>
      </SkillsDataTable>
    </div>

    <EditAdminGroup
        v-if="editAdminGroupInfo.showDialog"
        v-model="editAdminGroupInfo.showDialog"
        :admin-group="editAdminGroupInfo.adminGroupDef"
        :is-edit="editAdminGroupInfo.isEdit"
        @admin-group-saved="updateAdminGroupDef"
        :enable-return-focus="true"/>

    <removal-validation
      v-if="deleteAdminGroupInfo.showDialog"
      :item-name="deleteAdminGroupInfo.adminGroupDef.name"
      item-type="Admin Group"
      v-model="deleteAdminGroupInfo.showDialog"
      :enable-return-focus="true"
      @do-remove="deleteAdminGroup">
        <div>
            Deletion <b>cannot</b> be undone and permanently removes admin role to any associated projects, quizzes and surveys for <b>all</b> group members. Proceed with caution!
        </div>
    </removal-validation>

  </div>
</template>

<style scoped>

</style>