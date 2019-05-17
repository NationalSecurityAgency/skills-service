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
            <p>Welcome to the User Skills Management Dashboard. It looks like you're using PKI authentication, but you don't currently have a user with root permissions. At least one user must have root privileges.</p>
            <p class="mt-4">Click Continue to grant root privileges to your account.</p>
            <form @submit="$emit('grantRoot')">
              <div class="field">
                <div class="text-center">
                  <button class="btn btn-primary" :disabled="errors.any()">
                      <i class="fas fa-arrow-circle-right"/> Continue
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "RootPki"
};
</script>

<style scoped>
#button-control {
  text-align: center;
  padding-bottom: 1.5rem;
}
</style>
