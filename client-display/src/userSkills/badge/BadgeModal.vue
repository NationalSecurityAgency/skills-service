<template>
  <section>
    <modal
      class="badge-modal"
      @dismiss="handleClose">
      <div slot="header">
        <div style="text-align: center;">
          <span
            type="button"
            class="close"
            @click="handleClose">&times;</span>
        </div>
        <ribbon
          v-if="badge"
          :color="ribbonColor">
          {{ badge.badge }}
        </ribbon>
      </div>

      <div
        v-if="badge"
        class="badge-body">

        <div>
          <popper
            trigger="hover"
            :options="{ placement: 'top' }">
            <div
              slot="reference">
              <i
                class="badge-description-icon"
                :class="badge.iconClass" />
              <i
                v-if="badge.gem"
                class="fas fa-gem gem-indicator" />
            </div>
            <div
              v-if="badge.gem"
              class="popper">
              <div class="popover-title">
                <span class="progress-popup-label">
                  Gem Information
                </span>
              </div>
              <div class="popover-content-row">
                <span class="progress-popup-label">Expires:</span>
                <span class="progress-popup-value">
                  {{ gemExpirationDate }}
                </span>
              </div>
            </div>
          </popper>
          <span
            v-if="badge.description"
            class="user-skill-subject-description"
            v-html="parseMarkdown(badge.description)"/>
        </div>

        <div>
        </div>

        <skills-progress-list
          :subject="badge"
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
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import SkillsProgressList from '@/userSkills/modal/SkillsProgressList.vue';
  import Popper from 'vue-popperjs';
  import marked from 'marked';

  import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

  import 'vue-popperjs/dist/css/vue-popper.css';

  export default {
    components: {
      Modal,
      ModalHeader,
      ModalFooter,
      Ribbon,
      SkillsProgressList,
      ToggleButton,
      Popper,
    },
    props: {
      badge: Object,
      ribbonColor: String,
      show: false,
    },
    data() {
      return {
        initialized: false,
        showDescriptions: false,
      };
    },
    computed: {
      gemExpirationDate() {
        let dateString = '';
        if (this.badge.gem) {
          // Parse date manually. avoid large moment.js import for such a small thing..
          dateString = this.badge.endDate.replace(/T.*/, '');
        }
        return dateString;
      },

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

<style scoped>
  .badge-modal {
    width: 1100px;
    min-height: 350px;
    textl-align: center;
  }

  .badge-modal.modal-mask {
    width: 100%;
  }

  .badge-body {
    background-color: #fcfcfc;
    max-height: 645px;
    overflow: auto;
    min-height: 350px;
  }

  .badge-description-icon {
    color: gold;
    font-size: 80px;
    display: inline-block;
  }

  .description-toggle-container {
    display: inline-block;
    padding-left: 25px;
  }

  .user-skill-subject-description {
    text-align: center;
    font-style: italic;
    padding: 10px;
  }

  .user-skill-subject-description p {
    max-width: 375px;
  }

  .gem-indicator {
    color: #FF7070;
    font-size: 25px;
  }
</style>
