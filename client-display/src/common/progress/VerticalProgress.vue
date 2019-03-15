<template>
  <div class="user-skill-progress-layers">
    <progress-bar
      :val="totalProgress"
      :size="barSize"
      :bar-color="totalProgressBeforeToday ? totalProgressBarColor : beforeTodayBarColor"
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
        default: 18,
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
  }
</style>
