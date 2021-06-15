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
  <span>
    <span data-cy="projectCreated">
      <span class="text-secondary small font-italic">Created: </span><slim-date-cell :value="created"/>
      <span class="text-secondary small mx-2 d-none d-md-inline">|</span>
    </span>
    <br class="d-md-none"/>
    <span data-cy=projectLastReportedSkill>
      <span class="text-secondary small font-italic">Last Reported Skill: </span><slim-date-cell :value="lastReportedSkill" :fromStartOfDay="true"/>
    </span>
  </span>
</template>

<script>
  import SlimDateCell from '../utils/table/SlimDateCell';

  export default {
    name: 'ProjectDates',
    components: {
      SlimDateCell,
    },
    props: ['created', 'lastReportedSkill'],
  };
</script>

<style scoped>

</style>
