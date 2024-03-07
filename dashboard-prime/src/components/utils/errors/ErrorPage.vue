<script setup>

</script>

<template>
  <Card class="mt-4" data-cy="errorPage">
    <template #content>
      <div class="text-center">
        <div class="text-color-secondary">
          <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-heart-broken fa-stack-1x fa-inverse"></i>
                    </span>
        </div>
        <div class="text-2xl text-primary">Tiny-bit of an error!</div>

        <p>
          Oh no, something went wrong! An error occurred and we are unable to process your request, please try again
          later.
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
  </Card>
</template>

<style scoped>

</style>