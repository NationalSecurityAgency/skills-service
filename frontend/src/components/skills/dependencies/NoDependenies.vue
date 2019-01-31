<template>
  <div class="section has-text-grey has-text-centered">

    <div class="columns is-centered">
      <div class="column is-narrow box icon-container">
        <i class="fa fa-vector-square icon-style"></i>
      </div>
    </div>
    <h1 class="title skills-pad-top-1-rem has-text-grey">No Dependencies Yet...</h1>
    <p>
      {{ message }}
    </p>
  </div>
</template>

<script>
  export default {
    name: 'NoDependenies',
    props: ['message'],
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
