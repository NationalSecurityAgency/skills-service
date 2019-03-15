<template>
  <div>
    <span
      type="button"
      class="modal-close-x"
      @click="emitButtonClick('cancel')">&times;</span>
    <i
      class="modal-header-icon"
      :class="iconClass"/>
    <span class="modal-title">{{ title }}</span>
  </div>
</template>

<script>
  export default {
    props: {
      iconClass: String,
      title: String,
      hrefList: Array,
    },
    methods: {
      emitButtonClick(type) {
        this.$emit(type);
      },
    },
  };
</script>

<style scoped>
  .modal-title {
    font-family: inherit;
    font-size: 21px;
    line-height: 1.1;
    vertical-align: middle;
    color: #698dad;
  }

  .modal-header-icon {
    font-size: 40px;
    color: #d0d9e8;
    vertical-align: middle;
  }

  .modal-close-x {
    margin-top: -2px;
    font-size: 24px;
    font-weight: 700;
    line-height: 1;
    float: right;
    opacity: 0.2;
    color: #000;
    text-shadow: 0 1px 0 #fff;
    cursor: pointer;
  }
</style>
