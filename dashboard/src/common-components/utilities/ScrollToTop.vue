/*
Copyright 2020 SkillTree

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
import { ref, onMounted } from 'vue';

const scrollDistance = ref(0);

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
});

const handleScroll = () => {
  scrollDistance.value = window.scrollY;
};

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
</script>

<template>
  <Button type="button" class="skills-theme-btn rounded-full text-xl" v-if="scrollDistance > 200"
          id="returnToTopButton" @click="scrollToTop" aria-label="Jump to the top of the page">
    <i class="fas fa-arrow-up" aria-hidden="true"></i>
  </Button>
</template>

<style scoped>
  #returnToTopButton {
    position: fixed;
    bottom: 70px;
    right: 15px;
    opacity: 0.6;
    z-index: 10;
    height: 56px;
  }

  #returnToTopButton:hover {
    opacity: 1;
  }
</style>
