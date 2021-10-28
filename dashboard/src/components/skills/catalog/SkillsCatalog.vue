/*
Copyright 2021 SkillTree

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
  <!-- need panels/layout, etc -->
  <div id="skills-catalog" class="mb-3">
    <sub-page-header title="Skill Catalog">
      <catalog-small-nav :nav-cards="navCards" />
    </sub-page-header>

    <!-- on FF charts end up pushing column to the next row; this is a workaround -->
    <div style="width: 99%;">
      <router-view></router-view>
      <catalog-nav-cards :nav-cards="navCards"/>
    </div>
<!--    <exported-skills :project-id="projectId" />

    <skills-imported-from-catalog :project-id="projectId" />-->
  </div>

</template>

<script>

  import { createNamespacedHelpers } from 'vuex';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import CatalogNavCards from '@/components/skills/catalog/CatalogNavCards';
  import CatalogSmallNav from '@/components/skills/catalog/CatalogSmallNav';
  /* import ExportedSkills from '@/components/skills/catalog/ExportedSkills';
  import SkillsImportedFromCatalog from '@/components/skills/catalog/SkillsImportedFromCatalog'; */

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'SkillsCatalog',
    components: {
      SubPageHeader,
      CatalogNavCards,
      CatalogSmallNav,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        navCards: [{
          title: 'Imported Skills',
          subtitle: 'View/Manage Imported Skills',
          description: 'View and manage Skills imported from other Projects',
          icon: 'far fa-arrow-alt-circle-down skills-color-imported',
          pathName: 'ImportedSkills',
        }, {
          title: 'Exported Skills',
          subtitle: 'View/Manage Exported Skills',
          description: 'View and manage Skills exported from this Project to other Projects',
          icon: 'far fa-arrow-alt-circle-up skills-color-exported',
          pathName: 'ExportedSkills',
        }],
      };
    },
    mounted() {
      // should this also load the stats for each card or should that only happen on specific load of that
      // content?
      this.loadProjectDetailsState({ projectId: this.$route.params.projectId });
    },
    watch: {
      '$route.params.projectId': function watcher() {
        this.projectId = this.$route.params.projectId;
      },
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
    },
  };
</script>

<style scoped>

</style>
