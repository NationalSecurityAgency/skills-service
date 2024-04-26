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
<template>
  <button type="button" class="btn btn-info btn-floating btn-lg skills-theme-btn" v-if="scrollDistance > 200" id="returnToTopButton" @click="scrollToTop"
    tabindex="-1" aria-hidden="true">
    <i class="fas fa-arrow-up" aria-hidden="true"></i>
  </button>
</template>

<script>
  export default {
    name: 'ScrollToTop',
    data() {
      return {
        scrollDistance: 0,
      };
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll);
    },
    methods: {
      handleScroll() {
        this.scrollDistance = window.scrollY;
      },
      scrollToTop() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
    },
  };
</script>

<style scoped>
  #returnToTopButton {
    position: fixed;
    bottom: 70px;
    right: 15px;
    border-radius: 50px;
    opacity: 0.6;
    z-index: 10;
  }

  #returnToTopButton:hover {
    opacity: 1;
  }
</style>
