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
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router'
import SlimDateCell from '@/components/utils/table/SlimDateCell.vue';
import ProjectService from '@/components/projects/ProjectService';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';

const props = defineProps(['created', 'loadLastReportedDate']);
const route = useRoute();

const isLoading = ref(true);
const lastReportedSkill = ref(null);

onMounted(() => {
  doLoadDate();
})

const doLoadDate = () => {
  if (props.loadLastReportedDate) {
    ProjectService.getLatestSkillEventForProject(route.params.projectId)
        .then((res) => {
          lastReportedSkill.value = res.lastReportedSkillDate;
        })
        .finally(() => {
          isLoading.value = false;
        });
  } else {
    isLoading.value = false;
  }
};
</script>

<template>
  <span>
    <span data-cy="projectCreated">
      <span class="text-muted-color small italic mr-1">Created: </span>
      <SlimDateCell :value="created"/>
      <span v-if="loadLastReportedDate"
            class="text-secondary small mx-2 d-none d-md-inline">|</span>
    </span>
<!--    <br v-if="loadLastReportedDate" class="d-md-none"/>-->
    <span v-if="loadLastReportedDate" data-cy="projectLastReportedSkill">
      <span class="text-muted-color small italic">Last Reported Skill: </span>
      <ProgressSpinner
        v-if="isLoading"
        aria-label="Loading"
        class="ml-1 !w-[1.5rem] !h-[1.5rem]" />
      <SlimDateCell v-if="!isLoading" :value="lastReportedSkill" :fromStartOfDay="true" data-cy="projectLastReportedSkillValue"/>
    </span>
  </span>
</template>

<style scoped>

</style>
