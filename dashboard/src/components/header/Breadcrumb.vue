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

<!--
 Please note that we heavily rely on routes' naming convention to build the breadcrumb
 Generally we expect pattern of '/entity/id/entity2/id2' which then will map to
 'entity:id / entity2:id2' breadcrumb; If the number of entities is even then the last item
 will not have entity/label, for example '/entity/id/entity2/id2/last' will produce:
 'entity:id / entity2:id2 / last'

 You can optionally override the last items display in the router config:
 meta: { breadcrumb: 'Add Skill Event' },
-->
<template>
  <nav aria-label="breadcrumb" class="border-bottom" role="navigation" data-cy="breadcrumb-bar">
    <ol class="breadcrumb">
      <li v-for="(item, index) of items" :key="item.label" class="breadcrumb-item" data-cy="breadcrumb-item">
         <span v-if="index === items.length-1" style="color: #e7e7e7" :data-cy="`breadcrumb-${item.value}`">
           <span v-if="item.label" class="breadcrumb-item-label text-uppercase" aria-current="page">{{ item.label }}: </span><span>{{ item.value }}</span>
         </span>
         <span v-else>
           <router-link :to="item.url" class="text-white" :data-cy="`breadcrumb-${item.value}`">
             <span v-if="item.label" class="breadcrumb-item-label text-uppercase">{{ item.label }}: </span>
             <span class="">{{ item.value }}</span>
           </router-link>
         </span>
      </li>
    </ol>
  </nav>
</template>

<script>
  import SettingsService from '../settings/SettingsService';

  const projectAndRankingPathItem = 'progress-and-rankings';
  export default {
    name: 'Breadcrumb',
    data() {
      return {
        items: [],
        idsToExcludeFromPath: ['subjects', 'skills', 'projects', 'crossProject', 'dependency', 'global'],
        keysToExcludeFromPath: [],
        ignoreNext: false,
        projectDisplayName: 'Project',
        subjectDisplayName: 'Subject',
        groupDisplayName: 'Group',
        skillDisplayName: 'Skill',
        currentProjectId: null,
      };
    },
    mounted() {
      this.build();
    },
    computed: {
      skillsClientDisplayPath() {
        return this.$store.getters.skillsClientDisplayPath;
      },
      skillsClientDisplayPathParts() {
        return this.skillsClientDisplayPath.path.replace(/^\/+|\/+$/g, '').split('/');
      },
      hasSkillsClientDisplayPath() {
        return this.skillsClientDisplayPath && this.skillsClientDisplayPath.path;
      },
      dashboardPath() {
        return this.$route.path;
      },
      dashboardPathParts() {
        return this.dashboardPath.replace(/^\/+|\/+$/g, '').split('/');
      },
    },
    watch: {
      $route: function routeChange() {
        this.build();
      },
      skillsClientDisplayPath() {
        this.build();
      },
    },
    methods: {
      forceNavigate(to) {
        this.$router.push(to);
      },
      build() {
        this.handleCustomLabels().then(() => {
          this.buildBreadcrumb();
        });
      },
      handleCustomLabels() {
        return new Promise((resolve) => {
          const isProgressAndRankingsProject = this.$route.name === 'MyProjectSkills';
          if (isProgressAndRankingsProject) {
            const currentProjectId = this.$route.params.projectId;
            if (this.currentProjectId !== currentProjectId) {
              SettingsService.getClientDisplayConfig(currentProjectId).then((response) => {
                this.projectDisplayName = response.projectDisplayName;
                this.subjectDisplayName = response.subjectDisplayName;
                this.groupDisplayName = response.groupDisplayName;
                this.skillDisplayName = response.skillDisplayName;
                this.currentProjectId = currentProjectId;
                resolve();
              });
            } else {
              resolve();
            }
          } else {
            this.projectDisplayName = 'Project';
            this.subjectDisplayName = 'Subject';
            this.groupDisplayName = 'Group';
            this.skillDisplayName = 'Skill';
            this.currentProjectId = null;
            resolve();
          }
        });
      },
      buildBreadcrumb() {
        const newItems = [];
        let res = this.dashboardPathParts;
        if (this.hasSkillsClientDisplayPath) {
          res = [...res, ...this.skillsClientDisplayPathParts];
        }
        let key = null;

        const lastItemInPathCustomName = this.$route.meta.breadcrumb;

        res.forEach((item, index) => {
          let value = item === 'administrator' ? 'Projects' : item;
          if (value) {
            if (!this.ignoreNext && item !== 'global') {
              // treat crossProject as a special case
              if (value === 'crossProject') {
                this.ignoreNext = true;
                key = 'Dependency';
                return;
              }
              if (value === projectAndRankingPathItem && !this.isProgressAndRankingEnabled()) {
                return;
              }
              if (index === this.dashboardPathParts.length - 1 && lastItemInPathCustomName) {
                key = null;
                value = lastItemInPathCustomName;
              }

              if (key) {
                if (!this.shouldExcludeKey(key)) {
                  newItems.push(this.buildResItem(key, value, res, index));
                }
                key = null;
              } else {
                // must exclude items in the path because each page with navigation
                // doesn't have a sub-route in the url, for example:
                // '/projects/projectId' will conceptually map to '/projects/projectId/subjects'
                // but there is no '/project/projectId/subjects' route configured so when parsing something like
                // '/projects/projectId/subjects/subjectId/stats we must end up with:
                //    'projects / project:projectId / subject:subjectId / stats'
                // notice that 'subjects' is missing
                if (!this.shouldExcludeValue(value)) {
                  newItems.push(this.buildResItem(key, value, res, index));
                }
                if (value !== 'Projects' && value !== projectAndRankingPathItem && value !== lastItemInPathCustomName) {
                  key = value;
                }
              }
            } else {
              this.ignoreNext = false;
            }
          }
        });

        this.items = newItems;
      },
      buildResItem(key, item, res, index) {
        const decodedItem = decodeURIComponent(item);
        return {
          label: key ? this.prepKey(key) : null,
          value: !key ? this.capitalize(this.hyphenToCamelCase(decodedItem)) : decodedItem,
          url: this.getUrl(res, index + 1),
        };
      },
      getUrl(arr, endIndex) {
        const dashboardUrlSize = this.dashboardPathParts.length;
        let url = `/${arr.slice(0, Math.min(endIndex, dashboardUrlSize)).join('/')}/`;
        if (this.hasSkillsClientDisplayPath && endIndex >= dashboardUrlSize) {
          const skillsClientDisplayPath = (endIndex > dashboardUrlSize) ? `/${arr.slice(dashboardUrlSize, endIndex).join('/')}` : '/';
          const queryParams = new URLSearchParams(window.location.search);
          queryParams.set('skillsClientDisplayPath', skillsClientDisplayPath);
          url += `?${queryParams.toString()}`;
        }
        return url;
      },
      prepKey(key) {
        const res = key.endsWith('s') ? key.substring(0, key.length - 1) : key;
        return this.capitalize(this.substituteCustomLabels(res));
      },
      substituteCustomLabels(label) {
        if (label.toLowerCase() === 'project') {
          return this.projectDisplayName;
        }
        if (label.toLowerCase() === 'subject') {
          return this.subjectDisplayName;
        }
        if (label.toLowerCase() === 'group') {
          return this.groupDisplayName;
        }
        if (label.toLowerCase() === 'skill') {
          return this.skillDisplayName;
        }
        return label;
      },
      hyphenToCamelCase(value) {
        return value.replace(/-([a-z])/g, (g) => ` ${g[1].toUpperCase()}`);
      },
      capitalize(value) {
        return value.charAt(0).toUpperCase() + value.slice(1);
      },
      shouldExcludeValue(item) {
        return this.idsToExcludeFromPath.some((searchForMe) => item === searchForMe);
      },
      shouldExcludeKey(key) {
        return this.keysToExcludeFromPath.some((searchForMe) => key === searchForMe);
      },
      isProgressAndRankingEnabled() {
        return this.$store.getters.config.rankingAndProgressViewsEnabled === true || this.$store.getters.config.rankingAndProgressViewsEnabled === 'true';
      },
    },
  };
</script>

<style scoped>
  .breadcrumb {
    /* 1*/
    background: linear-gradient(87deg, #264653, #2d8779);
    border-radius: 0px;
    margin: 0px;
    padding-left: 1.5rem;
  }
  .breadcrumb-item-label {
    font-size: 0.9rem;
  }

  .breadcrumb li {
    display: inline;
    max-width: 15rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: white;
  }
</style>
