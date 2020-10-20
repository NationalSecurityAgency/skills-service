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
  <div style="vertical-align: text-top !important;">
    <span class="border border-info rounded d-inline-block bg-white p-1 text-center icon">
      <span v-if="type === types[0]">
        <i class="fa fa-trophy text-muted"/>
      </span>
      <span v-if="type === types[1]">
            <i class="fa fa-cubes text-muted"/>
      </span>
      <span v-if="type === types[2]">
            <i class="fa fa-graduation-cap text-muted"/>
      </span>
      <span v-if="type === types[3]">
            <i class="fa fa-award text-muted"/>
      </span>
      <span v-if="isUnknownType">
            <i class="fa fa-award text-muted"/>
      </span>
    </span>
    <span style="vertical-align: text-top !important;">
      {{ type }}
    </span>
  </div>
</template>

<script>
  export default {
    name: 'AchievementType',
    props: ['type'],
    data() {
      return {
        types: [
          'Overall',
          'Subject',
          'Skill',
          'Badge',
        ],
      };
    },
    computed: {
      isUnknownType() {
        return this.types.find((item) => item === this.type) === undefined;
      },
    },
  };
</script>

<style scoped>
.icon {
  width: 2rem;
}
</style>
