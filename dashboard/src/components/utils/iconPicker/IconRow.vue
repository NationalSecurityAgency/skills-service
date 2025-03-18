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

import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';

const announcer = useSkillsAnnouncer()

const props = defineProps({
  item: {
    type: Object,
    default() {
      return {};
    },
  },
  options: {
    type: Object,
    default() {
      return {};
    },
  },
});

const emit = defineEmits(['icon-selected']);

const handleClick = (name, cssClass) => {
  announcer.polite(`${name} icon selected`);
  emit('icon-selected', { name, cssClass });
}
</script>

<template>
  <div :class="['flex flex-row justify-content-start p-2', { 'surface-hover': options.odd }]" style="height: 100px">
    <template v-for="(el, index) of item" :key="index">
      <div class="icon-item w-[5rem]">
        <button class="p-link text-blue-400"
           @click.stop.prevent="handleClick(el.name, el.cssClass)"
           :class="`icon-${el.name}`"
           :data-cy="`${el.cssClass}-link`"
           :aria-label="`select icon ${el.name}`">
               <span class="icon is-large">
                   <i :class="el.cssClass"></i>
               </span>
        </button><br/>
        <span class="iconName">{{ el.name }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.icon-row {
  display: flex;
  flex-direction: row;
  justify-content: space-evenly;
  padding-top: 12px;
  padding-bottom: 12px;
}

.icon-item {
  border-radius: 3px;
  color: inherit;
  flex: auto;
  text-align: center;
}

.icon-item i {
  box-sizing: content-box;
  text-align: center;
  border-radius: 3px;
  font-size: 3rem;
  width: 48px;
  height: 48px;
  display: inline-block;
}

.tab-content div {
  width: 100%;
}

.delete-icon {
  left: 0;
  visibility: hidden;
}

.is-tiny {
  height: .5rem;
  width: .5rem;
  font-size: .5rem;
  padding-right: .5rem;
}

.icon-item:hover .delete-icon {
  z-index: 1000;
  display: inline-block;
  visibility: visible;
}
</style>
