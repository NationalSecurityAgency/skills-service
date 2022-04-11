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
  <div class="text-primary">
    <b-form-checkbox
      :disabled="disabled"
      :id="`${logicalId}`"
      v-model="selectedInternal"
      :name="`checkbox_${this.projectId}_${this.skillId}`"
      :value="true"
      :unchecked-value="false"
      :inline="true"
      @change="handleChange($event)"
      @input="handleInput($event)"
      :data-cy="`skillSelect_${logicalId}`"
      :aria-label="`Import Skill ${projectId} ${skillId}`"
    >
      <span>{{ skillName }}</span>
    </b-form-checkbox>
  </div>
</template>

<script>

  export default {
    name: 'ImportCheckbox',
    props: {
      disabled: {
        type: Boolean,
        default: false,
      },
      projectId: String,
      skillId: String,
      selected: {
        type: Boolean,
        default: false,
      },
      skillName: String,
    },
    data() {
      return {
        selectedInternal: this.selected,
      };
    },
    watch: {
      selected(value) {
        this.selectedInternal = value;
      },
    },
    computed: {
      logicalId() {
        return `${this.projectId}-${this.skillId}`;
      },
    },
    methods: {
      handleChange(changeEvent) {
        this.$emit('importSelection', { selected: changeEvent });
      },
      handleInput(event) {
        this.$emit('importInput', event);
      },
    },
  };
</script>

<style scoped>

</style>
