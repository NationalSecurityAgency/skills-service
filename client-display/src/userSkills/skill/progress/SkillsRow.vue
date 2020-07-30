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
  <skill-progress :skill="skill" :show-description="showDescription" @progressbar-clicked="skillRowClicked" />
</template>

<script>
  import SkillProgress from '@/userSkills/skill/progress/SkillProgress';

  export default {
    components: {
      SkillProgress,
    },
    props: {
      skill: Object,
      showDescription: Boolean,
    },
    methods: {
      skillRowClicked() {
        const params = { skillId: this.skill.skillId };
        if (this.skill.crossProject && this.skill.projectId) {
          params.crossProjectId = this.skill.projectId;
        }
        this.$router.push({
          name: 'skillDetails',
          params,
        });
      },
    },
  };
</script>

<style scoped>
</style>
