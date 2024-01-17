<script setup>
import { ref, watch } from 'vue'
import { usePrimeVue } from 'primevue/config';
const availableThemes = [
  { name: 'lara-dark-blue', code: 'ara-dark-blue' },
  { name: 'lara-dark-amber', code: 'lara-dark-amber' },
  { name: 'lara-dark-green', code: 'lara-dark-green' },
  { name: 'lara-light-blue', code: 'lara-light-blue' },
  { name: 'lara-light-green', code: 'lara-light-green' },
]
const currentTheme = ref({ name: 'lara-light-green', code: 'lara-light-green' })
const PrimeVue = usePrimeVue();

watch(currentTheme, async (newTheme, oldTheme) => {
  PrimeVue.changeTheme(oldTheme.code, newTheme.code, 'theme-link', () => {});
})

</script>

<template>
  <Dropdown v-model="currentTheme"
            :options="availableThemes"
            optionLabel="name"
            placeholder="Select Theme"
            class="w-full md:w-14rem" />
<!--  <Button @click="switchTheme">Switch</Button>-->
</template>

<style scoped>

</style>