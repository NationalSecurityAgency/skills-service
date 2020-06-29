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
    <div class="loading-overlay"></div>
    <div class="position-absolute align-center spinner">
      <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'ServerTableLoadingMask',
  };
</script>

<style scoped>
  /*
  NOTE - for this component to work properly, the vue-tables2 server table must have the following style applied to it:

  table {
    width: 100%;
    position: relative;
  }

  this will need to be present in the parent component that uses this component
   */
  .loading-overlay {
    position: absolute;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: white;
    opacity: 0.9;
    filter: blur(5px);
  }

  .spinner {
    position: absolute;
    left: 50%;
    right: 50%;
    bottom: 60%;
    top: 40%;
  }
</style>
