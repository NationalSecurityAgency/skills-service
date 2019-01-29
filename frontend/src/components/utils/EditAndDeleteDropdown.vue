<template>
  <b-dropdown position="is-bottom-left">
    <button class="button" slot="trigger" :class="{'is-loading': isLoading}">
                <span class="icon is-small">
                  <i class="fas fa-bars"/>
                </span>
    </button>

    <b-dropdown-item v-on:click="emit('edited')">
      <div class="level">
        <div class="level-right">
          <div class="level-item">
                <span class="icon is-medium has-text-info">
                 <i class="fas fa-edit"/>
                </span>
          </div>
          <div class="level-item">
            <h2 class="has-text-info">Edit</h2>
          </div>
        </div>
      </div>
    </b-dropdown-item>

    <b-dropdown-item v-on:click="emit('deleted')">
      <div class="level is-fullwidth">
        <div class="level-right">
          <div class="level-item">
                <span class="icon is-medium has-text-info">
                 <i class="fas fa-trash"/>
                </span>
          </div>
          <div class="level-item">
            <span class="has-text-info">Delete</span>
          </div>
        </div>
      </div>
    </b-dropdown-item>

    <hr class="dropdown-divider"/>

    <b-dropdown-item v-on:click="emit('move-up')" :disabled="isFirst">
      <div class="level is-fullwidth">
        <div class="level-right">
          <div class="level-item">
                <span class="icon is-medium has-text-info">
                 <i class="fas fa-arrow-circle-up"/>
                </span>
          </div>
          <div class="level-item">
            <span class="has-text-info">Move Up</span>
          </div>
        </div>
      </div>
    </b-dropdown-item>

    <b-dropdown-item v-on:click="emit('move-down')" :disabled="isLast">
      <div class="level is-fullwidth">
        <div class="level-right">
          <div class="level-item">
                <span class="icon is-medium has-text-info">
                 <i class="fas fa-arrow-circle-down"/>
                </span>
          </div>
          <div class="level-item">
            <span class="has-text-info">Move Down</span>
          </div>
        </div>
      </div>
    </b-dropdown-item>

  </b-dropdown>
</template>

<script>
  export default {
    name: 'EditAndDeleteDropdown',
    props: ['isFirst', 'isLast', 'isLoading'],
    methods: {
      emit(eventName) {
        this.$emit(eventName);
      },
    },
  };
</script>

<style scoped>

</style>
