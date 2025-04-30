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
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import SkillsService from '@/components/skills/SkillsService';
import SelfReportService from '@/components/skills/selfReport/SelfReportService';
import SubjectsService from '@/components/subjects/SubjectsService';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsSelector from "@/components/skills/SkillsSelector.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import DateCell from "@/components/utils/table/DateCell.vue";
import { SkillsReporter } from '@skilltree/skills-client-js';
import SubjectSelector from "@/components/skills/SubjectSelector.vue";

const emit = defineEmits(['conf-added', 'conf-removed']);
const announcer = useSkillsAnnouncer();
const route = useRoute();
const props = defineProps({
  userInfo: Object
});

const loadingMeta = ref({
  skills: true,
  subjects: true,
  loadingSkillsUnderASubject: false,
  numSkillsToProcess: 0,
});
const availableSkills = ref([]);
const availableSubjects = ref([]);
const selectedSkills = ref(null);
const selectedSubject = ref(null);
const addedSubjectSkillsStats = ref({
  addedSubject: false,
  numSkillsAdded: 0,
  numSkillsAlreadyConfigured: 0,
});
const pageSize = 4;
const possiblePageSizes = [4, 10, 15, 20];
const totalRows = ref(0);
const sortBy = ref('updated');
const sortOrder = ref(-1);
const data = ref([]);

onMounted(() => {
  const hasConf = props.userInfo.skillConf && props.userInfo.skillConf.length > 0;
  if (hasConf) {
    data.value = props.userInfo.skillConf.map((u) => ({ ...u }));
    updatePaging();
  }
  loadAvailableSkills();
});

let loading = computed(() => {
  return loadingMeta.value.skills || loadingMeta.value.subjects || loadingMeta.value.loadingSkillsUnderASubject || loadingMeta.value.numSkillsToProcess > 0;
});

const hadData = computed(() => {
  return data.value && data.value.length > 0;
});

const updatePaging = () => {
  totalRows.value = data.value.length;
};

const removeSkill = (item) => {
  resetSubjAddedInfo();
  removeTagConf(item)
      .then(() => {
        updatePaging();
      });
};

const loadAvailableSkills = () => {
  SkillsService.getProjectSkills(route.params.projectId, null, false, true)
      .then((loadedSkills) => {
        const alreadySelectedSkillIds = data.value.map((item) => item.skillId);
        availableSkills.value = loadedSkills.filter((item) => !alreadySelectedSkillIds.includes(item.skillId));
      }).finally(() => {
    loadingMeta.value.skills = false;
  });
  SubjectsService.getSubjects(route.params.projectId)
      .then((subjects) => {
        availableSubjects.value = subjects;
      }).finally(() => {
    loadingMeta.value.subjects = false;
  });
};

const selectSkill = (newItem) => {
  resetSubjAddedInfo();
  selectedSkills.value = newItem;
};

const resetSubjAddedInfo = () => {
  addedSubjectSkillsStats.value.numSkillsAdded = 0;
  addedSubjectSkillsStats.value.numSkillsAlreadyConfigured = 0;
  addedSubjectSkillsStats.value.addedSubject = false;
};

const addSkillToConf = () => {
  resetSubjAddedInfo();

  if (selectedSkills.value) {
    const { skillId } = selectedSkills.value;
    SelfReportService.configureApproverForSkillId(route.params.projectId, props.userInfo.userId, skillId).then((res) => {
      data.value.push(res);
      updatePaging();
      emit('conf-added', res);
      availableSkills.value = availableSkills.value.filter((item) => item.skillId !== skillId);
      selectedSkills.value = null;
      nextTick(() => announcer.polite(`Added workload configuration successfully for ${skillId} skill.`));
    });
  }
  if (selectedSubject.value) {
    const existingSkills = data.value.map((s) => s.skillId);
    loadingMeta.value.loadingSkillsUnderASubject = true;
    const { subjectId } = selectedSubject.value;
    selectedSubject.value = null;
    SkillsService.getSubjectSkills(route.params.projectId, subjectId, true).then((subjectSkills) => {
      const skillsToAdd = subjectSkills.filter((s) => s.type === 'Skill' && existingSkills.indexOf(s.skillId) < 0);
      const numSkillsToAdd = skillsToAdd.length;
      loadingMeta.value.numSkillsToProcess = numSkillsToAdd;

      addedSubjectSkillsStats.value.addedSubject = true;
      addedSubjectSkillsStats.value.numSkillsAdded = numSkillsToAdd;
      addedSubjectSkillsStats.value.numSkillsAlreadyConfigured = subjectSkills.length - numSkillsToAdd;

      let addSkillPromises = [];
      skillsToAdd.forEach((sToAdd) => {
        let newPromise = SelfReportService.configureApproverForSkillId(route.params.projectId, props.userInfo.userId, sToAdd.skillId)
        addSkillPromises.push(newPromise);
      });

      Promise.all(addSkillPromises).then((responses) => {
        responses.forEach((res) => {
          data.value.push(res);
          updatePaging();
          emit('conf-added', res);
          availableSkills.value = availableSkills.value.filter((item) => item.skillId !== res.skillId);
          loadingMeta.value.numSkillsToProcess -= 1;
        })
      }).finally(() => {
        nextTick(() => announcer.polite(`Added workload configuration successfully for ${numSkillsToAdd} skills.`));
        loadingMeta.value.loadingSkillsUnderASubject = false;
      });
    });
  }
};

const selectSubject = (newItem) => {
  resetSubjAddedInfo();
  selectedSubject.value = newItem;
};

const removeTagConf = (removedItem) => {
    data.value = data.value.map((i) => ({ ...i, deleteInProgress: i.id === removedItem.id }));
    return SelfReportService.removeApproverConfig(route.params.projectId, removedItem.id)
        .then(() => {
          data.value = data.value.filter((i) => i.id !== removedItem.id);
          emit('conf-removed', removedItem);
          nextTick(() => announcer.polite('Removed workload configuration successfully.'));
        });
}
</script>

<template>
  <Card :pt="{ body: { class: 'p-0!' } }" data-cy="splitWorkloadBySkillCard">
    <template #header>
      <SkillsCardHeader title="Split Workload By Skill"></SkillsCardHeader>
    </template>
    <template #content>
      <div v-if="!(loadingMeta.skills || loadingMeta.subjects)" class="flex gap-1 items-center pt-4 px-4 flex-col lg:flex-row">
        <div class="flex flex-1 flex-col mx-1 mt-1 w-full">
          <div class="mb-1">Add a Single Skill</div>
          <skills-selector
              :disabled="selectedSubject !== null || loading"
              :options="availableSkills"
              @added="selectSkill"
              @search-change="selectSkill(null)"
              placeholder="Select skill"/>
        </div>
        <div class="flex mx-1 text-center">
          <span class="text-center">OR</span>
        </div>
        <div class="flex flex-1 flex-col mx-1 mt-1 self-end w-full">
          <div class="mb-1">Add <b>ALL</b> Skills under a Subject</div>
          <subject-selector v-if="availableSubjects && availableSubjects.length > 0"
                            :disabled="(selectedSkills && selectedSkills.length > 0) || loading"
                            :options="availableSubjects"
                            :selected="selectedSubject"
                            @added="selectSubject"
                            @removed="selectedSubject = null"
                            :onlySingleSelectedValue="true"
                            :warnBeforeRemoving="false"/>
        </div>
        <div class="mx-1 mt-1 text-center lg:self-end">
          <SkillsButton
              aria-label="Add Tag Value"
              @click="addSkillToConf"
              v-skills="'ConfigureSelfApprovalWorkload'"
              data-cy="addSkillConfBtn"
              :disabled="loading || (!selectedSubject && (!selectedSkills || selectedSkills.length === 0))"
              label="Add"
              icon="fas fa-plus-circle">
          </SkillsButton>
        </div>
      </div>

      <skills-spinner v-if="loading" :is-loading="loading" class="mb-8"/>
      <div v-if="!loading">
        <SkillsDataTable v-if="hadData" class="mt-4"
                         :value="data"
                         show-gridlines
                         striped-rows
                         paginator
                         tableStoredStateId="skillApprovalConfSpecificUsersTable"
                         data-cy="skillApprovalSkillConfTable"
                         aria-label="Approval Configuration Skills"
                         pt:pcPaginator:paginatorContainer:aria-label="Approval Configuration Skills Paginator"
                         :rows="pageSize"
                         :rowsPerPageOptions="possiblePageSizes"
                         v-model:sort-field="sortBy"
                         v-model:sort-order="sortOrder">
          <Column field="skillName" header="Skill" sortable>

          <template #body="slotProps">
            <div class="flex" :data-cy="`skillCell-${slotProps.data.skillId}`">
              <div class="flex flex-1">
                {{ slotProps.data.skillName }}
              </div>
              <div>
                <SkillsButton title="Delete Skill"
                          :aria-label="`Remove ${slotProps.data.skillName} tag.`"
                          data-cy="deleteBtn"
                          @click="removeSkill(slotProps.data)"
                          :disabled="slotProps.data.deleteInProgress"
                          :loading="slotProps.data.deleteInProgress"
                          size="small" icon="fas fa-trash">
                </SkillsButton>
              </div>
            </div>

          </template>
          </Column>
          <Column field="updated" header="Configured On" sortable>
            <template #body="slotProps">
              <date-cell :value="slotProps.data.updated" />
            </template>
          </Column>

          <template #paginatorstart>
            <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
          </template>
        </SkillsDataTable>
        <no-content2 v-if="!hadData" title="Not Configured Yet..."
                     class="p-2 py-8"
                     data-cy="noSkillConf"
                     icon="fas fa-graduation-cap">
          You can split approval workload by routing approval requests for selected skills approval requests to <span class="text-primary font-weight-bold">{{userInfo.userIdForDisplay}}</span>.
        </no-content2>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>