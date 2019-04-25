<template>
  <div class="card">
    <div class="card-body row text-secondary text-center">
      <div class="col">
                    <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-heart-broken fa-stack-1x fa-inverse"></i>
                    </span>
      </div>
      <div class="w-100"></div>
      <div class="col pt-2">
        <h3 class="h3">{{ title }}</h3>
      </div>
      <div class="w-100"></div>
      <div class="col">
        {{ subTitle }}
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'NoContent3',
    props: {
      title: String,
      subTitle: String,
    },
  };
</script>

<style scoped>

</style>
