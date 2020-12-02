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
    <div class="container text-primary">
        <div v-if="!loading.dependencies && !loading.skill">
            <skills-title>Skill Overview</skills-title>
            <skill-overview class="my-2" :skill="skill"></skill-overview>
            <skill-dependencies v-if="dependencies && dependencies.length > 0" :dependencies="dependencies"
                                :skill-id="$route.params.skillId"></skill-dependencies>
        </div>
        <div v-else>
            <skills-spinner :loading="loading.dependencies || loading.skill" class="mt-5"/>
        </div>
    </div>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillOverview from '@/userSkills/skill/SkillOverview';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import SkillsTitle from '@/common/utilities/SkillsTitle';

  export default {
    name: 'SkillDetails',
    components: {
      SkillsTitle,
      SkillOverview,
      'skill-dependencies': () => import(/* webpackChunkName: 'skillDependencies' */'@/userSkills/skill/dependencies/SkillDependencies'),
      SkillsSpinner,
    },
    data() {
      return {
        loading: {
          dependencies: true,
          skill: true,
        },
        dependencies: [],
        skill: {},
      };
    },
    mounted() {
      this.loadData();
    },
    watch: {
      $route: 'loadData',
    },
    methods: {
      loadData() {
        this.loading.dependencies = true;
        this.loading.skill = true;
        this.dependencies = [];
        this.skill = {};
        this.loadDependencies();
        this.loadSkillSummary();
      },
      loadDependencies() {
        if (!this.$route.params.crossProjectId) {
          UserSkillsService.getSkillDependencies(this.$route.params.skillId)
            .then((res) => {
              this.dependencies = res.dependencies;
              this.loading.dependencies = false;
            });
        } else {
          this.loading.dependencies = false;
        }
      },
      loadSkillSummary() {
        // const projectId = this.$route.params.crossProjectId ? this.$route.params.crossProjectId : this.$route.params.projectId;
        // console.log(`loading skill summary using projectId [${projectId}`);
        // console.log(this.$route.params);
        // UserSkillsService.getSkillSummary(this.$route.params.skillId, projectId)
        UserSkillsService.getSkillSummary(this.$route.params.skillId, this.$route.params.crossProjectId)
          .then((res) => {
            this.skill = res;
            this.loading.skill = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
