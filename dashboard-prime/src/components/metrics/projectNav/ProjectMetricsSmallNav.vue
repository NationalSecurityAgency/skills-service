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
import { useRoute } from 'vue-router';

defineProps(['navCards']);
const route = useRoute();

</script>

<template>
  <div class="flex gap-1">
    <router-link :to="{ name: navItem.pathName }" v-for="navItem in navCards" :key="navItem.title" tabindex="-1">
      <SkillsButton class=""
                    :aria-label="`${navItem.title} metrics`"
                    :data-cy="`${navItem.title}-metrics-link`"
                    :class="{
                      'bg-primary' : route.name === navItem.pathName,
                      'text-white' : route.name === navItem.pathName
                    }"> <i :class="navItem.icon"/><span class="sr-only">{{navItem.title}}</span></SkillsButton>
    </router-link>
  </div>
</template>

<style scoped>

</style>