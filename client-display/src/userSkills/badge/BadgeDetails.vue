<template>
  <section
    class="badge-detail-container">
    <ribbon
      v-if="badge"
      :color="ribbonColor">
      {{ badge.badge }}
    </ribbon>
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
  </section>
</template>

<script>
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import SkillsProgressList from '@/userSkills/modal/SkillsProgressList.vue';
  import Popper from 'vue-popperjs';
  import marked from 'marked';

  import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

  import 'vue-popperjs/dist/css/vue-popper.css';

  export default {
    components: {
      Ribbon,
      SkillsProgressList,
      ToggleButton,
      Popper,
    },
    props: {
      badge: Object,
      ribbonColor: String,
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
  .badge-detail-container {
    max-width: 875px;
    margin: 0 auto;
  }

  .badge-body {
    background-color: #fcfcfc;
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
