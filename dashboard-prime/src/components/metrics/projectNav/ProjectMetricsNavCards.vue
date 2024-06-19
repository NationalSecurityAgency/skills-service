<script setup>
import MetricNavCard from "@/components/metrics/projectNav/MetricNavCard.vue";

defineProps(['navCards'])
</script>

<template>
  <div class="flex gap-3 flex-column lg:flex-row mt-4">
      <metric-nav-card v-for="(navItem) in navCards" :key="navItem.title" class="flex-1"
                       :title="navItem.title" :subtitle="navItem.subtitle" :description="navItem.description"
                       :path-name="navItem.pathName" :icon="navItem.icon" />
  </div>
</template>

<style scoped>

</style>