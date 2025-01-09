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
import {object, string} from "yup";
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import { computed } from 'vue';

const emit = defineEmits(['do-approve', 'do-reject', 'done']);
const model = defineModel();
const appConfig = useAppConfig()
const route = useRoute();

const props = defineProps({
  requestType: {
    type: String,
    required: true,
    validator: (value) => ['Approve', 'Reject'].includes(value)
  },
  selectedItems: {
    type: Array,
    required: true,
  }
})

const isApprove = computed(() => props.requestType === 'Approve')
const isReject = computed(() => props.requestType === 'Reject')
const modalTitle = computed(() => isApprove.value ? 'Approve Skills' : 'Reject Skills')

const initialData = {
  approvalRequiredMsg: ''
}
const schema = object({
  'approvalRequiredMsg': string()
      .trim()
      .customDescriptionValidator('Rejection Message')
      .max(appConfig.maxSelfReportRejectionMessageLength)
      .label('Rejection Message')
})
const rejectOrApproveSkills = (values) => {
  const ids = props.selectedItems.map((item) => item.id);
  if (isReject.value) {
    return SelfReportService.reject(route.params.projectId, ids, values.approvalRequiredMsg).then(() => {
      emit('do-reject', ids);
    });
  } else {
    return SelfReportService.approve(route.params.projectId, ids/*, values.approvalRequiredMsg*/).then(() => {
      emit('do-approve', ids);
    });
  }
}

const done = () => {
  emit('done');
}
</script>

<template>
  <SkillsInputFormDialog v-model="model"
                         :maximizable="false"
                         id="approvalRequiredMsg"
                         :enable-return-focus="true"
                         :initial-values="initialData"
                         :style="{ width: '40rem !important' }"
                         ok-button-icon="fas fa-arrow-alt-circle-right"
                         :ok-button-label="isApprove ? 'Approve' : 'Reject'"
                         :validation-schema="schema"
                         :save-data-function="rejectOrApproveSkills"
                         @on-cancel="done"
                         :header="modalTitle">
    <div id="rejectionApprovalTitleInModal" class="flex gap-2" :data-cy="isReject ? 'rejectionTitle' : 'approvalTitle'">
      <div class="flex text-center">
        <i class="far fa-thumbs-down text-warning" style="font-size: 3rem"/>
      </div>
      <div class="flex flex-1">
        <p class="h6">This will {{ isReject ? 'reject' : 'approve' }} user's request(s) to get points. Users will be notified and you can provide an optional message below.</p>
      </div>
    </div>
    <SkillsTextarea :data-cy="isReject ? 'rejectionInputMsg' : 'approvalInputMsg'"
                    aria-describedby="rejectionApprovalTitleInModal"
                    :aria-label="isReject ? 'Optional Rejection Message' : 'Optional Approval Message'"
                    rows="5"
                    name="approvalRequiredMsg">
    </SkillsTextarea>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
