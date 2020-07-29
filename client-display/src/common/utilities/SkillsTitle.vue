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
    <div class="py-2 mt-2 mb-3 card-body skills-page-title-text-color card rounded bg-white text-info text-uppercase">
        <div v-if="backButton" class="position-absolute">
            <h2><button @click="navigateBack" type="button" class="btn btn-outline-info" data-cy="back">
                    <i class="fas fa-arrow-left"></i>
                </button>
            </h2>
        </div>

        <h2 :class="{'ml-5': backButton}">
            <slot/>
        </h2>
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
  };
</script>

<style>
</style>
