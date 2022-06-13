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
  <b-modal :id="`edtRequiredNumSkillsModal-${group.skillId}`"
           title="Required Number of Skills"
           v-model="show"
           @hide="publishHidden"
           :no-close-on-backdrop="true"
           :centered="true"
           data-cy="EditSkillGroupModal"
           header-bg-variant="info"
           ok-title="Save"
           @ok="handleSave"
           :cancel-disabled="loading"
           :ok-disabled="loading || numSkillsRequired.original === numSkillsRequired.selected"
           header-text-variant="light" no-fade>
    <b-overlay :show="loading" rounded="sm" opacity="0.4" spinner-variant="info">
    <b-form inline :data-cy="`editRequiredModal-${group.skillId}`">
      <span class="mr-1 text-secondary">Required: </span>
      <b-form-select size="sm" v-model="numSkillsRequired.selected"
                     :options="numSkillsRequired.options" data-cy="requiredSkillsNumSelect"/>
      <span class="ml-1">skills</span>
      <div v-b-tooltip.hover.v-info class="ml-1 text-warning"
        title="A Group allows a Skill to be defined by the collection of other Skills within a Project. A Skill Group can require the completion of some or all of the included Skills before the group be achieved.">
        <i class="fas fa-question-circle"></i>
      </div>
    </b-form>
    </b-overlay>
  </b-modal>

</template>

<script>
  export default {
    name: 'EditNumRequiredSkills',
    props: {
      group: Object,
      skills: Array,
      value: {
        type: Boolean,
        required: true,
      },
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    data() {
      return {
        show: this.value,
        numSkillsRequired: {
          options: [],
          selected: null,
          original: null,
        },
        loading: false,
      };
    },
    mounted() {
      this.updateNumSkillsRequired();
    },
    methods: {
      publishHidden(e) {
        this.$emit('hidden', e);
      },
      updateNumSkillsRequired() {
        const numSkills = this.skills.length;
        const options = [];
        for (let i = 1; i < numSkills; i += 1) {
          options.push({ value: i, text: `${i} out of ${numSkills}` });
        }
        options.push({ value: -1, text: 'ALL SKILLS' });
        this.numSkillsRequired.options = options;
        this.numSkillsRequired.selected = this.group.numSkillsRequired;
        this.numSkillsRequired.original = this.numSkillsRequired.selected;
      },
      handleSave() {
        if (this.numSkillsRequired.selected < this.skills.length) {
          const updatedGroup = {
            ...this.group,
            numSkillsRequired: this.numSkillsRequired.selected,
          };
          this.$emit('group-changed', updatedGroup);
        }
      },
    },
  };
</script>

<style scoped>

</style>
