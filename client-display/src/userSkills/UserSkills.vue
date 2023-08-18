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
  <div>
    <skills-spinner :loading="loading.userSkills"/>

    <div v-if="!loading.userSkills">
      <skills-title :back-button="false" :animate-power-by-label="true">{{ pageTitle }}</skills-title>
      <project-description v-if="!isSummaryOnly && description && displayProjectDescription" :description="description"></project-description>
      <user-skills-header :display-data="displayData" class="mb-3"/>
      <subjects-container v-if="!isSummaryOnly" :subjects="displayData.userSkills.subjects" />
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex';
  import QuizRunService from '@/common-components/quiz/QuizRunService';
  import UserSkillsHeader from '@/userSkills/header/UserSkillsHeader';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SubjectsContainer from '@/userSkills/subject/SubjectsContainer';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import SkillDisplayDataLoadingMixin from '@/userSkills/SkillDisplayDataLoadingMixin';
  import ProjectDescription from '@/userSkills/ProjectDescription';

  import '@/common/filter/NumberFilter';
  import '@/common/filter/PluralFilter';

  import SkillsTitle from '@/common/utilities/SkillsTitle';

  export default {
    mixins: [SkillDisplayDataLoadingMixin],

    components: {
      UserSkillsHeader,
      SkillsTitle,
      SubjectsContainer,
      SkillsSpinner,
      ProjectDescription,
    },
    data() {
      return {
        subjectIcon: 'fa-trophy',
        userSkillsSubjectModalSubject: null,
        userSkillsSubjectModalSubjectIcon: null,
        showUserSkillsSubjectModal: false,
        contentHeightNotifier: null,
      };
    },
    watch: {
      userId() {
        UserSkillsService.setUserId(this.userId);
        QuizRunService.setUserId(this.userId);
        this.fetchData();
      },
      version() {
        this.fetchData();
      },
    },
    computed: {
      ...mapGetters([
        'displayProjectDescription',
      ]),
      version() {
        return this.$store.state.version;
      },
      isSummaryOnly() {
        return this.$store.state.isSummaryOnly;
      },
      pageTitle() {
        if (this.$store.state.themeModule.landingPageTitle) {
          return this.$store.state.themeModule.landingPageTitle;
        }
        return 'User Skills';
      },
      description() {
        return this.displayData?.userSkills?.projectDescription;
      },
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        this.resetLoading();
        this.loadUserSkills();
        // this.loadPointsHistory();
        this.loadUserSkillsRanking();
      },
    },
  };
</script>

<style lang="scss">
  .user-skills-panel {
    background-color: #fff;
    border: 1px solid #ddd;
    border-radius: 4px;
    box-shadow: 0 1px 1px rgba(0, 0, 0, .05);
  }

  .skills-icon {
    display: inline-block;
    color: #b1b1b1;
    margin: 5px 0;
  }

  .user-skills-more-info {
    font-size: 13px;
    padding: 5px 0;
  }

  .skill-row {
    padding: 8px 20px;
  }

  .skill-label {
    margin-bottom: 0;
    width: 100%;
  }

  .item-complete-icon {
    color: #59ad52;
  }
</style>
