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
import {computed, reactive} from 'vue'
import Card from 'primevue/card'

const props = defineProps({
  title: [String, Number],
  subTitle: String,
  iconClass: String,
  iconColor: {
    type: String,
    require: false,
  },
  addBorder: {
    type: Boolean,
    default: false
  }
});

const cardPt = computed(() => {
  const res = {content: {class: 'p-0'} };
  if (props.addBorder) {
    res.root = {class: '!border'}
  }
  return res
})

const styleObject = {
  color: props.iconColor,
}
</script>

<template>
  <Card :pt="cardPt">
    <template #content>
      <div class="flex flex-col sm:flex-row text-center sm:text-left">
        <div style="min-width: 5rem;" class="text-center">
          <i class="fa-3x" :class="iconClass" :style="styleObject" aria-hidden="true"/>
        </div>
        <div>
          <div class="text-2xl mb-2 uppercase" data-cy="mediaInfoCardTitle" style="overflow-wrap: break-word; text-wrap: wrap;">{{ title }}</div>
          <div class="text-sm w-min-10rem" data-cy="mediaInfoCardSubTitle">
            <slot>
              {{ subTitle }}
            </slot>
          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped></style>
