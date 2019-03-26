<template>
  <section>
    <modal
      class="user-skill-subject-window"
      @dismiss="handleClose">
      <div slot="header">
        <div style="text-align: center;">
          <span
            type="button"
            class="close"
            @click="handleClose">&times;</span>
        </div>
        <ribbon
          v-if="subject"
          :color="ribbonColor">
          {{ subject.subject }}
        </ribbon>
      </div>

      <div
        v-if="subject && initialized"
        class="user-skill-subject-body">
        <div class="user-skill-subject-overall">
          <user-skills-header
            :user-skills="subject" />
        </div>

        <div>
          <p
            v-if="showDescriptions && subject.description"
            class="user-skill-subject-description"
            v-html="parseMarkdown(subject.description)"/>
        </div>

        <skills-progress-list
          :subject="subject"
          :show-descriptions="showDescriptions" />
      </div>

      <div
        slot="footer">
        <div class="pull-left">
          <span>
            Need help?
            <a
              :href="helpTipHref" style="padding-right: 10px"
              target="_blank">Click here!</a>
          </span>
          <div class="description-toggle-container">
            <span>
            <span class="text-muted">User skills descriptions:&nbsp;</span>
            </span>
            <toggle-button
              v-model="showDescriptions"
              :labels="{ checked: 'On', unchecked: 'Off' }"
              @change="toggleDescriptions">
            </toggle-button>
          </div>
        </div>

        <button
          class="btn btn-primary pull-right"
          type="button"
          @click="handleClose">
        OK
        </button>
      </div>
    </modal>
  </section>
</template>

<script>
  import Modal from '@/common/modal/Modal.vue';
  import ModalHeader from '@/common/modal/ModalHeader.vue';
  import ModalFooter from '@/common/modal/ModalFooter.vue';
  import UserSkillsHeader from '@/userSkills/UserSkillsHeader.vue';
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import SkillsProgressList from '@/userSkills/modal/SkillsProgressList.vue';

  import marked from 'marked';

  import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

  export default {
    components: {
      Modal,
      ModalHeader,
      ModalFooter,
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
    computed: {
      helpTipHref() {
        return '';
      },
    },
    methods: {
      parseMarkdown(text) {
        return marked(text);
      },

      toggleDescriptions(event) {
        this.showDescriptions = event.value;
      },

      handleClose() {
        this.$emit('ok');
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
    padding-left: 25px;
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

  .user-skill-subject-body {
    background-color: #fcfcfc;
    max-height: 645px;
    overflow: auto;
  }

  .user-skill-subject-overall {
    background-color: #ffffff;
    border-bottom: 1px solid #e5e5e5;
  }

  .user-skill-subject-description {
    text-align: left;
    font-style: italic;
    padding: 10px;
  }
</style>
