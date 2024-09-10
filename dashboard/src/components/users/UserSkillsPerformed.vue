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
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useProjectUserState } from '@/stores/UseProjectUserState.js';
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js';
import { useProjConfig } from '@/stores/UseProjConfig.js';
import { useColors } from '@/skills-display/components/utilities/UseColors.js';
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import dayjs from 'dayjs'
import InputGroup from 'primevue/inputgroup';
import { FilterMatchMode } from 'primevue/api';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import InputGroupAddon from 'primevue/inputgroupaddon';
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue';
import InputText from 'primevue/inputtext';
import Column from 'primevue/column';
import DateCell from '@/components/utils/table/DateCell.vue';
import ShowMore from "@/components/skills/selfReport/ShowMore.vue";
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';
import UsersService from '@/components/users/UsersService.js'
import StringHighlighter from '@/common-components/utilities/StringHighlighter.js'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const dialogMessages = useDialogMessages()
const timeUtils = useTimeUtils()
const projConfig = useProjConfig()
const colors = useColors()

const route = useRoute()
const projectUserState = useProjectUserState()
const responsive = useResponsiveBreakpoints()
const numberFormat = useNumberFormat()

const projectId = ref(route.params.projectId)
const userId = ref(route.params.userId)
const displayName = ref('Skills Performed Table');
const data = ref([]);
const table = ref({
  items: [],
  options: {
    busy: true,
    bordered: true,
    outlined: true,
    stacked: 'md',
    sortBy: 'performedOn',
    sortDesc: true,
    tableDescription: 'User\'s Skill Events',
    fields: null,
    pagination: {
      server: true,
      currentPage: 1,
      totalRows: 1,
      pageSize: 10,
      possiblePageSizes: [5, 10, 15, 20, 50],
    },
  },
});
const sortInfo = ref({ sortOrder: -1, sortBy: 'performedOn' })
const filtering = ref(false)
const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS }
})
const totalRows = ref(null);

const userIdForDisplay = ref(null);
const showDeleteDialog = ref(false);
const overallErrMsg = ref(null);

const clearFilter = () => {
  filters.value.global.value = null
  filtering.value = false
  loadData()
}
const onFilter = (filterEvent) => {
  filtering.value = true
  loadData()
}
const pageChanged = (pagingInfo) => {
  table.value.options.pagination.pageSize = pagingInfo.rows
  table.value.options.pagination.currentPage = pagingInfo.page + 1
  loadData()
}
const sortField = (column) => {
  // set to the first page
  table.value.options.pagination.currentPage = 1
  loadData();
}
const loadTableFields = () => {
  const fields = [
    {
      key: 'skillId',
      label: 'Skill Id',
      imageClass: 'fas fa-graduation-cap',
      sortable: true,
    },
    {
      key: 'performedOn',
      label: 'Performed On',
      imageClass: 'fas fa-clock',
      sortable: true,
    },
  ];
  if (!projConfig.isReadOnlyProj) {
    fields.push({
      key: 'control',
      label: 'Delete',
      sortable: false,
    });
  }
  table.value.options.fields = fields;
}
onMounted(() => {
  projConfig.afterProjConfigStateLoaded().then(() => {
    loadTableFields()
    projectUserState.loadUserDetailsState(projectId.value, route.params.userId)
    loadData();
  });
})
const loadData = () => {
  UsersService.getUserInfo(projectId.value, userId.value).then((res) => {
    if (res) {
      userIdForDisplay.value = res.userIdForDisplay;
    }
  });
  loadTableData();
};
const loadTableData = () => {
  table.value.options.busy = true;
  overallErrMsg.value = null;
  const url = getUrl();
  UsersService.ajaxCall(url, {
    query: filters.value.global.value ? filters.value.global.value.trim() : '',
    limit: table.value.options.pagination.pageSize,
    ascending: sortInfo.value.sortOrder === 1,
    page: table.value.options.pagination.currentPage,
    byColumn: 0,
    orderBy: sortInfo.value.sortBy
  }).then((res) => {
    table.value.items = res.data?.map((item) => {
      const skillNameHtml = item.skillName && filters.value.global.value ? StringHighlighter.highlight(item.skillName, filters.value.global.value) : null;
      const skillIdHtml = item.skillId && filters.value.global.value ? StringHighlighter.highlight(item.skillId, filters.value.global.value) : null;
      return { skillNameHtml, skillIdHtml, ...item };
    });
    table.value.options.pagination.totalRows = res.count;
    totalRows.value = res.count
    table.value.options.busy = false;
  });
};
const getUrl = () => {
  return `/admin/projects/${encodeURIComponent(projectId.value)}/performedSkills/${encodeURIComponent(userId.value)}`;
};
const getDate = (row) => {
  return dayjs(row.performedOn)
      .format('LLL');
};
const deleteSkill = (row) => {
  overallErrMsg.value = null;
  dialogMessages.msgConfirm({
    message: `Removing skill [${row.skillId}] performed on [${getDate(row)}]. This will permanently remove this user's performed skill and cannot be undone.`,
    header: 'Please Confirm!',
    acceptLabel: 'YES, Delete It!',
    rejectLabel: 'Cancel',
    accept: () => {
      doDeleteSkill(row);
    },
  });
};
const deleteSelectedSkills = () => {
  overallErrMsg.value = null;
  dialogMessages.msgConfirm({
    message: `Removing ${selectedSkills.value.length} selected skill(s). This will permanently remove this user's performed skills and cannot be undone.`,
    header: 'Please Confirm!',
    acceptLabel: 'YES, Delete Them!',
    rejectLabel: 'Cancel',
    accept: () => {
      table.value.options.busy = true;
      const filteredSkills = selectedSkills.value.filter((it) => !it.importedSkill);
      const performedSkills = filteredSkills?.map((it) => it.id)

      if(performedSkills.length > 0) {
        UsersService.bulkDeleteSkillEvents(projectId.value, userId.value, performedSkills).then((data) => {
          if (data.success) {
            selectedSkills.value = [];
            loadData();
            projectUserState.loadUserDetailsState(projectId.value, userId.value);
          } else {
            overallErrMsg.value = `Skills were not removed.  ${data.explanation}`;
          }
        }).finally(() => {
          table.value.options.busy = false;
        });
      } else {
        overallErrMsg.value = `Cannot delete skill events for skills imported from the catalog.`;
        table.value.options.busy = false;
      }
    },
  });
}
const deleteAllSkills = () => {
  showDeleteDialog.value = true;
};
const doDeleteSkill = (skill) => {
  table.value.options.busy = true;
  overallErrMsg.value = null;
  UsersService.deleteSkillEvent(projectId.value, skill, userId.value)
      .then((data) => {
        if (data.success) {
          loadData();
          projectUserState.loadUserDetailsState(projectId.value, userId.value);
        } else {
          overallErrMsg.value = `Skill '${skill.skillId}' was not removed.  ${data.explanation}`;
        }
      })
      .finally(() => {
        table.value.options.busy = false;
      });
};
const doDeleteAllSkills = () => {
  table.value.options.busy = true;
  overallErrMsg.value = null;
  UsersService.deleteAllSkillEvents(projectId.value, userId.value)
      .then((data) => {
        if (data.success) {
          loadData();
          projectUserState.loadUserDetailsState(projectId.value, userId.value);
        } else {
          overallErrMsg.value = `Skill events were not removed.  ${data.explanation}`;
        }
      }).finally(() => {
    table.value.options.busy = false;
  });
};
const setSkillFilter = (filterValue) => {
  filters.value.global.value = filterValue;
  loadData();
};
const highlight = (value) => {
  const filterValue = filters.value.global.value;
  if (filterValue && filterValue.trim().length > 0) {
    const highlighted = StringHighlighter.highlight(value, filterValue);
    return highlighted || value;
  } else {
    return value;
  }
}

const selectedSkills = ref([]);
</script>

<template>
<div>
  <SubPageHeader title="Performed Skills" aria-label="Performed Skills" />

  <Message v-if="overallErrMsg" severity="error">{{overallErrMsg}}</Message>
  <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
    <template #content>

      <SkillsSpinner :is-loading="!table.options.fields"/>
      <div v-if="table.options.fields">
        <SkillsDataTable
            tableStoredStateId="performedSkillsTable"
            :value="table.items"
            :loading="table.options.busy"
            v-model:selection="selectedSkills"
            show-gridlines
            striped-rows
            lazy
            paginator
            data-cy="performedSkillsTable"
            aria-label="Performed Skills"
            v-model:filters="filters"
            :globalFilterFields="['skillId']"
            @filter="onFilter"
            @page="pageChanged"
            @sort="sortField"
            :rows="table.options.pagination.pageSize"
            :rowsPerPageOptions="table.options.pagination.possiblePageSizes"
            :total-records="table.options.pagination.totalRows"
            v-model:sort-field="sortInfo.sortBy"
            v-model:sort-order="sortInfo.sortOrder">
          <template #header>
            <div class="flex gap-1">
              <InputGroup>
                <InputGroupAddon>
                  <i class="fas fa-search" aria-hidden="true" />
                </InputGroupAddon>
                <InputText class="flex flex-grow-1"
                           v-model="filters['global'].value"
                           v-on:keydown.enter="onFilter"
                           data-cy="performedSkills-skillIdFilter"
                           placeholder="Skill filter"
                           aria-label="Skill ID Filter" />
              </InputGroup>
            </div>
            <div class="flex flex-wrap pt-3">
              <div class="flex-1">
                <SkillsButton label="Filter"
                              icon="fa fa-filter"
                              size="small"
                              outlined
                              @click="onFilter"
                              aria-label="Filter performed skills"
                              data-cy="performedSkills-filterBtn"/>
                <SkillsButton id="filterResetBtn"
                              class="ml-1"
                              label="Reset"
                              icon="fa fa-times"
                              size="small"
                              outlined
                              @click="clearFilter"
                              aria-label="Reset filter for performed skills"
                              data-cy="performedSkills-resetBtn"/>
              </div>
              <div class="flex">
                <SkillsButton label="Delete Selected"
                              icon="fa fa-trash"
                              size="small"
                              outlined
                              :disabled="selectedSkills.length === 0"
                              @click="deleteSelectedSkills"
                              aria-label="Remove selected performed skills from user"
                              data-cy="performedSkills-deleteSelected"/>
                <SkillsButton label="Delete All"
                              icon="fa fa-trash"
                              size="small"
                              outlined
                              class="ml-2"
                              :disabled="table.items.length === 0"
                              @click="deleteAllSkills"
                              aria-label="Remove all performed skills from user"
                              data-cy="performedSkills-deleteAll"/>
              </div>
            </div>
          </template>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
          </template>

          <template #empty>
            <div class="flex justify-content-center flex-wrap h-12rem">
              <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle fa-3x"
                 aria-hidden="true"></i>
              <span class="w-full">
                  <span class="flex align-items-center justify-content-center">There are no records to show</span>
                  <span v-if="filtering" class="flex align-items-center justify-content-center">  Click
                      <SkillsButton class="flex flex align-items-center justify-content-center px-1"
                                    label="Reset"
                                    link
                                    size="small"
                                    @click="clearFilter"
                                    aria-label="Reset filter for performed skills"
                                    data-cy="noResults-performedSkills-resetBtn" /> to clear the existing filter.
                </span>
              </span>
            </div>
          </template>
          <Column selectionMode="multiple" :class="{'flex': responsive.md.value }">
            <template #header>
              <span class="mr-1 lg:mr-0 md:hidden">
                <i class="fas fa-check-double" aria-hidden="true"></i> Select Skills:
              </span>
            </template>
          </Column>
          <Column v-for="(col, index) in table.options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
                  :class="{'flex': responsive.md.value }">
            <template #header>
              <span v-if="col.key === 'controls'" class="sr-only">Controls Heading - Not sortable</span>
              <span v-else><i :class="[col.imageClass, colors.getTextClass(index + 1)]" aria-hidden="true"></i> {{ col.label }}</span>
            </template>
            <template #body="slotProps">
              <div v-if="slotProps.field === 'skillId'" class="flex flex-row flex-wrap"
                   :data-cy="`row${slotProps.index}-skillCell`">
                <div class="flex flex-column">
                  <div class="flex align-items-start justify-content-start">
                    <highlighted-value :value="slotProps.data.skillName"
                                       :filter="filters.global.value"/>
                    <Tag v-if="slotProps.data.importedSkill === true" severity="success" class="uppercase ml-1"
                         data-cy="importedTag">Imported
                    </Tag>
                  </div>
                  <div>
                    <show-more :limit="50" :contains-html="true"
                               :text="`ID: ${highlight(slotProps.data.skillId)}`"/>
                  </div>
                </div>
                <div class="flex flex-grow-1 align-items-start justify-content-end">
                  <SkillsButton icon="fas fa-search-plus"
                                outlined
                                class="ml-2"
                                @click="setSkillFilter(slotProps.data.skillName)"
                                aria-label="Filter by Skill Name"
                                data-cy="addSkillFilter"
                                size="small" />
                </div>
              </div>
              <div v-else-if="slotProps.field === 'performedOn'">
                <DateCell :value="slotProps.data[col.key]" />
              </div>
              <div v-else-if="slotProps.field === 'control'">
                <SkillsButton @click="deleteSkill(slotProps.data)"
                              :id="`deleteSkill-${slotProps.data.skillId}`"
                              icon="fa fa-trash"
                              size="small"
                              outlined
                              :track-for-focus="true"
                              data-cy="deleteEventBtn"
                              v-if="slotProps.data.importedSkill === false"
                              :aria-label="`remove skill ${slotProps.data.skillId} from user`" />
              </div>
              <div v-else>
                <span :data-cy="`row${slotProps.index}-${slotProps.field}`">{{ slotProps.data[col.key] }}</span>
              </div>
            </template>
          </Column>
        </SkillsDataTable>
      </div>

    </template>
  </Card>
  <RemovalValidation
      v-if="showDeleteDialog"
      v-model="showDeleteDialog"
      @do-remove="doDeleteAllSkills"
      removal-text-prefix="This will delete all skill events for"
      :item-name="userIdForDisplay"
      :enable-return-focus="true">
    <div>
      Deletion <b>cannot</b> be undone and permanently removes all skill events for this user.
    </div>
  </RemovalValidation>
</div>
</template>

<style scoped>

</style>