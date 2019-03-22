<template>
    <div class="container is-fluid">
        <div class="columns section">
            <div class="column is-half is-offset-one-quarter">
                <div class="card">
                    <div class="card-header has-background-grey-lighter">
                        <span class="icon is-large">
                          <i class="card-header-icon fas fa-award"/>
                        </span>
                        <p class="card-header-title">User Skills Management Dashboard</p>
                    </div>
                    <div class="card-content">
                        <p>
                            Welcome to the User Skills Management Dashboard.  It looks like you're using PKI authentication, but you don't currently have a user with root permissions. At least one user must have root privileges.
                        </p>
                        <p style="margin-top:2em">
                            Click Continue to grant root privileges to your account. </p>
                    </div>
                    <form @submit="$emit('grantRoot')">
                            <div class="field">
                                <div id="button-control" class="control">
                                    <button id="continue-button" class="button is-primary"
                                            :disabled="errors.any()">
                                      <span class="icon is-small">
                                        <i class="fas fa-arrow-circle-right"/>
                                      </span>
                                        <span>Continue</span>
                                    </button>
                                </div>
                            </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
  export default {
    name: 'RootPki',
  };
</script>

<style scoped>
    #button-control {
        text-align: center;
        padding-bottom: 1.5rem;
    }
</style>
