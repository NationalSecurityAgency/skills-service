/*
Copyright 2024 SkillTree

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
<script setup>
const props = defineProps({
  msg: String,
  htmlMsg: String,
  targetId: {
    type: String,
    default: 'helpMsg',
  },
  tabIndex: {
    type: Boolean,
    default: true,
  },
  nextFocusEl: HTMLElement,
});

const emit = defineEmits(['shown', 'hidden']);

const tooltipShown = (e) => {
  emit('shown', e);
};

const tooltipHidden = (e) => {
  emit('hidden', e);
};

const handleEscape = () => {
  document.activeElement.blur();
  props.nextFocusEl?.focus();
};
</script>

<template>
  <span>
    <i v-if="tabIndex" :id="targetId" v-tooltip.top="msg"
       class="fas fa-question-circle text-secondary"
       :aria-label="`Help Message is ${msg}`"
       role="alert"
       tabindex="0"
       @keydown.esc="handleEscape"/>
    <i v-else :id="targetId" v-tooltip.top="msg"
       class="fas fa-question-circle text-secondary"
       :aria-label="`Help Message is ${msg}`"
       role="alert"
       @keydown.esc="handleEscape"/>

<!--    <b-tooltip v-if="htmlMsg" :target="targetId"-->
<!--               @shown="tooltipShown"-->
<!--               @hidden="tooltipHidden">-->
<!--      <span v-html="htmlMsg" />-->
<!--    </b-tooltip>-->
<!--    <b-tooltip v-else :target="targetId"-->
<!--               :title="msg"-->
<!--               @shown="tooltipShown"-->
<!--               @hidden="tooltipHidden"/>-->
  </span>
</template>

<style scoped></style>
