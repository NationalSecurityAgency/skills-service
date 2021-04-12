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
  <b-card body-class="p-0 d-flex flex-column">
    <div class="flex-grow-1">
      <b-row class="justify-content-between no-gutters px-3 pt-3 pb-1 mr-3">
        <b-col cols="5">
          <div class="text-uppercase text-secondary">Badges</div>
          <div class="mt-2 ml-1 text-dark">
            <span style="font-size: 2.5rem;" class="text-warning" data-cy="numAchievedBadges">{{ numAchievedBadges }}</span> <span class="text-secondary" style="font-size: 1.2rem;" data-cy="numBadgesAvailable">/ {{ totalBadges }}</span>
          </div>
          <div v-if="totalGlobalBadges > 0"><b-badge variant="info" data-cy="numAchievedGlobalBadges">Global Badges: {{ numAchievedGlobalBadges }} / {{ totalGlobalBadges }}</b-badge></div>
          <div v-if="totalGems > 0"><b-badge variant="info" data-cy="numAchievedGemBadges">Gems: {{ numAchievedGemBadges }} / {{ totalGems }}</b-badge></div>
        </b-col>
        <b-col cols="7" style="font-size: 4.4rem;" class="text-right mb-4">
          <span class="fa-stack">
            <i class="fas fa-circle fa-stack-2x text-header"></i>
            <i class="fas fa-trophy fa-stack-1x skills-color-points"></i>
          </span>
        </b-col>
      </b-row>
    </div>
    <b-row class="justify-content-between no-gutters border-top text-muted small">
      <b-col class="p-2">
        <span data-cy="badges-num-footer">Be proud to earn those badges!!</span>
      </b-col>
    </b-row>
  </b-card>
</template>

<script>
  export default {
    name: 'BadgesNumCard',
    props: {
      totalBadges: {
        type: Number,
        required: true,
      },
      totalGems: {
        type: Number,
        required: true,
      },
      totalGlobalBadges: {
        type: Number,
        required: true,
      },
      numAchievedBadges: {
        type: Number,
        required: true,
      },
      numAchievedGemBadges: {
        type: Number,
        required: true,
      },
      numAchievedGlobalBadges: {
        type: Number,
        required: true,
      },
    },
    data() {
      return {
      };
    },
  };
</script>

<style scoped>

</style>
