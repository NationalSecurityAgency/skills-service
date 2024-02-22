<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import Card from 'primevue/card';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import SkillsTable from '@/components/skills/SkillsTable.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useBadgeState } from '@/stores/UseBadgeState.js';
import SkillsService from '@/components/skills/SkillsService.js';
import BadgesService from '@/components/badges/BadgesService.js';
import { SkillsReporter } from '@skilltree/skills-client-js'
import SkillsSelector from "@/components/skills/SkillsSelector.vue";
import { useProjConfig } from '@/stores/UseProjConfig.js'

const projConf = useProjConfig();
const badgeState = useBadgeState();
const route = useRoute();
const emit = defineEmits(['skills-changed']);

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
let learningPathViolationErr = ref({
  show: false,
  skillName: '',
});

onMounted(() => {
  projectId.value = route.params.projectId;
  badgeId.value = route.params.badgeId;
  badge.value = route.params.badge;
  if (!badge.value) {
    loadBadgeInfo();
  }
  loadAssignedBadgeSkills();
});

const loadAssignedBadgeSkills = () => {
  SkillsService.getBadgeSkills(projectId.value, badgeId.value)
      .then((loadedSkills) => {
        // in case of 403 request is still resolved but redirected to an error page
        // this avoids JS errors in console
        const validRequest = Array.isArray(loadedSkills);
        if (validRequest) {
          badgeSkills.value = loadedSkills;
        }
        loading.value.badgeSkills = false;
        if (validRequest) {
          loadAvailableBadgeSkills();
        }
      });
};

const loadAvailableBadgeSkills = () => {
  SkillsService.getProjectSkills(projectId.value, null, false, true)
      .then((loadedSkills) => {
        const badgeSkillIds = badgeSkills.value.map((item) => item.skillId);
        availableSkills.value = loadedSkills.filter((item) => !badgeSkillIds.includes(item.skillId));
        loading.value.availableSkills = false;
      });
};

const loadBadgeInfo = () => {
  BadgesService.getBadge(projectId.value, badgeId.value)
      .then((badge) => {
        badge.value = badge;
        loading.value.badgeInfo = false;
      });
};

const deleteSkill = (skill) => {
  const msg = `Are you sure you want to remove Skill "${skill.name}" from Badge "${badge.value.name}"?`;
  // msgConfirm(msg, 'WARNING: Remove Required Skill').then((res) => {
  //   if (res) {
      skillDeleted(skill);
    // }
  // });
};

const skillDeleted = (deletedItem) => {
  loading.value.skillOp = true;
  SkillsService.removeSkillFromBadge(projectId.value, badgeId.value, deletedItem.skillId)
      .then(() => {
        badgeSkills.value = badgeSkills.value.filter((entry) => entry.skillId !== deletedItem.skillId);
        availableSkills.value.unshift(deletedItem);
        badgeState.loadBadgeDetailsState({ projectId: projectId.value, badgeId: badgeId.value });
        loading.value.skillOp = false;
        emit('skills-changed', deletedItem);
      });
};

const skillAdded = (newItem) => {
  console.log(newItem);
  loading.value.skillOp = true;
  SkillsService.assignSkillToBadge(projectId.value, badgeId.value, newItem.skillId)
      .then(() => {
        badgeSkills.value.push(newItem);
        availableSkills.value = availableSkills.value.filter((item) => item.skillId !== newItem.skillId);
        badgeState.loadBadgeDetailsState(projectId.value, badgeId.value );
        loading.value.skillOp = false;
        emit('skills-changed', newItem);
        SkillsReporter.reportSkill('AssignGemOrBadgeSkills');
      }).catch((e) => {
    if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'LearningPathViolation') {
      loading.value.skillOp = false;
      learningPathViolationErr.value.show = true;
      learningPathViolationErr.value.skillName = newItem.name;
    } else {
      const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
      // handlePush({
      //   name: 'ErrorPage',
      //   query: { errorMessage },
      // });
    }
  });
};
</script>

<template>
  <div>
    <sub-page-header title="Skills"/>

    <Card>
      <template #content>
        <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp || loading.badgeInfo">
          <skills-selector :options="availableSkills"
                           v-if="!projConf.isReadOnlyProj"
                           class="search-and-nav border rounded"
                           v-on:added="skillAdded"
                           select-label="Select skill(s)"
                           :onlySingleSelectedValue="true">
          </skills-selector>
          <div v-if="learningPathViolationErr.show" class="alert alert-danger mx-3" data-cy="learningPathErrMsg">
            <i class="fas fa-exclamation-triangle" aria-hidden="true" />
            Failed to add <b>{{ learningPathViolationErr.skillName }}</b> skill to the badge.
            Adding this skill would result in a <b>circular/infinite learning path</b>.
  <!--          Please visit project's <b-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path</b-link> page to review.-->
          </div>

  <!--        <simple-skills-table v-if="badgeSkills && badgeSkills.length > 0" class="mt-2"-->
  <!--                             :skills="badgeSkills" v-on:skill-removed="deleteSkill"></simple-skills-table>-->

          <div v-if="badgeSkills && badgeSkills.length > 0">
            Skills exist
          </div>
          <no-content2 v-else title="No Skills Selected Yet..." icon="fas fa-award" class="mb-5"
                       message="Please use drop-down above to start adding skills to this badge!"></no-content2>
        </loading-container>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
