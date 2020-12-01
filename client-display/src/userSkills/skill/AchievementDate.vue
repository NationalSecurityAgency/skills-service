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
  <div class="row" data-cy="achievementDate">
    <div class="col text-center text-md-left">
      <div class="m-0 p-0">
        <div class="text-white text-center badge badge-info" style="font-size: 1.7rem;"><i class="fas fa-medal"></i></div>
        <div class="d-inline-block ml-2 text-left">
          <div>Achieved on <span class="text-dark skills-page-title-text-color" data-cy="achievementOn">{{ date | dayjs("MMMM Do YYYY") }}</span></div>
          <div class="text-black-50 small skills-page-title-text-color" data-cy="timePassed">{{ date | relativeTime()  }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'AchievementDate',
    props: ['date'],
  };
</script>

<style scoped>

</style>
