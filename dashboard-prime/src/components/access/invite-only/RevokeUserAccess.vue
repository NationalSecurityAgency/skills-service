<script setup>
import { computed, onMounted, ref, nextTick } from 'vue'
import SkillsCardHeader from '@/components/utils/cards/SkillsCardHeader.vue'
import InputText from 'primevue/inputtext'
import { useRoute } from 'vue-router'
import AccessService from '@/components/access/AccessService.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import Column from 'primevue/column'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAnnouncer } from '@vue-a11y/announcer'
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import { FilterMatchMode } from 'primevue/api'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'

const route = useRoute()
const responsive = useResponsiveBreakpoints()
const colors = useColors()
const appConfig = useAppConfig()
const announcer = useAnnouncer()
const dialogMessages =useDialogMessages()

const userField = ref('')
const applyFilters = () => {

}
const reset = () => {
  // this.filters.user = '';
  // this.table.options.pagination.currentPage = 1;
  // this.loadData().then(() => {
  //   this.$nextTick(() => this.$announcer.polite('Revoke user access table filters have been removed'));
  // });
}

const tableIsBusy = ref(false)
const data = ref([])
const loadingData = ref(true)
const pageSize = ref(5)
const possiblePageSizes = [5, 10, 15, 20]
const sortInfo = ref({ sortOrder: -1, sortBy: 'userId' })
const totalRows = ref(0)
const currentPage = ref(1)


const loadData = () => {
  tableIsBusy.value = true
  const pageParams = {
    query: tableFilters.value.userId.value || '',
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder === 1,
    page: currentPage.value,
    orderBy: sortInfo.value.sortBy
  }

  return AccessService.getUserRolesForProject(route.params.projectId, 'ROLE_PRIVATE_PROJECT_USER', pageParams).then((res) => {
    data.value = res.data
    totalRows.value = res.totalCount
    tableIsBusy.value = false
  })
}

const hasData = ref(false)

onMounted(() => {
  loadData().then(() => {
    hasData.value = totalRows.value > 0
    loadingData.value = false
  })
})

const getUserDisplay = (props, fullName = false) => {
  const userDisplay = props.userIdForDisplay ? props.userIdForDisplay : props.userId
  const oAuthProviders = appConfig.oAuthProviders
  let userName = ''
  if (fullName && props.firstName && props.lastName) {
    userName = ` (${props.lastName}, ${props.firstName})`
  }
  if (oAuthProviders) {
    const indexOfDash = userDisplay.lastIndexOf('-')
    if (indexOfDash > 0) {
      const provider = userDisplay.substr(indexOfDash + 1)
      if (oAuthProviders.includes(provider)) {
        return `${userDisplay.substr(0, indexOfDash)}${userName}`
      }
    }
  }
  return `${userDisplay}${userName}`
}

const revokeAccess = (userId, userIdForDisplay) => {
  const msg = `Are you sure you want to revoke ${userIdForDisplay}'s access to this Project? ${userIdForDisplay}'s achievements will NOT be deleted,
        however ${userIdForDisplay} will no longer be able to access the training profile.`;
  dialogMessages.msgConfirm(msg, 'Revoke Access', (val) => {
      tableIsBusy.value = true;
      AccessService.deleteUserRole(route.params.projectId, userId, 'ROLE_PRIVATE_PROJECT_USER').then(() => {
        loadData();
        nextTick(() => {
          announcer.polite(`Revoked project access for user ${userIdForDisplay}`);
        });
      }).finally(() => {
        tableIsBusy.value = false;
      });
  })
}

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows
  currentPage.value = pagingInfo.page + 1
  loadData()
}
const sortField = (column) => {
  // set to the first page
  currentPage.value = 1
  loadData()
}

const onFilter = () => {
  // set to the first page
  currentPage.value = 1
  loadData()
}

const tableFilters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  userId: { value: null, matchMode: FilterMatchMode.CONTAINS },
})

</script>

<template>
  <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
    <template #header>
      <SkillsCardHeader title="Project User: Revoke">
        <template #headerIcon><i class="fas fa-user-lock mr-2 text-red-500"
                                 aria-hidden="true"/></template>
      </SkillsCardHeader>
    </template>
    <template #content>
      <div class="">
        <skills-spinner :is-loading="loadingData" />
        <no-content2 v-if="!hasData"
                     class="py-5"
                     title="No one one joined yet"
                     message="Once a user has accepted an invitation to join a project configured for Invite Only visibility, that user will show up under the Revoke Access table at which point their access can be revoked and they will no longer have access to the project." />
        <div v-if="!loadingData">
          <div v-if="hasData">
            <SkillsDataTable
              v-if="hasData"
              :is-busy="tableIsBusy"
              tableStoredStateId="revokeInvitesTable"
              :value="data"
              data-cy="privateProjectUsersTable"
              paginator
              lazy
              @page="pageChanged"
              @sort="sortField"
              @filter="onFilter"
              :rows="pageSize"
              :rowsPerPageOptions="possiblePageSizes"
              :totalRecords="totalRows"
              filterDisplay="row"
              :globalFilterFields="['userId']"
              v-model:filters="tableFilters"
              v-model:sort-field="sortInfo.sortBy"
              v-model:sort-order="sortInfo.sortOrder">
              <template #empty>
                No Results
              </template>
              <template #paginatorstart>
                <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
              </template>

              <Column field="userId" header="User"
                      :showFilterMenu="false"
                      :sortable="true" :class="{'flex': responsive.md.value }">
                <template #header>
                  <i class="fas fa-envelope-open-text mr-1" :class="colors.getTextClass(0)" aria-hidden="true"></i>
                </template>
                <template #filter="{ filterModel, filterCallback }">
                  <InputText v-model="filterModel.value"
                             type="text"
                             class="p-column-filter"
                             data-cy="privateProjectUsers-userIdFilter"
                             style="min-width: 10rem"
                             @input="filterCallback()"
                             placeholder="Search by User ID" />
                </template>
                <template #body="slotProps">
                  <div class="flex">
                    <div class="flex-1">
                      <highlighted-value
                        :filter="tableFilters['userId'].value || ''"
                        :value="getUserDisplay(slotProps.data, true)" />
                    </div>
                    <div>
                      <SkillsButton
                        icon="fas fa-user-slash"
                        label="Remove Access"
                        data-cy="privateProjectUsersTable_revokeUserAccessBtn"
                        @click="revokeAccess(slotProps.data.userId, getUserDisplay(slotProps.data))"
                        :aria-label="`Remove project access for ${getUserDisplay(slotProps.data)}`"
                        />
                    </div>
                  </div>
                </template>
              </Column>

            </SkillsDataTable>

          </div>
        </div>

      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>