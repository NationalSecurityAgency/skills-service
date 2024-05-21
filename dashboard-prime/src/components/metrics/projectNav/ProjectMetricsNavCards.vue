<script setup>
import MetricNavCard from "@/components/metrics/projectNav/MetricNavCard.vue";

defineProps(['navCards'])
</script>

<template>
  <div class="flex gap-3 flex-row">
    <div v-for="(navItem) in navCards" :key="navItem.title" class="flex">
      <metric-nav-card :title="navItem.title" :subtitle="navItem.subtitle" :description="navItem.description"
                       :path-name="navItem.pathName" :icon="navItem.icon" />
    </div>
  </div>
</template>

<style scoped>

</style>