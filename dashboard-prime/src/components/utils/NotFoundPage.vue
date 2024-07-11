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

const props = defineProps({
  explanation: String,
})

const route = useRoute();
const router = useRouter()

const timer = ref(-1);
const isOldLink = ref(false);
const newLinkValue = ref('');

onMounted(() => {
  if (route && route.redirectedFrom) {
    const { redirectedFrom } = route;
    if (redirectedFrom.path.startsWith('/projects') || redirectedFrom.path.startsWith('/metrics') || redirectedFrom.path.startsWith('/globalBadges')) {
      if (redirectedFrom.path === '/projects/' || redirectedFrom.path === '/projects') {
        newLinkValue.value = '/administrator/';
      } else {
        newLinkValue.value = `/administrator${redirectedFrom.fullPath}`;
      }
      isOldLink.value = true;
    }
  }
});


watch(isOldLink, (newVal) => {
  if (newVal === true) {
    timer.value = 10;
  }
});

watch(timer, (newValue) => {
  if (newValue > 0) {
    setTimeout(() => {
      timer.value -= 1;
    }, 1000);
  } else {
    router.replace(newLinkValue.value).catch((error) => {
      // eslint-disable-next-line
      console.error(error);
      router.push('/error');
    });
  }
});

</script>

<template>
  <div class="my-5">
    <div class="text-center text-color-secondary">
      <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-exclamation-triangle fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-center text-color-secondary text-2xl mt-2">
      Resource Not Found
    </div>

    <div class="container-fluid">
      <div class="row justify-content-center text-red-500 mt-3">
        <div class="col col-sm-8 col-md-6 col-lg-4 text-center" data-cy="notFoundExplanation">
          <p v-if="explanation">
            {{ explanation }}
          </p>
          <p v-else-if="isOldLink" data-cy="oldLinkRedirect" class="font-light">
            It looks like you may have followed an old link. You will be forwarded to <router-link :to="newLinkValue" data-cy="newLink">
            {{ newLinkValue }}</router-link> in {{ timer }} seconds.
          </p>
          <p v-else data-cy="notFoundDefaultExplanation">
            The resource you requested cannot be located.
          </p>
        </div>
      </div>
      <div class="text-center">
        <router-link to="/" tabindex="-1">
          <SkillsButton
              label="Take Me Home"
              icon="fas fa-home"
              outlined
              size="medium"
              severity="info"
              data-cy=takeMeHome
              class="" />
        </router-link>
      </div>
    </div>

  </div>
</template>

<style scoped>
</style>