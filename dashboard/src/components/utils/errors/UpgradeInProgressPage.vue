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
  <Card class="mt-6" data-cy="upgradeInProgressError">
    <template #content>
      <div class="text-center py-8">
        <div class="text-muted-color">
          <div class="flex justify-center">
            <div class="rounded-full w-24 h-24 m-2 bg-surface-500 dark:bg-surface-300 font-bold flex items-center justify-center">
              <i class="text-surface-0 dark:text-surface-900 text-6xl fas fa-tools"></i>
            </div>
          </div>
        </div>
        <div class="text-2xl text-primary">A database upgrade is in progress!</div>

        <p data-cy="errExplanation">
          A database upgrade is currently in progress, no modifications or changes to Projects/Subjects/Skills/Badges/Quizzes/Surveys
          are allowed until the upgrade has completed. Please try again later.
        </p>

        <router-link to="/">
          <SkillsButton
            label="Take Me Home"
            icon="fas fa-home"
            outlined
            size="medium"
            severity="info"
            data-cy=takeMeHome
            class="mt-8" />
        </router-link>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>