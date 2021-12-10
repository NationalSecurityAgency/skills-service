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
  <loading-container class="child-row" v-bind:is-loading="loading.skills" :data-cy="`childRowDisplay_${skillInfo.skillId}`">

    <div v-if="isImported" class="mt-3 alert alert-info" header="Skill Catalog">
      This skill was <b>imported</b> from the <b-badge class=""><i class="fas fa-book"></i> CATALOG</b-badge> and was initially
      defined in the <b class="text-primary">{{ skillInfo.copiedFromProjectName }}</b> project.
      This skill is <b-badge>Read-Only</b-badge> and can only edited in the <b class="text-primary">{{ skillInfo.copiedFromProjectName }}</b> project
    </div>

    <div class="row">
      <div class="col-12 col-md-6 mt-2">
        <media-info-card :title="`${totalPoints} Points`" icon-class="fas fa-calculator text-success"
                         data-cy="skillOverviewTotalpoints">
          <strong>{{ skillInfo.pointIncrement | number }}</strong> points <i class="fa fa-times text-muted" aria-hidden="true"/>
          <strong> {{ skillInfo.numPerformToCompletion | number }}</strong> repetition<span v-if="skillInfo.numPerformToCompletion>1">s</span> to Completion
        </media-info-card>
      </div>
      <div class="col-12 col-md-6 mt-2">
        <media-info-card :title="timeWindowTitle(skillInfo)" icon-class="fas fa-hourglass-half text-info"
                         data-cy="skillOverviewTimewindow">
          {{ timeWindowDescription(skillInfo) }}
        </media-info-card>
      </div>
      <div class="col-12 col-md-6 mt-2">
        <media-info-card :title="`Version # ${skillInfo.version}`" icon-class="fas fa-code-branch text-warning"
                         data-cy="skillOverviewVersion">
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
        <markdown-text v-if="description" :text="description" data-cy="skillOverviewDescription"/>
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
        <a v-if="skillInfo.helpUrl" :href="helpUrl" target="_blank" rel="noopener" class="skill-url"
           data-cy="skillOverviewHelpUrl"><span v-if="rootHelpUrl" class="border rounded pt-1 pl-1 pb-1 root-help-url" v-b-tooltip.hover="'Root Help URL was configured in the project\'s settings.'"><i class="fas fa-cogs"></i> {{ rootHelpUrl }}</span>{{ skillInfo.helpUrl }}</a>
        <span v-else class="text-muted">
          Not Specified
        </span>
      </span>
    </div>

    <b-card v-if="skillInfo.sharedToCatalog" class="mt-3" header="Skill Catalog">
      This skill was exported to the <b-badge class=""><i class="fas fa-book"></i> CATALOG</b-badge>.
      Please visit
      <b-button data-cy="navigateToProjCatalog" variant="outline-info" size="sm"
                   :to="{ name:'SkillsCatalog', params: { projectId: this.projectId} }"
                   aria-label="View Project's Catalog">Skill Catalog</b-button> page to manage exported skills.
    </b-card>

  </loading-container>
</template>

<script>
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
        loading: {
          skills: true,
        },
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
      skill(val) {
        this.skillInfo = val;
      },
    },
    computed: {
      totalPoints() {
        return NumberFilter(this.skillInfo.totalPoints);
      },
      description: function markDownDescription() {
        if (this.skillInfo && this.skillInfo.description) {
          return this.skillInfo.description;
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
      rootHelpUrlSetting() {
        return this.$store.getters.projConfig && this.$store.getters.projConfig['help.url.root'];
      },
      rootHelpUrl() {
        if (!this.rootHelpUrlSetting || this.skillInfo?.helpUrl?.toLowerCase()?.startsWith('http')) {
          return null;
        }
        if (this.rootHelpUrlSetting.endsWith('/')) {
          return this.rootHelpUrlSetting.substring(0, this.rootHelpUrlSetting.length - 1);
        }
        return this.rootHelpUrlSetting;
      },
      helpUrl() {
        if (!this.skillInfo?.helpUrl) {
          return null;
        }
        const rootHelpUrlSetting = this.rootHelpUrl;
        if (rootHelpUrlSetting) {
          return `${rootHelpUrlSetting}${this.skillInfo.helpUrl}`;
        }
        return this.skillInfo.helpUrl;
      },
      isImported() {
        return this.skillInfo && this.skillInfo.copiedFromProjectId && this.skillInfo.copiedFromProjectId.length > 0;
      },
    },
    methods: {
      loadSkills() {
        this.loading.skills = true;
        if (this.skill) {
          this.skillInfo = this.skill;
          this.loading.skills = false;
        } else {
          SkillsService.getSkillDetails(this.projectId, this.subjectId, this.parentSkillId)
            .then((response) => {
              this.skillInfo = response;
            })
            .finally(() => {
              this.loading.skills = false;
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
    height: 1.5em;
    overflow: hidden;
    display: block;
  }

  .root-help-url {
    background-color: #eeeeee;
    border-color: black !important;
  }

</style>
