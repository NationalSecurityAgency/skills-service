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
  <b-modal id="removalValidation" size="lg" :hide-footer="!isComplete"
           :no-close-on-esc="true"
           :hide-header-close="!isComplete"
           :title="title"
           v-model="show"
           :no-close-on-backdrop="true" :centered="true" body-class="p-0 m-0 border-primary"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog"
           @hidden="allDone"
           @close="allDone">
    <div class="px-4 py-5 text-center" data-cy="lengthyOpModal">
      <div v-if="!isComplete">
        <i class="fas fa-running border p-2 rounded mb-1 text-white bg-info"
           style="font-size: 2.5rem"/>
        <div class="h4 text-primary mb-3" data-cy="title">{{ progressMessage }}</div>
        <lengthy-operation-progress-bar :value="true" height="15px" :animated="true"/>
        <div class="text-secondary mt-1">This operation takes a little while so buckle up!</div>
      </div>
      <div v-else>
        <i class="fas fa-check-double border p-2 rounded mb-1 text-white bg-info"
           style="font-size: 2.5rem"/>
        <div class="h4 text-primary mb-1 mt-1">We are all done!</div>
        <div class="text-secondary" data-cy="successMessage">{{ successMessage }}</div>
      </div>
    </div>

    <div slot="modal-footer" class="w-100">
      <b-button variant="success" size="sm" class="float-right" @click="allDone"
                data-cy="allDoneBtn">
        Done
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import LengthyOperationProgressBar from '@/components/utils/LengthyOperationProgressBar';

  export default {
    name: 'LengthyOperationProgressBarModal',
    components: { LengthyOperationProgressBar },
    props: {
      value: {
        type: Boolean,
        required: true,
      },
      title: {
        type: String,
        required: true,
      },
      progressMessage: {
        type: String,
        required: true,
      },
      isComplete: {
        type: Boolean,
        required: true,
      },
      successMessage: {
        type: String,
        default: 'Operation completed successfully!',
      },
    },
    data() {
      return {
        show: this.value,
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      allDone() {
        this.$emit('operation-done');
        this.show = false;
      },
    },
  };
</script>

<style scoped>

</style>
