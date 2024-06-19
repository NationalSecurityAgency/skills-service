<script setup>
import { userErrorState } from '@/stores/UserErrorState.js'
import { useRouter } from 'vue-router'

const errorState = userErrorState()
const router = useRouter()

const goHomeAndRefresh = () => {
  router.replace({ path: '/'}).then(() => {
    window.location.reload();
  })
}
</script>

<template>
  <Card class="mt-4" data-cy="errorPage">
    <template #content>
      <div class="text-center py-5">
        <div class="text-color-secondary">
          <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fa-stack-1x fa-inverse" :class="errorState.icon"></i>
                    </span>
        </div>
        <div class="text-2xl text-primary">{{ errorState.title }}</div>

        <p v-if="errorState.explanation" data-cy="errExplanation">
          {{ errorState.explanation }}
        </p>

        <SkillsButton
          label="Take Me Home"
          icon="fas fa-home"
          outlined
          size="medium"
          severity="info"
          data-cy=takeMeHome
          @click="goHomeAndRefresh"
          class="mt-5" />
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>