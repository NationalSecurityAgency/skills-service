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

const emit = defineEmits(['do-reject']);
const model = defineModel();
const appConfig = useAppConfig()
const route = useRoute();

const props = defineProps({
  selectedItems: {
    type: Array,
    required: true,
  }
})

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
const rejectSkills = (values) => {
  const ids = props.selectedItems.map((item) => item.id);
  return SelfReportService.reject(route.params.projectId, ids, values.approvalRequiredMsg).then(() => {
    emit('do-reject', ids);
  });
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
                         :ok-button-icon="'fas fa-arrow-alt-circle-right'"
                         ok-button-label="Reject"
                         :validation-schema="schema"
                         :save-data-function="rejectSkills"
                         @on-cancel="done"
                         header="Reject Skills">
    <div id="rejectionTitleInModal" class="flex gap-2" data-cy="rejectionTitle">
      <div class="flex text-center">
        <i class="far fa-thumbs-down text-warning" style="font-size: 3rem"/>
      </div>
      <div class="flex flex-1">
        <p class="h6">This will reject user's request(s) to get points. Users will be notified and you can provide an optional message below.</p>
      </div>
    </div>
    <SkillsTextarea data-cy="rejectionInputMsg"
                    aria-describedby="rejectionTitleInModal"
                    aria-label="Optional Rejection Message"
                    rows="5"
                    name="approvalRequiredMsg">
    </SkillsTextarea>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
