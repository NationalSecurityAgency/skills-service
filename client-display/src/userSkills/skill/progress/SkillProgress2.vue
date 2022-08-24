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
  <div class="text-left" data-cy="skillProgress">
    <div v-if="skill.crossProject" class="row border-bottom mb-3 text-primary text-center">
      <div class="col-md-6 text-md-left">
        <div class="h4"><span class="text-muted">{{ projectDisplayName }}:</span> {{ skill.projectName }}</div>
      </div>
      <div class="col-md-6 text-md-right text-success text-uppercase">
        <div class="h5"><i class="fa fa-vector-square"/> Cross-{{ projectDisplayName }} {{ skillDisplayName }}</div>
      </div>
    </div>

    <div v-if="skill.crossProject && !isSkillComplete" class="alert alert-primary text-center" role="alert">
      This is a cross-{{ projectDisplayName.toLowerCase() }} {{ skillDisplayName.toLowerCase() }}! In order to complete this {{ skillDisplayName.toLowerCase() }} please visit <strong>{{
        skill.projectName
      }}</strong> {{ projectDisplayName.toLowerCase() }}! Happy playing!!
    </div>

    <div class="row">
      <div class="col text-md-left">
        <div class="h4"
             @click="skillClicked"
             @keydown.enter="skillClicked"
             :class="{ 'skill-name-url' : allowDrillDown }" data-cy="skillProgressTitle"
             :tabindex="`${skill.isSkillType ? 0 : -1}`"
             :aria-label="`${skill.isSkillType ? `Navigate to ${skill.skill}` : skill.skill }`">
          <div class="d-inline-block skills-theme-primary-color" :class="{ 'text-success' : skill.isSkillsGroupType,
                                          'text-info' : skill.isSkillType && !skill.childSkill,
                                          'text-secondary' : skill.childSkill }">
            <div class="py-1 border-secondary d-inline-block">
              <span v-if="skill.isSkillsGroupType"><i class="fas fa-layer-group mr-1 overflow-hidden"></i></span>
              <div class="text-truncate d-inline-block">
                <span class="mr-1">
                  <i v-if="!skill.copiedFromProjectId && !skill.isSkillsGroupType"
                     class="fas fa-graduation-cap text-secondary"></i>
                  <i v-if="skill.copiedFromProjectId" class="fas fa-book text-secondary"></i>
                </span>
                <span v-if="!skill.skillHtml">{{ skill.skill }}</span>
                <span v-if="skill.skillHtml" v-html="skill.skillHtml"></span>
              </div>
              <div v-if="skill.copiedFromProjectId" class="text-truncate d-inline-block ml-2"
                   style="max-width: 15rem;"><span class="text-secondary font-italic"> in </span>
                <span class="font-italic">{{ skill.copiedFromProjectName }}</span>
              </div>

              <div v-if="skill.isSkillsGroupType && skill.numSkillsRequired > 0 && skill.numSkillsRequired < skill.children.length"
                   v-b-tooltip.hover
                   :title="`A ${this.groupDisplayName} allows a ${this.skillDisplayName} to be defined by the collection ` +
                    `of other ${this.skillDisplayName}s within a ${this.projectDisplayName}. A ${this.skillDisplayName} Group can require the completion of some or all of the included ${this.skillDisplayName}s before the group be achieved.`"
                   class="ml-2 d-inline-block border rounded p-1 text-primary border-success overflow-hidden"
                   style="font-size: 0.9rem"
                   data-cy="groupSkillsRequiredBadge">
                <span class="">Requires </span> <b-badge variant="success">{{ skill.numSkillsRequired }}</b-badge> <span class="font-italic">out of</span> <b-badge variant="secondary">{{ skill.children.length }}</b-badge> skills
              </div>

              <b-badge v-if="isLastSeenSkill" id="lastSeenIndicator" variant="info" style="font-size: 0.9rem" class="ml-2 overflow-hidden">
                <i class="fas fa-eye"></i> Last Viewed
              </b-badge>
              <b-badge v-if="skill.selfReporting && skill.selfReporting.enabled"
                  variant="success" style="font-size: 0.9rem" class="ml-2 overflow-hidden"><i class="fas fa-check-circle"></i> Self Reportable</b-badge>
            </div>
          </div>
        </div>
      </div>
      <div class="col-auto text-right"
           :class="{ 'text-success' : isSkillComplete, 'text-primary': !isSkillComplete }"
           data-cy="skillProgress-ptsOverProgressBard">
        <span v-if="isSkillComplete" :data-cy="`skillCompletedCheck-${skill.skillId}`" class="pr-1"><i class="fa fa-check"/></span>
        <div v-if="skill.type === 'SkillsGroup'" class="d-inline">
          <animated-number :num="numChildSkillsComplete"/>
          / {{ numSkillsRequired | number }} Skill{{(numSkillsRequired === 1) ? '' : 's'}} {{someSkillsAreOptional ? 'Required' : ''}}
        </div>
        <div v-else class="d-inline">
          <animated-number :num="skill.points"/>
          / {{ skill.totalPoints | number }} Points
        </div>
        <div v-if="this.skillHasLastSeenDate()">
          Last Viewed: {{ this.getFormattedDate() }}
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <progress-bar :skill="skill" v-on:progressbar-clicked="skillClicked"
                      :bar-size="skill.groupId ? 12 : 22"
                      :class="{ 'skills-navigable-item' : allowDrillDown }" data-cy="skillProgressBar"/>
      </div>
    </div>

    <div v-if="showDescription || (skill.type === 'SkillsGroup' && showGroupDescriptions)" :data-cy="`skillDescription-${skill.skillId}`">
      <div v-if="skill.type === 'SkillsGroup'">
        <p class="skills-text-description text-primary mt-3" style="font-size: 0.9rem;">
          <markdown-text v-if="skill.description && skill.description.description" :text="skill.description.description"/>
        </p>
      </div>
      <div v-if="skill.type === 'Skill'">
        <div v-if="locked" class="text-center text-muted locked-text">
            *** Skill has <b>{{ skill.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
            <span v-if="allowDrillDown">Click <i class="fas fa-lock icon"></i> to see its dependencies.</span>
            <span v-else>Please see its dependencies below.</span>
          ***
        </div>

        <p v-if="skill.subjectName" class="text-secondary mt-3">
          {{ subjectDisplayName }}: {{ skill.subjectName }}
        </p>

        <achievement-date v-if="skill.achievedOn" :date="skill.achievedOn" class="mt-2"/>

        <partial-points-alert v-if="!allowDrillDown" :skill="skill" :is-locked="locked"/>
        <skill-summary-cards v-if="!locked" :skill="skill" class="mt-3"></skill-summary-cards>
        <catalog-import-status :skill="skill" />
        <p class="skills-text-description text-primary mt-3" style="font-size: 0.9rem;">
          <markdown-text v-if="skill.description && skill.description.description" :text="skill.description.description"/>
        </p>

        <div>
          <skill-overview-footer :skill="skill" v-on:points-earned="pointsEarned"/>
        </div>
      </div>
    </div>

    <div v-if="skill.isSkillsGroupType && childSkillsInternal" class="ml-4 mt-3">
      <div v-for="(childSkill, index) in childSkillsInternal"
           :key="`group-${skill.skillId}_skill-${childSkill.skillId}`"
           class="skills-theme-bottom-border-with-background-color"
           :class="{ 'separator-border-thick' : showDescription }"
      >
        <skill-progress2
            :id="`group-${skill.skillId}_skillProgress-${childSkill.skillId}`"
            class="mb-3"
            :skill="childSkill"
            :subjectId="subjectId"
            :badgeId="badgeId"
            :type="type"
            :enable-drill-down="true"
            :show-description="showDescription"
            :show-group-descriptions="showGroupDescriptions"
            :data-cy="`group-${skill.skillId}_skillProgress-${childSkill.skillId}`"
            @points-earned="onChildSkillPointsEarned"
        ></skill-progress2>

        <hr v-if="index < (childSkillsInternal.length - 1)"/>
      </div>
    </div>

  </div>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import ProgressBar from '@/userSkills/skill/progress/ProgressBar';
  import SkillSummaryCards from '@/userSkills/skill/progress/SkillSummaryCards';
  import PartialPointsAlert from '@/userSkills/skill/PartialPointsAlert';
  import AchievementDate from '@/userSkills/skill/AchievementDate';
  import NavigationErrorMixin from '@/common/utilities/NavigationErrorMixin';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';
  import CatalogImportStatus from '@/userSkills/skill/progress/CatalogImportStatus';
  import SkillOverviewFooter from '@/userSkills/skill/SkillOverviewFooter';
  import AnimatedNumber from '@/userSkills/skill/progress/AnimatedNumber';
  import dayjs from '@/common-components/DayJsCustomizer';
  import Vue from 'vue';
  import VueScrollTo from 'vue-scrollto';

  Vue.use(VueScrollTo);

  export default {
    name: 'SkillProgress2',
    mixins: [NavigationErrorMixin],
    components: {
      CatalogImportStatus,
      AnimatedNumber,
      SkillOverviewFooter,
      AchievementDate,
      PartialPointsAlert,
      SkillSummaryCards,
      ProgressBar,
      MarkdownText,
    },
    props: {
      skill: Object,
      showDescription: {
        type: Boolean,
        default: true,
      },
      showGroupDescriptions: {
        type: Boolean,
        default: false,
      },
      enableDrillDown: {
        type: Boolean,
        default: false,
      },
      subjectId: {
        type: String,
        required: false,
      },
      badgeId: {
        type: String,
        required: false,
      },
      type: {
        type: String,
        default: 'subject',
      },
      childSkillHighlightString: {
        type: String,
        default: '',
      },
      lastSeenSkills: {
        type: Object,
        default: null,
      },
    },
    data() {
      return {
        childSkillsInternal: [],
      };
    },
    mounted() {
      this.initChildSkills();
      this.highlightChildSkillName();

      if (this.isLastSeenSkill) {
        this.scrollToLastSeenSkill();
      }
    },
    computed: {
      locked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
      isSkillComplete() {
        return this.skill && this.skill.meta && this.skill.meta.complete;
      },
      allowDrillDown() {
        return this.enableDrillDown && this.skill.isSkillType;
      },
      isSkillsGroupWithChildren() {
        return this.skill?.isSkillsGroupType && this.skill?.children && this.skill?.children.length > 0;
      },
      numChildSkillsComplete() {
        return this.isSkillsGroupWithChildren ? this.skill.children.filter((childSkill) => childSkill.meta.complete).length : 0;
      },
      numSkillsRequired() {
        if (this.isSkillsGroupWithChildren) {
          return this.skill.numSkillsRequired === -1 ? this.skill.children.length : this.skill.numSkillsRequired;
        }
        return 0;
      },
      someSkillsAreOptional() {
        return this.isSkillsGroupWithChildren && this.skill.numSkillsRequired !== -1 && this.skill.numSkillsRequired < this.skill.children.length;
      },
      isLastSeenSkill() {
        if (this.lastSeenSkills && this.lastSeenSkills[this.skill.projectId] && this.lastSeenSkills[this.skill.projectId][this.subjectId]) {
          return this.lastSeenSkills[this.skill.projectId][this.subjectId].lastSeenSkill === this.skill.skillId;
        }
        return false;
      },
    },
    watch: {
      'skill.children': function updateChildSkills() {
        this.initChildSkills();
        this.highlightChildSkillName();
      },
    },
    methods: {
      initChildSkills() {
        if (this.isSkillsGroupWithChildren) {
          this.childSkillsInternal = this.skill.children.map((item) => ({ ...item, childSkill: true }));
        }
      },
      highlightChildSkillName() {
        if (!this.childSkillHighlightString || this.childSkillHighlightString.trim().length === 0) {
          this.childSkillsInternal = this.childSkillsInternal.map((item) => ({ ...item, skillHtml: null }));
        } else if (this.childSkillsInternal && this.childSkillsInternal.length > 0) {
          this.childSkillsInternal = this.childSkillsInternal.map((item) => {
            const skillHtml = StringHighlighter.highlight(item.skill, this.childSkillHighlightString);
            return ({ ...item, skillHtml });
          });
        }
      },
      onChildSkillPointsEarned(pts, skillId) {
        this.$emit('points-earned', pts, this.skill.skillId, skillId);
      },
      pointsEarned(pts) {
        this.$emit('points-earned', pts, this.skill.skillId);
      },
      skillClicked() {
        if (this.allowDrillDown) {
          const route = this.getSkillDetailsRoute();
          const params = this.getParams();

          this.handlePush({
            name: route,
            params,
          });
        }
      },
      getParams() {
        const params = { skillId: this.skill.skillId };
        if (this.subjectId) {
          params.subjectId = this.subjectId;
        }
        if (this.badgeId) {
          params.badgeId = this.badgeId;
        }
        if (this.skill.crossProject && this.skill.projectId) {
          params.crossProjectId = this.skill.projectId;
        }
        params.projectId = this.skill.projectId;
        return params;
      },
      getSkillDetailsRoute() {
        let route = 'skillDetails';
        if (this.type === 'badge') {
          route = 'badgeSkillDetails';
        } else if (this.type === 'global-badge') {
          route = 'globalBadgeSkillDetails';
        }
        return route;
      },
      skillHasLastSeenDate() {
        if (this.skill.projectId && this.subjectId && this.lastSeenSkills[this.skill.projectId] && this.lastSeenSkills[this.skill.projectId][this.subjectId] && this.lastSeenSkills[this.skill.projectId][this.subjectId].skillHistory[this.skill.skillId]) {
          return true;
        }
        return false;
      },
      getFormattedDate() {
        return dayjs(this.lastSeenSkills[this.skill.projectId][this.subjectId].skillHistory[this.skill.skillId]).fromNow();
      },
      scrollToLastSeenSkill() {
        const lastSeenIndicator = document.getElementById('lastSeenIndicator');
        if (lastSeenIndicator && this.$route.params.jumpToLastSeen) {
          VueScrollTo.scrollTo('#lastSeenIndicator', 750, {
            y: true,
            x: false,
            easing: 'ease-in',
            offset: -25,
          });
        }
      },
    },
  };
</script>

<style scoped>
.skill-name-url:hover {
  cursor: pointer;
  text-decoration: underline;
}
.locked-text {
  font-size: 0.8rem;
}
</style>
