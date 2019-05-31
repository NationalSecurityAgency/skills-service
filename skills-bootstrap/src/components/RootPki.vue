<template>
  <bootstrap-container title="User Skills Dashboard">
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
  </bootstrap-container>
</template>

<script>
import BootstrapContainer from './BootstrapContainer';
export default {
  name: "RootPki",
  components: { BootstrapContainer }
};
</script>

<style scoped>
#button-control {
  text-align: center;
  padding-bottom: 1.5rem;
}
</style>
