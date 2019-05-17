<template>
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 col-lg-6 mt-5">
        <div class="card">
          <div class="card-header">
            <strong>
              <i class="fas fa-award mr-3"/>User Skills Management Dashboard
            </strong>
          </div>
          <div class="card-body">
            <p>The root account has been successfully created!</p>
            <p style="margin-top:2em">Click Continue to proceed the User Skills Management Dashboard.</p>
            <div class="field">
                <button id="continue-button" class="btn btn-outline-primary" @click="$emit('proceed')">
                    <i class="fas fa-arrow-circle-right"/> Continue
                </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Success"
};
</script>

<style scoped>
#button-control {
  text-align: center;
  padding-bottom: 1.5rem;
}
</style>
