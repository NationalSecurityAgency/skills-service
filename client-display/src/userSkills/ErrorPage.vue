<template>
  <div class="error-container" style="padding-top: 100px;">
    <div class="">
      <header class="error-header">
        <p class="error-title">Unexpected Error Occurred</p>
      </header>
    </div>

    <div class="">
      <div class="error-body">
        <i class="fa fa-sad-tear error-icon"></i>
        <div style="padding-top: 2rem;">
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
    props: {
      errorMessage: null,
    },
  };
</script>

<style lang="scss" scoped>
  .error-container {
    margin: 0 auto;
    width: 45rem;
  }

  .skills-pad-top-1-rem {
    padding-top: 1rem;
  }

  .error-header {
    background-color: #161616;
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

  .error-icon {
    font-size: 5rem;
  };
</style>
