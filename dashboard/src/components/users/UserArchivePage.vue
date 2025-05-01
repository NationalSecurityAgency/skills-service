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

import { useRoute } from 'vue-router';
import { onMounted, ref } from 'vue';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import { useFocusState } from '@/stores/UseFocusState.js';
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import UsersService from '@/components/users/UsersService.js'
import NoContent2 from '@/components/utils/NoContent2.vue';
import SkillsDisplayPathAppendValues from '@/router/SkillsDisplayPathAppendValues.js';
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue';

const announcer = useSkillsAnnouncer()
const route = useRoute()
const userInfo = useUserInfo()
const appConfig = useAppConfig()
const focusState = useFocusState()
const responsive = useResponsiveBreakpoints()

const initialLoad = ref(true)
const archivedUsers = ref([])
const options = ref({
  busy: true,
  bordered: true,
  outlined: true,
  stacked: 'md',
  sortBy: 'userIdForDisplay',
  sortDesc: true,
  fields: [
    {
      key: 'userIdForDisplay',
      label: 'User',
      sortable: true
    }
  ],
  pagination: {
    server: false,
    currentPage: 1,
    totalRows: 0,
    pageSize: 5,
    possiblePageSizes: [5, 10, 20, 50]
  }
})
const sortInfo = ref({ sortOrder: -1, sortBy: 'userIdForDisplay' })

onMounted(() => {
  loadData()
})

const loadData = () => {
  const params = {
    limit: options.value.pagination.pageSize,
    ascending: sortInfo.value.sortOrder === 1,
    page: options.value.pagination.currentPage,
    orderBy: sortInfo.value.sortBy
  }
  return UsersService.getArchivedUsers(route.params.projectId, params)
      .then((res) => {
        archivedUsers.value = res.data
        options.value.pagination.totalRows = res.totalCount
        options.value.busy = false
      })
      .finally(() => {
        initialLoad.value = false;
        options.value.busy = false;
      })
}

const restoreUser = (user) => {
  options.value.busy = true
  const { userIdForDisplay, userId } = user
  UsersService.restoreArchivedUser(route.params.projectId, userId)
      .then(() => {
        loadData()
            .finally(() => {
              document.getElementById('backToProjectUsersBtn').focus()
              announcer.polite(`Admin ${userIdForDisplay} was restored`)
            })
      })
}
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

const pageChanged = (pagingInfo) => {
  options.value.pagination.pageSize = pagingInfo.rows
  options.value.pagination.currentPage = pagingInfo.page + 1
  loadData()
}
const sortField = (column) => {
  // set to the first page
  options.value.pagination.currentPage = 1
  loadData()
}

</script>

<template>
  <Card :pt="{ body: { class: 'p-0!' } }">
    <template #header>
      <SkillsCardHeader title="Archived Project Users"></SkillsCardHeader>
    </template>
    <template #content>
      <SkillsSpinner :is-loading="initialLoad" />
      <div v-if="!initialLoad">
        <SkillsDataTable
            tableStoredStateId="userArchiveTable"
            aria-label="User Archive Table"
            :value="archivedUsers"
            :loading="options.busy"
            show-gridlines
            striped-rows
            lazy
            paginator
            id="userArchiveTable"
            data-cy="userArchiveTable"
            :total-records="options.pagination.totalRows"
            :rows="options.pagination.pageSize"
            :rowsPerPageOptions="options.pagination.possiblePageSizes"
            @page="pageChanged"
            @sort="sortField"
            v-model:sort-field="sortInfo.sortBy"
            v-model:sort-order="sortInfo.sortOrder">

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ options.pagination.totalRows }}</span>
          </template>

          <Column v-for="col of options.fields" :key="col.key" :field="col.key" :sortable="col.sortable"
                  :class="{'flex': responsive.md.value }">
            <template #header>
                <span><i class="fas fa-user skills-color-users" aria-hidden="true"></i> {{ col.label }}</span>
            </template>
            <template #body="slotProps">
              <div v-if="slotProps.field === 'userIdForDisplay'" class="flex flex-row flex-wrap"
                   :data-cy="`archivedUser_${slotProps.data.userId}`">
                <div class="flex items-start justify-start">
                  <router-link
                      :to="calculateClientDisplayRoute(slotProps.data)"
                      aria-label="View user details"
                      data-cy="usersTable_viewDetailsLink"
                  >
                    {{ userInfo.getUserDisplay(slotProps.data, true) }}
                  </router-link>
                </div>
                <div class="flex grow items-start justify-end">
                  <SkillsButton :data-cy="`restoreUser-${slotProps.data.userId}`"
                                :id="`restoreUser-${slotProps.data.userId}`"
                                @click="restoreUser(slotProps.data)"
                                icon="fas fa-undo"
                                size="small"
                                outlined
                                label="Restore"
                                :track-for-focus="true"
                                :aria-label="`restore ${slotProps.data.userId} from the user archive`" />
                </div>
              </div>
            </template>
          </Column>
          <template #empty>
            <no-content2 title="No Archived Users..." icon="fas fa-folder-open">
              <div>
                <p>
                  There are no archived users for this project.  Users that have been archived will be excluded from project statistics.  Users that have been archived will be shown and can be restored here.
                </p>
              </div>
            </no-content2>
          </template>
        </SkillsDataTable>

      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>