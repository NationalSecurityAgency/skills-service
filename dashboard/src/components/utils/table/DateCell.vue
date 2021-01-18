<template>
  <div>
    <div>
      <span>{{ value | date }}</span>
      <b-badge v-if="isToday(value)" variant="info" class="ml-2">Today</b-badge>
    </div>
    <div class="text-muted small">
      {{ value | timeFromNow }}
    </div>
  </div>
</template>

<script>
  import dayjs from '../../../DayJsCustomizer';

  export default {
    name: 'DateCell',
    props: ['value'],
    methods: {
      isToday(timestamp) {
        return dayjs(timestamp)
          .isSame(new Date(), 'day');
      },
    },
  };
</script>

<style scoped>

</style>
