/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <label class="checkbox">
    <input type="checkbox" v-model="selected" v-on:change="onChange">
    Search All Skills
    <b-tooltip label="By default only skills under current subject will be considered."
               position="is-left" animanted="true" type="is-light">
      <span aria-hidden="true"><i class="fas fa-question-circle"/></span>
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
