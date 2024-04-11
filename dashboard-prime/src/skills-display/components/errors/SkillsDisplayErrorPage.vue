<script setup>
import { userErrorState } from '@/stores/UserErrorState.js'

const errorState = userErrorState()
</script>

<template>
  <Card class="mt-4" data-cy="errorPage">
    <template #content>
      <div class="text-center py-5">
        <div class="text-color-secondary">
          <span class="fa-stack fa-3x text-orange-400" style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fa-stack-1x fa-inverse" :class="errorState.icon"></i>
                    </span>
        </div>
        <div class="text-2xl text-primary">{{ errorState.title }}</div>

        <p v-if="errorState.explanation" data-cy="errExplanation">
          {{ errorState.explanation }}
        </p>

      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>