/*
Copyright 2026 SkillTree

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
import SkillTreeLogoSvg from '@/components/brand/SkillTreeLogoSvg.vue';

defineProps({
  icon: {
    type: String,
    required: true,
  },
  labelledBy: {
    type: String,
    required: true,
  },
});
</script>

<template>
  <div class="grid min-h-[min(44rem,calc(100vh-4rem))] place-items-center bg-[radial-gradient(circle_at_50%_18%,rgb(37_99_235/2%),transparent_22rem),linear-gradient(180deg,rgb(37_99_235/4%),transparent_24rem)] px-4 py-12 max-sm:place-items-start max-sm:pt-16">
    <section class="flex w-full max-w-136 flex-col gap-15 text-center" :aria-labelledby="labelledBy">
      <div class="flex flex-col items-center gap-1">
        <div class="flex justify-center">
          <SkillTreeLogoSvg logo-width="15rem" />
        </div>
        <div class="text-orange-800">Micro-Learning Gamification Platform</div>
      </div>

      <div class="relative rounded-b-[1.25rem] border border-gray-200 bg-white px-10 pb-10 pt-16 shadow-xl shadow-gray-900/10 max-sm:px-5 max-sm:pb-6 max-sm:pt-15">
        <div class="absolute inset-x-0 top-0 h-1 bg-linear-to-r from-blue-400 via-blue-600 to-cyan-400" aria-hidden="true"></div>
        <div
          class="absolute left-1/2 top-0 grid size-19 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full border-[0.4rem] border-white bg-blue-600 text-[1.6rem] text-white shadow-lg shadow-gray-900/15"
          aria-hidden="true">
          <i :class="icon"></i>
        </div>

        <slot></slot>
      </div>
    </section>
  </div>
</template>
