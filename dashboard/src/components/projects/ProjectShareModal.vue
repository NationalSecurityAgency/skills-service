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
  <b-modal id="removalValidation" size="lg" title="Share Discoverable Project"
           v-model="show"
           :ok-only="true"
           :no-close-on-backdrop="true" :centered="true" body-class="p-4 mx-0"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog"
           @hide="publishHidden"
  >
    <div class="h6 text-primary text-center">To share your project, the following URL was copied.</div>
    <div class="input-group text-center">
      <input class="form-control font-italic text-center"
             style="background: #f5f5f5;"
             :value="shareUrl"
             data-cy="projShareUrl"
             readonly />
      <div class="input-group-append">
        <div class="input-group-text"><i class="fas fa-copy" aria-hidden="true"/></div>
      </div>
    </div>
    <div class="text-center text-success mt-2">
      <i class="fas fa-check-double" aria-hidden="true"></i> URL was copied!
    </div>
    <div class="text-primary mt-1  text-center">
      Please feel free to paste and share it with new users.
    </div>

    <div slot="modal-footer" class="text-right">
      <b-button @click="publishHidden" variant="success" data-cy="shareProjOkBtn">OK</b-button>
    </div>

  </b-modal>
</template>

<script>
  export default {
    name: 'ProjectShareModal',
    props: {
      value: {
        type: Boolean,
        required: true,
      },
      shareUrl: {
        type: String,
        required: true,
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
    },
  };
</script>

<style scoped>

</style>
