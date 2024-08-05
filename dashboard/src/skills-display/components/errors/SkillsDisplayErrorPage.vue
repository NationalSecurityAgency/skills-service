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
import { userErrorState } from '@/stores/UserErrorState.js'

const errorState = userErrorState()
</script>

<template>
  <Card class="mt-4" data-cy="errorPage">
    <template #content>
      <div class="text-center py-5">
        <div class="text-color-secondary">
          <div class="flex justify-content-center">
            <div class="border-circle w-6rem h-6rem m-2 surface-500 font-bold flex align-items-center justify-content-center">
              <i class="text-0 text-6xl" :class="errorState.icon"></i>
            </div>
          </div>
        </div>
        <div class="text-2xl text-primary">{{ errorState.title }}</div>
        <p v-if="errorState.explanation" data-cy="errExplanation">
          {{ errorState.explanation }}
        </p>

      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>