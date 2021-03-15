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
    <section>
        <skills-spinner :loading="loading.userSkills"></skills-spinner>

        <div v-if="!loading.userSkills"
             class="user-skill-subject-body">
            <skills-title>{{ displayData.userSkills.subject }}</skills-title>

            <div class="user-skill-subject-overall">
                <user-skills-header :display-data="displayData"/>
            </div>

            <div v-if="displayData.userSkills.description" class="card mt-2">
              <div class="card-header">
                <h3 class="h6 card-title mb-0 float-left">Description</h3>
              </div>
              <div class="card-body">
                <markdown-text :text="displayData.userSkills.description" class="d-block text-left"/>
              </div>
              <div v-if="displayData.userSkills.helpUrl" class="card-footer text-left">
                    <a :href="displayData.userSkills.helpUrl" target="_blank" rel="noopener"
                       class="btn btn-sm btn-outline-info skills-theme-btn">
                      Learn More <i class="fas fa-external-link-alt"></i>
                    </a>
              </div>
            </div>

            <skills-progress-list :subject="displayData.userSkills"/>
        </div>
    </section>
</template>

<script>
  import UserSkillsHeader from '@/userSkills/UserSkillsHeader';
  import SkillDisplayDataLoadingMixin from '@/userSkills/SkillDisplayDataLoadingMixin';
  import SkillsTitle from '@/common/utilities/SkillsTitle';
  import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import MarkdownText from '@/common/utilities/MarkdownText';

  export default {
    mixins: [SkillDisplayDataLoadingMixin],
    components: {
      MarkdownText,
      UserSkillsHeader,
      SkillsTitle,
      SkillsProgressList,
      SkillsSpinner,
    },
    watch: {
      $route: 'fetchData',
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        this.resetLoading();
        this.loadSubject();
        this.loadUserSkillsRanking();
      },
    },
  };
</script>

<style>

</style>
