<script setup>
import { useRoute } from 'vue-router';
import Card from 'primevue/card';
import { useSkillsState } from '@/stores/UseSkillsState.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import ChildRowSkillsDisplay from '@/components/skills/ChildRowSkillsDisplay.vue';

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
