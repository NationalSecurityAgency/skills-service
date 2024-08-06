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
import { ref, onMounted, computed } from 'vue';

const props = defineProps({
  text: {
    type: String,
    required: true,
  },
  limit: {
    type: Number,
    required: false,
    default: 50,
  },
  containsHtml: {
    type: Boolean,
    required: false,
  },
  isInline: {
    type: Boolean,
    required: false,
    default: false,
  },
});

let slop = ref(15);
let displayFullText = ref(false);

onMounted(() => {
  displayFullText.value = props.text.length < props.limit + slop.value;
});

const truncate = computed(() => {
  return props.text.length >= props.limit + slop.value;
});

const toDisplay = computed(() => {
  if (displayFullText.value) {
    return `${props.text}`;
  }
  return `${props.text.substring(0, props.limit)}`;
});
</script>

<template>
  <div data-cy="showMoreText" class="text-break" :class="{'inline-block' : isInline}">
    <span>
      <span v-if="containsHtml" v-html="toDisplay"></span><span v-else data-cy="smtText">{{toDisplay}}</span>
      <a v-if="truncate" size="xs" variant="outline-info"
              class=""
              @click="displayFullText = !displayFullText"
              aria-label="Show/Hide truncated text"
              data-cy="showMoreOrLessBtn">
        <small v-if="displayFullText" data-cy="showLess"> &lt;&lt; less</small>
        <small v-else data-cy="showMore"><em>... &gt;&gt; more</em></small>
      </a>
    </span>
  </div>
</template>

<style scoped></style>
