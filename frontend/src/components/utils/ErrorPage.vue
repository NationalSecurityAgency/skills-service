<template>
  <div class="mt-5">
    <div class="text-center text-secondary">
      <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-heart-broken fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-center text-secondary">
      <h4 class="">Tiny-bit of an error!</h4>
    </div>

    <div class="row justify-content-center text-danger mt-3">
      <div class="col col-sm-8 col-md-6 col-lg-4 text-center">
        <p>
          Oh no, something went wrong! An error occurred and we are unable to process your request, please try again
          later.
        </p>
      </div>
    </div>

    <div class="text-center">
      <b-button href="/" variant="outline-primary" class="p-2"><i class="fas fa-home mr-1"/>Take Me Home</b-button>
<!--      <b-button class="ml-2 p-2" variant="outline-info"><i class="fas fa-envelope mr-1"></i>Report This Issue</b-button>-->
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
      previousUrl() {
        return this.$store.state.previousUrl;
      },
    },
    mounted() {
      if (this.previousUrl) {
        // eslint-disable-next-line
        history.replaceState(history.state, '', this.previousUrl);
      }
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
