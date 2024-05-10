<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import DateCell from "@/components/utils/table/DateCell.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import ExistingUserInput from "@/components/utils/ExistingUserInput.vue";
import { SkillsReporter } from '@skilltree/skills-client-js'

const route = useRoute();
const announcer = useSkillsAnnouncer();
const props = defineProps({
  userInfo: Object,
});
const emit = defineEmits(['conf-added', 'conf-removed']);

const currentSelectedUser = ref(null);
const loading = ref(false);
const data = ref([]);
const sortBy = ref('updated');
const sortOrder = ref(-1);
const pageSize = 4;
const possiblePageSizes = [4, 10, 15, 20];

onMounted(() => {
  const hasConf = props.userInfo.userConf && props.userInfo.userConf.length > 0;
  if (hasConf) {
    data.value = props.userInfo.userConf.map((u) => ({ ...u }));
  }
});

let hadData = computed(() => {
  return data.value && data.value.length > 0;
});

const addConf = () => {
  SkillsReporter.reportSkill('ConfigureSelfApprovalWorkload');
  loading.value = true;
  const currentUserId = currentSelectedUser.value.dn ? currentSelectedUser.value.dn : currentSelectedUser.value.userId;
  SelfReportService.configureApproverForUserId(route.params.projectId, props.userInfo.userId, currentUserId)
      .then((res) => {
        data.value.push(res);
        emit('conf-added', res);
        nextTick(() => announcer.polite(`Added workload configuration successfully for ${currentUserId} user.`));
        currentSelectedUser.value = null;
      }).finally(() => {
    loading.value = false;
  });
};

const removeTagConf = (removedItem) => {
  data.value = data.value.map((i) => ({ ...i, deleteInProgress: i.id === removedItem.id }));
  return SelfReportService.removeApproverConfig(route.params.projectId, removedItem.id)
      .then(() => {
        data.value = data.value.filter((i) => i.id !== removedItem.id);
        emit('conf-removed', removedItem);
        nextTick(() => announcer.polite('Removed workload configuration successfully.'));
      });
}
</script>

<template>
  <Card>
    <template #header>
      <SkillsCardHeader title="Split Workload By Specific Users"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex mx-2">
        <div class="flex flex-1 px-1">
          <existing-user-input
              class="w-full"
              v-model="currentSelectedUser"
              :can-enter-new-user="false"
              name="User Id"
              aria-label="Select User Id"
              data-cy="userIdInput"/>
        </div>
        <div>
          <SkillsButton
              aria-label="Add Specific User"
              data-cy="addUserConfBtn"
              @click="addConf"
              :disabled="!currentSelectedUser"
              icon="fas fa-plus-circle" label="Add">
          </SkillsButton>
        </div>
      </div>

      <skills-spinner v-if="loading" :is-loading="loading" class="mb-5"/>
      <div v-if="!loading">
        <SkillsDataTable v-if="hadData" class="mt-3"
                        :value="data" paginator
                        tableStoredStateId="skillApprovalConfSpecificUsersTable"
                        data-cy="skillApprovalConfSpecificUsersTable"
                        show-gridlines
                        striped-rows
                        :rows="pageSize"
                        :rowsPerPageOptions="possiblePageSizes"
                        v-model:sort-field="sortBy"
                        v-model:sort-order="sortOrder">
          <Column field="userId" header="User" sortable>
            <template #body="slotProps">
              <div class="flex" :data-cy="`userIdCell-${slotProps.data.userId}`">
                <div class="flex flex-1">
                  {{ slotProps.data.userIdForDisplay }}
                </div>
                <div class="flex">
                  <SkillsButton title="Delete Skill"
                            variant="outline-danger"
                            :aria-label="`Remove ${slotProps.data.userId} tag.`"
                            @click="removeTagConf(slotProps.data)"
                            :disabled="slotProps.data.deleteInProgress"
                            data-cy="deleteBtn"
                            icon="fas fa-trash"
                            size="small"
                            :loading="slotProps.data.deleteInProgress">
                  </SkillsButton>
                </div>
              </div>
            </template>
          </Column>
          <Column field="updated" header="Updated On" sortable>
            <template #body="slotProps">
              <date-cell :value="slotProps.data.updated" />
            </template>
          </Column>
          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ data.length }}</span>
          </template>
        </SkillsDataTable>

        <no-content2 v-if="!hadData" title="Not Configured Yet..."
                     class="my-5"
                     data-cy="noUserConf"
                     icon-size="fa-2x"
                     icon="fas fa-user-plus">
          You can split the approval workload by routing approval requests for specific users to <span class="text-primary font-weight-bold">{{userInfo.userIdForDisplay}}</span>.
        </no-content2>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>