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
import { useRouter } from 'vue-router';
import Logo1 from '@/components/brand/Logo1.vue';

const router = useRouter();

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
    <div class="grid justify-content-center text-center" data-cy="resetConfirmation">
      <div class="col md:col-8 lg:col-7 xl:col-4 mt-3" style="min-width: 20rem;">
        <div class="mt-5">
          <logo1 />
          <div class="text-3xl mt-4 text-primary">Password Successfully Reset!</div>
        </div>
        <Card class="mt-3 text-left">
          <template #content>
            <p>Your password has been successfully reset! You will be forwarded to the login page in {{ timer }} seconds.</p>
            <div class="flex justify-content-center mt-2">
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