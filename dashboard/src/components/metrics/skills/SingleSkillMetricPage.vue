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
    <sub-page-header title="Metrics"/>
    <div class="row mb-2">
      <div class="col">
        <stats-card title="Achieved" :statNum="23400" icon="fa fa-trophy text-info">
          Number of users that achieved this skill
        </stats-card>
      </div>
      <div class="col">
        <stats-card title="In Progress" :statNum="1453" icon="fa fa-running text-primary">
          Number of Users with some points earned toward the skill
        </stats-card>
      </div>
      <div class="col">
        <stats-card title="Last Achieved" :statNum="1603125996410" :calculate-time-from-now="true"
                    icon="fa fa-clock text-warning">
          This skill was last achieved on <span class="text-success">{{ 1603125996410 | date}}</span>.
        </stats-card>
      </div>

    </div>

    <b-card header="Users that Achieved this Skill">
      <skill-achieved-by-users-over-time/>
    </b-card>
    <b-card header="Applied Skill Events over time" class="mt-2">
      <applied-skill-events-over-time />
    </b-card>
  </div>
</template>

<script>
  import SubPageHeader from '@//components/utils/pages/SubPageHeader';
  import SkillAchievedByUsersOverTime from './SkillAchievedByUsersOverTime';
  import AppliedSkillEventsOverTime from './AppliedSkillEventsOverTime';
  import StatsCard from '../utils/StatsCard';

  export default {
    name: 'SingleSkillMetricPage',
    components: {
      StatsCard,
      AppliedSkillEventsOverTime,
      SkillAchievedByUsersOverTime,
      SubPageHeader,
    },
  };
</script>

<style scoped>

</style>
