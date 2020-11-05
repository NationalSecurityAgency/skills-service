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
    <div class="col-sm" role="presentation">
      <span class="fa-stack">
        <i class="fas fa-angle-up fa-stack-1x first"></i>
        <i class="fas fa-angle-up fa-stack-1x second"></i>
        <i class="fas fa-angle-up fa-stack-1x third"></i>
        <i class="fas fa-angle-up fa-stack-1x fourth"></i>
      </span>
      <span class="small footer-text">SkillTree Dashboard</span>
    </div>
    <div class="col-sm text-right">
      <span class="small mr-2 footer-text" aria-label="dashboard software version">v{{ $store.getters.config.dashboardVersion }}</span>
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
.first {
  color: #2a9d8fff;
  top: 5px;
}
.second {
  /*top: -5px;*/
  color: #e9c369ff;
}
.third {
  top: -5px;
  color: #f4a261ff;
}
.fourth {
  top: -10px;
  color: #e76f51ff;
}
  .footer-text {
    color: lightgrey;
  }

</style>
