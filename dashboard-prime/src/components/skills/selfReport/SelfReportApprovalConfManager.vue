<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import NoContent2 from "@/components/utils/NoContent2.vue";
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import AccessService from '@/components/access/AccessService.js';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsDataTable from "@/components/utils/table/SkillsDataTable.vue";

const route = useRoute();
const announcer = useSkillsAnnouncer();

const loading = ref(true);
const table = ref({
  items: [],
  options: {
    busy: false,
        bordered: true,
        outlined: true,
        stacked: 'md',
        detailsTdClass: 'p-0 m-0',
        sortBy: 'requestedOn',
        sortDesc: true,
        emptyText: 'You are the only user',
        tableDescription: 'Configure Approval Workload',
        fields: [
      {
        key: 'userIdForDisplay',
        label: 'Approver',
        sortable: true,
        tdClass: 'p-0 m-0',
      },
      {
        key: 'roleName',
        label: 'Role',
        sortable: true,
      },
      {
        key: 'workload',
        label: 'Approval Workload',
        sortable: false,
      },
    ],
        pagination: {
      remove: true,
          server: true,
          currentPage: 1,
          totalRows: 1,
          pageSize: 5,
          possiblePageSizes: [5, 10, 15, 20],
    },
  },
});

onMounted(() => {
  loadData();
});

const hasMoreThanOneApprover = computed(() => {
  return table.value.items && table.value.items.length > 1;
});

const userTagConfKey = computed(() => {
  // return $store.getters.config.approvalConfUserTagKey;
});

const userTagConfLabel = computed(() => {
  // return $store.getters.config.approvalConfUserTagLabel ? $store.getters.config.approvalConfUserTagLabel : $store.getters.config.approvalConfUserTagKey;
});


const toggleConfDetails = (data) => {
  // eslint-disable-next-line no-underscore-dangle
  table.value.items = table.value.items.map((item) => ({ ...item, _showDetails: data.item.userId === item.userId ? !item._showDetails : false }));
};

const loadData = () => {
  const pageParams = {
    limit: 200,
    ascending: true,
    page: 1,
    orderBy: 'userId',
  };
  const roles = ['ROLE_PROJECT_ADMIN', 'ROLE_PROJECT_APPROVER']; // = [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER];
  AccessService.getUserRoles(route.params.projectId, roles, pageParams)
      .then((users) => {
        SelfReportService.getApproverConf(route.params.projectId)
            .then((approverConf) => {
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
      tagConf = tagConf.map((t) => ({ ...t, userTagKeyLabel: t.userTagKey.toLowerCase() === userTagConfKey?.toLowerCase() ? userTagConfLabel : t.userTagKey }));
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
  table.value.items = res;
};

const removeTagConf = (removedConf) => {
  removeConf(removedConf, 'tagConf');
};

const updatedConf = (newConf) => {
  const itemToUpdate = table.value.items.find((i) => i.userId === newConf.approverUserId);
  itemToUpdate.allConf.push(newConf);
  updateTable(table.value.items);
};

const removeConf = (removedConf) => {
  const itemToUpdate = table.value.items.find((i) => i.userId === removedConf.approverUserId);
  itemToUpdate.allConf = itemToUpdate.allConf.filter((i) => i.id !== removedConf.id);
  updateTable(table.value.items);
};

const handleFallback = (checked, rowItem) => {
  const itemToUpdate = table.value.items.find((i) => i.userId === rowItem.userId);
  itemToUpdate.loading = true;
  if (checked) {
    SelfReportService.configureApproverForFallback(route.params.projectId, rowItem.userId)
        .then((newConf) => {
          itemToUpdate.allConf.push(newConf);
          updateTable(table.value.items);
          // close expanded child
          table.value.items = table.value.items.map((i) => ({ ...i, _showDetails: false }));
          nextTick(() => announcer.polite(`Assigned ${newConf.approverUserId} as a fallback approver.`));
        })
        .finally(() => {
          itemToUpdate.loading = false;
        });
  } else {
    SelfReportService.removeApproverConfig(route.params.projectId, rowItem.fallbackConf.id)
        .then(() => {
          itemToUpdate.allConf = itemToUpdate.allConf.filter((i) => i.id !== rowItem.fallbackConf.id);
          updateTable(table.value.items);
          nextTick(() => announcer.polite('Removed workload configuration successfully.'));
        }).finally(() => {
      itemToUpdate.loading = false;
    });
  }
};

// v-model:selection="selectedItems"
// v-model:expandedRows="expandedRows"
// :rows="pageSize"
// :rowsPerPageOptions="possiblePageSizes"
// :totalRecords="totalRows"
// :busy="loading"
// :sort-field="sortBy"
// :sort-order="sortOrder"
// @page="pageChanged"
// data-key="id"
// @sort="sortTable"
</script>

<template>
  <Card>
    <template #header>
      <SkillsCardHeader title="Configure Approval Workload"></SkillsCardHeader>
    </template>
    <template #content>

      <SkillsDataTable v-if="hasMoreThanOneApprover"
                       :value="table.items"
                       tableStoredStateId="skillApprovalConfTable"
                       data-cy="skillApprovalConfTable" paginator lazy>
        <Column field="userIdForDisplay"></Column>
        <Column field="roleName"></Column>
        <Column field="workload"></Column>
      </SkillsDataTable>

      <no-content2 v-if="!hasMoreThanOneApprover" title="Not Available" class="my-5" icon-size="fa-2x" icon="fas fa-cogs" data-cy="approvalConfNotAvailable">
        The ability to split the approval workload is unavailable because there is only <Badge variant="info">1</Badge> Admin for this project.
        Please add <b>Admins</b> or <b>Approvers</b> on the <router-link :to="{ name: 'ProjectAccess' }" style="text-decoration: underline" data-cy="navToAccessPage"><i class="fas fa-shield-alt skills-color-access" aria-hidden="true"/>Access</router-link> page in order to start using this feature.
      </no-content2>
    </template>
  </Card>
</template>

<style scoped>

</style>