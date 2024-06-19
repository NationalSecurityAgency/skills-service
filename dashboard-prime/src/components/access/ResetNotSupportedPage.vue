/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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