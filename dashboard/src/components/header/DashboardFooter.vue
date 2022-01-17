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
  <div class="row" data-cy="dashboardFooter">
    <div class="col-sm" role="presentation">
      <div class="row no-gutters">
        <div class="col-auto">
          <div class="fa-stack">
            <i class="fas fa-angle-up fa-stack-1x first"></i>
            <i class="fas fa-angle-up fa-stack-1x second"></i>
            <i class="fas fa-angle-up fa-stack-1x third"></i>
            <i class="fas fa-angle-up fa-stack-1x fourth"></i>
          </div>
        </div>
        <div class="col">
          <div class="small footer-text">
            <div class="">
              SkillTree Dashboard
            </div>
            <div v-if="supportLinksProps && supportLinksProps.length > 0">
              <span v-for="(supportLink, index) in supportLinksProps" :key="supportLink.label">
                <a :href="supportLink.link" class="footer-text" :data-cy="`supportLink-${supportLink.label}`" target="_blank"><u><i :class="supportLink.icon" class="mr-1"/>{{ supportLink.label }}</u></a>
                <span v-if="index < supportLinksProps.length - 1" class="mx-1">|</span>
              </span>
            </div>
          </div>
        </div>
      </div>

    </div>
    <div class="col-sm text-right" data-cy="dashboardVersionContainer">
      <span class="small mr-2 footer-text" :title="skillTreeVersionTitle" data-cy="dashboardVersion">v{{ $store.getters.config.dashboardVersion }}</span>
      <i class="fas fa-code-branch"></i>
    </div>
  </div>
</footer>
</template>

<script>
  export default {
    name: 'DashboardFooter',
    computed: {
      skillTreeVersionTitle() {
        const dateString = window.dayjs(this.$store.getters.config.artifactBuildTimestamp).format('llll [(]Z[ from UTC)]');
        return `Build Date: ${dateString}`;
      },
      supportLinksProps() {
        const configs = this.$store.getters.config;
        const dupKeys = Object.keys(configs).filter((conf) => conf.startsWith('supportLink')).map((filteredConf) => filteredConf.substr(0, 12));
        const keys = dupKeys.filter((v, i, a) => a.indexOf(v) === i);
        return keys.map((key) => ({
          link: configs[key],
          label: configs[`${key}Label`],
          icon: configs[`${key}Icon`],
        }));
      },
    },
  };
</script>

<style scoped>
.first {
  color: #2a9d8fff;
  top: 5px;
}
.second {
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
    color: lightgrey !important;
  }
.no-gutters {
    margin-right: 0;
    margin-left: 0;

  > .col,
  > [class*="col-"] {
    padding-right: 0;
    padding-left: 0;
  }
}
</style>
