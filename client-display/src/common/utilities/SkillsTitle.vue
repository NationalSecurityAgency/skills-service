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
    <div class="titleBody card-body skills-page-title-text-color text-info text-uppercase m-0 py-2">
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

      <h1 :class="{'mx-5': showBackButton}" data-cy="title" class="skills-title m-0" >
        <slot/>
      </h1>

      <div class="row powered-by-row" style="top:0.4rem; right: .5rem;">
        <div class="col">
          <powered-by-skilltree class="float-right" :animate-power-by-label="animatePowerByLabel"/>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
  import PoweredBySkilltree from '../../userSkills/footer/PoweredBySkilltree';

  export default {
    name: 'SkillsTitle',
    components: { PoweredBySkilltree },
    props: {
      backButton: { type: Boolean, default: true },
      animatePowerByLabel: Boolean,
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
.titleBody {
  min-height: 3.5rem !important;
}
.powered-by-row {
  position: absolute !important;
}

@media (max-width: 675px) {
  .powered-by-row {
    position: relative !important;
  }
}
</style>
