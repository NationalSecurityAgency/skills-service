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
  <span>
    <i :id="targetId"
       class="fas fa-question-circle text-secondary"
       :aria-label="`Help Message is ${msg}`"
       role="alert"
       tabindex="0"
       @keydown.esc="handleEscape"/>

    <b-tooltip :target="targetId"
               :title="msg"
               @shown="tooltipShown"
               @hidden="tooltipHidden"/>
  </span>
</template>

<script>
  export default {
    name: 'InlineHelp',
    props: {
      msg: String,
      targetId: {
        type: String,
        default: 'helpMsg',
      },
      nextFocusEl: HTMLElement,
    },
    methods: {
      tooltipShown(e) {
        this.$emit('shown', e);
      },
      tooltipHidden(e) {
        this.$emit('hidden', e);
      },
      handleEscape() {
        document.activeElement.blur();
        this.nextFocusEl?.focus();
      },
    },
  };
</script>

<style scoped>

</style>
