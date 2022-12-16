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
  <div v-on:keydown.space="flipSelected" @click="flipSelected" tabindex="0">
    <i v-if="!selected" class="far fa-square"></i>
    <i v-if="selected" class="far fa-check-square text-success"></i>
  </div>
</template>

<script>
  export default {
    name: 'SelectCorrectAnswer',
    props: {
      value: Boolean,
    },
    data() {
      return {
        selected: this.value,
      };
    },
    watch: {
      selected(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      flipSelected() {
        this.selected = !this.selected;
      },
    },
  };
</script>

<style scoped>
i {
  font-size: 2.1rem;
  color: #b6b5b5;
  cursor: pointer;
}
</style>
