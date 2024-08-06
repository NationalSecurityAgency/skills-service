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
  explanation: String,
})
</script>

<template>
  <div class="my-5">
    <div class="text-center text-color-secondary">
      <div class="flex justify-content-center">
        <div class="border-circle w-6rem h-6rem m-2 surface-500 font-bold flex align-items-center justify-content-center">
          <i class="text-0 text-6xl fas fa-exclamation-triangle"></i>
        </div>
      </div>
    </div>
    <div class="text-center text-color-secondary text-2xl mt-2">
      Resource Not Found
    </div>

    <div class="container-fluid">
      <div class="row justify-content-center text-red-500 mt-3">
        <div class="col col-sm-8 col-md-6 col-lg-4 text-center" data-cy="notFoundExplanation">
          <p v-if="explanation">
            {{ explanation }}
          </p>
          <p v-else data-cy="notFoundDefaultExplanation">
            The resource you requested cannot be located.
          </p>
        </div>
      </div>
      <div class="text-center">
        <router-link to="/" tabindex="-1">
          <SkillsButton
              label="Take Me Home"
              icon="fas fa-home"
              outlined
              size="medium"
              severity="info"
              data-cy=takeMeHome
              class="" />
        </router-link>
      </div>
    </div>

  </div>
</template>

<style scoped>
</style>