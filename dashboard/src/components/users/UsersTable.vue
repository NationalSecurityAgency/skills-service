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
import { nextTick, onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import InputText from 'primevue/inputtext'
import Slider from 'primevue/slider'
import Column from 'primevue/column'
import ProgressBar from 'primevue/progressbar'
import UsersService from './UsersService.js'
import DateCell from '@/components/utils/table/DateCell.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useExportUtil } from '@/components/utils/UseExportUtil.js';
import SkillsDisplayPathAppendValues from '@/router/SkillsDisplayPathAppendValues.js'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import {useUserInfo} from "@/components/utils/UseUserInfo.js";
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useProjConfig } from '@/stores/UseProjConfig.js';
import {useProjDetailsState} from "@/stores/UseProjDetailsState.js";
import TableNoRes from "@/components/utils/table/TableNoRes.vue";

const route = useRoute()
const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const appConfig = useAppConfig()
const userInfo = useUserInfo()
const exportUtil = useExportUtil()
const numberFormat = useNumberFormat()
const projConfig = useProjConfig()
const projectDetailsState = useProjDetailsState()

let filters = ref({
  user: '',
  progress: [0, 100],
})

const data = ref([])
const isLoading = ref(true)
const isExporting = ref(false)
const totalPoints = ref(0)
const currentPage = ref(1)
const totalRows = ref(1)
const pageSize = ref(5)
const possiblePageSizes = [5, 10, 15, 20]
const sortInfo = ref({ sortOrder: -1, sortBy: 'lastUpdated' })
const selectedRows = ref([])

const showUserTagColumn = computed(() => {
 return !!(appConfig.usersTableAdditionalUserTagKey && appConfig.usersTableAdditionalUserTagLabel);
})

const tagKey = computed(() => {
  return appConfig.usersTableAdditionalUserTagKey;
});

onMounted(() => {
  loadData()
})

const applyFilters = () => {
  currentPage.value = 1
  if (filters.value.progress[1] > 100) {
    filters.value.progress[1] = 100
  }
  if (filters.value.progress[0] < 0) {
    filters.value.progress[0] = 0
  }

  loadData().then(() => {
    let filterMessage = 'Users table has been filtered by'
    if (filters.value.user) {
      filterMessage += ` ${filters.value.user}`
    }
    if (filters.value.progress[0] > 0) {
      filterMessage += `${filters.value.user ? ' and' : ''} users with at least ${filters.value.progress[0]} percent complete`
    }
    if (filters.value.progress < 100) {
      filterMessage += `${filters.value.user ? ' and' : ''} users with at most ${filters.value.progress[1]} percent complete`
    }
    nextTick(() => announcer.polite(filterMessage))
  })
}

const reset = () => {
  filters.value.user = ''
  filters.value.progress = [0, 100]
  currentPage.value = 1
  loadData().then(() => {
    nextTick(() => announcer.polite('Users table filters have been removed'))
  })
}

const getUrl = () => {
  let url = `/admin/projects/${encodeURIComponent(route.params.projectId)}`
  if (route.params.skillId) {
    url += `/skills/${encodeURIComponent(route.params.skillId)}`
  } else if (route.params.badgeId) {
    url += `/badges/${encodeURIComponent(route.params.badgeId)}`
  } else if (route.params.subjectId) {
    url += `/subjects/${encodeURIComponent(route.params.subjectId)}`
  } else if (route.params.tagKey && route.params.tagFilter) {
    url += `/userTags/${encodeURIComponent(route.params.tagKey)}/${encodeURIComponent(route.params.tagFilter)}`
  }
  url += '/users'
  return url
}

const isProjectLevel = computed(() => {
  return !(route.params.skillId || route.params.badgeId || route.params.subjectId || (route.params.tagKey && route.params.tagFilter))
})

const calculateClientDisplayRoute = (props) => {
  return {
    name: `SkillsDisplay${SkillsDisplayPathAppendValues.SkillsDisplayPreview}`,
    params: {
      projectId: route.params.projectId,
      userId: props.userId,
      dn: props.dn
    }
  }
}

const loadData = () => {
  const url = getUrl()
  isLoading.value = true
  return UsersService.ajaxCall(url, {
    query: filters.value.user,
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder !== -1,
    page: currentPage.value,
    byColumn: 0,
    orderBy: sortInfo.value.sortBy,
    minimumPoints: filters.value.progress[0],
    maximumPoints: filters.value.progress[1],
  }).then((res) => {
    data.value = res.data
    totalRows.value = res.count
    totalPoints.value = res.totalPoints
    selectedRows.value = []
    isLoading.value = false
  })
}

const calcPercent = (userPoints) => {
  if (!totalPoints.value) {
    return 'N/A'
  }
  return Math.trunc((userPoints / totalPoints.value) * 100)
}

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
  currentPage.value = pagingInfo.page + 1
  loadData()
}

const sortField = () => {
  // set to the first page
  currentPage.value = 1
  loadData()
}

const exportUsers = () => {
  const url = `${getUrl()}/export/excel`
  isLoading.value = true
  isExporting.value = true
  return exportUtil.ajaxDownload(url, {
    query: filters.value.user,
    ascending: sortInfo.value.sortOrder !== -1,
    page: currentPage.value,
    orderBy: sortInfo.value.sortBy,
    minimumPoints: filters.value.progress[0],
    maximumPoints: filters.value.progress[1],
  }).then((res) => {
    isLoading.value = false
    isExporting.value = false
  })
}

const archiveUsers = () => {
  const userIds = selectedRows.value.map((row) => row.userId)
  UsersService.archiveUsers(route.params.projectId, userIds).then(() => {
    loadData()
  })
}
</script>

<template>
  <div class="w-full">
    <div class="px-6 py-4">
      <div class="flex flex-col lg:flex-row gap-6 my-2">
        <div class="xl:flex-1">
          <div>
            <label for="userFilter">User Filter</label>
          </div>
          <InputText id="userFilter" v-model="filters.user" v-on:keydown.enter="applyFilters"
                     class="w-full mt--3"
                     data-cy="users-skillIdFilter" aria-label="user filter" />
        </div>
        <div class="flex-1">
          <div>
            <label for="minimumProgress">User Progress Filter</label>
          </div>
          <div class="flex gap-2">
            <div class="flex-1">
              <div class="flex mt-4 items-center">
                <span class="mr-4">0%</span>
                <div class="flex flex-1 flex-col">
                  <Slider v-model="filters.progress" v-on:keydown.enter="applyFilters" :min="0" :max="100" range
                          data-cy="users-progress-range" aria-label="user progress range filter" />
                </div>
                <span class="ml-4">100%</span>
              </div>
            </div>
            <div class="flex gap-2 w-[14rem]">
              <InputGroup class="p-0 m-0">
                <InputGroupAddon> &gt;= </InputGroupAddon>
                <InputText v-model.number="filters.progress[0]" v-on:keydown.enter="applyFilters" :min="0" :max="100" id="minimumProgress"
                           data-cy="users-progress-input" aria-label="minimum user progress input filter" inputId="minimumProgress"
                           class="p-0 m-0" />
              </InputGroup>
              <InputGroup class="p-0 m-0">
                <InputGroupAddon> &lt;{{ filters.progress[1] === 100 ? '=' : ''}} </InputGroupAddon>
                <InputText v-model.number="filters.progress[1]" v-on:keydown.enter="applyFilters" :min="0" :max="100" id="maximumProgress"
                           data-cy="users-max-progress-input" aria-label="maximum user progress input filter" inputId="maximumProgress"
                           class="p-0 m-0" />
              </InputGroup>
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-2 mt-2 mb-6">
        <SkillsButton icon="fa fa-filter" label="Filter" outlined @click="applyFilters" data-cy="users-filterBtn" size="small" />
        <SkillsButton icon="fa fa-times" label="Reset" outlined @click="reset" class="ml-1" data-cy="users-resetBtn" size="small" />
      </div>
    </div>
    <div>
      <SkillsDataTable
        :value="data" :loading="isLoading" size="small" stripedRows showGridlines paginator lazy
        :totalRecords="totalRows" :rows="pageSize" @page="pageChanged"
        tableStoredStateId="usersTable" data-cy="usersTable"
        aria-label="Users"
        :rowsPerPageOptions="possiblePageSizes"
        v-model:sort-field="sortInfo.sortBy"
        v-model:sort-order="sortInfo.sortOrder"
        v-model:selection="selectedRows"
        @sort="sortField"
        data-key="userId"
      >
        <template #loading>
          <div>
            <Message v-if="isExporting" icon="fas fa-download" severity="contrast" :closable="false">Exporting, please wait...</Message>
            <SkillsSpinner :is-loading="true"></SkillsSpinner>
          </div>
        </template>
        <template v-if="isProjectLevel" #header>
          <div class="flex justify-end flex-wrap">
            <SkillsButton
                v-if="!projConfig.isReadOnlyProj"
                class="mr-2"
                :disabled="selectedRows <= 0"
                size="small"
                icon="fas fa-archive"
                label="Archive"
                @click="archiveUsers"
                data-cy="archiveUsersTableBtn" />
            <SkillsButton
                :disabled="totalRows <= 0"
                size="small"
                icon="fas fa-download"
                label="Export"
                @click="exportUsers"
                data-cy="exportUsersTableBtn" />
          </div>
        </template>
        <Column v-if="isProjectLevel && !projConfig.isReadOnlyProj" selectionMode="multiple" :class="{'flex': responsive.md.value }">
          <template #header>
            <span class="mr-1 lg:mr-0 lg:hidden"><i class="fas fa-check-double" aria-hidden="true"></i> Select Rows:</span>
          </template>
        </Column>
        <Column field="userId" header="User" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-user skills-color-users mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <router-link
              :to="calculateClientDisplayRoute(slotProps.data)"
              aria-label="View user details"
              data-cy="usersTable_viewDetailsLink"
            >
              {{ userInfo.getUserDisplay(slotProps.data, true) }}
            </router-link>
          </template>
        </Column>
        <Column v-if="showUserTagColumn"
                :field="appConfig.usersTableAdditionalUserTagKey"
                :header="appConfig.usersTableAdditionalUserTagLabel"
                :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-tag mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <router-link
                v-if="showUserTagColumn && slotProps.data.userTag"
                :to="{ name: 'UserTagMetrics', params: { projectId: route.params.projectId, tagKey: tagKey, tagFilter: slotProps.data.userTag } }"
                class="text-info mb-0 pb-0 preview-card-title"
                :aria-label="`View metrics for ${slotProps.data.userTag}`"
                role="link"
                data-cy="usersTable_viewUserTagMetricLink">
              {{ slotProps.data.userTag }}
            </router-link>
          </template>
        </Column>
        <Column field="totalPoints" header="Progress" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="far fa-arrow-alt-circle-up mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <div :data-cy="`usr_progress-${slotProps.data.userId}`" class="w-full">
              <div class="flex">
                <div class="flex flex-auto">
                  <span class="font-weight-bold text-primary"
                        :aria-label="`${calcPercent(slotProps.data.totalPoints)} percent completed`"
                        data-cy="progressPercent">{{ calcPercent(slotProps.data.totalPoints) }}%</span>
                </div>
                <div class="flex flex-auto justify-end">
                  <span class="text-primary font-weight-bold"
                        :aria-label="`${slotProps.data.totalPoints} out of ${totalPoints} total points`"
                        data-cy="progressCurrentPoints">{{ slotProps.data.totalPoints?.toLocaleString() }}</span> /
                  <span class="italic" data-cy="progressTotalPoints">{{ totalPoints?.toLocaleString() }}</span>
                </div>
              </div>
              <ProgressBar style="height: 5px;" :value="calcPercent(slotProps.data.totalPoints)" :showValue="false"
                           class="lg:min-w-[12rem] xl:min-w-[20rem]"
                           :aria-label="`Progress for ${slotProps.data.userId} user`" />
              <div v-if="slotProps.data.userMaxLevel || slotProps.data.userMaxLevel === 0" class="row"
                   data-cy="progressLevels">
                <div class="col">
                  <i class="fas fa-trophy skills-color-levels" aria-hidden="true" /> <span class="italic">Current Level: </span>
                  <span v-if="slotProps.data.userMaxLevel === 0" data-cy="progressCurrentLevel">None</span>
                  <span v-else class="font-weight-bold" data-cy="progressCurrentLevel">{{ slotProps.data.userMaxLevel
                    }}</span>
                </div>
              </div>
            </div>
          </template>
        </Column>
        <Column field="firstUpdated" header="Points First Earned" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="far fa-clock mr-1" :class="colors.getTextClass(3)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.firstUpdated" />
          </template>
        </Column>
        <Column field="lastUpdated" header="Points Last Earned" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="far fa-clock mr-1" :class="colors.getTextClass(3)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.lastUpdated" />
          </template>
        </Column>

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(totalRows) }}</span>
        </template>

        <template #empty>
          <table-no-res />
        </template>
      </SkillsDataTable>
    </div>
  </div>
</template>

<style scoped>
</style>
