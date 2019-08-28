<template>
  <loading-container class="child-row" v-bind:is-loading="isLoading">

    <div class="row">
      <div class="col-12 col-md-12 col-xl mb-md-3 mb-xl-0">
        <media-info-card :title="`${skillInfo.totalPoints} Points`" icon-class="fas fa-calculator text-success">
          <strong>{{ skillInfo.pointIncrement }}</strong> points <i class="fa fa-times text-muted"/>
          <strong> {{ skillInfo.numPerformToCompletion }}</strong> repetition<span v-if="skillInfo.numPerformToCompletion>1">s</span> to Completion
        </media-info-card>
      </div>
      <div class="col-12  col-md-6 col-xl my-3 my-md-0">
        <media-info-card :title="timeWindowTitle" icon-class="fas fa-hourglass-half text-info">
          {{ timeWindowDescription }}
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
    props: {
      projectId: {
        type: String,
      },
      subjectId: {
        type: String,
      },
      parentSkillId: {
        type: String,
      },
      skill: {
        type: Object,
      },
      // increment this counter to force component to reload data from the server
      refreshCounter: {
        type: Number,
        default: 0,
      },
    },
    data() {
      return {
        isLoading: true,
        skillInfo: {},
      };
    },
    mounted() {
      this.loadSkills();
    },
    watch: {
      refreshCounter() {
        this.loadSkills();
      },
    },
    computed: {
      timeWindowTitle() {
        let title = '';
        if (!this.skillInfo.timeWindowEnabled) {
          title = 'Time Window Disabled';
        } else if (this.skillInfo.numPerformToCompletion === 1) {
          title = 'Time Window N/A';
        } else {
          title = `${this.skillInfo.pointIncrementIntervalHrs} Hour`;
          if (this.skillInfo.pointIncrementIntervalHrs > 1) {
            title = `${title}s`;
          }
          if (this.skillInfo.pointIncrementIntervalMins > 0) {
            title = `${title} ${this.skillInfo.pointIncrementIntervalMins} Minute`;
            if (this.skillInfo.pointIncrementIntervalMins > 1) {
              title = `${title}s`;
            }
          }
        }
        return title;
      },
      timeWindowDescription() {
        const numOccur = this.skillInfo.numPointIncrementMaxOccurrences;
        let desc = 'Minimum Time Window between occurrences to receive points';
        if (!this.skillInfo.timeWindowEnabled) {
          desc = 'Each occurrence will receive points immediately';
        } else if (numOccur > 1) {
          desc = `Up to ${numOccur} occurrences within this time window to receive points`;
        } else if (this.skillInfo.numPerformToCompletion === 1) {
          desc = 'Only one event is required to complete this skill.';
        }
        return desc;
      },
      description: function markDownDescription() {
        if (this.skillInfo && this.skillInfo.description) {
          return marked(this.skillInfo.description, { sanitize: true, smartLists: true });
        }
        return null;
      },
    },
    methods: {
      loadSkills() {
        this.isLoading = true;
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

  div ol li {
    font-size: 1.1rem;
    padding: 0 1rem 0 0.5rem;
  }

</style>
