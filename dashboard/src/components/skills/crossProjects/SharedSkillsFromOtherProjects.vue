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
  <metrics-card id="shared-skills-from-others-panel"
    title="Skills Shared From Other Projects" :no-padding="true">
      <loading-container :is-loading="loading">
        <div v-if="sharedSkills && sharedSkills.length > 0" class="my-4">
          <shared-skills-table :shared-skills="sharedSkills" :disable-delete="true"></shared-skills-table>
        </div>
        <div v-else class="my-5">
          <no-content2 title="No Shared Skills Yet..." icon="far fa-handshake"
                       message="Coordinate with other projects to share skills with this project."></no-content2>
        </div>

      </loading-container>
  </metrics-card>
</template>

<script>
  import NoContent2 from '../../utils/NoContent2';
  import SkillsShareService from './SkillsShareService';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SharedSkillsTable from './SharedSkillsTable';
  import MetricsCard from '../../metrics/utils/MetricsCard';

  export default {
    name: 'SharedSkillsFromOtherProjects',
    components: {
      MetricsCard,
      SharedSkillsTable,
      LoadingContainer,
      NoContent2,
    },
    props: ['projectId'],
    data() {
      return {
        loading: true,
        sharedSkills: [],
      };
    },
    mounted() {
      this.loadSharedSkills();
    },
    methods: {
      loadSharedSkills() {
        this.loading = true;
        SkillsShareService.getSharedWithmeSkills(this.projectId)
          .then((data) => {
            this.sharedSkills = data;
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>
  #shared-skills-from-others-panel .title {
    color: #3273dc;
    font-weight: normal;
  }

  #shared-skills-from-others-panel .title strong {
    font-weight: bold;
  }
</style>
