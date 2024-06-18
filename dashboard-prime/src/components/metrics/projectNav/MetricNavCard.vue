<script setup>
defineProps(['title', 'subtitle', 'description', 'icon', 'pathName']);
</script>

<template>
  <Card>
    <template #content>
      <div>
        <div class="flex">
          <div class="flex d-inline-block mt-1 mr-3 border rounded p-2">
            <i class="fa-3x" :class="icon"/>
          </div>
          <div class="flex flex-1 flex-column">
            <div class="h4 mb-2">{{ title }}</div>
            <div class="font-light text-sm">
              <slot name="subtitle">
                {{ subtitle }}
              </slot>
            </div>
          </div>
        </div>
        <p class="mt-2">
          <small>{{ description }}</small>
        </p>
      </div>

    </template>
    <template #footer>
      <div class="text-center">
        <router-link :to="{ name: pathName }" :data-cy="`metricsNav-${title}`">
          <SkillsButton size="small" :label="`${title}`" icon="fas fa-arrow-right" />
        </router-link>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>