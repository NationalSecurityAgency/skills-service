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
defineProps({
  title: String
})
</script>

<template>
  <Card :pt="{ footer: { class: 'p-0 h-6rem' }, content: { class: 'p-0' }, body: { class: 'p-0' } }" class="h-full">
    <template #content>
      <div class="flex h-full gap-2">
        <div class="flex-1 pl-3 pt-3">
          <div class="uppercase">
            <slot name="title">{{ title }}</slot>
          </div>
          <div class="mt-2">
            <slot name="left-content"></slot>
          </div>
        </div>
        <div class="pr-3 text-right">
          <slot name="right-content"></slot>
        </div>
      </div>
      <div>
        <Divider />
        <div class="px-3 pb-3">
          <slot name="footer"></slot>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>