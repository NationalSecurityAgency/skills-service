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
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import NoContent2 from "@/components/utils/NoContent2.vue";
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import AccessService from '@/components/access/AccessService.js';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";
import SelfReportApprovalConfUserTag from "@/components/skills/selfReport/SelfReportApprovalConfUserTag.vue";
import SelfReportApprovalConfSkill from "@/components/skills/selfReport/SelfReportApprovalConfSkill.vue";
import SelfReportApprovalConfSpecificUsers from "@/components/skills/selfReport/SelfReportApprovalConfSpecificUsers.vue";
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import UserRolesUtil from '@/components/utils/UserRolesUtil.js';
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'

const route = useRoute();
const announcer = useSkillsAnnouncer();
const appConfig = useAppConfig();
const colors = useColors()
const responsive = useResponsiveBreakpoints()

const data = ref([]);
const loading = ref(true);
const pageSize = ref(5);
const possiblePageSizes = [5, 10, 15, 20];
const currentPage = ref(1);
const totalRows = ref(0);
const sortBy = ref('userId');
const sortOrder = ref(-1);

onMounted(() => {
  loadData();
});

const hasMoreThanOneApprover = computed(() => {
  return data.value && data.value.length > 1;
});

const userTagConfKey = computed(() => {
  return appConfig.approvalConfUserTagKey;
});

const userTagConfLabel = computed(() => {
  return appConfig.approvalConfUserTagLabel ? appConfig.approvalConfUserTagLabel : appConfig.approvalConfUserTagKey;
});

const toggleConfDetails = (dataToToggle) => {
  toggleRow(dataToToggle.userId);
};

const loadData = () => {
  const pageParams = {
    limit: pageSize.value,
    ascending: sortOrder.value === 1,
    page: currentPage.value,
    orderBy: sortBy.value,
  };

  const roles = ['ROLE_PROJECT_ADMIN', 'ROLE_PROJECT_APPROVER'];
  AccessService.getUserRoles(route.params.projectId, roles, pageParams).then((users) => {
    totalRows.value = users.totalCount;
    SelfReportService.getApproverConf(route.params.projectId).then((approverConf) => {
      const basicTableInfo = users.data.map((u) => {
        const allConf = approverConf.filter((c) => c.approverUserId === u.userId);
        return {
          userIdForDisplay: u.userIdForDisplay,
          userId: u.userId,
          roleName: u.roleName,
          allConf,
        };
      });
      updateTable(basicTableInfo);
    }).finally(() => {
      loading.value = false;
    });
  });
};

const updateTable = (basicTableInfo) => {
  let hasAnyFallbackConf = false;
  let numConfigured = 0;
  let res = basicTableInfo.map((row) => {
    const { allConf } = row;
    let tagConf = allConf.filter((c) => c.userTagKey);
    if (tagConf && tagConf.length > 0) {
      tagConf = tagConf.map((t) => ({ ...t, userTagKeyLabel: t.userTagKey.toLowerCase() === userTagConfKey.value?.toLowerCase() ? userTagConfLabel : t.userTagKey }));
    }
    const userConf = allConf.filter((c) => c.userId);
    const skillConf = allConf.filter((c) => c.skillId);
    const fallbackConf = allConf.find((c) => !c.skillId && !c.userId && !c.userTagKey);
    if (fallbackConf) {
      hasAnyFallbackConf = true;
    }
    const hasConf = tagConf?.length > 0 || userConf?.length > 0 || skillConf?.length > 0;
    if (hasConf) {
      numConfigured += 1;
    }
    return {
      ...row,
      tagConf,
      userConf,
      skillConf,
      allConf,
      fallbackConf,
      isFallbackConfPresent: fallbackConf !== null && fallbackConf !== undefined,
      hasAnyFallbackConf,
      hasConf,
      // eslint-disable-next-line no-underscore-dangle
      _showDetails: !!row._showDetails,
    };
  });
  res = res.map((item) => ({
    ...item,
    lastOneWithoutConf: numConfigured >= (res.length - 1) && !item.hasConf,
    hasAnyFallbackConf,
  }));
  data.value = res;
};


const updatedConf = (newConf) => {
  const itemToUpdate = data.value.find((i) => i.userId === newConf.approverUserId);
  itemToUpdate.allConf.push(newConf);
  updateTable(data.value);
};

const removeConf = (removedConf) => {
  const itemToUpdate = data.value.find((i) => i.userId === removedConf.approverUserId);
  itemToUpdate.allConf = itemToUpdate.allConf.filter((i) => i.id !== removedConf.id);
  updateTable(data.value);
};

const handleFallback = (checked, rowItem) => {
  const itemToUpdate = data.value.find((i) => i.userId === rowItem.userId);
  itemToUpdate.loading = true;
  if (checked) {
    SelfReportService.configureApproverForFallback(route.params.projectId, rowItem.userId)
        .then((newConf) => {
          itemToUpdate.allConf.push(newConf);
          updateTable(data.value);
          // close expanded child
          collapseRow(rowItem.userId);
          nextTick(() => announcer.polite(`Assigned ${newConf.approverUserId} as a fallback approver.`));
          SkillsReporter.reportSkill('ConfigureSelfApprovalWorkload');
        })
        .finally(() => {
          itemToUpdate.loading = false;
        });
  } else {
    SelfReportService.removeApproverConfig(route.params.projectId, rowItem.fallbackConf.id)
        .then(() => {
          itemToUpdate.allConf = itemToUpdate.allConf.filter((i) => i.id !== rowItem.fallbackConf.id);
          updateTable(data.value);
          nextTick(() => announcer.polite('Removed workload configuration successfully.'));
          SkillsReporter.reportSkill('ConfigureSelfApprovalWorkload');
        }).finally(() => {
      itemToUpdate.loading = false;
    });
  }
};

let expandedRows = ref([]);

const toggleRow = (row) => {
  if(expandedRows.value[row]) {
    delete expandedRows.value[row];
  }
  else {
    expandedRows.value[row] = true;
  }

  expandedRows.value = { ...expandedRows.value };
}

const collapseRow = (row) => {
  if(expandedRows.value[row]) {
    delete expandedRows.value[row];
  }

  expandedRows.value = { ...expandedRows.value };
}

const pageChanged = (pagingInfo) => {
  currentPage.value = pagingInfo.page + 1;
  pageSize.value = pagingInfo.rows;
  loadData();
};

const sortTable = (sortContext) => {
  sortBy.value = sortContext.sortField;
  sortOrder.value = sortContext.sortOrder;

  // set to the first page
  currentPage.value = 1;
  loadData();
};
</script>

<template>
  <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
    <template #header>
      <SkillsCardHeader title="Configure Approval Workload"></SkillsCardHeader>
    </template>
    <template #content>
      <SkillsSpinner :is-loading="loading" />

      <SkillsDataTable v-if="hasMoreThanOneApprover"
                       :value="data"
                       v-model:expandedRows="expandedRows"
                       dataKey="userId"
                       show-gridlines
                       striped-rows
                       lazy
                       paginator
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes"
                       @page="pageChanged"
                       @sort="sortTable"
                       :totalRecords="totalRows"
                       :sort-field="sortBy"
                       :sort-order="sortOrder"
                       tableStoredStateId="skillApprovalConfTable"
                       aria-label="Confogire Approval Workload"
                       data-cy="skillApprovalConfTable">
        <Column field="userId" sortable :class="{'flex': responsive.md.value }">
          <template #header>
            <span class=""><i class="fas fa-user" :class="colors.getTextClass(0)" aria-hidden="true"/> Approver</span>
          </template>
          <template #body="slotProps">
            {{ slotProps.data.userIdForDisplay }}
          </template>
        </Column>
        <Column field="roleName" sortable :class="{'flex': responsive.lg.value }">
          <template #header>
            <span class=""><i class="fas fa-id-card" :class="colors.getTextClass(1)" aria-hidden="true"/> Role</span>
          </template>
          <template #body="slotProps">
            {{ UserRolesUtil.userRoleFormatter(slotProps.data.roleName) }}
          </template>
        </Column>
        <Column field="workload" :class="{'flex': responsive.lg.value }">
          <template #header>
            <span class=""><i class="fas fa-users" :class="colors.getTextClass(2)" aria-hidden="true"/> Approval Workload</span>
          </template>
          <template #body="slotProps">
            <div class="flex gap-2 flex-column sm:flex-row" :data-cy="`workloadCell_${slotProps.data.userId}`">
              <div class="flex flex-1 gap-2 align-items-center">
                <div v-if="!slotProps.data.hasConf" class="flex flex-1 gap-2 align-items-center">
                  <InputSwitch
                      :aria-label="`Enable and disable fallback for ${slotProps.data.userId} approve`"
                      @update:modelValue="handleFallback($event, slotProps.data)"
                      data-cy="fallbackSwitch"
                      v-model="slotProps.data.isFallbackConfPresent" />
                    <span v-if="!slotProps.data.hasAnyFallbackConf">Default Fallback - All Unmatched Requests</span>
                    <span v-if="slotProps.data.hasAnyFallbackConf && !slotProps.data.fallbackConf">Not Handling Approval Workload</span>
                    <span v-if="slotProps.data.fallbackConf">Assigned Fallback - All Unmatched Requests</span>
                </div>
                <div v-if="slotProps.data.tagConf && slotProps.data.tagConf.length > 0">
                  <div v-for="tConf in slotProps.data.tagConf" :key="tConf.userTagValue">Users in <span class="font-italic text-secondary">{{tConf.userTagKeyLabel}}:</span> <span>{{tConf.userTagValue}}</span></div>
                </div>
                <div v-if="slotProps.data.userConf && slotProps.data.userConf.length > 0" >
                  <Tag variant="success">{{slotProps.data.userConf.length}}</Tag> Specific User{{ slotProps.data.userConf.length > 1 ? 's' : '' }}
                </div>
                <div v-if="slotProps.data.skillConf && slotProps.data.skillConf.length > 0" >
                  <Tag variant="info">{{ slotProps.data.skillConf.length }}</Tag> Specific Skill{{ slotProps.data.skillConf.length  > 1 ? 's' : '' }}
                </div>
              </div>
              <div>
                <SkillsButton size="small"
                          :aria-label="`Edit ${slotProps.data.userIdForDisplay} approval workload`"
                          variant="outline-primary"
                          :disabled="slotProps.data.isFallbackConfPresent || slotProps.data.lastOneWithoutConf"
                          data-cy="editApprovalBtn"
                          @click="toggleConfDetails(slotProps.data)"
                          :icon="expandedRows[slotProps.data.userId] ? 'fas fa-arrow-alt-circle-up' : 'fas fa-edit'"
                          :label="expandedRows[slotProps.data.userId] ? 'Collapse' : 'Edit'" />
              </div>
            </div>
          </template>
        </Column>

        <template #expansion="slotProps">
          <div :data-cy="`expandedChild_${slotProps.data.userId}`">
            <self-report-approval-conf-user-tag v-if="userTagConfKey"
                                                :user-info="slotProps.data"
                                                :tag-key="userTagConfKey"
                                                :tag-label="userTagConfLabel"
                                                @conf-added="updatedConf"
                                                @conf-removed="removeConf"
                                                class=""/>
            <self-report-approval-conf-skill
                :user-info="slotProps.data"
                @conf-added="updatedConf"
                @conf-removed="removeConf"
                class="mt-3"/>
            <self-report-approval-conf-specific-users
                :user-info="slotProps.data"
                @conf-added="updatedConf"
                @conf-removed="removeConf"
                class="mt-3"/>
          </div>
        </template>
      </SkillsDataTable>

      <no-content2 v-if="!hasMoreThanOneApprover && !loading" title="Not Available" class="py-8" icon-size="fa-2x" icon="fas fa-cogs" data-cy="approvalConfNotAvailable">
        The ability to split the approval workload is unavailable because there is only <Badge variant="info">1</Badge> Admin for this project.
        Please add <b>Admins</b> or <b>Approvers</b> on the <router-link :to="{ name: 'ProjectAccess' }" style="text-decoration: underline" data-cy="navToAccessPage"><i class="fas fa-shield-alt skills-color-access" aria-hidden="true"/>Access</router-link> page in order to start using this feature.
      </no-content2>
    </template>
  </Card>
</template>

<style scoped>

</style>