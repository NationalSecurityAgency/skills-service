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
    padding-left: 1rem;
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
