<template>
  <div v-if="shouldDisplay" class="columns section">
    <div class="column"></div>
    <div class="column has-text-centered is-one-third container">
      <nav class="panel">
        <p class="panel-heading">
          <i class="fas fa-info-circle"/> {{ title }}
        </p>
        <p class="panel-block">
          <slot name="content"></slot>
        </p>
      </nav>
    </div>
    <div class="column"></div>
  </div>
</template>

<script>
  export default {
    name: 'NoContent',
    props: ['shouldDisplay', 'title', 'message'],
  };
</script>

<style lang="scss" scoped>
  @import "../../styles/palette";

  .container {
    min-width: 20rem;
  }

  .container .panel-heading {
    background-color: $green-palette-color4;
    color: whitesmoke;
  }
  .container .panel-block {
    background-color: $background-color2;
  }

</style>
