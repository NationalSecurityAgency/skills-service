<script setup>

import Logo1 from '@/components/brand/Logo1.vue';
</script>

<template>
  <div>
    <div class="grid justify-content-center text-center" data-cy="resetNotSupported">
      <div class="col md:col-8 lg:col-7 xl:col-5 mt-3" style="min-width: 20rem;">
        <div class="mt-5">
          <logo1 />
          <div class="text-3xl mt-4 text-primary">Password Reset not currently enabled</div>
        </div>
        <Card class="mt-3 text-left">
          <template #content>
            Password Reset is not currently enabled on this system. Please contact your SkillTree administrator. Return to the <router-link :to="{ name: 'Login' }" data-cy="loginPage">login page</router-link>?
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>