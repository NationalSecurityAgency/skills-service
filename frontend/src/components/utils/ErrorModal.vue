<template>
  <div id="errorModal" style="width: 450px;">
    <modal-header title="Unexpected Error Occurred" :is-error=true @cancel-clicked="$parent.close()"></modal-header>
    <section>
      <div class="notification">
            Oh no, something went wrong!  An error occurred and we are unable to process your request,
            please try again later.  If the error persists, please contact us and let us know.

            <p v-if="errorMessage" class="skills-pad-top-1-rem ">Details: <strong>{{ errorMessage }}</strong></p>
          </div>
    </section>
  </div>
</template>

<script>
  import ModalHeader from './modal/ModalHeader';

  export default {
    name: 'ErrorModal',
    components: { ModalHeader },
    props: ['errorMessage'],
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
