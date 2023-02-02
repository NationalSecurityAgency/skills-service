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
                <user-skills-header :display-data="displayDataHeader"/>
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
            <skills-progress-list @points-earned="refreshHeader" :subject="displayData.userSkills" @scrollTo="scrollToLastViewedSkill" />
        </div>
    </section>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import UserSkillsHeader from '@/userSkills/header/UserSkillsHeader';
  import SkillDisplayDataLoadingMixin from '@/userSkills/SkillDisplayDataLoadingMixin';
  import SkillsTitle from '@/common/utilities/SkillsTitle';
  import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import ScrollSkillIntoViewMixin from '@/userSkills/utils/ScrollSkillIntoViewMixin';

  export default {
    mixins: [SkillDisplayDataLoadingMixin, ScrollSkillIntoViewMixin],
    components: {
      MarkdownText,
      UserSkillsHeader,
      SkillsTitle,
      SkillsProgressList,
      SkillsSpinner,
    },
    beforeRouteEnter(to, from, next) {
      if (to.name === 'subjectDetails' && from.name === 'skillDetails') {
        next((vm) => {
          // eslint-disable-next-line no-param-reassign
          vm.jumpToLastViewed = true;
        });
      } else {
        next();
      }
    },
    watch: {
      $route: 'fetchData',
      displayData: {
        deep: true,
        handler() {
          this.displayDataHeader = this.displayData;
        },
      },
    },
    data() {
      return {
        displayDataHeader: this.displayData,
      };
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        this.resetLoading();
        this.loadSubject().then((res) => {
          let foundLastViewedSkill;
          res.skills.forEach((item) => {
            if (item.isLastViewed === true) {
              foundLastViewedSkill = item;
            } else if (item.type === 'SkillsGroup' && !foundLastViewedSkill) {
              foundLastViewedSkill = item.children.find((childItem) => childItem.isLastViewed === true);
            }
          });
          this.lastViewedSkillId = foundLastViewedSkill ? foundLastViewedSkill.skillId : null;
          this.autoScrollToLastViewedSkill();
        });
        this.loadUserSkillsRanking();
      },
      refreshHeader(event) {
        if (event.subjectId && event.skillId) {
          const newDisplayData = {};
          this.rawLoadUserSkillsRanking().then((resp) => {
            newDisplayData.userSkillsRanking = resp;
            this.rawLoadUserSubject(false).then((resp2) => {
              newDisplayData.userSkills = resp2;
              this.displayDataHeader = newDisplayData;
            });
          });
        }
      },
    },
  };
</script>

<style>

</style>
