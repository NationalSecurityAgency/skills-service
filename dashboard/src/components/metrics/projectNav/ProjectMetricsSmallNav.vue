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
  <b-button-group>
    <b-button :to="{ name: navItem.pathName }" v-for="navItem in navCards" :key="navItem.title"
              :class="{'bg-primary' : $route.name === navItem.pathName, 'skills-white-text' : $route.name === navItem.pathName}"
              variant="outline-info"> <i :class="navItem.icon"/></b-button>
  </b-button-group>
</template>

<script>
  export default {
    name: 'ProjectMetricsSmallNav',
    props: ['navCards'],
    data() {
      return {
        routerPageName: this.$route.name,
      };
    },
  };
</script>

<style scoped>
.skills-white-text i {
  color: white !important;
}
</style>
