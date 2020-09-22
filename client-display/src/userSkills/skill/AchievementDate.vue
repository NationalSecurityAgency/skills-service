<template>
  <div class="row" data-cy="achievement-date">
    <div class="col text-center text-md-left">
      <div class="m-0 p-0">
        <div class="text-white text-center badge badge-info" style="font-size: 1.7rem;"><i class="fas fa-medal"></i></div>
        <div class="d-inline-block ml-2 text-left">
          <div data-cy="achievement-on">Achieved on <span class="text-dark">{{ date | moment("MMMM Do YYYY") }}</span></div>
          <div class="text-black-50 small" data-cy="time-passed">{{ date | moment("from", "now")  }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'AchievementDate',
    props: ['date'],
  };
</script>

<style scoped>

</style>
