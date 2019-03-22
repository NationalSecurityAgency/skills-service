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
                            The root account has been successfully created!
                        </p>
                        <p style="margin-top:2em">
                            Click Continue to proceed the User Skills Management Dashboard. </p>
                    </div>
                    <div class="field">
                        <div id="button-control" class="control">
                            <button id="continue-button" class="button is-primary" @click="$emit('proceed')">
                              <span class="icon is-small">
                                <i class="fas fa-arrow-circle-right"/>
                              </span>
                                <span>Continue</span>
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
    name: 'Success',
  };
</script>

<style scoped>
    #button-control {
        text-align: center;
        padding-bottom: 1.5rem;
    }
</style>
