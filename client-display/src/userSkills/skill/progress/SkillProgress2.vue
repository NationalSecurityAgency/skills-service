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
    <div v-if="skillInternal.crossProject" class="row border-bottom mb-3 text-primary text-center">
      <div class="col-md-6 text-md-left">
        <h4><span class="text-muted">Project:</span> {{ skillInternal.projectName }}</h4>
      </div>
      <div class="col-md-6 text-md-right text-success text-uppercase">
        <h5><i class="fa fa-vector-square"/> Cross-project Skill</h5>
      </div>
    </div>

    <div v-if="skillInternal.crossProject && !isSkillComplete" class="alert alert-primary text-center" role="alert">
      This is a cross-project skill! In order to complete this skill please visit <strong>{{
        skillInternal.projectName
      }}</strong> project! Happy playing!!
    </div>

    <div class="row">
      <div class="col text-md-left">
        <h4 @click="skillClicked" :class="{ 'skill-name-url' : enableDrillDown }" data-cy="skillProgressTitle">{{ skillInternal.skill }}</h4>
      </div>
      <div class="col-auto text-right" :class="{ 'text-success' : isSkillComplete, 'text-primary': !isSkillComplete }">
        <span v-if="isSkillComplete" class="pr-1"><i class="fa fa-check"/></span>
        <animated-number :num="skillInternal.points"/>
        / {{ skillInternal.totalPoints | number }} Points
      </div>
    </div>
    <div class="row">
      <div class="col">
        <progress-bar :skill="skillInternal" v-on:progressbar-clicked="skillClicked"
                      :class="{ 'skills-navigable-item' : enableDrillDown }" data-cy="skillProgressBar"/>
      </div>
    </div>
    <div v-if="showDescription">
      <div v-if="locked" class="text-center text-muted locked-text">
          *** Skill has <b>{{ skillInternal.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
          <span v-if="enableDrillDown">Click <i class="fas fa-lock icon"></i> to see its dependencies.</span>
          <span v-else>Please see its dependencies below.</span>
        ***
      </div>

      <achievement-date v-if="skillInternal.achievedOn" :date="skillInternal.achievedOn" class="mt-2"/>

      <partial-points-alert v-if="!enableDrillDown" :skill="skillInternal" :is-locked="locked"/>
      <skill-summary-cards v-if="!locked" :skill="skillInternal" class="mt-3"></skill-summary-cards>

      <p class="skills-text-description text-primary mt-3">
        <markdown-text :text="skill.description.description"/>
      </p>

      <div>
        <skill-overview-footer :skill="skillInternal" v-on:points-earned="pointsEarned"/>
      </div>
    </div>
  </div>
</template>

<script>
  import ProgressBar from '@/userSkills/skill/progress/ProgressBar';
  import SkillSummaryCards from '@/userSkills/skill/progress/SkillSummaryCards';
  import MarkdownText from '@/common/utilities/MarkdownText';
  import PartialPointsAlert from '@/userSkills/skill/PartialPointsAlert';
  import AchievementDate from '@/userSkills/skill/AchievementDate';
  import SkillOverviewFooter from '../SkillOverviewFooter';
  import AnimatedNumber from './AnimatedNumber';

  export default {
    name: 'SkillProgress2',
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
    },
    data() {
      return {
        skillInternal: {},
      };
    },
    created() {
      this.skillInternal = { ...this.skill };
    },
    computed: {
      locked() {
        return this.skillInternal.dependencyInfo && !this.skillInternal.dependencyInfo.achieved;
      },
      isSkillComplete() {
        return this.skillInternal.points === this.skillInternal.totalPoints;
      },
    },
    methods: {
      pointsEarned(pts) {
        this.skillInternal.points += pts;
        this.skillInternal.todaysPoints += pts;
        if (this.skillInternal.points === this.skillInternal.totalPoints) {
          this.skillInternal.achievedOn = new Date();
        }
      },
      skillClicked() {
        if (this.enableDrillDown) {
          const params = { skillId: this.skill.skillId };
          if (this.skill.crossProject && this.skill.projectId) {
            params.crossProjectId = this.skill.projectId;
          }
          this.$router.push({
            name: 'skillDetails',
            params,
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
