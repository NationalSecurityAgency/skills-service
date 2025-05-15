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
  },
  headingTagToUse: {
    type: String,
    default: null
  }
});

const cardPt = computed(() => {
  const res = {content: {class: 'p-0'} };
  if (props.addBorder) {
    res.root = {class: 'border!'}
  }
  return res
})

const styleObject = {
  color: props.iconColor,
}

const titleTag = computed(() => props.headingTagToUse || 'div')
</script>

<template>
  <Card :pt="cardPt">
    <template #content>
      <div class="flex flex-col gap-3 sm:flex-row text-center sm:text-left">
        <div class="text-center sm:min-w-20">
          <i class="fa-3x" :class="iconClass" :style="styleObject" aria-hidden="true"/>
        </div>
        <div>
          <div class="text-2xl mb-2 uppercase flex gap-2 min-h-[2.5rem]"
               data-cy="mediaInfoCardTitle"
               style="overflow-wrap: break-word; text-wrap: wrap;">
            <component :is="titleTag" class="flex-1">{{ title }}</component>
            <div><slot name="right-of-title"></slot></div>
          </div>
          <div class="text-sm md:min-w-40" data-cy="mediaInfoCardSubTitle">
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
