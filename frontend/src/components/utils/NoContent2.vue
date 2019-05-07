<template>
  <div class="text-secondary text-center">
    <div class="row">
      <div class="col">
        <span class="fa-stack fa-3x " style="vertical-align: top;">
          <i class="fas fa-circle fa-stack-2x"></i>
          <i class="fa-stack-1x fa-inverse" :class="icon"></i>
        </span>
      </div>
      <div class="w-100"></div>
      <div class="col pt-2">
        <h3 class="h3">{{ title }}</h3>
      </div>
    </div>
    <div class="row justify-content-center ">
      <div class="col col-md-8 col-lg-7 col-xl-5">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'NoContent2',
    props: ['title', 'message', 'icon'],
  };
</script>

<style scoped>
  .icon-style {
    font-size: 4rem;
    color: #969696;
  }

  .icon-container {
    background: #ededed;
  }
</style>
