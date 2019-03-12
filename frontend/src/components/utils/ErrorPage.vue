<template>
  <div class="skills-bordered-component2" style="padding-top: 100px;">
  <div class="columns">
    <header class="error-header column is-half is-offset-one-quarter">
      <p class="error-title">Unexpected Error Occurred</p>
    </header>
  </div>

  <div class="columns">
    <div class="error-body column is-half is-offset-one-quarter">
      <div>
        Oh no, something went wrong!  An error occurred and we are unable to process your request,
        please try again later.  If the error persists, please contact us and let us know.

        <p v-if="errorMessage" class="skills-pad-top-1-rem ">Details: <strong>{{ errorMessage }}</strong></p>
      </div>
    </div>
  </div>
  </div>
</template>

<script>

  export default {
    name: 'ErrorPage',
    computed: {
      errorMessage() {
        return this.$route.query.errorMessage;
      },
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped>
  @import "../../styles/palette";

  .error-header {
    background-color: $red-palette-color3;
    border-top-left-radius: 7px;
    border-top-right-radius: 7px;
    padding: 1rem;
  }
  .error-title {
    color: whitesmoke;
    font-size: 1.5rem;
  }
  .error-body {
    border: 1px solid #ddd;
    border-bottom-left-radius: 7px;
    border-bottom-right-radius: 7px;
    padding: 1rem;
  }

</style>
