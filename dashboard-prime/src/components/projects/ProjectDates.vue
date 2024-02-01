<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router'
import SlimDateCell from '@/components/utils/table/SlimDateCell.vue';
import ProjectService from '@/components/projects/ProjectService';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';

const props = defineProps(['created', 'loadLastReportedDate']);
const route = useRoute();

let isLoading = ref(true);
let lastReportedSkill = ref(null);

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
      <span class="text-color-secondary small font-italic">Created: </span>
      <SlimDateCell :value="created"/>
      <span v-if="loadLastReportedDate"
            class="text-secondary small mx-2 d-none d-md-inline">|</span>
    </span>
<!--    <br v-if="loadLastReportedDate" class="d-md-none"/>-->
    <span v-if="loadLastReportedDate" data-cy="projectLastReportedSkill">
      <span class="text-color-secondary small font-italic">Last Reported Skill: </span>
      <SkillsSpinner v-if="isLoading" label="Loading..." small type="grow" variant="info"></SkillsSpinner>
      <SlimDateCell v-if="!isLoading" :value="lastReportedSkill" :fromStartOfDay="true" data-cy="projectLastReportedSkillValue"/>
    </span>
  </span>
</template>

<style scoped>

</style>
