<template>
  <div class="container">
    <skills-spinner :loading="loading.userSkills || loading.pointsHistory || loading.userSkillsRanking"/>

    <div v-if="!loading.userSkills && !loading.pointsHistory && !loading.userSkillsRanking">
      <skills-title>User Skills</skills-title>

      <user-skills-header :display-data="displayData" @hook:updated="contentHeightUpdated" />
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

  import '@/common/filter/NumberFilter';
  import '@/common/filter/PluralFilter';

  import ProgressBar from 'vue-simple-progress';
  import VerticalProgressBar from '@/common/progress/VerticalProgress.vue';

  import SkillsTitle from '@/common/utilities/SkillsTitle.vue';
  import StarProgress from '@/common/progress/StarProgress.vue';

  import { debounce } from 'lodash';

  const debouncedContentHeightUpdated = debounce((context) => {
    context.$emit('height-change');
  }, 5);

  export default {
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
    props: {
      userId: String,
      token: String,
      projectId: {
        type: String,
        required: true,
      },
      serviceUrl: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        loading: {
          userSkills: true,
          pointsHistory: true,
          userSkillsRanking: true,
        },
        displayData: {
          userSkills: null,
          pointsHistory: null,
          userSkillsRanking: null,
        },
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
        this.getUserSkills();
      },
    },
    updated() {
      this.contentHeightUpdated();
    },
    mounted() {
      this.contentHeightNotifier = () => debouncedContentHeightUpdated(this);
      window.addEventListener('resize', this.contentHeightNotifier);
      UserSkillsService.setServiceUrl(this.serviceUrl);
      UserSkillsService.setProjectId(this.projectId);
      UserSkillsService.setUserId(this.userId);
      UserSkillsService.setToken(this.token);
      this.getCustomIconCss();
      this.getUserSkills();
      this.loadPointsHistory();
      this.loadUserSkillsRanking();
    },
    beforeDestroy() {
      window.removeEventListener('resize', this.contentHeightNotifier);
    },
    methods: {
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
      getUserSkills() {
        UserSkillsService.getUserSkills()
                .then((response) => {
                  this.displayData.userSkills = response;
                  this.loading.userSkills = false;
                });
      },
      loadUserSkillsRanking(subjectId) {
        UserSkillsService.getUserSkillsRanking(subjectId)
                .then((response) => {
                  this.displayData.userSkillsRanking = response;
                  this.loading.userSkillsRanking = false;
                });
      },

      loadPointsHistory(subjectId) {
        UserSkillsService.getPointsHistory(subjectId)
                .then((result) => {
                  this.displayData.pointsHistory = result;
                  this.loading.pointsHistory = false;
                });
      },
      contentHeightUpdated() {
        debouncedContentHeightUpdated(this);
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
