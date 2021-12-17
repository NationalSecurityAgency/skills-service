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
  <b-modal id="importSkillsFromCatalog" size="md" title="Removal Safety Check"
           v-model="show"
           :no-close-on-backdrop="true" :centered="true" body-class="px-0 mx-0"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog"
           @hide="publishHidden"
           aria-label="Removal Safety Check">
    <div class="px-2">
      <div data-cy="removalSafetyCheckMsg">
        <slot />
      </div>

      <hr />

      <div>
        <p>Please type <span class="font-italic font-weight-bold text-primary">{{ validationText }}</span> to permanently remove the record.</p>
        <b-form-input v-model="currentValidationText" data-cy="currentValidationText"></b-form-input>
      </div>
    </div>

    <div slot="modal-footer" class="w-100">
      <b-button variant="danger" size="sm" class="float-right ml-2"
                @click="removeAction" data-cy="removeButton" :disabled="removeDisabled"><i
        class="fas fa-trash"></i> Yes, Do Remove!
      </b-button>
      <b-button variant="secondary" size="sm" class="float-right" @click="publishHidden" data-cy="closeRemovalSafetyCheck">
        <i class="fas fa-times"></i> Cancel
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  export default {
    name: 'RemovalValidation',
    props: {
      value: {
        type: Boolean,
        required: true,
      },
      validationText: {
        type: String,
        required: false,
        default: 'Delete Me',
      },
    },
    data() {
      return {
        show: this.value,
        currentValidationText: '',
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      removeDisabled() {
        return this.currentValidationText !== this.validationText;
      },
    },
    methods: {
      publishHidden(e) {
        this.show = false;
        this.$emit('hidden', { ...e });
      },
      removeAction() {
        this.show = false;
        this.$emit('do-remove');
      },
    },
  };
</script>

<style scoped>

</style>
