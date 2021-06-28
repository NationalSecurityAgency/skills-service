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
    <div class="text-primary">
        <div v-if="!loading.dependencies && !loading.skill">
            <skills-title>Skill Overview</skills-title>
            <div class="card">
              <div class="card-body text-center text-sm-left">
                <skill-progress2 :skill="skill" @points-earned="onPointsEarned"/>
              </div>
            </div>
            <skill-dependencies class="mt-2" v-if="dependencies && dependencies.length > 0" :dependencies="dependencies"
                                :skill-id="$route.params.skillId" :subject-id="this.$route.params.subjectId"></skill-dependencies>
        </div>
        <div v-else>
            <skills-spinner :loading="loading.dependencies || loading.skill" class="mt-5"/>
        </div>
    </div>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import SkillsTitle from '@/common/utilities/SkillsTitle';
  import SkillProgress2 from '@/userSkills/skill/progress/SkillProgress2';
  import SkillEnricherUtil from '../utils/SkillEnricherUtil';

  export default {
    name: 'SkillDetails',
    components: {
      SkillsTitle,
      'skill-dependencies': () => import(/* webpackChunkName: 'skillDependencies' */'@/userSkills/skill/dependencies/SkillDependencies'),
      SkillsSpinner,
      SkillProgress2,
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
          const skillId = this.isDependency() ? this.$route.params.dependentSkillId : this.$route.params.skillId;
          UserSkillsService.getSkillDependencies(skillId)
            .then((res) => {
              this.dependencies = res.dependencies;
              this.loading.dependencies = false;
            });
        } else {
          this.loading.dependencies = false;
        }
      },
      loadSkillSummary() {
        const skillId = this.isDependency() ? this.$route.params.dependentSkillId : this.$route.params.skillId;
        UserSkillsService.getSkillSummary(skillId, this.$route.params.crossProjectId)
          .then((res) => {
            this.skill = res;
            this.loading.skill = false;
          });
      },
      onPointsEarned(pts) {
        this.skill = SkillEnricherUtil.addPts(this.skill, pts);
      },
      isDependency() {
        const routeName = this.$route.name;
        return routeName === 'dependentSkillDetails' || routeName === 'crossProjectSkillDetails';
      },
    },
  };
</script>

<style scoped>

</style>
