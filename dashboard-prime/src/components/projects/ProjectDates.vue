<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router'
// import SlimDateCell from '@/components/utils/table/SlimDateCell';
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
          lastReportedSkill = res.lastReportedSkillDate;
        })
        .finally(() => {
          isLoading = false;
        });
  } else {
    isLoading = false;
  }
};
</script>

<template>
  <span>
    <span data-cy="projectCreated">
      <span class="text-secondary small font-italic">Created: </span>
<!--      <slim-date-cell :value="created"/>-->
      <span v-if="loadLastReportedDate"
            class="text-secondary small mx-2 d-none d-md-inline">|</span>
    </span>
    <br v-if="loadLastReportedDate" class="d-md-none"/>
    <span v-if="loadLastReportedDate" data-cy="projectLastReportedSkill">
      <span class="text-secondary small font-italic">Last Reported Skill: </span>
      <SkillsSpinner v-if="isLoading" label="Loading..." small type="grow" variant="info"></SkillsSpinner>
<!--      <slim-date-cell v-if="!isLoading" :value="lastReportedSkill" :fromStartOfDay="true" data-cy="projectLastReportedSkillValue"/>-->
    </span>
  </span>
</template>

<style scoped>

</style>
