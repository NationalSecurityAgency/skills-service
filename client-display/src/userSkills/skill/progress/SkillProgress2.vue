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
        <div class="h4"><span class="text-muted">Project:</span> {{ skill.projectName }}</div>
      </div>
      <div class="col-md-6 text-md-right text-success text-uppercase">
        <div class="h5"><i class="fa fa-vector-square"/> Cross-project Skill</div>
      </div>
    </div>

    <div v-if="skill.crossProject && !isSkillComplete" class="alert alert-primary text-center" role="alert">
      This is a cross-project skill! In order to complete this skill please visit <strong>{{
        skill.projectName
      }}</strong> project! Happy playing!!
    </div>

    <div class="row">
      <div class="col text-md-left">
        <div class="h4" @click="skillClicked" :class="{ 'skill-name-url' : allowDrillDown }" data-cy="skillProgressTitle">
          <div class="d-inline" :class="{ 'text-success' : skill.isSkillsGroupType,
                                          'text-info' : skill.isSkillType && !skill.childSkill,
                                          'text-secondary' : skill.childSkill }">
            <span v-if="skill.isSkillsGroupType"><i class="fas fa-layer-group mr-1"></i></span>

            <span v-if="skill.skillHtml" v-html="skill.skillHtml"></span>
            <span v-else>{{ skill.skill }}</span>
          </div>

          <b-badge v-if="skill.selfReporting && skill.selfReporting.enabled"
              variant="success" style="font-size: 0.8rem" class="ml-2"><i class="fas fa-check-circle"></i> Self Reportable</b-badge>
        </div>
      </div>
      <div class="col-auto text-right"
           :class="{ 'text-success' : isSkillComplete, 'text-primary': !isSkillComplete }"
           data-cy="skillProgress-ptsOverProgressBard">
        <span v-if="isSkillComplete" class="pr-1"><i class="fa fa-check"/></span>
        <animated-number :num="skill.points"/>
        / {{ skill.totalPoints | number }} Points
      </div>
    </div>
    <div class="row">
      <div class="col">
        <progress-bar :skill="skill" v-on:progressbar-clicked="skillClicked"
                      :bar-size="skill.groupId ? 12 : 22"
                      :class="{ 'skills-navigable-item' : allowDrillDown }" data-cy="skillProgressBar"/>
      </div>
    </div>
    <div v-if="showDescription && skill.isSkillType">
      <div v-if="locked" class="text-center text-muted locked-text">
          *** Skill has <b>{{ skill.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
          <span v-if="allowDrillDown">Click <i class="fas fa-lock icon"></i> to see its dependencies.</span>
          <span v-else>Please see its dependencies below.</span>
        ***
      </div>

      <p v-if="skill.subjectName" class="text-secondary mt-3">
        Subject: {{ skill.subjectName }}
      </p>

      <achievement-date v-if="skill.achievedOn" :date="skill.achievedOn" class="mt-2"/>

      <partial-points-alert v-if="!allowDrillDown" :skill="skill" :is-locked="locked"/>
      <skill-summary-cards v-if="!locked" :skill="skill" class="mt-3"></skill-summary-cards>

      <p class="skills-text-description text-primary mt-3">
        <markdown-text v-if="skill.description && skill.description.description" :text="skill.description.description"/>
      </p>

      <div>
        <skill-overview-footer :skill="skill" v-on:points-earned="pointsEarned"/>
      </div>

      <hr v-if="!isLast"/>
    </div>

    <div v-if="skill.isSkillsGroupType && childSkillsInternal" class="ml-3 mt-3">
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
            :data-cy="`group-${skill.skillId}_skillProgress-${childSkill.skillId}`"
            @points-earned="onChildSkillPointsEarned"
            :is-last="index === (childSkillsInternal.length - 1)"
        ></skill-progress2>
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
  import SkillOverviewFooter from '../SkillOverviewFooter';
  import AnimatedNumber from './AnimatedNumber';
  import SkillEnricherUtil from '../../utils/SkillEnricherUtil';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';

  export default {
    name: 'SkillProgress2',
    mixins: [NavigationErrorMixin],
    components: {
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
      isLast: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        childSkillsInternal: [],
      };
    },
    mounted() {
      if (this.skill.isSkillsGroupType && this.skill?.children && this.skill?.children.length > 0) {
        this.childSkillsInternal = this.skill.children.map((item) => ({ ...item, childSkill: true }));
      }
    },
    computed: {
      locked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
      isSkillComplete() {
        return this.skill.meta.complete;
      },
      allowDrillDown() {
        return this.enableDrillDown && this.skill.isSkillType;
      },
    },
    watch: {
      childSkillHighlightString() {
        this.highlightChildSkillName();
      },
    },
    methods: {
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
        SkillEnricherUtil.updateSkillPtsInList(this.skills, pts, skillId);
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
