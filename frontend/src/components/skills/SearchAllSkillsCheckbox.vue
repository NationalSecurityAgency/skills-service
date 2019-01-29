<template>
  <label class="checkbox">
    <input type="checkbox" v-model="selected" v-on:change="onChange">
    Search All Skills
    <b-tooltip label="By default only skills under current subject will be considered."
               position="is-left" animanted="true" type="is-light">
      <span><i class="fas fa-question-circle"></i></span>
    </b-tooltip>
  </label>
</template>

<script>
  export default {
    name: 'SearchAllSkillsCheckbox',
    props: ['value'],
    data() {
      return {
        selected: [],
      };
    },
    mounted() {
      this.selected = this.value;
    },
    methods: {
      onChange() {
        this.$emit('input', this.selected);
        this.$emit('selection-changed', this.selected);
      },
    },
  };
</script>

<style scoped>

</style>
