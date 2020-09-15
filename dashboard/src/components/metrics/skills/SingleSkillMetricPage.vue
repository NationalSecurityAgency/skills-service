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
        <b-card>
          <div class="row align-items-center">
            <div class="col">
              <h5 class="text-uppercase text-muted">Achieved</h5>
              <h3><strong>23,400</strong></h3>
            </div>
            <div class="col text-right text-info h-100">
              <i class="fa fa-trophy" style="font-size: 3rem;"/>
            </div>
          </div>
          <p class="text-muted small">Number of users that achieved this skill</p>
        </b-card>
      </div>
      <div class="col">
        <b-card>
          <div class="row align-items-center">
            <div class="col">
              <h5 class="text-uppercase text-muted">In Progress</h5>
              <h3><strong>1,553</strong></h3>
            </div>
            <div class="col text-right text-primary h-100">
              <i class="fa fa-running" style="font-size: 3rem;"/>
            </div>
          </div>
          <p class="text-muted small">Number of Users with some points earned toward the skill</p>
        </b-card>
      </div>
      <div class="col">
        <b-card>
          <div class="row align-items-center">
            <div class="col">
              <h5 class="text-uppercase text-muted">Last Achieved</h5>
              <h3><strong>2 hrs ago</strong></h3>
            </div>
            <div class="col text-right h-100 text-warning">
              <i class="fa fa-clock" style="font-size: 3rem;"/>
            </div>
          </div>
          <p class="text-muted small">A user achieved this skill 2 hours ago</p>
        </b-card>
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

  export default {
    name: 'SingleSkillMetricPage',
    components: {
      AppliedSkillEventsOverTime,
      SkillAchievedByUsersOverTime,
      SubPageHeader,
    },
  };
</script>

<style scoped>

</style>
