/*
Copyright 2020 SkillTree

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
<template>
  <metrics-card title="Project definitions comparison" data-cy="trainingProfileComparator">

    <div v-if="!loading && enoughProjectsSelected">
      <multiselect v-model="projects.selected"
                 :options="projects.available"
                 label="name"
                 :multiple="true"
                 track-by="projectId"
                 :hide-selected="true"
                 :max="5"
                 data-cy="trainingProfileComparatorProjectSelector"/>
      <div class="mt-4">
      <b-row>
        <b-col xl>
          <training-profile-comparison-chart :series="numSkillsChart.series" :labels="numSkillsChart.labels"
            title="Number of Skills" title-icon="fas fa-graduation-cap" data-cy="numOfSkillsChart"/>
        </b-col>
        <b-col xl class="mt-3 mt-xl-0">
          <training-profile-comparison-chart :series="numPointsChart.series" :labels="numPointsChart.labels"
                                             :horizontal="true"
                                             title-icon="far fa-arrow-alt-circle-up"
                                             title="Total Available Points"
                                             data-cy="totalAvailablePointsChart"/>
        </b-col>
      </b-row>
      <b-row class="mt-3">
        <b-col xl>
          <training-profile-comparison-chart :series="numSubjectsChart.series" :labels="numSubjectsChart.labels"
                                             title="Number of Subjects" title-icon="fas fa-cubes" data-cy="numOfSubjChart"/>
        </b-col>
        <b-col xl class="mt-3 mt-xl-0">
          <training-profile-comparison-chart :series="numBadgesChart.series" :labels="numBadgesChart.labels"
                                             title="Number of Badges" title-icon="fas fa-award" data-cy="numOfBadgesChart"/>
        </b-col>
      </b-row>
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

  </metrics-card>
</template>

<script>
  import Multiselect from 'vue-multiselect';
  import MetricsCard from '../utils/MetricsCard';
  import TrainingProfileComparisonChart from './TrainingProfileComparisonChart';
  import NoContent2 from '../../utils/NoContent2';

  export default {
    name: 'TrainingProfileComparator',
    components: {
      NoContent2, TrainingProfileComparisonChart, MetricsCard, Multiselect,
    },
    props: ['availableProjects'],
    data() {
      return {
        loading: true,
        projects: {
          selected: [],
          available: [],
        },
        numSkillsChart: {
          labels: [],
          series: [],
        },
        numPointsChart: {
          labels: [],
          series: [],
        },
        numBadgesChart: {
          labels: [],
          series: [],
        },
        numSubjectsChart: {
          labels: [],
          series: [],
        },
      };
    },
    mounted() {
      this.projects.available = this.availableProjects.map((proj) => ({ ...proj }));
      const numProjectsToSelect = Math.min(this.availableProjects.length, 4);
      const availableSortedByMostSkills = this.projects.available.sort((a, b) => b.numSkills - a.numSkills);
      const projs = availableSortedByMostSkills.slice(0, numProjectsToSelect);
      this.projects.selected = projs;
      this.genDataForCharts();
    },
    computed: {
      enoughOverallProjects() {
        return this.projects.available && this.projects.available.length >= 2;
      },
      enoughProjectsSelected() {
        return this.projects.selected && this.projects.selected.length >= 2;
      },
    },
    watch: {
      'projects.selected': function rebuild() {
        this.genDataForCharts();
      },
    },
    methods: {
      genDataForCharts() {
        this.numSkillsChart.labels = this.projects.selected.map((proj) => proj.name);
        this.numSkillsChart.series = this.projects.selected.map((proj) => proj.numSkills);

        this.numPointsChart.labels = this.projects.selected.map((proj) => proj.name);
        this.numPointsChart.series = this.projects.selected.map((proj) => proj.totalPoints);

        this.numBadgesChart.labels = this.projects.selected.map((proj) => proj.name);
        this.numBadgesChart.series = this.projects.selected.map((proj) => proj.numBadges);

        this.numSubjectsChart.labels = this.projects.selected.map((proj) => proj.name);
        this.numSubjectsChart.series = this.projects.selected.map((proj) => proj.numSubjects);

        this.loading = false;
      },
    },
  };
</script>

<style scoped>

</style>
