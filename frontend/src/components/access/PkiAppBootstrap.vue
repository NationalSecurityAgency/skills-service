<template>
  <div>
    <div class="row justify-content-center text-center">
      <div class="col col-md-8 col-lg-7 col-xl-4 mt-3" style="min-width: 20rem;">
        <div class="mt-5">
          <i class="fa fa-users fa-4x text-secondary"></i>
          <h2 class="mt-4 text-info">SkillTree Dashboard</h2>
        </div>

        <div class="card text-center">
          <div class="card-body p-4">
            <div v-if="isLoading" class="justify-content-center mt-1">
              <b-spinner label="Loading..." style="width: 2rem; height: 2rem;" variant="secondary"/>
              <h5 class="mt-2 text-primary">Getting Things Ready!</h5>
              <h6 class="text-secondary">This may take just a second...</h6>
            </div>
            <div v-else>
              <p><i class="far fa-check-square text-success"></i> The root account has been successfully
              created!</p>
              <p><i class="far fa-check-square text-success"></i> Inception self-training project
                created!
              </p>
              <p style="margin-top:2em">Please proceed the User Skills Dashboard.</p>
              <a class="btn btn-outline-primary" href="/">Let's Get Started!</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import BootstrapService from './BootstrapService';

  export default {
    name: 'PkiAppBootstrap',
    data() {
      return {
        isLoading: true,
      };
    },
    mounted() {
      BootstrapService.grantRoot()
        .then(() => {
          this.isLoading = false;
        });
    },
  };
</script>

<style scoped>

</style>
