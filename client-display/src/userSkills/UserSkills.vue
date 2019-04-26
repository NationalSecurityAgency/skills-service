<template>
  <div class="container">
    <skills-spinner :loading="!isLoaded"/>

    <div v-if="isLoaded">
      <skills-title>User Skills</skills-title>

      <user-skills-header :display-data="displayData" />
      <subjects-container :subjects="displayData.userSkills.subjects" />
    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import UserSkillsHeader from '@/userSkills/UserSkillsHeader.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SubjectsContainer from '@/userSkills/subject/SubjectsContainer.vue';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';
  import SkillDisplayDataLoadingMixin from '@/userSkills/SkillDisplayDataLoadingMixin.vue';

  import '@/common/filter/NumberFilter';
  import '@/common/filter/PluralFilter';

  import ProgressBar from 'vue-simple-progress';
  import VerticalProgressBar from '@/common/progress/VerticalProgress.vue';

  import SkillsTitle from '@/common/utilities/SkillsTitle.vue';
  import StarProgress from '@/common/progress/StarProgress.vue';

  export default {
    mixins: [SkillDisplayDataLoadingMixin],

    components: {
      MyProgressSummary,
      UserSkillsHeader,
      ProgressBar,
      VerticalProgressBar,
      SkillsTitle,
      StarProgress,
      SubjectsContainer,
      SkillsSpinner,
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
        this.fetchData();
      },
    },
    mounted() {
      this.getCustomIconCss();
      this.fetchData();
    },
    methods: {
      fetchData() {
        this.resetLoading();
        this.loadUserSkills();
        this.loadPointsHistory();
        this.loadUserSkillsRanking();
      },
      getCustomIconCss() {
        UserSkillsService.getCustomIconCss()
          .then((css) => {
            if (css) {
              const head = document.getElementsByTagName('head')[0];

              const customIconStyles = document.createElement('style');
              customIconStyles.id = 'skill-custom-icons';
              customIconStyles.type = 'text/css';
              customIconStyles.innerText = css;
              head.appendChild(customIconStyles);
            }
          });
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
