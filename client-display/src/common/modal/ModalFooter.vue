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
        target="_blank">Click here!</a>
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
