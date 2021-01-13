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
          <div>
            <span>Achieved skill</span> <b-badge variant="success" style="font-size: 1rem;">2 days</b-badge> ago
          </div>
          <div class="my-2">
            <b-badge variant="info" style="font-size: 1rem;">0 skills</b-badge>  in the last week
          </div>
          <div class="my-2">
            <b-badge variant="info" style="font-size: 1rem;">12 skills</b-badge>  in the last month
          </div>
                    <!--        <div><b-badge variant="info">Gems: 0</b-badge></div> -->
  <!--        <span class="fa-stack">-->
  <!--          <i class="fas fa-circle fa-stack-2x text-header"></i>-->
  <!--          <i class="fas fa-trophy fa-stack-1x text-info"></i>-->
  <!--        </span>-->
        </b-col>
      </b-row>
    </div>
    <b-row class="justify-content-between no-gutters border-top text-muted small mt-4">
      <b-col class="p-2">
        It's been a while, perhaps earn another skill?
      </b-col>
    </b-row>
  </b-card>
</template>

<script>
  export default {
    name: 'LastEarnedCard',
    data() {
      return {
      };
    },
  };
</script>

<style scoped>

</style>
