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
  <div class="text-center pt-5 w-100">
    <div class="text-center">
      <b-spinner variant="info" label="Loading" class="align-middle" style="width: 3rem; height: 3rem;" />
      <h4 class="text-muted mt-2">Loading...</h4>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'MetricsSpinner',
  };
</script>

<style scoped>
.card-img-overlay {
  z-index: 9999 !important;
  background-color: rgba(255,255,255,0.5);
}
</style>
