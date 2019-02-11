<template>
  <div class="section has-text-grey has-text-centered">

    <div class="columns is-centered">
      <div class="column is-narrow box icon-container">
        <i class="icon-style" :class="icon"></i>
      </div>
    </div>
    <h1 class="title skills-pad-top-1-rem has-text-grey">{{ title }}</h1>
    <p>
      {{ message }}
    </p>
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
