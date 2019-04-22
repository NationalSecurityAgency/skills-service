<template>
  <div class="user-skill-progress-layers" :class="{ 'cursor-pointer': isClickable, 'locked-background': isLocked }">
    <div class="locked-border"></div>
    <span v-if="isLocked" class="locked-icon">
      <i class="fas fa-lock"></i>
    </span>
    <progress-bar
      :val="totalProgress"
      :size="barSize"
      :bar-color="totalProgressBeforeToday ? totalProgressBarColor : beforeTodayBarColor"
      :bg-color="isLocked ? 'E6E6E6' : 'aeaeae'"
      class="complete-total"/>
    <progress-bar
      v-if="totalProgress !== 100"
      :val="totalProgressBeforeToday"
      :size="barSize"
      :bar-color="beforeTodayBarColor"
      bg-color="transparent"
      class="complete-before-today" />
  </div>
</template>

<script>
  import ProgressBar from 'vue-simple-progress';

  export default {
    components: {
      ProgressBar,
    },
    props: {
      isLocked: Boolean,
      isClickable: Boolean,
      totalProgress: Number,
      totalProgressBeforeToday: Number,
      beforeTodayBarColor: {
        type: String,
        default: '#337ab7',
      },
      totalProgressBarColor: {
        type: String,
        default: '#97c9f5',
      },
      barSize: {
        type: Number,
        default: 22,
      },
    },
  };
</script>

<style scoped>
  .complete-before-today {
    position: absolute !important;
    top: 0;
    left: 0;
    width: 100%;
  }

  .user-skill-progress-layers {
    position: relative;
    background-color: #e8e8e8;
  }

  .locked-background {
    background-color: #e8e8e8;
  }

  .locked-border {
    position: absolute;
    width: 100%;
    height: 100%;
    border: lightgrey solid 2px;
  }

  .locked-icon {
    position: absolute;
    /*top: -5px;*/
    left: 50%;
    color: #5d5d5d;
  }

  .cursor-pointer {
    cursor: pointer;
  }

</style>
