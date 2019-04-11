<template>
  <div class="user-skills-container">
    <ribbon>
      User Skills
    </ribbon>

    <div v-if="!isLoaded" class="user-skills-spinner">
      <vue-simple-spinner
        size="large"
        message="Loading..."/>
    </div>

    <div v-if="isLoaded">
      <user-skills-header
        v-if="!error.message"
        :user-skills="userSkills"
        @hook:updated="contentHeightUpdated" />
      <div
        v-if="error.message"
        class="user-skills-error-message user-skills-panel">
        <h1>{{ error.message }}</h1>
        <p>{{ error.details }}</p>
      </div>
    </div>
    <div
      v-if="isLoaded && !error.message">
      <subjects-container :subjects="userSkills.subjects" />
    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import UserSkillsHeader from '@/userSkills/UserSkillsHeader.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SubjectsContainer from '@/userSkills/subject/SubjectsContainer.vue';

  import '@/common/filter/NumberFilter';

  import ProgressBar from 'vue-simple-progress';
  import VerticalProgressBar from '@/common/progress/VerticalProgress.vue';
  import Spinner from 'vue-simple-spinner';

  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import StarProgress from '@/common/progress/StarProgress.vue';

  import { debounce } from 'lodash';

  import Popper from 'vue-popperjs';
  import 'vue-popperjs/dist/css/vue-popper.css';

  const debouncedContentHeightUpdated = debounce((context) => {
    context.$emit('height-change');
  }, 5);

  export default {
    components: {
      MyProgressSummary,
      UserSkillsHeader,
      ProgressBar,
      VerticalProgressBar,
      Popper,
      Ribbon,
      StarProgress,
      SubjectsContainer,
      'vue-simple-spinner': Spinner,
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
        isLoaded: false,
        error: { message: null, details: null },
        userSkills: null,
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
            this.userSkills = response;
            this.error = { message: null, details: null };
            this.isLoaded = true;
          })
          .catch(() => {
            this.isLoaded = true;
            this.error = {
              message: 'Something Went Wrong',
              details: 'Unable to retrieve Skills.  Try again later.',
            };
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

  .user-skills-spinner {
    height: 50px;
    position: relative;
    padding: 50px;
  }

  .skill-tile-label {
    font-size: 19px;
    color: #333;
    width: 100%;
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

  .user-skills-error-message {
    display: inline-block;
    margin: 20px;
    padding: 20px;
    text-align: center;
    width: 600px;
  }

  .user-skills-error-message h1 {
    color: #438843;
    font-size: 22px;
    font-weight: bold;
  }

  .user-skills-error-message p {
    font-size: 15px;
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
