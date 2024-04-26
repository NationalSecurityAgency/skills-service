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
  <div v-if="errorsToShow" class="alert alert-danger" data-cy="questionErrors">
    <i class="fas fa-exclamation-triangle" aria-hidden="true"></i> Please fix the following:
    <div v-for="e in (errorsToShow.length > 5 ? errorsToShow.slice(0, 3) : errorsToShow)" :key="e" class="ml-4">
      - {{ e }}
    </div>
    <div v-if="errorsToShow.length > 5" class="ml-4">
      <b-collapse id="collapse-additional-errors" v-model="additionalErrorsShown">
        <div v-for="e in errorsToShow.slice(3, errorsToShow.length)" :key="e">
          - {{ e }}
        </div>
      </b-collapse>
      <b-link v-b-toggle.collapse-additional-errors>
        <span v-if="!additionalErrorsShown"><i class="fas fa-arrow-alt-circle-down" aria-hidden="true"></i> Expand {{ errorsToShow.length - 3 }} more...</span>
        <span v-else><i class="fas fa-arrow-alt-circle-up" aria-hidden="true"></i> Collapse</span>
      </b-link>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'QuizRunValidationWarnings',
    props: {
      errorsToShow: Array,
    },
    data() {
      return {
        additionalErrorsShown: false,
      };
    },
  };
</script>

<style scoped>

</style>
