<template>
  <div
    class="user-skills-panel user-skills-container">
    <ribbon>
      User Skills
    </ribbon>

    <div
      v-if="!isLoaded"
      class="user-skills-spinner">
      <vue-simple-spinner
        size="large"
        message="Loading..."/>
    </div>

    <div v-if="isLoaded">
      <user-skills-header
        v-if="!error.message"
        :user-skills="userSkills" />
      <div class="user-skills-more-info text-muted">
        <strong>Need more information?</strong>
        <a
          href="#"
          target="_blank">
          Click here!
        </a>
      </div>
      <div
        v-if="error.message"
        class="user-skills-error-message user-skills-panel">
        <h1>{{ error.message }}</h1>
        <p>{{ error.details }}</p>
      </div>
    </div>
    <div
      v-if="isLoaded && !error.message">
      <badges-container :badges="userSkills.badges" />
      <subjects-container :subjects="userSkills.subjects" />
    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import UserSkillsHeader from '@/userSkills/UserSkillsHeader.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SubjectsContainer from '@/userSkills/subject/SubjectsContainer.vue';
  import BadgesContainer from '@/userSkills/badge/BadgesContainer.vue';

  import '@/common/filter/NumberFilter';

  import ProgressBar from 'vue-simple-progress';
  import VerticalProgressBar from '@/common/progress/VerticalProgress.vue';
  import Spinner from 'vue-simple-spinner';

  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import StarProgress from '@/common/progress/StarProgress.vue';

  import Popper from 'vue-popperjs';
  import 'vue-popperjs/dist/css/vue-popper.css';

  const getDocumentHeight = () => {
    const body = document.body;
    const html = document.documentElement;

    return Math.max( body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight );
  };

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
      BadgesContainer,
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
      };
    },
    watch: {
      userId() {
        UserSkillsService.setUserId(this.userId);
        this.getUserSkills();
      },
    },
    mounted() {
      UserSkillsService.setServiceUrl(this.serviceUrl);
      UserSkillsService.setProjectId(this.projectId);
      UserSkillsService.setUserId(this.userId);
      UserSkillsService.setToken(this.token);
      this.getCustomIconCss();
      this.getUserSkills();
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
          })
          .finally(() => {
            const payload = {
              contentHeight: getDocumentHeight(),
            };
            parent.postMessage(`skills::frame-loaded::${JSON.stringify(payload)}`, '*');
        });
      },
    },
  };
</script>

<style lang="scss">
  .modal-open {
    overflow: hidden;
  }

  .modal-backdrop {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 1040;
    background-color: #000;

    &.fade {
      filter: alpha(opacity=0);
      opacity: 0;
    }

    &.in {
      filter: alpha(opacity=50);
      opacity: .5;
    }
  }

  .user-skills-panel {
    background-color: #fff;
    border: 1px solid #ddd;
    border-radius: 4px;
    box-shadow: 0 1px 1px rgba(0, 0, 0, .05);
    margin-bottom: 17px;
  }

  .user-skills-container {
    width: 1000px;
    margin-top: 20px;
    text-align: center;
  }

  .user-skills-spinner {
    height: 50px;
    position: relative;
    padding: 50px;
  }

  .progress-circle-wrapper {
    position: relative;
  }

  .progress-circle {
    position: relative;
    width: 120px;
    margin: 10px 0;
    display: inline-block;
  }

  .skill-tile-label {
    font-size: 19px;
    color: #333;
    width: 100%;
  }

  .progress-circle-wrapper p {
    display: block;
    font-size: 17px;
    color: #333;
  }

  .skills-icon {
    display: inline-block;
    color: #b1b1b1;
    margin: 5px 0;
  }

  .pc-percent {
    font-size: 2em;
  }

  .circle-number {
    position: absolute;
    color: #565656;
    font-weight: lighter;
    line-height: 1;
    top: 50%;
    bottom: auto;
    left: 50%;
    transform: translateY(-50%) translateX(-50%);
    font-size: 22px;
    text-align: center;
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
