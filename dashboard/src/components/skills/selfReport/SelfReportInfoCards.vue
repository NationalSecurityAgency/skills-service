<template>
  <div class="row">
    <div class="col" v-for="card in cards" :key="card.label">
      <div class="card h-100" >
        <div class="card-body">
          <div class="row">
            <div class="col text-left">
              <div class="h5 card-title text-uppercase text-muted mb-0 small">{{card.label}}</div>
              <span class="h5 font-weight-bold mb-0">{{ card.count | number}}</span>
            </div>
            <div class="col-auto text-secondary">
              <i :class="card.icon" style="font-size: 2.2rem;"></i>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'SelfReportInfoCards',
    data() {
      return {
        cards: [{
          label: 'Disabled',
          count: 10,
          icon: 'far fa-times-circle',
        }, {
          label: 'Approval Required',
          count: 5,
          icon: 'far fa-thumbs-up',
        }, {
          label: 'Honor System',
          count: 0,
          icon: 'far fa-meh-rolling-eyes',
        }],
      };
    },
  };
</script>

<style scoped>

</style>
