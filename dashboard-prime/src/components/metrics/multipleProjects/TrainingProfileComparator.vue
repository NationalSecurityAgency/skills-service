<script setup>
import { ref, onMounted, computed } from 'vue';
import NoContent2 from "@/components/utils/NoContent2.vue";
import TrainingProfileComparisonChart from "@/components/metrics/multipleProjects/TrainingProfileComparisonChart.vue";
import AutoComplete from "primevue/autocomplete";

const props = defineProps(['availableProjects']);

const loading = ref(true);
const projects = ref({
  selected: [],
  available: [],
});

const numSkillsChart = ref({
  labels: [],
  series: [],
});

const numPointsChart = ref({
  labels: [],
  series: [],
});

const numBadgesChart = ref({
  labels: [],
  series: [],
});

const numSubjectsChart = ref({
  labels: [],
  series: [],
});

const enoughOverallProjects = computed(() => {
  return props.availableProjects && props.availableProjects.length >= 2;
});

const enoughProjectsSelected = computed(() => {
  return projects.value.selected && projects.value.selected.length >= 2;
});

const beforeListSlotText = computed(() => {
  if (projects.value.selected.length >= 5) {
    return 'Maximum of 5 options selected. First remove a selected option to select another.';
  }
  return '';
});

onMounted(() => {
  projects.value.available = props.availableProjects.map((proj) => ({ ...proj }));
  const numProjectsToSelect = Math.min(props.availableProjects.length, 4);
  const availableSortedByMostSkills = projects.value.available.sort((a, b) => a.projectId.localeCompare(b.projectId));
  projects.value.selected = availableSortedByMostSkills.slice(0, numProjectsToSelect);
  genDataForCharts();
})

const genDataForCharts = (filter) => {
  numSkillsChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numSkillsChart.value.series = projects.value.selected.map((proj) => proj.numSkills);

  numPointsChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numPointsChart.value.series = projects.value.selected.map((proj) => proj.totalPoints);

  numBadgesChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numBadgesChart.value.series = projects.value.selected.map((proj) => proj.numBadges);

  numSubjectsChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numSubjectsChart.value.series = projects.value.selected.map((proj) => proj.numSubjects);

  projects.value.available = props.availableProjects.map((proj) => ({ ...proj }));
  projects.value.available = projects.value.available.filter((el) => !projects.value.selected.some((sel) => sel.projectId === el.projectId));

  if( filter ) {
    projects.value.available = projects.value.available.filter((el) => el.name.toLowerCase().includes(filter));
  }

  loading.value = false;
};

const filter = (event) => {
  genDataForCharts(event.query.toLowerCase());
}
</script>

<template>
  <Card data-cy="trainingProfileComparator">
    <template #header>
      <SkillsCardHeader title="Project definition comparison"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex w-full">
        <AutoComplete
            v-model="projects.selected"
            :suggestions="projects.available"
            :delay="500"
            :completeOnFocus="true"
            dropdown
            @item-unselect="genDataForCharts"
            @item-select="genDataForCharts"
            multiple
            optionLabel="name"
            inputClass="w-full"
            class="w-full"
            @complete="filter"
            data-cy="trainingProfileComparatorProjectSelector"
            placeholder="Select option">
        </AutoComplete>
      </div>
      <div v-if="!loading && enoughProjectsSelected">
        <div class="flex gap-4 mt-4">
          <div class="flex flex-1">
            <training-profile-comparison-chart :series="numSkillsChart.series" :labels="numSkillsChart.labels"
                                               title="Number of Skills" title-icon="fas fa-graduation-cap" data-cy="numOfSkillsChart"/>
          </div>
          <div class="flex flex-1">
            <training-profile-comparison-chart :series="numPointsChart.series" :labels="numPointsChart.labels"
                                               :horizontal="true"
                                               title-icon="far fa-arrow-alt-circle-up"
                                               title="Total Available Points"
                                               data-cy="totalAvailablePointsChart"/>
          </div>
        </div>
        <div class="flex gap-4 mt-4">
          <div class="flex flex-1">
            <training-profile-comparison-chart :series="numSubjectsChart.series" :labels="numSubjectsChart.labels"
                                               title="Number of Subjects" title-icon="fas fa-cubes" data-cy="numOfSubjChart"/>
          </div>
          <div class="flex flex-1">
            <training-profile-comparison-chart :series="numBadgesChart.series" :labels="numBadgesChart.labels"
                                               title="Number of Badges" title-icon="fas fa-award" data-cy="numOfBadgesChart"/>
          </div>
        </div>
      </div>

      <no-content2 v-if="!loading && !enoughOverallProjects"
                   class="my-5"
                   title="Feature is disabled"
                   icon="fas fa-poo"
                   message="At least 2 projects must exist for this feature to work. Please create more projects to enable this feature."/>
      <no-content2 v-if="!loading && enoughOverallProjects && !enoughProjectsSelected"
                   class="my-5"
                   title="Need more projects"
                   message="Please select at least 2 projects using the search above"/>
    </template>
  </Card>
</template>

<style>
.p-autocomplete-multiple-container {
  width: 100% !important;
}
</style>