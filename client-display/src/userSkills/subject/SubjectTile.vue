<template>
  <div class="card subject-tile skills-navigable-item" style="height: 100%" data-cy="subjectTile">
    <div class="card-body text-primary">
      <ribbon :color="ribbonColor" class="subject-tile-ribbon">
        {{ subject.subject }}
      </ribbon>

      <i :class="subject.iconClass" class="d-inline-block subject-tile-icon"/>
      <h2 class="skill-tile-label text-primary pt-1">Level {{ subject.skillsLevel }}</h2>
      <star-progress :number-complete="subject.skillsLevel" class="py-1"/>

      <div class="row">
        <div class="col-3">
          <label class="skill-label text-left" style="min-width: 5rem;">Overall</label>
        </div>
        <div class="col-9">
          <label class="skill-label text-right">
            {{ subject.points | number }} / {{ subject.totalPoints | number }}
          </label>
        </div>
        <div class="col-12">
          <vertical-progress-bar
            :before-today-bar-color="beforeTodayColor"
            :total-progress-bar-color="earnedTodayColor"
            :total-progress="progress.total"
            :total-progress-before-today="progress.totalBeforeToday"/>
        </div>
      </div>

      <div class="row mt-3">
        <div v-if="!progress.allLevelsComplete" class="col-5">
          <label class="skill-label text-left" style="min-width: 10rem;">Next Level</label>
        </div>
        <div v-if="!progress.allLevelsComplete" class="col-7">
          <label class="skill-label text-right">
            {{ subject.levelPoints | number }} / {{ subject.levelTotalPoints | number }}
          </label>
        </div>
        <div v-if="progress.allLevelsComplete" class="col-12">
          <label class="skill-label text-center text-uppercase"><i class="fas fa-check text-success"/> All levels complete</label>
        </div>
        <div class="col-12">
          <progress-bar
            v-if="progress.allLevelsComplete"
            :val="progress.level"
            :size="18"
            :bar-color="completeColor"
            class="progress-border"/>
          <vertical-progress-bar v-else
            :before-today-bar-color="beforeTodayColor"
            :total-progress-bar-color="earnedTodayColor"
            :total-progress="progress.level"
            :total-progress-before-today="progress.levelBeforeToday"/>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
  import ProgressBar from 'vue-simple-progress';

  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import StarProgress from '@/common/progress/StarProgress.vue';
  import VerticalProgressBar from '@/common/progress/VerticalProgress.vue';

  /* Hack for ribbon color. Ultimately backend will send ribbon color */
  let index = 0;

  export default {
    components: {
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
      beforeTodayColor() {
        return this.$store.state.themeModule.progressIndicators.beforeTodayColor;
      },
      earnedTodayColor() {
        return this.$store.state.themeModule.progressIndicators.earnedTodayColor;
      },
      completeColor() {
        return this.$store.state.themeModule.progressIndicators.completeColor;
      },
      incompleteColor() {
        return this.$store.state.themeModule.progressIndicators.incompleteColor;
      },
      progress() {
        let levelBeforeToday = 0;
        const { subject } = this;
        if (subject.levelPoints > subject.todaysPoints) {
          levelBeforeToday = ((subject.levelPoints - subject.todaysPoints) / subject.levelTotalPoints) * 100;
        } else {
          levelBeforeToday = 0;
        }

        let level = 0;
        if (subject.totalPoints > 0) {
          if (subject.levelTotalPoints === -1) {
            level = 100;
          } else {
            level = (subject.levelPoints / subject.levelTotalPoints) * 100;
          }
        }

        return {
          total: subject.totalPoints > 0 ? (subject.points / subject.totalPoints) * 100 : 0,
          totalBeforeToday: subject.totalPoints > 0 ? ((subject.points - subject.todaysPoints) / subject.totalPoints) * 100 : 0,
          level,
          levelBeforeToday,
          allLevelsComplete: subject.totalPoints > 0 && subject.levelTotalPoints < 0,
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
  .subject-tile .subject-tile-icon {
    font-size: 60px;
    height: 60px;
    width: 60px;
    color: #b1b1b1;
    background-repeat: no-repeat;
    background-size: 60px 60px;
  }

  .subject-tile .skill-tile-label {
    font-size: 1.3rem;
    width: 100%;
  }

  .subject-tile .progress-border {
    border: lightgrey solid 2px;
  }
</style>
