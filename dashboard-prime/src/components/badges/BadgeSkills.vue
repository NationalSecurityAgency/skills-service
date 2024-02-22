<script setup>
import { ref } from 'vue';
import Card from 'primevue/card';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import SkillsTable from '@/components/skills/SkillsTable.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'

let loading = ref({
  availableSkills: true,
      badgeSkills: true,
      skillOp: false,
      badgeInfo: false,
});

let badgeSkills = ref([]);
let availableSkills = ref([]);
let projectId = ref(null);
let badgeId = ref(null);
let badge = ref(null);
let self = ref(null);
let learningPathViolationErr = ref({
  show: false,
  skillName: '',
});
</script>

<template>
  <div>
    <sub-page-header title="Skills"/>

    <Card>
      <template #content></template>
      <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp || loading.badgeInfo">
<!--        <skills-selector2 v-if="!isReadOnlyProj" :options="availableSkills" class="mb-2 m-3"-->
<!--                          v-on:added="skillAdded"-->
<!--                          :onlySingleSelectedValue="true"></skills-selector2>-->
        <div v-if="learningPathViolationErr.show" class="alert alert-danger mx-3" data-cy="learningPathErrMsg">
          <i class="fas fa-exclamation-triangle" aria-hidden="true" />
          Failed to add <b>{{ learningPathViolationErr.skillName }}</b> skill to the badge.
          Adding this skill would result in a <b>circular/infinite learning path</b>.
<!--          Please visit project's <b-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path</b-link> page to review.-->
        </div>

<!--        <skills-table v-if="skillsState.hasSkills && badgeSkills && badgeSkills.length" />-->
<!--        <simple-skills-table v-if="badgeSkills && badgeSkills.length > 0" class="mt-2"-->
<!--                             :skills="badgeSkills" v-on:skill-removed="deleteSkill"></simple-skills-table>-->

        <no-content2 v-else title="No Skills Selected Yet..." icon="fas fa-award" class="mb-5"
                     message="Please use drop-down above to start adding skills to this badge!"></no-content2>
      </loading-container>
    </Card>
  </div>
</template>

<style scoped></style>
