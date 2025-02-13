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
import { useRoute } from 'vue-router';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import LoadingContainer from "@/components/utils/LoadingContainer.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import SkillsSelector from "@/components/skills/SkillsSelector.vue";
import Column from "primevue/column";
import {useBadgeState} from "@/stores/UseBadgeState.js";
import {storeToRefs} from "pinia";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";

const dialogMessages = useDialogMessages()
const announcer = useSkillsAnnouncer();
const emit = defineEmits(['skills-changed']);
const route = useRoute();
const badgeState = useBadgeState();
const { badge } = storeToRefs(badgeState);

const loading = ref({
  availableSkills: true,
  badgeSkills: true,
  skillOp: false,
});
const badgeSkills = ref([]);
const availableSkills = ref([]);
const badgeId = ref(null);
const afterListSlotText = ref('');
const search = ref('');

onMounted(() => {
  badgeId.value = route.params.badgeId;
  loadBadge();
  loadAssignedBadgeSkills();
})

watch(() => route.params.badgeId, async () => {
  badgeId.value = route.params.badgeId;
  loadBadge();
  loadAssignedBadgeSkills();
})

const loadBadge = () => {
  if (route.params.badge) {
    badge.value = route.params.badge;
    badgeSkills.value = badge.value.requiredSkills;
    loading.value.badgeSkills = false;
  } else {
    badgeState.loadGlobalBadgeDetailsState(badgeId.value ).then(() => {
      badge.value = badgeState.badge;
      badgeSkills.value = badge.value.requiredSkills;
      loading.value.badgeSkills = false;
    });
  }
};

const loadAssignedBadgeSkills = () => {
  loadAvailableBadgeSkills('');
};

const loadAvailableBadgeSkills = (query) => {
  GlobalBadgeService.suggestProjectSkills(badgeId.value, query)
      .then((res) => {
        let badgeSkillIds = [];
        if (badgeSkills.value) {
          badgeSkillIds = badgeSkills.value.map((item) => `${item.projectId}${item.skillId}`);
        }
        availableSkills.value = [];
        if (res && res.suggestedSkills) {
          availableSkills.value = res.suggestedSkills.filter((item) => !badgeSkillIds.includes(`${item.projectId}${item.skillId}`));
        }
        if (res?.totalAvailable > res?.suggestedSkills?.length) {
          afterListSlotText.value = `Showing ${res.suggestedSkills.length} of ${res.totalAvailable} results.  Use search to narrow results.`;
        } else {
          afterListSlotText.value = '';
        }
        loading.value.availableSkills = false;
      });
};

const deleteSkill = (skill) => {
  const msg = `Removing this skill will award this badge to users that fulfill all of the remaining requirements.
        Are you sure you want to remove Skill "${skill.name}" from Badge "${badge.value.name}"?`;

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
  GlobalBadgeService.removeSkillFromBadge(badgeId.value, deletedItem.projectId, deletedItem.skillId)
      .then(() => {
        badgeSkills.value = badgeSkills.value.filter((item) => `${item.projectId}${item.skillId}` !== `${deletedItem.projectId}${deletedItem.skillId}`);
        availableSkills.value.unshift(deletedItem);
        badgeState.loadGlobalBadgeDetailsState( badgeId.value ).then(() => {
          announcer.polite('skill has been removed from global badge')
          badge.value = badgeState.badge;
        });
        loading.value.skillOp = false;
        emit('skills-changed', deletedItem);
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
    GlobalBadgeService.assignSkillToBadge(badgeId.value, newItem.projectId, newItem.skillId)
        .then(() => {
          badgeSkills.value.push(newItem);
          availableSkills.value = availableSkills.value.filter((item) => `${item.projectId}${item.skillId}` !== `${newItem.projectId}${newItem.skillId}`);
          badgeState.loadGlobalBadgeDetailsState(badgeId.value).then(() => {
            announcer.polite('skill has been added to global badge')
            badge.value = badgeState.badge;
          });
          loading.value.skillOp = false;
          emit('skills-changed', newItem);
        })
        .finally(() => {
          focusOnSkillsSelector()
        })
  }
  searchChanged('');
};

const searchChanged = (query) => {
  search.value = query;
  loadAvailableBadgeSkills(query);
};
</script>

<template>
  <div>
    <sub-page-header title="Skills"/>

    <Card :pt="{ body: { class: '!p-0' } }">
      <template #content>
        <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp">
          <div class="px-4 py-6">
           <skills-selector :options="availableSkills"
                            ref="skillsSelector"
                            v-on:added="skillAdded" v-on:search-change="searchChanged"
                            :internal-search="false" :show-project="true"
                            :after-list-slot-text="afterListSlotText"></skills-selector>
          </div>
          <div v-if="badgeSkills && badgeSkills.length > 0">
            <SkillsDataTable
                tableStoredStateId="globalBadgeSkillsTable"
                aria-label="Global Badge Skills"
                :value="badgeSkills"
                paginator
                :rows="5"
                :totalRecords="badgeSkills.length"
                :rowsPerPageOptions="[5, 10, 15, 20]"
                data-cy="badgeSkillsTable">
              <Column header="Project ID" field="projectId" sortable></Column>
              <Column header="Skill Name" field="name" style="width: 40%;" sortable>
                <template #body="slotProps">
                  <router-link v-if="slotProps.data.subjectId" :id="slotProps.data.skillId" :to="{ name:'SkillOverview',
                    params: { projectId: slotProps.data.projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId }}"
                               class="btn btn-sm btn-outline-hc ml-2" :data-cy="`manage_${slotProps.data.skillId}`">
                    {{ slotProps.data.name }}
                  </router-link>
                  <div v-else>
                    {{ slotProps.data.name }}
                  </div>
                </template>
              </Column>
              <Column header="Skill ID" field="skillId" sortable></Column>
              <Column header="Delete">
                <template #body="slotProps">
                  <SkillsButton v-on:click="deleteSkill(slotProps.data)" size="small"
                                :id="`deleteSkill_${slotProps.data.skillId}`"
                                :track-for-focus="true"
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

          <no-content2 v-else title="No Skills Added Yet..." icon="fas fa-award" class="py-8"
                       message="Please use drop-down above to start adding skills to this badge!"></no-content2>
        </loading-container>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>