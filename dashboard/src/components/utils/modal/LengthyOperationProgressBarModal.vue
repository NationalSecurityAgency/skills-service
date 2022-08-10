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
  <b-modal id="removalValidation" size="lg" :hide-header="true" :hide-footer="true"
           v-model="show"
           :no-close-on-backdrop="true" :centered="true" body-class="p-0 m-0 border-primary"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog"
           @hide="publishHidden">
    <div class="text-center border-top border-primary py-1 bg-primary text-white">
    </div>
    <div class="px-4 py-5 text-center" data-cy="lengthyOpModal">
      <div v-if="!isComplete">
        <i class="fas fa-running border p-2 rounded mb-1 text-white bg-info"
           style="font-size: 2.5rem"/>
        <div class="h4 text-primary mb-3" data-cy="title">{{ title }}</div>
        <lengthy-operation-progress-bar :value="true" height="15px" :animated="true"/>
        <div class="text-secondary mt-1">This operation takes a little while so buckle up!</div>
      </div>
      <div v-else>
        <i class="fas fa-smile border p-2 rounded mb-1 text-white bg-success"
           style="font-size: 2.5rem"/>
        <div class="h4 text-primary mb-1 mt-1">We are all done!</div>
        <div class="text-secondary mb-2" data-cy="successMessage">{{ successMessage }}</div>
        <b-button variant="success" size="sm" @click="allDone" data-cy="allDoneBtn"><i
          class="fas fa-check"/> OK
        </b-button>
      </div>
    </div>
    <div class="text-center border-top border-primary py-1 bg-primary text-white">
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
      publishHidden(e) {
        this.show = false;
        this.$emit('hidden', { ...e });
      },
      allDone() {
        this.show = false;
        this.$emit('operation-done');
      },
    },
  };
</script>

<style scoped>

</style>
