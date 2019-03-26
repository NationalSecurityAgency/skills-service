<template>
  <div>

    HELLO!?!?!?
    <!--<div-->
      <!--v-if="subject && initialized"-->
      <!--class="user-skill-subject-body">-->
      <!--<div class="user-skill-subject-overall">-->
        <!--<user-skills-header-->
          <!--:user-skills="subject" />-->
      <!--</div>-->

      <!--<div>-->
        <!--<p-->
          <!--v-if="showDescriptions && subject.description"-->
          <!--class="user-skill-subject-description"-->
          <!--v-html="parseMarkdown(subject.description)"/>-->
      <!--</div>-->

      <!--<skills-progress-list-->
        <!--:subject="subject"-->
        <!--:show-descriptions="showDescriptions" />-->
    <!--</div>-->
  </div>
</template>

<script>
  import UserSkillsHeader from '@/userSkills/UserSkillsHeader.vue';
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import SkillsProgressList from '@/userSkills/modal/SkillsProgressList.vue';

  import marked from 'marked';

  import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

  export default {
    components: {
      UserSkillsHeader,
      ToggleButton,
      Ribbon,
      SkillsProgressList,
    },
    props: {
      starArray: Array,
      ribbonColor: String,
      subject: Object,
    },
    data() {
      return {
        initialized: false,
        showDescriptions: false,
      };
    },
    mounted() {
      this.showDescriptions = false;
      this.initialized = true;
    },
  };
</script>

<style scoped>

</style>
