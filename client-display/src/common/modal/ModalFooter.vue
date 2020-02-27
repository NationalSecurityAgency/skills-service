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
  <div
    role="footer"
    class="">
    <div
      v-if="helpUrl"
      class="modal-footer-help-link href">
      Need help?
      <a
        :href="helpUrl"
        target="_blank"
        rel="noopener">Click here!</a>
    </div>

    <button
      v-if="!hideCancel"
      :disabled="disableButtons"
      class="button footer-cancel-btn"
      @click="emitButtonClick('cancel')">{{ cancelText }}
    </button>

    <button
      v-if="!hideOk"
      :disabled="!isValid || disableButtons"
      class="button footer-ok-btn"
      @click="emitButtonClick('ok')">{{ okText }}
    </button>
  </div>
</template>

<script>
  export default {
    props: {
      helpUrl: { type: String },
      cancelText: { type: String, default: 'Cancel' },
      hideCancel: { type: Boolean, default: false },
      okText: { type: String, default: 'OK' },
      hideOk: { type: Boolean, default: false },
      isValid: { type: Boolean, default: true },
      disableButtons: { type: Boolean, default: false },
    },
    methods: {
      emitButtonClick(type) {
        this.$emit(type);
      },
    },
  };
</script>

<style>
  .button.footer-ok-btn {
    color: #fff;
    border-color: #264e26;
    background-color: #326632;
  }

  .button.footer-ok-btn:hover {
    color: #fff;
  }

  .modal-footer-help-link {
    float: left;
  }
</style>
