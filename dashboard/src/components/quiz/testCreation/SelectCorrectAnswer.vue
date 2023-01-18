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
  <span v-on:keydown.space="flipSelected" @click="flipSelected" tabindex="0"
        aria-label="Select as the correct answer" :class="{ 'cursorPointer': !readOnly}">
    <i v-if="!selected" class="far" :class="{ 'fa-square' : !isRadioIcon, 'fa-circle': isRadioIcon }" :style="{ 'font-size': fontSize }"></i>
    <i v-if="selected" class="far text-success" :class="{ 'fa-check-square' : !isRadioIcon, 'fa-check-circle': isRadioIcon }" :style="{ 'font-size': fontSize }"></i>
  </span>
</template>

<script>
  export default {
    name: 'SelectCorrectAnswer',
    props: {
      value: Boolean,
      readOnly: {
        type: Boolean,
        default: false,
      },
      fontSize: {
        type: String,
        default: '2.1rem',
      },
      isRadioIcon: {
        type: Boolean,
        default: false,
      },
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
        if (!this.readOnly) {
          this.selected = !this.selected;
          this.$emit('selected', this.selected);
        }
      },
    },
  };
</script>

<style scoped>
i {
  color: #b6b5b5;
}
.cursorPointer {
  cursor: pointer;
}
</style>
