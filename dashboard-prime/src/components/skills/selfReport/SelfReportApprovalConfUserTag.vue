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
import { ref, nextTick, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import NoContent2 from "@/components/utils/NoContent2.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import * as yup from "yup";
import {useForm} from "vee-validate";

const emit = defineEmits(['conf-added', 'conf-removed']);
const announcer = useSkillsAnnouncer();
const route = useRoute();
const props = defineProps({
  userInfo: Object,
  tagLabel: String,
  tagKey: String,
});

const schema = yup.object({
  'tagInput': yup.string().required().test('uniqueName', `There is already an entry for this ${props.tagLabel} value.`, (value) => {
    return !data.value.find((i) => value.toLowerCase() === i.userTagValue?.toLowerCase());
  }).matches(/^\w+$/, () => `${props.tagLabel} may only contain alpha-numeric characters`)
})
const { meta } = useForm({
  validationSchema: schema
});

const data = ref([]);
const enteredTag = ref('');
const sortBy = ref('updated');
const sortOrder = ref(-1);
const pageSize = 4;
const possiblePageSizes = [4, 10, 15, 20];

const hadData = computed(() => {
  return data.value && data.value.length > 0;
});

onMounted(() => {
  const hasTagConf = props.userInfo.tagConf && props.userInfo.tagConf.length > 0;
  if (hasTagConf) {
    data.value = props.userInfo.tagConf.map((u) => ({ ...u }));
  }
});
const addTagConf = () => {
  if (enteredTag.value && enteredTag.value !== '') {
    SelfReportService.configureApproverForUserTag(route.params.projectId, props.userInfo.userId, props.tagKey, enteredTag.value)
      .then((res) => {
        data.value.push(res);
        enteredTag.value = '';
        emit('conf-added', res);
        nextTick(() => announcer.polite(`Added workload configuration successfully for ${enteredTag.value} ${props.tagLabel}.`));
      });
  }
};

const removeTagConf = (removedIem) => {
  data.value = data.value.map((i) => ({ ...i, deleteInProgress: i.id === removedIem.id }));
  SelfReportService.removeApproverConfig(route.params.projectId, removedIem.id)
      .then(() => {
        data.value = data.value.filter((i) => i.id !== removedIem.id);
        emit('conf-removed', removedIem);
        nextTick(() => announcer.polite(`Removed workload configuration successfully for ${removedIem.userTagValue} ${props.tagLabel}.`));
      });
};
</script>

<template>
<Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
  <template #header>
    <SkillsCardHeader :title="'Split Workload By ' + tagLabel"></SkillsCardHeader>
  </template>
  <template #content>
    <div class="flex gap-2 px-3 pt-3 flex-column sm:flex-row">
      <div class="flex flex-1">
        <SkillsTextInput
            class="w-full"
            name="tagInput"
            :placeholder="`Enter ${tagLabel} value`"
            aria-label="Enter Tag Label"
            v-on:keydown.enter="addTagConf"
            v-skills="'ConfigureSelfApprovalWorkload'"
            v-model="enteredTag"
            data-cy="userTagValueInput" />
      </div>
      <div>
        <SkillsButton size="small"
            aria-label="Add Tag Value"
            @click="addTagConf"
            v-skills="'ConfigureSelfApprovalWorkload'"
            data-cy="addTagKeyConfBtn"
            :disabled="!enteredTag || !meta.valid"
            label="Add"
            icon="fas fa-plus-circle">
        </SkillsButton>
      </div>
    </div>

    <SkillsDataTable v-if="hadData" class="mt-3" data-cy="tagKeyConfTable"
                     :rows="pageSize"
                     :rowsPerPageOptions="possiblePageSizes"
                     v-model:sort-field="sortBy"
                     v-model:sort-order="sortOrder"
                     :value="data"
                     paginator
                     tableStoredStateId="skillApprovalConfSpecificUsersTable">
      <Column :header="tagLabel" field="userTagValue" sortable>
        <template #body="slotProps">
          <div class="flex" :data-cy="`tagValue_${slotProps.data.userTagValue}`">
            <div class="flex flex-1">
              {{ slotProps.data.userTagValue }}
            </div>
            <div class="flex">
              <SkillsButton title="Delete Skill"
                        data-cy="deleteBtn"
                        show-gridlines
                        striped-rows
                        :aria-label="`Remove ${slotProps.data.userTagValue} tag.`"
                        @click="removeTagConf(slotProps.data)"
                        :disabled="slotProps.data.deleteInProgress"
                        size="small" icon="fas fa-trash" :loading="slotProps.data.deleteInProgress">
              </SkillsButton>
            </div>
          </div>
        </template>
      </Column>
      <Column header="Configured On" field="updated" sortable>
        <template #body="slotProps">
          <date-cell :value="slotProps.data.updated" />
        </template>
      </Column>
      <template #paginatorstart>
        <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ data.length }}</span>
      </template>
    </SkillsDataTable>

    <no-content2 v-if="!hadData" title="Not Configured Yet..."
                 class="py-5"
                 icon-size="fa-2x"
                 data-cy="noTagKeyConf"
                 icon="fas fa-user-tag">
      You can split the approval workload by routing approval requests for users with the selected <span class="text-info">{{tagLabel}}</span> to <span class="text-primary font-weight-bold">{{userInfo.userIdForDisplay}}</span>.
    </no-content2>
  </template>
</Card>
</template>

<style scoped>

</style>