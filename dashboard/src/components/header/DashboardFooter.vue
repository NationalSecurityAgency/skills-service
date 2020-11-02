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
<footer class="bg-primary page-footer font-small text-white-50 p-3 px-4 border-top border-info">
  <div class="row">
    <div class="col">
      <img src="/static/img/skilltree_logo_arrows.png" style="height: 1.5rem; font-family: Carlito " />
      <span class="ml-2 small">SkillTree Dashboard</span>
    </div>
    <div class="col text-right">
      <span class="small mr-2">v{{ $store.getters.config.dashboardVersion }}</span>
      <i class="fas fa-code-branch"></i>
    </div>
  </div>
</footer>
</template>

<script>
  export default {
    name: 'DashboardFooter',
  };
</script>

<style scoped>

</style>
