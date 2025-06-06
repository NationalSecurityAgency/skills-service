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

onMounted(() => {
  projects.value.available = props.availableProjects.map((proj) => ({ ...proj }));
  const numProjectsToSelect = Math.min(props.availableProjects.length, 4);
  const availableSortedByMostSkills = projects.value.available.sort((a, b) => a.projectId.localeCompare(b.projectId));
  projects.value.selected = availableSortedByMostSkills.slice(0, numProjectsToSelect);
  genDataForCharts();
})

const genDataForCharts = (filter) => {
  loading.value = true;
  numSkillsChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numSkillsChart.value.series = projects.value.selected.map((proj) => proj.numSkills);

  numPointsChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numPointsChart.value.series = projects.value.selected.map((proj) => proj.totalPoints);

  numBadgesChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numBadgesChart.value.series = projects.value.selected.map((proj) => proj.numBadges);

  numSubjectsChart.value.labels = projects.value.selected.map((proj) => proj.name);
  numSubjectsChart.value.series = projects.value.selected.map((proj) => proj.numSubjects);

  if(projects.value.selected.length < 5) {
    projects.value.available = props.availableProjects.map((proj) => ({...proj}));
    projects.value.available = projects.value.available.filter((el) => !projects.value.selected.some((sel) => sel.projectId === el.projectId));

    if( filter ) {
      projects.value.available = projects.value.available.filter((el) => el.name.toLowerCase().includes(filter));
    }
  } else {
    projects.value.available = [];
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
      <SkillsCardHeader title="Project definition comparison" title-tag="h2"></SkillsCardHeader>
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
            :pt="{ dropdown: { 'aria-label': 'click to select an item' } }"
            placeholder="Select option">
          <template #empty>
            <div v-if="projects.selected.length === 5" class="ml-6" data-cy="trainingProfileMaximumReached">
              Maximum of 5 options selected. First remove a selected option to select another.
            </div>
            <div v-else class="ml-6">
              No results found
            </div>
          </template>
        </AutoComplete>
      </div>
      <div v-if="!loading && enoughProjectsSelected">
        <div class="flex flex-col xl:flex-row gap-6 mt-6">
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
        <div class="flex flex-col xl:flex-row gap-6 mt-6">
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
                   class="my-8"
                   title="Feature is disabled"
                   icon="fas fa-poo"
                   message="At least 2 projects must exist for this feature to work. Please create more projects to enable this feature."/>
      <no-content2 v-if="!loading && enoughOverallProjects && !enoughProjectsSelected"
                   class="my-8"
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