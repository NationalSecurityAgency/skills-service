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
  <div v-if="isLoading" class="d-flex justify-content-center mt-5">
    <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
  </div>
</template>

<script>
  export default {
    name: 'SkillsSpinner',
    props: {
      isLoading: Boolean,
    },
  };
</script>

<style scoped>

</style>
