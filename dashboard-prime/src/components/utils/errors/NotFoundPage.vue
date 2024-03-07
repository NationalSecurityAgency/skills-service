<script setup>

const props = defineProps({
  explanation: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="my-5 text-center">
    <div class="text-color-secondary">
      <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-exclamation-triangle fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-2xl text-primary">Resource Not Found</div>

    <p v-if="explanation">
      {{ explanation }}
    </p>
    <p v-else data-cy="notFoundDefaultExplanation">
      The resource you requested cannot be located.
    </p>
    <SkillsButton
      label="Take Me Home"
      icon="fas fa-home"
      href="/"
      outlined
      size="medium"
      severity="info"
      class="p-2" />

  </div>
</template>

<style scoped>

</style>