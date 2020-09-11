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
    <div class="container">
        <skills-spinner :loading="loading" />

        <div v-if="!loading">
            <skills-title>Global Badge Details</skills-title>

            <div class="card">
                <div class="card-body">
                    <badge-details-overview :badge="badge"></badge-details-overview>
                </div>
            </div>

            <div v-for="projectSummary in projectSummaries" :key="projectSummary.projectId" class="card mt-1">
                <h4 class="card-header text-sm-left text-secondary text-center col">Project: {{ projectSummary.projectName }}</h4>
                <div class="card-body">
                    <project-level-row v-if="projectSummary && projectSummary.projectLevel" :projectLevel="projectSummary.projectLevel" />
                    <skills-progress-list v-if="projectSummary && projectSummary.skills" :subject="projectSummary" :show-descriptions="showDescriptions" :helpTipHref="helpTipHref" type="global-badge"/>
                </div>
            </div>
            <div v-if="!(projectSummaries && projectSummaries.length > 0)">
                <no-data-yet class="my-2"
                             title="No Skills or Project Levels have not been added yet." sub-title="Please contact a Skills Supervisor."/>
            </div>
        </div>
    </div>
</template>

<script>
  import BadgeDetailsOverview from '@/userSkills/badge/BadgeDetailsOverview';
  import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList';
  import ProjectLevelRow from '@/userSkills/badge/ProjectLevelRow';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillsTitle from '@/common/utilities/SkillsTitle';
  import NoDataYet from '@/common/utilities/NoDataYet';

  export default {
    components: {
      SkillsTitle,
      SkillsProgressList,
      BadgeDetailsOverview,
      SkillsSpinner,
      ProjectLevelRow,
      NoDataYet,
    },
    data() {
      return {
        loading: true,
        badge: null,
        initialized: false,
        showDescriptions: false,
      };
    },
    computed: {
      helpTipHref() {
        return this.badge ? this.badge.helpUrl : '';
      },
      projectSummaries() {
        return this.badge.projectLevelsAndSkillsSummaries.map((item) => ({
          badgeId: this.badge.badgeId,
          projectId: item.projectId,
          projectName: item.projectName,
          skills: item.skills,
          projectLevel: item.projectLevel,
        }));
      },
    },
    watch: {
      $route: 'fetchData',
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        UserSkillsService.getBadgeSkills(this.$route.params.badgeId, true)
          .then((badgeSummary) => {
            this.badge = badgeSummary;
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
