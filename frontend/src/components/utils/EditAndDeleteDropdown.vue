<template>
  <b-dropdown variant="outline-secondary" right>

    <b-dropdown-item v-on:click="emit('edited')" class="mb-1">
      <span class="has-text-info px-2"> <i class="fas fa-edit"/> Edit</span>
    </b-dropdown-item>

    <b-dropdown-item v-on:click="emit('deleted')">
      <span class="has-text-info px-2"> <i class="fas fa-trash"/> Delete</span>
    </b-dropdown-item>

    <hr class="my-2"/>

    <b-dropdown-item v-on:click="emit('move-up')" :disabled="isFirst" class="mb-1">
      <span class="has-text-info px-2"> <i class="fas fa-arrow-circle-up"/> Move Up</span>
    </b-dropdown-item>

    <b-dropdown-item v-on:click="emit('move-down')" :disabled="isLast">
      <span class="has-text-info px-2"> <i class="fas fa-arrow-circle-down"/> Move Down</span>
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
