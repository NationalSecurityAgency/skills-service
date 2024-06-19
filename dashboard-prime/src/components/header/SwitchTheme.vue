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
import { usePrimeVue } from 'primevue/config'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

const PrimeVue = usePrimeVue()
const themeHelper = useThemesHelper()

const onChange = (newValue) => {
  const newCss = newValue.value
  const oldCss = themeHelper.themeOptions.find((i) => i.value !== newCss).value
  PrimeVue.changeTheme(oldCss, newCss, 'theme-link')
}
</script>

<template>
  <SelectButton
    v-model="themeHelper.currentTheme.value"
    :options="themeHelper.themeOptions"
    optionLabel="name"
    dataKey="value"
    @update:modelValue="onChange"
    aria-label="Theme Switcher">
    <template #option="slotProps">
      <i :class="slotProps.option.icon" class="text-xl"></i>
    </template>
  </SelectButton>
</template>

<style scoped>

</style>