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

</script>

<template>
  <Card :pt="{ content: { class: 'p-0 h-full' }, body: { class: 'p-0 h-full' } }">
    <template #content>
      <div class="flex flex-column h-full">
        <div>
          <slot name="header"></slot>
        </div>
        <div class="flex-1">
          <slot name="content"></slot>
        </div>
        <div>
          <slot name="footer"></slot>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>