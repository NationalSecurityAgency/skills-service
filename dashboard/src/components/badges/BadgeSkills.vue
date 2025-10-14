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
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router';
import Card from 'primevue/card';
import Column from 'primevue/column';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useBadgeState } from '@/stores/UseBadgeState.js';
import SkillsService from '@/components/skills/SkillsService.js';
import BadgesService from '@/components/badges/BadgesService.js';
import { SkillsReporter } from '@skilltree/skills-client-js'
import SkillsSelector from "@/components/skills/SkillsSelector.vue";
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { storeToRefs } from 'pinia';
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import { useUpgradeInProgressErrorChecker } from '@/components/utils/errors/UseUpgradeInProgressErrorChecker.js'

const dialogMessages = useDialogMessages()
const projConf = useProjConfig();
const badgeState = useBadgeState();
const { badge } = storeToRefs(badgeState);
const route = useRoute();
const router = useRouter();
const emit = defineEmits(['skills-changed']);
const responsive = useResponsiveBreakpoints()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()

const loading = ref({
  availableSkills: true,
  badgeSkills: true,
  skillOp: false,
  badgeInfo: false,
});

const badgeSkills = ref([]);
const availableSkills = ref([]);
const projectId = ref(null);
const badgeId = ref(null);
const learningPathViolationErr = ref({
  show: false,
  skillName: '',
});
const nameQuery = ref(null);
const hideManageButton = ref(false);
const isReadOnly = ref(false);
const rows = ref(5);
const rowsPerPage = [5, 10, 15, 20];

onMounted(() => {
  projectId.value = route.params.projectId;
  badgeId.value = route.params.badgeId;
  badge.value = route.params.badge;
  if (!badge.value) {
    loadBadgeInfo();
  }
  loadAssignedBadgeSkills();
});

watch( () => route.params.badgeId, () => {
  badgeId.value = route.params.badgeId;
  loadBadgeInfo();
  loadAssignedBadgeSkills();
});

const updateRows = (newRows) => {
  rows.value = newRows;
}

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
  SkillsService.getProjectSkills(projectId.value, nameQuery.value, false, true)
      .then((loadedSkills) => {
        const badgeSkillIds = badgeSkills.value.map((item) => item.skillId);
        availableSkills.value = loadedSkills.filter((item) => !badgeSkillIds.includes(item.skillId));
        loading.value.availableSkills = false;
      });
};

const loadBadgeInfo = () => {
  BadgesService.getBadge(projectId.value, badgeId.value)
      .then((retrievedBadge) => {
        badge.value = retrievedBadge;
        loading.value.badgeInfo = false;
      });
};

const deleteSkill = (skill) => {
  const msg = `Are you sure you want to remove Skill "${skill.name}" from Badge "${badge.value.name}"?`;
  dialogMessages.msgConfirm({
    message: msg,
    header: 'WARNING: Remove Required Skill',
    acceptLabel: 'YES, Delete It!',
    rejectLabel: 'Cancel',
    accept: () => {
      skillDeleted(skill);
    },
    reject: () => {
      nextTick(() => {
        const deleteButton = document.getElementById(`deleteSkill_${skill.skillId}`)
        if (deleteButton) {
          deleteButton.focus()
        }
      })
    }
  });
};

const skillDeleted = (deletedItem) => {
  loading.value.skillOp = true;
  SkillsService.removeSkillFromBadge(projectId.value, badgeId.value, deletedItem.skillId)
      .then(() => {
        badgeSkills.value = badgeSkills.value.filter((entry) => entry.skillId !== deletedItem.skillId);
        availableSkills.value.unshift(deletedItem);
        badgeState.loadBadgeDetailsState(projectId.value, badgeId.value);
        loading.value.skillOp = false;
        badge.value = badgeState.badge.value;
      }).finally(() => {
        focusOnSkillsSelector()
      });
};

const skillsSelector = ref(null)
const focusOnSkillsSelector = () => {
  nextTick(() => {
    skillsSelector.value?.focus()
  })
}
const skillAdded = (newItem) => {
  if (newItem) {
    loading.value.skillOp = true;
    SkillsService.assignSkillToBadge(projectId.value, badgeId.value, newItem.skillId)
        .then(() => {
          badgeSkills.value.push(newItem);
          availableSkills.value = availableSkills.value.filter((item) => item.skillId !== newItem.skillId);
          badgeState.loadBadgeDetailsState(projectId.value, badgeId.value );
          loading.value.skillOp = false;
          SkillsReporter.reportSkill('AssignGemOrBadgeSkills');
          nameQuery.value = null
          filterSkills('');
        }).catch((e) => {
      if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'LearningPathViolation') {
        loading.value.skillOp = false;
        learningPathViolationErr.value.show = true;
        learningPathViolationErr.value.skillName = newItem.name;
      } else if(upgradeInProgressErrorChecker.isUpgrading(e)) {
        upgradeInProgressErrorChecker.navToUpgradeInProgressPage()
      } else {
        const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
        router.push({ name: 'ErrorPage', query: { errorMessage } });
      }
    }).finally(() => {
      focusOnSkillsSelector()
    });
  }
};

const filterSkills = (searchQuery) => {
  nameQuery.value = searchQuery;
  loadAvailableBadgeSkills();
}

</script>

<template>
  <div>
    <sub-page-header title="Skills"/>

    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp || loading.badgeInfo">
          <div class="p-4">
            <skills-selector :options="availableSkills"
                             ref="skillsSelector"
                             v-if="!projConf.isReadOnlyProj"
                             class="search-and-nav border rounded-sm"
                             v-on:added="skillAdded"
                             @search-change="filterSkills"
                             :internal-search="false"
                             :showClear="false">
            </skills-selector>
            <Message v-if="learningPathViolationErr.show" severity="error" data-cy="learningPathErrMsg">
              Failed to add <b>{{ learningPathViolationErr.skillName }}</b> skill to the badge.
              Adding this skill would result in a <b>circular/infinite learning path</b>.
              Please visit project's <router-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path</router-link> page to review.
            </Message>
          </div>
          <div v-if="badgeSkills && badgeSkills.length > 0">
            <SkillsDataTable
              tableStoredStateId="badgeSkillsTable"
              aria-label="Badge Skills"
              :value="badgeSkills"
              :paginator="badgeSkills.length > 5"
              :rows="rows"
              :totalRecords="badgeSkills.length"
              :rowsPerPageOptions="rowsPerPage"
              @update:rows="updateRows"
              data-cy="badgeSkillsTable">
              <Column header="Skill Name" field="name" sortable :class="{'flex': responsive.md.value }">
                <template #body="slotProps">
                  <router-link v-if="slotProps.data.subjectId && !hideManageButton" :id="slotProps.data.skillId" :to="{ name:'SkillOverview',
                    params: { projectId: slotProps.data.projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId }}"
                               class="btn btn-sm btn-outline-hc ml-2"
                               :data-cy="`manage_${slotProps.data.skillId}`">
                    {{ slotProps.data.name }}
                  </router-link>
                </template>
              </Column>
              <Column header="Skill ID" field="skillId" sortable :class="{'flex': responsive.md.value }"></Column>
              <Column header="Total Points" field="totalPoints" sortable :class="{'flex': responsive.md.value }"></Column>
              <Column header="Delete" :class="{'flex': responsive.md.value }">
                <template #body="slotProps">
                  <SkillsButton v-if="!projConf.isReadOnlyProj" v-on:click="deleteSkill(slotProps.data)" size="small"
                                :id="`deleteSkill_${slotProps.data.skillId}`"
                                :disabled="badge && badge.enabled === 'true' && badgeSkills.length === 1"
                          :data-cy="`deleteSkill_${slotProps.data.skillId}`" icon="fas fa-trash" label="Delete"
                          :aria-label="`remove dependency on ${slotProps.data.skillId}`">
                  </SkillsButton>
                </template>
              </Column>

              <template #paginatorstart>
                <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ badgeSkills.length }}</span>
              </template>
            </SkillsDataTable>
          </div>
          <no-content2 v-else title="No Skills Selected Yet..." icon="fas fa-award" class="py-8"
                       message="Please use drop-down above to start adding skills to this badge!"></no-content2>
        </loading-container>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
