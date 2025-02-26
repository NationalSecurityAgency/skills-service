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
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Logo1 from '@/components/brand/Logo1.vue';

const router = useRouter();
const route = useRoute();

const email = ref(route.params.email);
const timer = ref(-1);

onMounted(() => {
  timer.value = 10;
});

watch(timer, (value) => {
  if (value > 0) {
    setTimeout(() => {
      timer.value -= 1;
    }, 1000);
  } else {
    router.push({ name: 'Login' });
  }
})
</script>

<template>
  <div>
    <div class="pt-10" data-cy="resetRequestConfirmation">
      <div class="max-w-md lg:max-w-xl mx-auto">
        <div class="mt-8 text-center">
          <logo1 class="mb-4" />
          <Message :closable="false" role="heading" aria-level="1">Reset Password For SkillTree Dashboard</Message>
        </div>
        <Card class="mt-4 text-left">
          <template #content>
            <p>A password reset link has been sent to <span class="text-primary font-weight-bold">{{ email }}</span>. You will be forwarded to the login page in {{ timer }} seconds.</p>
            <div class="flex justify-center mt-2">
              <router-link :to="{ name: 'Login' }" tabindex="-1">
                <SkillsButton icon="fas fa-sign-in-alt"
                              outlined
                              size="small"
                              data-cy="loginPage"
                              id="loginPageBtn"
                              label="Return to Login Page">
                </SkillsButton>
              </router-link>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>