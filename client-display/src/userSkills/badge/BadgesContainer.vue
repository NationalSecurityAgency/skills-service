<template>
  <div>
    <badge-modal
      v-if="badgeModal.show"
      :badge="badgeModal.badge"
      :ribbon-color="'gold'"
      @ok="badgeModal.show = false"
      @cancel="badgeModal.show = false"/>
    <div
      v-if="badges.length !== 0"
      class="badges-container">
      <ribbon
        :color="'#c74a41'"
        class="subject-tile-ribbon">
        Badges
      </ribbon>
      <div class="positioner">
        <span
          class="previous-button"
          @click="goPrevious"><i class="fas fa-chevron-left" /></span>
        <div style="width: 85%; margin: auto; display: inline-block">
          <tiny-slider
            ref="tinyslider"
            :controls="false"
            :items="5">
            <div
              v-for="(badge, index) in badges"
              :key="`unique-badge-${index}`"
              class="btn user-skills-panel"
              @click="openBadgeStats(badge, index)" >
              <badge-tile :badge="badge" />
            </div>
          </tiny-slider>
        </div>
        <span
          class="next-button"
          @click="goNext"><i class="fas fa-chevron-right" /></span>
      </div>
    </div>
  </div>
</template>

<script>
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import BadgeTile from '@/userSkills/badge/BadgeTile.vue';
  import BadgeModal from '@/userSkills/badge/BadgeModal.vue';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  import VueTinySlider from 'vue-tiny-slider';
  import 'tiny-slider/dist/tiny-slider.css';

  export default {
    components: {
      BadgeTile,
      Ribbon,
      BadgeModal,
      'tiny-slider': VueTinySlider,
    },
    props: {
      badges: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        badgeModal: {
          show: false,
          badge: null,
        },
      };
    },
    methods: {
      goNext() {
        const { slider } = this.$refs.tinyslider;
        slider.goTo('next');
      },
      goPrevious() {
        const { slider } = this.$refs.tinyslider;
        slider.goTo('prev');
      },
      openBadgeStats(badge) {
        UserSkillsService.getBadgeSkills(badge.badgeId)
          .then((badgeSummary) => {
            this.badgeModal = {
              badge: badgeSummary,
              show: true,
            };
          });
      },
    },
  };
</script>

<style>
  .badges-container  .non-semantic-protector.subject-tile-ribbon h1.category-ribbon {
    font-size: 18px;
    width: 25%;
  }
</style>

<style scoped>
  .badges-container {
    text-align: center;
  }

  /* I couldnt for the life of me get flex box to work with tiny-slider. Its dirty but positioning manually */
  .positioner {
    position: relative;
  }

  .previous-button,.next-button {
    font-size: 25px;
    cursor: pointer;
    color: #b1b1b1;
  }

  .previous-button:hover, .next-button:hover {
    color: green;
  }

  .previous-button {
    position: absolute;
    top: 55px;
    left: 55px;
  }

  .next-button {
    position: absolute;
    top: 55px;
    right: 55px;
  }
</style>
