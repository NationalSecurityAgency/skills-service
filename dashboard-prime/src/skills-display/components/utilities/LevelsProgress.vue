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
import { ref } from 'vue'

const props = defineProps({
  level: Number,
  totalLevels: Number
})
const levelInternal = ref(props.level)
</script>

<template>
  <Rating
    v-model="levelInternal"
    :stars="totalLevels"
    readonly
    disabled
    :cancel="false"
    class="override-readonly-opacity" />
</template>

<style>
.override-readonly-opacity.p-rating {
  opacity: 1 !important;
}
</style>