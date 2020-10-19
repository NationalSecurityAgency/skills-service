<template>
  <div class="card h-100">
    <div class="card-body">
      <div class="row">
        <div class="col">
          <h5 class="text-uppercase text-muted card-title small mb-1">{{ title }}</h5>
          <span class="h4 font-weight-bold mb-0">
            <span v-if="!calculateTimeFromNow">{{ statNum | number }}</span>
            <span v-else>{{ statNum | timeFromNow }}</span>
          </span>
        </div>
        <div class="col-auto">
          <i :class="icon" style="font-size: 2.2rem;"/>
        </div>
      </div>
      <p class="text-muted small mt-3 mb-0"><slot /></p>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'StatsCard',
    props: {
      title: {
        type: String,
        required: true,
      },
      statNum: {
        type: Number,
        required: true,
      },
      icon: {
        type: String,
        required: true,
      },
      calculateTimeFromNow: {
        type: Boolean,
        required: false,
        default: false,
      },
    },
  };
</script>

<style scoped>

</style>
