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
import { computed } from 'vue'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import SkillTreeLogoSvg from '@/components/brand/SkillTreeLogoSvg.vue'
import SplashPageCard from "@/components/myProgress/SplashPageCard.vue";

const responsive = useResponsiveBreakpoints()
const logoWidth = computed(() => responsive.sm.value ? '15rem' : '23rem')
</script>

<template>
  <div class="masthead mt-4 border rounded-border border-surface">
    <h1 class="sr-only">SkillTree Progress and Rankings</h1>
    <div class="masthead-content">
      <div class="container-fluid above-background">
        <div class="flex justify-center mt-16">
          <SkillTreeLogoSvg :logo-width="logoWidth"/>
        </div>
        <div class="text-center mb-8 italic above-background text-lg">A micro-learning <b>gamification</b>
          training
          platform
        </div>
        <div class="flex flex-col lg:flex-row gap-6 px-6">
          <splash-page-card
              title="Gamification"
              icon="fas fa-trophy text"
              border-color="border-t-green-700"
              icon-border-color="border-green-700"
              icon-color="text-green-700 dark:text-green-600">
            Earn <b>points <i class="far fa-arrow-alt-circle-up skills-color-points"
                              aria-hidden="true" /></b>, achieve <b>badges <i
              class="fas fa-award skills-color-badges" aria-hidden="true" /></b> and complete
            <b>skills <i class="fas fa-graduation-cap skills-color-skills"
                         aria-hidden="true" /></b>
            all from the comfort of your own chair!
          </splash-page-card>
          <splash-page-card
              title="Go At your Own Pace"
              icon="fas fa-clock"
              border-color="border-t-yellow-500"
              icon-border-color="border-yellow-500"
              icon-color="text-yellow-500">
            Learn new <b>skills <i class="fas fa-graduation-cap skills-color-skills"
                                   aria-hidden="true" /></b> and effectively master advanced
            knowledge when it's <b>most convenient <i
              class="fas fa-hourglass-half text-success" aria-hidden="true"></i></b> for you!
          </splash-page-card>
          <splash-page-card
              title="Community"
              icon="fas fa-users"
              border-color="border-t-indigo-500/75"
              icon-border-color="border-indigo-500"
              icon-color="text-indigo-500/75 dark:text-indigo-300">
            Take the learning journey <b>together <i class="fas fa-users skills-color-users"
                                                     aria-hidden="true" /></b> with your
            peers and enjoy <b>friendly <i class="fas fa-handshake text-info"
                                           aria-hidden="true" /></b> competition!
          </splash-page-card>
        </div>
        <div class="text-center mt-8">
          <div class="mb-4 uppercase text-2xl text-primary">Get Started Now</div>
          <div>
            <router-link :to="{ name: 'DiscoverProjectsPage' }" tabindex="-1">
              <SkillsButton
                label="Projects Catalog"
                icon="fas fa-tasks"
                size="large"
                severity="info"
                :outlined="false"
                data-cy="manageMyProjsBtnInNoContent"
                class="animate-fadein animate-duration-500" />
            </router-link>
          </div>
        </div>
      </div>
      <div class="bg-circle-1 bg-circle"></div>
      <div class="bg-circle-2 bg-circle"></div>
      <div class="bg-circle-3 bg-circle"></div>
      <div class="bg-circle-4 bg-circle"></div>
    </div>
  </div>
</template>

<style scoped>
.masthead img {
  position: relative;
  height: 12rem;
  margin-top: 2rem;
  margin-bottom: 1rem;
}

.masthead {
  position: relative;
  overflow: hidden;
}

.masthead .masthead-content {
  z-index: 1;
  position: relative;
  height: calc(100vh - 10rem) !important;
  min-height: 42rem !important;
}

.masthead .bg-circle {
  z-index: 0;
  position: absolute;
  border-radius: 100%;
  background: linear-gradient(0deg, #2d8779 0%, #f5fffe 100%);
}

.masthead .bg-circle-1 {
  height: 90rem;
  width: 90rem;
  bottom: -55rem;
  left: -55rem;
}

.masthead .bg-circle-2 {
  height: 50rem;
  width: 50rem;
  top: -25rem;
  right: -25rem;
}

.masthead .bg-circle-3 {
  height: 20rem;
  width: 20rem;
  bottom: -10rem;
  right: 5%;
}

.masthead .bg-circle-4 {
  height: 30rem;
  width: 30rem;
  top: -22rem;
  left: -5rem;
}

.above-background {
  z-index: 50 !important;
  position: relative;
}

/* bootstrap lg breakpoint */
@media (max-width: 992px) {
  .masthead .masthead-content {
    min-height: 53rem !important;
  }
}

/* bootstrap md breakpoint */
@media (max-width: 768px) {
  .masthead .masthead-content {
    min-height: 70rem !important;
  }
}

/* zoom 400% @ 1280px browser window */
@media (max-width: 400px) {
  .masthead .masthead-content {
    min-height: 75rem !important;
  }
}
</style>