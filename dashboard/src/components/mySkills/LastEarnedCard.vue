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
  <b-card body-class="p-0 d-flex flex-column">
    <div class="flex-grow-1">
      <b-row class="justify-content-between no-gutters px-3 pt-3 pb-1">
        <b-col cols="4">
          <div class="text-uppercase text-secondary">Earned</div>
          <i class="fas fa-calendar-alt mt-3 ml-2 mb-2 skills-color-events" style="font-size: 5rem;" />
        </b-col>
        <b-col cols="8" class="mb-4 pt-4 pl-2 small">
          <div v-if="mostRecentAchievedSkill !== null">
            <span>Last Achieved skill</span> <b-badge variant="success" style="font-size: 1rem;">{{ mostRecentAchievedSkill | timeFromNow }}</b-badge>
          </div>
          <div class="my-2">
            <b-badge variant="info" style="font-size: 1rem;">{{ numAchievedSkillsLastWeek }} skills</b-badge> in the last week
          </div>
          <div class="my-2">
            <b-badge variant="info" style="font-size: 1rem;">{{ numAchievedSkillsLastMonth }} skills</b-badge> in the last month
          </div>
        </b-col>
      </b-row>
    </div>
    <b-row class="justify-content-between no-gutters border-top text-muted small mt-4">
      <b-col class="p-2">
        {{ getFooterText() }}
      </b-col>
    </b-row>
  </b-card>
</template>

<script>
  import dayjs from '../../DayJsCustomizer';

  export default {
    name: 'LastEarnedCard',
    props: {
      numAchievedSkillsLastMonth: {
        type: Number,
        required: true,
      },
      numAchievedSkillsLastWeek: {
        type: Number,
        required: true,
      },
      mostRecentAchievedSkill: {
        type: String,
        required: false, // will be null id no skills have been earned yet
      },
    },
    data() {
      return {
      };
    },
    methods: {
      isWithinOneWeek(timestamp) {
        return dayjs(timestamp).isAfter(dayjs().subtract(7, 'day'));
      },
      getFooterText() {
        if (this.mostRecentAchievedSkill !== null) {
          if (this.isWithinOneWeek(this.mostRecentAchievedSkill)) {
            return 'Keep up the good work!!';
          }
          return 'It\'s been a while, perhaps earn another skill?';
        }
        return 'You have not achieved any skills yet, time to get started!';
      },
    },
  };
</script>

<style scoped>

</style>
