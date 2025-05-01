/*
Copyright 2025 SkillTree

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
defineProps({
  title: String,
  borderColor: {
    type: String,
    default: 'border-t-green-700'
  },
  icon: String,
  iconColor: {
    type: String,
    default: 'text-green-700'
  },
  iconBorderColor: {
    type: String,
    default: 'border-green-700'
  }
})
</script>

<template>
  <Card :pt="{ root : { class: `border-t-4 ${borderColor}` }}">
    <template #content>
      <div class="text-primary uppercase text-xl text-center">{{ title }}</div>
      <div class="flex gap-2 mt-2 items-center lg:items-start">
        <Avatar
            :pt="{ root : { class: `border-1 ${iconBorderColor}` }, icon: { class: iconColor }}"
            :icon="icon" class="text-primary bg-primary-contrast border card-1-icon w-min-3rem"
            size="large" />
        <div>
          <slot />
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>