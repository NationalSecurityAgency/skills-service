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
import Card from 'primevue/card';
import { useSkillsState } from '@/stores/UseSkillsState.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import ChildRowSkillsDisplay from '@/components/skills/ChildRowSkillsDisplay.vue';
import SlideDeck from "@/components/slides/SlideDeck.vue";

const route = useRoute();
const skillsState = useSkillsState();
</script>

<template>
  <div>
    <sub-page-header title="Overview"/>
    <loading-container :is-loading="skillsState.loadingSkill">
      <Card>
        <template #content>
          <child-row-skills-display
            v-if="skillsState.skill && skillsState.skill.skillId" 
            :skill="skillsState.skill" />
        </template>
      </Card>
    </loading-container>
  </div>
</template>

<style scoped></style>
