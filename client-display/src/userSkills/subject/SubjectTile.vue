<template>
  <div class="skill-tile">
    <ribbon
      :color="ribbonColor"
      class="subject-tile-ribbon">
      {{ subject.subject }}
    </ribbon>
    <i
      :class="subject.iconClass"
      class="fa subject-tile-icon"/>
    <h2 class="skill-tile-label">Level {{ subject.skillsLevel }}</h2>
    <popper
      trigger="hover"
      :options="{ placement: 'top' }">
      <star-progress
        :number-complete="subject.skillsLevel"
        slot="reference" />
      <div class="popper">
        <div>Level {{ subject.skillsLevel }} out of 5</div>
      </div>
    </popper>
    <div class="skill-row">
      <div
        v-if="subject.levelTotalPoints >= 0"
        class="col-xs-3">
        <label class="skill-label text-left">Level</label>
      </div>
      <div
        v-if="subject.levelTotalPoints >= 0"
        class="col-xs-9">
        <label class="skill-label text-right">{{ subject.levelPoints | number }} / {{ subject.levelTotalPoints |
          number }}</label>
      </div>
      <div
        v-if="subject.levelTotalPoints < 0"
        class="col-xs-12">
        <label class="skill-label text-center">All levels complete</label>
      </div>
      <div class="col-xs-12">
        <progress-bar
          v-if="subject.levelTotalPoints === -1"
          :size="18"
          :val="progress.level"
          bar-color="#59ad52"
          class="complete-total"/>
        <popper
          v-if="subject.levelTotalPoints !== -1"
          trigger="hover"
          :options="{ placement: 'left' }">
          <div
            slot="reference">
            <vertical-progress-bar
              :total-progress="progress.level"
              :total-progress-before-today="progress.levelBeforeToday"/>
          </div>
          <div class="popper">
            <my-progress-summary
              :user-skills="subject"
              summary-type="level" />
          </div>
        </popper>
      </div>
    </div>
    <div class="skill-row">
      <div class="col-xs-3">
        <label class="skill-label text-left">Total</label>
      </div>
      <div class="col-xs-9">
        <label class="skill-label text-right">{{ subject.points | number }} / {{ subject.totalPoints | number
          }}</label>
      </div>
      <div class="col-xs-12">
        <popper
          trigger="hover"
          :options="{ placement: 'left' }">
          <div
            slot="reference">
            <vertical-progress-bar
              :total-progress="progress.total"
              :total-progress-before-today="progress.totalBeforeToday"/>
          </div>
          <div class="popper">
            <my-progress-summary
              :user-skills="subject"
              summary-type="subject" />
          </div>
        </popper>
      </div>
    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import StarProgress from '@/common/progress/StarProgress.vue';
  import VerticalProgressBar from '@/common/progress/VerticalProgress.vue';

  import Spinner from 'vue-simple-spinner';
  import Popper from 'vue-popperjs';
  import ProgressBar from 'vue-simple-progress';

  import 'vue-popperjs/dist/css/vue-popper.css';

  /* Hack for ribbon color. Ultimately backend will send ribbon color */
  let index = 0;

  export default {
    components: {
      Popper,
      Spinner,
      Ribbon,
      StarProgress,
      MyProgressSummary,
      VerticalProgressBar,
      ProgressBar,
    },
    props: {
      subject: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        ribbonColor: ['#4472ba', '#c74a41', '#59ad52', '#f7a35c', '#ba89b6', '#32b697'][index % 6],
      };
    },
    computed: {
      progress() {
        let levelBeforeToday = 0;
        const { subject } = this;
        if (subject.levelPoints > subject.todaysPoints) {
          levelBeforeToday = ((subject.levelPoints - subject.todaysPoints) / subject.levelTotalPoints) * 100;
        } else {
          levelBeforeToday = 0;
        }

        return {
          total: (subject.points / subject.totalPoints) * 100,
          totalBeforeToday: ((subject.points - subject.todaysPoints) / subject.totalPoints) * 100,
          level: subject.levelTotalPoints === -1 ? 100 : ((subject.levelPoints / subject.levelTotalPoints) * 100),
          levelBeforeToday,
        };
      },
    },
    created() {
      index += 1;
    },
  };
</script>

<style>
  .skill-tile  .non-semantic-protector.subject-tile-ribbon h1.category-ribbon {
    font-size: 18px;
    width: 65%;
  }
</style>

<style scoped>
  .skill-row {
    font-size: 12px;
    padding: 12px 20px;
    clear: both;
  }

  .subject-tile-icon {
    font-size: 60px;
    height: 60px;
    width: 60px;
    color: #b1b1b1;
    background-repeat: no-repeat;
    background-size: 60px 60px;
  }

  .skill-tile {
    text-align: center;
  }

  .skill-tile h2 {
    margin-top: 8.5px;
  }
</style>
