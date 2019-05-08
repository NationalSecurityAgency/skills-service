<template>
  <div id="shared-skills-from-others-panel" class="card">
    <div class="card-header">
      Skills Shared <strong>From</strong> Other Projects
    </div>
    <div class="card-body">
      <loading-container :is-loading="loading">
        <div v-if="sharedSkills && sharedSkills.length > 0" class="my-4">
          <shared-skills-table :shared-skills="sharedSkills" :disable-delete="true"></shared-skills-table>
        </div>
        <div v-else class="my-5">
          <no-content2 title="No Shared Skills Yet..." icon="far fa-handshake"
                       message="Coordinate with other projects to share skills with this project."></no-content2>
        </div>

      </loading-container>
    </div>
  </div>
</template>

<script>
  import NoContent2 from '../../utils/NoContent2';
  import SkillsShareService from './SkillsShareService';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SharedSkillsTable from './SharedSkillsTable';

  export default {
    name: 'SharedSkillsFromOtherProjects',
    components: {
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
