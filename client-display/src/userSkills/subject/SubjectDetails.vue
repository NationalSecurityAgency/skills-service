<template>
  <section class="container">
    <skills-spinner v-if="loading" :loading="loading" class="mt-5"></skills-spinner>

    <div v-if="!loading" class="user-skill-subject-body">
      <skills-title>{{ subject.subject }}</skills-title>

      <div class="user-skill-subject-overall">
        <user-skills-header :user-skills="subject" />
      </div>

      <div>
        <small
            v-if="showDescriptions && subject.description"
            class="font-italic d-block pl-3 pt-4 text-left"
            v-html="parseMarkdown(subject.description)"/>
      </div>

      <skills-progress-list
        :subject="subject"
        :show-descriptions="showDescriptions" />
    </div>
  </section>
</template>

<script>
  import UserSkillsHeader from '@/userSkills/UserSkillsHeader.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillsTitle from '@/common/utilities/SkillsTitle.vue';
  import SkillsProgressList from '@/userSkills/skill/progress/SkillsProgressList.vue';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';

  import marked from 'marked';

  import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

  export default {
    components: {
      UserSkillsHeader,
      ToggleButton,
      SkillsTitle,
      SkillsProgressList,
      SkillsSpinner,
    },
    props: {
      ribbonColor: String,
    },
    data() {
      return {
        loading: true,
        showDescriptions: false,
        subject: null,
      };
    },
    watch: {
      $route: 'fetchData',
    },
    computed: {
      helpTipHref() {
        return '';
      },
    },
    mounted() {
      this.showDescriptions = false;
      this.fetchData();
    },
    methods: {
      fetchData() {
        UserSkillsService.getSubjectSummary(this.$route.params.subjectId)
          .then((result) => {
            this.subject = result;
            this.loading = false;
          });
      },
      parseMarkdown(text) {
        return marked(text);
      },

      toggleDescriptions(event) {
        this.showDescriptions = event.value;
      },
    },
  };
</script>

<style>
  .skill-row {
    font-size: 12px;
    padding: 12px 20px;
    clear: both;
  }

  .user-skill-subject-window .modal-dialog {
    width: 100%;
  }

  .description-toggle-container {
    display: inline-block;
    padding: 25px 10px 10px 25px;
  }

  .user-skill-subject-body ul {
    margin-left: 15px;
    list-style-position: inside;
    color: #438843;
    margin-bottom: 0;
  }

  .user-skill-subject-body ul li {
    margin-left: 15px;
    padding: 3px 15px 0 0;
    position: relative;
  }
  .user-skill-subject-overall {
    border-bottom: 1px solid #e5e5e5;
  }

  .user-skill-subject-description {
    text-align: left;
    font-style: italic;
  }
</style>
