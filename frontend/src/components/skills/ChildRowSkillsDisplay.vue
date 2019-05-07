<template>
  <loading-container class="child-row" v-bind:is-loading="isLoading">

    <div class="row">
      <div class="col-12 col-md-12 col-xl mb-md-3 mb-xl-0">
        <media-info-card :title="`${skillInfo.totalPoints} Points`" icon-class="fas fa-calculator text-success">
          <strong>{{ skillInfo.pointIncrement }}</strong> Increment <i class="fa fa-times text-muted"/>
          <strong> {{ skillInfo.numPerformToCompletion }}</strong> Times to Completion
        </media-info-card>
      </div>
      <div class="col-12  col-md-6 col-xl my-3 my-md-0">
        <media-info-card :title="`${skillInfo.pointIncrementInterval} Hours`" icon-class="fas fa-hourglass-half text-info">
          Interval Increment
        </media-info-card>
      </div>
      <div class="col-12 col-md-6 col-xl">
        <media-info-card :title="`Version # ${skillInfo.version}`" icon-class="fas fa-code-branch text-warning">
          Version of this Skill
        </media-info-card>
      </div>
    </div>


    <div class="card mt-3">
      <div class="card-header">
        Description
      </div>
      <div class="card-body">
        <p v-if="description" v-html="description"></p>
        <p v-else class="text-muted">
          Not Specified
        </p>
      </div>
    </div>

    <div class="input-group mt-3">
      <div class="input-group-prepend">
        <div class="input-group-text"><i class="fas fa-link mr-1"></i> Help URL: </div>
      </div>
      <span class="form-control">
        <a v-if="skillInfo.helpUrl" :href="skillInfo.helpUrl" target="_blank">{{ skillInfo.helpUrl }}</a>
        <span v-else class="text-muted">
          Not Specified
        </span>
      </span>
    </div>
  </loading-container>
</template>

<script>
  import marked from 'marked';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsService from './SkillsService';
  import MediaInfoCard from '../utils/cards/MediaInfoCard';

  export default {
    name: 'ChildRowSkillsDisplay',
    components: { MediaInfoCard, LoadingContainer },
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
          SkillsService.getSkillDetails(this.projectId, this.subjectId, this.parentSkillId)
            .then((response) => {
              this.skillInfo = response;
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

  .child-row-container i {
    padding-left: 5px;
  }

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
