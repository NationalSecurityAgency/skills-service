/*
Copyright 2026 SkillTree

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
import {ref, watch} from "vue";
import SelfReportService from "@/components/skills/selfReport/SelfReportService.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import {useRoute} from "vue-router";
import SelfReportType from "@/components/skills/selfReport/SelfReportType.js";

const route = useRoute()
const props = defineProps({
  selfReportEnabled: Boolean,
  selfReportingType: String,
  skillIds: Array,
})
const numFormat = useNumberFormat()

const originalSelfReportingType = props.selfReportingType
watch(() => props.selfReportingType, () => {
  handleSelfReportingWarning();
})

const showWarning = ref(false)
const numPending = ref(0)
const numRejected = ref(0)
const newSelfReportingType = ref(null)
const handleSelfReportingWarning = () => {
  showWarning.value = false;
  if (SelfReportType.isApproval(originalSelfReportingType) && !SelfReportType.isApproval(props.selfReportingType)) {
    SelfReportService.getSkillApprovalsStats(route.params.projectId, props.skillIds)
        .then((res) => {
          const pendingApprovalsRes = res.find((item) => item.value === 'SkillApprovalsRequests');
          if (pendingApprovalsRes) {
            numPending.value = pendingApprovalsRes.count;
          }

          const pendingRejectionsRes = res.find((item) => item.value === 'SkillApprovalsRejected');
          if (pendingRejectionsRes) {
            numRejected.value = pendingRejectionsRes.count;
          }

          showWarning.value = pendingApprovalsRes.count > 0 || pendingRejectionsRes.count > 0;
        })
    newSelfReportingType.value = props.selfReportingType;
  } else {
    showWarning.value = false;
  }
}
</script>

<template>
  <Message class="pt-1"
           v-if="showWarning"
           :closable="false"
           severity="warn"
           data-cy="selfReportingTypeWarning">
    <div v-if="selfReportEnabled && SelfReportType.isHonorSystem(newSelfReportingType)">
      Switching this skill to the <i>Honor System</i> will automatically:
      <ul>
        <li v-if="numPending > 0">
          Approve <b>{{ numFormat.pretty(numPending) }} pending</b> request<span
            v-if="numPending>1">s</span>
        </li>
        <li v-if="numRejected > 0">
          Remove <b>{{ numRejected }} rejected</b> request<span v-if="numRejected>1">s</span>
        </li>
      </ul>
    </div>
    <div v-if="!selfReportEnabled || SelfReportType.isQuizOrSurvey(newSelfReportingType) || SelfReportType.isVideo(newSelfReportingType)">
      Disabling <i>Self Reporting</i> will automatically:
      <ul>
        <li v-if="numPending > 0">
          Remove <b>{{ numFormat.pretty(numPending) }} pending</b> request<span v-if="numPending>1">s</span>
        </li>
        <li v-if="numRejected > 0">
          Remove <b>{{ numRejected }} rejected</b> request<span v-if="numRejected>1">s</span>
        </li>
      </ul>
    </div>
  </Message>
</template>

<style scoped>

</style>