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
  <div v-if="skill.alreadyHasThisSkillId || skill.alreadyHasThisName" class="alert alert-warning"
       :data-cy="`alreadyExistWarning_${skill.projectId}-${skill.skillId}`">
    <i class="fas fa-exclamation-triangle text-danger"></i> <span class="text-danger font-weight-bold mr-1"
                                                                  style="font-size: 1.1rem">Cannot import!</span>
    <b>{{ itemThatDoesNotExist }}</b> already
    {{ this.skill.alreadyHasThisSkillId && this.skill.alreadyHasThisName ? 'exist' : 'exists' }} in this project!
  </div>
</template>

<script>
  export default {
    name: 'SkillAlreadyExistingWarning',
    props: {
      skill: Object,
    },
    computed: {
      itemThatDoesNotExist() {
        if (this.skill.alreadyHasThisSkillId && this.skill.alreadyHasThisName) {
          return 'Skill ID and name';
        }

        if (this.skill.alreadyHasThisSkillId) {
          return 'Skill ID';
        }

        return 'Skill name';
      },
    },
  };
</script>

<style scoped>

</style>
