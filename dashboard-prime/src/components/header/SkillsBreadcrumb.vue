<script setup>
import { ref } from 'vue'

const home = ref({
  icon: 'pi pi-home'
})
const items = ref([
  { label: 'Electronics' },
  { label: 'Computer' },
  { label: 'Accessories' },
  { label: 'Keyboard' },
  { label: 'Wireless' }
])
</script>

<template>
  <div class="card flex justify-content-center">
    <Breadcrumb :home="home" :model="items" />
  </div>
</template>

<style scoped></style>
