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