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
  <div>
    <div class="popover-title">
      <span
        class="progress-popup-label"
        v-html="summaryInfo.title"/>
    </div>

    <div class="popover-content-row">
      <span class="progress-popup-label">Overall:</span>
      <span
        class="progress-popup-value"
        v-html="summaryInfo.overallPoints"/>
    </div>

    <div class="popover-content-row">
      <span class="progress-popup-label">Today:</span>
      <span
        class="progress-popup-value"
        v-html="summaryInfo.todaysPoints"/>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      userSkills: {
        type: Object,
        required: true,
      },
      summaryType: {
        type: String,
        required: true,
        validate(value) {
          return ['level', 'subject', 'total', 'skill'].includes(value);
        },
      },
      titleOverride: String,
    },
    data() {
      return {
        title: '',
        overallPoints: '',
        todaysPoints: '',
      };
    },
    computed: {
      summaryInfo() {
        const summaryObject = { };
        switch (this.summaryType) {
        case 'level':
          summaryObject.title = 'Points Earned Toward Next Level';
          summaryObject.overallPoints = this.userSkills.levelPoints;
          summaryObject.todaysPoints = this.userSkills.todaysPoints < this.userSkills.levelPoints ? this.userSkills.todaysPoints : this.userSkills.levelPoints;
          break;
        case 'subject':
        case 'skill':
          summaryObject.title = this.type === 'skill' ? 'Points Earned Toward Skill' : 'Points Earned for this Subject';
          summaryObject.overallPoints = this.userSkills.points;
          summaryObject.todaysPoints = this.userSkills.todaysPoints;
          break;
        case 'total':
          summaryObject.title = 'Total Points Earned';
          summaryObject.overallPoints = this.userSkills.points;
          summaryObject.todaysPoints = this.userSkills.todaysPoints;
          break;
        default:
          throw new Error(`Unknown MyProgressSummary type [${this.summaryType}]`);
        }
        return summaryObject;
      },
    },
  };
</script>

<style>
  .progress-popup-label {
    font-weight: bold;
  }

  .popover-title {
    background-color: transparent;
    padding-bottom: 5px;
    margin-bottom: 5px;
    border-bottom: 1px solid #cccccc;
  }

  .popover-content-row {
    text-align: center;
  }
</style>
