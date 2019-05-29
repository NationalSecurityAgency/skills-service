<template>
  <div class="card" style="height: 100%">
    <div class="card-body">
      <ribbon :color="ribbonColor" class="subject-tile-ribbon">
        {{ subject.subject }}
      </ribbon>

      <i :class="subject.iconClass" class="fa subject-tile-icon"/>
      <h2 class="skill-tile-label pt-1">Level {{ subject.skillsLevel }}</h2>
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
          <label class="skill-label text-center">All levels complete</label>
        </div>
        <div class="col-12">
          <progress-bar v-if="progress.allLevelsComplete" :val="progress.level"
                        :size="18" bar-color="#59ad52" class="complete-total"/>
          <vertical-progress-bar
                  :total-progress="progress.level"
                  :total-progress-before-today="progress.levelBeforeToday"/>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
  import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
  import Ribbon from '@/common/ribbon/Ribbon.vue';
  import StarProgress from '@/common/progress/StarProgress.vue';
  import VerticalProgressBar from '@/common/progress/VerticalProgress.vue';

  import ProgressBar from 'vue-simple-progress';

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
  .subject-tile-icon {
    font-size: 60px;
    height: 60px;
    width: 60px;
    color: #b1b1b1;
    background-repeat: no-repeat;
    background-size: 60px 60px;
  }

  .skill-tile-label {
    font-size: 1.3rem;
    color: #333;
    width: 100%;
  }
</style>
