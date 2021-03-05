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
  <loading-container class="child-row" v-bind:is-loading="isLoading" :data-cy="`childRowDisplay_${skillInfo.skillId}`">

    <div class="row">
      <div class="col-12 col-md-6 mt-2">
        <media-info-card :title="`${totalPoints} Points`" icon-class="fas fa-calculator text-success">
          <strong>{{ skillInfo.pointIncrement | number }}</strong> points <i class="fa fa-times text-muted" aria-hidden="true"/>
          <strong> {{ skillInfo.numPerformToCompletion | number }}</strong> repetition<span v-if="skillInfo.numPerformToCompletion>1">s</span> to Completion
        </media-info-card>
      </div>
      <div class="col-12 col-md-6 mt-2">
        <media-info-card :title="timeWindowTitle(skillInfo)" icon-class="fas fa-hourglass-half text-info">
          {{ timeWindowDescription(skillInfo) }}
        </media-info-card>
      </div>
      <div class="col-12 col-md-6 mt-2">
        <media-info-card :title="`Version # ${skillInfo.version}`" icon-class="fas fa-code-branch text-warning">
          Mechanism of adding new skills without affecting existing software running.
        </media-info-card>
      </div>
      <div class="col-12 col-md-6 mt-2">
        <media-info-card :title="`Self Report: ${selfReportingTitle}`"
                         icon-class="fas fa-laptop skills-color-selfreport"
                         data-cy="selfReportMediaCard">
          <div v-if="skillInfo.selfReportingType">Users can <i>self report</i> this skill
            <span v-if="skillInfo.selfReportingType === 'Approval'">and will go into an <b class="text-primary">approval</b> queue.</span>
            <span v-if="skillInfo.selfReportingType === 'HonorSystem'">and will apply <b class="text-primary">immediately</b>.</span>
          </div>
          <div v-else>
            Self reporting is <b class="text-primary">disabled</b> for this skill.
          </div>
        </media-info-card>
      </div>
    </div>

    <div class="card mt-3">
      <div class="card-header">
        Description
      </div>
      <div class="card-body">
        <markdown-text v-if="description" :text="description"/>
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
        <a v-if="skillInfo.helpUrl" :href="skillInfo.helpUrl" target="_blank" rel="noopener" class="skill-url">{{ skillInfo.helpUrl }}</a>
        <span v-else class="text-muted">
          Not Specified
        </span>
      </span>
    </div>
  </loading-container>
</template>

<script>
  import marked from 'marked';
  import DOMPurify from 'dompurify';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsService from './SkillsService';
  import MediaInfoCard from '../utils/cards/MediaInfoCard';
  import NumberFilter from '../../filters/NumberFilter';
  import MarkdownText from '../utils/MarkdownText';
  import TimeWindowMixin from './TimeWindowMixin';

  export default {
    name: 'ChildRowSkillsDisplay',
    mixins: [TimeWindowMixin],
    components: { MarkdownText, MediaInfoCard, LoadingContainer },
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
      totalPoints() {
        return NumberFilter(this.skillInfo.totalPoints);
      },
      description: function markDownDescription() {
        if (this.skillInfo && this.skillInfo.description) {
          const compiled = marked(this.skillInfo.description, { smartLists: true });
          return DOMPurify.sanitize(compiled);
        }
        return null;
      },
      selfReportingTitle() {
        if (!this.skillInfo.selfReportingType) {
          return 'Disabled';
        }

        if (this.skillInfo.selfReportingType === 'HonorSystem') {
          return 'Honor System';
        }

        return this.skillInfo.selfReportingType;
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

  .markdown blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 1rem;
    border-left: 5px solid #eeeeee;
    color: #888;
    line-height: 1.5;
  }

  .markdown pre {
    border: 1px solid #dddddd !important;
    margin: 1rem;
    padding: 1rem;
    overflow: auto;
    font-size: 85%;
    border-radius: 6px;
    background-color: #f6f8fa;
  }

  .skill-url {
    /*white-space: pre-line;*/
    /*word-break: break-word;*/
    height: 1.5em;
    overflow: hidden;
    display: block;
  }

</style>
