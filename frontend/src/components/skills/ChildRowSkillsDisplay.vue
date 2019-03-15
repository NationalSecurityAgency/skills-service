<template>
  <loading-container class="child-row" v-bind:is-loading="isLoading">
    <div class="child-row-container">
      <div v-if="skillInfo.pointIncrement">
        <span class="title is-5">Skills Points: </span>
        <span class="subtitle skills-pad-left-1-rem">Increment:</span> <span class="points">{{ skillInfo.pointIncrement }}</span>,
        <span class="subtitle skills-pad-left-1-rem">Total:</span> <span class="points">{{ skillInfo.totalPoints }}</span>,
        <span class="subtitle skills-pad-left-1-rem">Interval Increment:</span> <span class="points">{{ skillInfo.pointIncrementInterval }}</span>
      </div>
      <span class="title is-5">Version:</span>
      <span class="points">{{ skillInfo.version }}</span>

      <div v-if="description" class="skills-pad-top-1-rem">
        <h2 class="title is-5">Description</h2>
        <div class="description-container">
          <p v-html="description"></p>
        </div>
      </div>

      <div v-if="skillInfo.helpUrl" class="skills-pad-top-1-rem">
        <span class="title is-5">Help URL:</span>
        <span class="skills-pad-left-1-rem"><a :href="skillInfo.helpUrl" target="_blank">{{ skillInfo.helpUrl }}</a></span>
      </div>
    </div>
  </loading-container>
</template>

<script>
  import axios from 'axios';
  import marked from 'marked';
  import LoadingContainer from '../utils/LoadingContainer';

  export default {
    name: 'ChildRowSkillsDisplay',
    components: { LoadingContainer },
    props: ['projectId', 'subjectId', 'parentSkillId', 'skill'],
    data() {
      return {
        isLoading: true,
        skillInfo: {},
      };
    },
    mounted() {
      this.loadSkills();
    },
    computed: {
      description: function markDownDescription() {
        if (this.skillInfo && this.skillInfo.description) {
          return marked(this.skillInfo.description, { sanitize: true, smartLists: true });
        }
        return null;
      },
    },
    methods: {
      loadSkills() {
        if (this.skill) {
          this.skillInfo = this.skill;
          this.isLoading = false;
        } else {
          axios.get(`/admin/projects/${this.projectId}/subjects/${this.subjectId}/skills/${this.parentSkillId}`)
            .then((response) => {
              this.isLoading = false;
              this.skillInfo = response.data;
              this.isLoading = false;
            })
            .finally(() => {
              this.isLoading = false;
          });
        }
      },
    },
  };
</script>

<style scoped>

  .name {
    font-size: 1.3rem;
    font-weight: bold;
  }

  .id-label {
    color: lightgray;
    font-style: italic;
    padding-left: 0.7rem;
  }

  .skill-id {

  }

  div ol li {
    font-size: 1.1rem;
    padding: 0 1rem 0 0.5rem;
  }

  .description-container {
    border: 1px dashed darkgray;
    padding: 1rem;
    margin-right: 1rem;
  }

  .child-row .title {
    padding-bottom: 0px;
    margin-bottom: 0.5rem;
  }

</style>
