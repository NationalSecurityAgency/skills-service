<template>
  <b-dropdown variant="outline-secondary" size="sm" right no-caret>
    <template slot="button-content"><i class="fas fa-bars"/></template>

    <b-dropdown-item v-on:click="emit('edited')" class="mb-1">
      <span class="text-primary"> <i class="fas fa-edit pr-1"/>Edit</span>
    </b-dropdown-item>

    <span v-b-tooltip.hover="deleteDisabledText">
      <b-dropdown-item v-on:click="emit('deleted')" :disabled="isDeleteDisabled" >
          <span class="text-danger"> <i class="fas fa-trash pr-1"/> Delete</span>
      </b-dropdown-item>
    </span>

    <hr class="my-2"/>

    <b-dropdown-item v-on:click="emit('move-up')" :disabled="isFirst" :class="'{mb-1: true, disabled: isFirst}'">
      <span class="text-info"> <i class="fas fa-arrow-circle-up pr-1"/> Move Up</span>
    </b-dropdown-item>

    <b-dropdown-item v-on:click="emit('move-down')" :disabled="isLast" :class="'{disabled: isLast}'">
      <span class="text-info"> <i class="fas fa-arrow-circle-down pr-1"/> Move Down</span>
    </b-dropdown-item>

  </b-dropdown>
</template>

<script>
  export default {
    name: 'EditAndDeleteDropdown',
    props: {
      isFirst: Boolean,
      isLast: Boolean,
      isLoading: Boolean,
      isDeleteDisabled: Boolean,
      deleteDisabledText: String,
    },
    methods: {
      emit(eventName) {
        this.$emit(eventName);
      },
    },
  };
</script>

<style scoped>

</style>
