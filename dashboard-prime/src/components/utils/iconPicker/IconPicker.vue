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
<script setup>
import { ref } from 'vue';

const emit = defineEmits(['select-icon', 'on-icon-selected']);

const props = defineProps({
  startIcon: String,
  customIconHeight: {
    type: Number,
    default: 48,
  },
  customIconWidth: {
    type: Number,
    default: 48,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

let hideAvailableIcons = ref(true);
let selectedIconClass = ref(props.startIcon);

const selectIcon = () => {
  emit('select-icon');
};

const onSelectedIcon = (selectedIcon) => {
  selectedIconClass.value = `${selectedIcon.css}`;
  hideAvailableIcons.value = true;
  emit('on-icon-selected', selectedIconClass.value);
  // this.$bvModal.hide('icons');
};

const close = () => {
  hideAvailableIcons.value = true;
};
</script>

<template>
  <button class="icon-button"
       @click="selectIcon"
       @keypress.enter="selectIcon"
       role="button"
       aria-roledescription="icon selector button"
       aria-label="icon selector"
       tabindex="0"
       :disabled="disabled"
       data-cy="iconPicker">
    <div class="text-primary" style="min-height: 4rem;">
      <i :class="[selectedIconClass]" />
    </div>
  </button>
</template>

<style scoped>
  i {
    font-size: 3rem;
  }

  .icon-button {
    border: 1px solid rgba(0, 0, 0, 0.125);
    border-radius: 0.25em;
    background-color: #fff;
  }

  .icon-button:disabled {
    background-color: lightgrey;
    cursor: none;
  }
</style>
