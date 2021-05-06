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
  <div class="card mb-3">
    <div class="card-body skills-page-title-text-color text-info text-uppercase m-0 py-2">
      <div class="row position-absolute" v-if="showBackButton">
        <div class="col">
          <div>
            <button @click="navigateBack" type="button" class="btn btn-outline-info skills-theme-btn m-0" data-cy="back" aria-label="navigate back">
              <i class="fas fa-arrow-left"></i>
              <span class="sr-only">Navigate back</span>
            </button>
          </div>
        </div>
      </div>

        <h1 :class="{'ml-5': showBackButton}" data-cy="title" class="skills-title m-0" >
            <slot/>
        </h1>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'SkillsTitle',
    props: {
      backButton: { type: Boolean, default: true },
    },
    methods: {
      navigateBack() {
        const previousRoute = this.$route.params.previousRoute || { name: 'home' };
        this.$router.push(previousRoute);
      },
    },
    computed: {
      showBackButton() {
        return this.backButton && this.$store.state.internalBackButton;
      },
    },
  };
</script>

<style>
</style>
