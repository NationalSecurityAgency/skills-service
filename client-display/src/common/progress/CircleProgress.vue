<template>
  <div class="progress-circle-wrapper">
    <label class="skill-tile-label">{{ title }}</label>
    <div class="progress-circle">
      <div class="text-muted circle-number">
        <span v-if="!isCompleted">
          <div>{{ totalCompletedPoints | number }}</div>
          <div style="font-size: 0.7rem;">out of</div>
          <div style="font-size: 0.9rem;">{{ totalPossiblePoints | number }}</div>
        </span>
        <i v-else class="fas fa-check text-success fa-2x"/>
      </div>
        <div
          slot="reference" >
          <radial-progress-bar
            :diameter="diameter"
            :start-color="beforeTodayProgressVal ? totalCompletedColor : completedBeforeTodayColor"
            :stop-color="beforeTodayProgressVal ? totalCompletedColor : completedBeforeTodayColor"
            :stroke-width="strokeWidth"
            :completed-steps="isCompleted ? 100 : totalCompletedPoints"
            :total-steps="isCompleted || totalPossiblePoints == 0 ? 100 : totalPossiblePoints"
            :inner-stroke-color="incompleteColor" />
          <radial-progress-bar
            v-if="!isCompleted && totalCompletedPoints !== totalPossiblePoints"
            :diameter="diameter"
            :start-color="completedBeforeTodayColor"
            :stop-color="completedBeforeTodayColor"
            :stroke-width="strokeWidth"
            :completed-steps="beforeTodayProgressVal"
            :total-steps="totalPossiblePoints > 0 ? totalPossiblePoints : 100"
            inner-stroke-color="transparent"
            class="complete-before-today"/>
        </div>
    </div>
    <slot name="footer" />
  </div>
</template>

<script>
  import RadialProgressBar from 'vue-radial-progress';

  export default {
    components: {
      RadialProgressBar,
    },
    props: {
      diameter: {
        type: Number,
        default: 160,
      },
      title: {
        type: String,
      },
      completedBeforeTodayColor: {
        type: String,
        default: '#14a3d2',
      },
      totalCompletedColor: {
        type: String,
        default: '#7ed6f3',
      },
      incompleteColor: {
        type: String,
        default: '#cdcdcd',
      },
      strokeWidth: {
        type: Number,
        default: 12,
      },
      pointsCompletedToday: {
        type: Number,
      },
      totalCompletedPoints: {
        type: Number,
      },
      totalPossiblePoints: {
        type: Number,
      },
    },
    computed: {
      isCompleted() {
        // If totalPossiblePoints is -1 it means this is charting Level progress and the user has completed this level
        return this.totalPossiblePoints === -1;
      },
      beforeTodayProgressVal() {
        const points = this.totalCompletedPoints;
        const todaysPoints = this.pointsCompletedToday;
        let returnVal;

        if (todaysPoints && points > todaysPoints) {
          returnVal = points - todaysPoints;
        } else if (todaysPoints && points < todaysPoints) {
          returnVal = 0;
        } else {
          returnVal = points;
        }
        return returnVal;
      },
    },
  };
</script>

<style scoped>
  .complete-before-today {
    position: absolute !important;
    width: 100%;
    top: 0;
    left: 0;
  }

  .circle-number {
    position: absolute;
    font-weight: lighter;
    line-height: 1;
    top: 50%;
    bottom: auto;
    left: 50%;
    transform: translateY(-50%) translateX(-50%);
    font-size: 22px;
    text-align: center;
  }

  .progress-circle-wrapper {
    position: relative;
  }

  .progress-circle {
    position: relative;
    margin: 10px 0;
    display: inline-block;
  }

  .skill-tile-label {
    font-size: 1.2rem;
    width: 100%;
  }

  .progress-circle-wrapper p {
    display: block;
    font-size: 17px;
    color: #333;
  }
</style>
