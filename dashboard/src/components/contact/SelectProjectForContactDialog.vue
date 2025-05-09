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

import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";

const model = defineModel()

const close = () => {
  model.value = false
}
</script>

<template>
  <SkillsDialog
      :style="{ width: '35rem' }"
      v-model="model"
      header="SkillTree Support"
      @on-cancel="close"
      ok-button-label="Got It"
      ok-button-icon="fa-solid fa-clipboard-check"
      :show-cancel-button="false"
      :maximizable="false"
  >
    Select project
  </SkillsDialog>
</template>

<style scoped>

</style>