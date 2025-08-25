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
import { computed, nextTick, onMounted, provide, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js';
import { SkillsReporter } from '@skilltree/skills-client-js'
import Sortable from 'sortablejs'
import BlockUI from 'primevue/blockui'
import LoadingContainer from '@/components/utils/LoadingContainer.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SubjectsService from '@/components/subjects/SubjectsService'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import Subject from './Subject.vue'
import JumpToSkill from './JumpToSkill.vue'
import EditSubject from '@/components/subjects/EditSubject.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useProjDetailsState } from '@/stores/UseProjDetailsState.js'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'

const projConfig = useProjConfig();
const announcer = useSkillsAnnouncer()
const appConfig = useAppConfig()
const emit = defineEmits(['subjects-changed']);
const route = useRoute();
const elementHelper = useElementHelper()
const subjectsState = useSubjectsState()
const projectDetailsState = useProjDetailsState()
const finalizeInfoState = useFinalizeInfoState()
const dropAndDragEnabled = ref(false)

const subjRef = ref([]);
const mainFocus = ref();
const subPageHeader = ref();

watch(
  () => route.params.projectId,
  async newId => {
    projectId.value = newId;
    doLoadSubjects();
  }
)


const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

let isLoadingData = ref(true);

let projectId = ref(null);
let sortOrder = ref({
  loading: false,
  loadingSubjectId: '-1',
});

onMounted(() => {
  projectId.value = route.params.projectId;
  doLoadSubjects();
});

const isLoading = computed(() => {
  return isLoadingData.value //|| config.isLoadingProjConfig;
});

const addSubjectDisabled = computed(() => subjectsState.subjects && subjectsState.subjects.length >= appConfig.maxSubjectsPerProject)
const addSubjectsDisabledMsg = computed(() => `The maximum number of Subjects allowed is ${appConfig.maxSubjectsPerProject}`)

const subjectDialogInfo = ref({
  show: false,
  isEdit: false,
  subject: {}
});
const openNewSubjectModal = (subject = {}, isEdit = false) => {
  subjectDialogInfo.value.isEdit = isEdit
  subjectDialogInfo.value.subject = subject
  subjectDialogInfo.value.show = true;
};
provide('createOrUpdateSubject', openNewSubjectModal)

const doLoadSubjects = () => {
  return subjectsState.loadSubjects()
    .finally(() => {
      isLoadingData.value = false
      enableDropAndDrop()
    })
}

const deleteSubject = (subject) => {
  isLoadingData.value = true;
  SubjectsService.deleteSubject(subject).then(() => {
    projectDetailsState.loadProjectDetailsState()
    doLoadSubjects().then(() => {
      isLoadingData.value = false;
      emit('subjects-changed', subject.subjectId)
      announcer.polite(`Subject ${subject.name} has been deleted`)
    });
  });
};

const updateSortAndReloadSubjects = (updateInfo) => {
  const sortedSubjects = subjectsState.subjects.sort((a, b) => {
    if (a.displayOrder > b.displayOrder) {
      return 1;
    }
    if (b.displayOrder > a.displayOrder) {
      return -1;
    }
    return 0;
  });
  const currentIndex = sortedSubjects.findIndex((item) => item.subjectId === updateInfo.id);
  const newIndex = updateInfo.direction === 'up' ? currentIndex - 1 : currentIndex + 1;
  if (newIndex >= 0 && (newIndex) < subjectsState.subjects.length) {
    isLoadingData.value = true;
    const { projectId } = route.params;
    SubjectsService.updateSubjectsDisplaySortOrder(projectId, updateInfo.id, newIndex)
        .finally(() => {
          doLoadSubjects()
              .then(() => {
                isLoadingData.value = false;
                const foundRef = subjRef.value[updateInfo.id];
                nextTick(() => {
                  foundRef.focusSortControl();
                });
              });
        });
  }
};

const subjectAdded = (subject) => {
  const existingIndex = subjectsState.subjects.findIndex((item) => item.subjectId === subject.originalSubjectId)
  if (existingIndex >= 0) {
    const existingSubject = subjectsState.subjects[existingIndex]
    const enabledStateChanged = subject.enabled !== existingSubject.enabled
    subjectsState.subjects.splice(existingIndex, 1, subject)
    if (enabledStateChanged) {
      projectDetailsState.loadProjectDetailsState()
      finalizeInfoState.loadInfo()
    }
  } else {
    subjectsState.subjects.push(subject)
    SkillsReporter.reportSkill('CreateSubject');
    if (!subject.enabled) {
      SkillsReporter.reportSkill('CreateSubjectInitiallyHidden')
    }
  }
  announcer.polite(`Subject ${subject.name} has been saved`);
  enableDropAndDrop()
};

const enableDropAndDrop = () => {
  if (subjectsState.subjects && subjectsState.subjects.length > 1 && !dropAndDragEnabled.value) {
    nextTick(() => {
      elementHelper.getElementById('subjectCards').then((cards) => {
        Sortable.create(cards, {
          handle: '.sort-control',
          animation: 150,
          ghostClass: 'skills-sort-order-ghost-class',
          onUpdate(event) {
            sortOrderUpdate(event);
          },
        });
        dropAndDragEnabled.value = true
      });
    });
  }
};

const sortOrderUpdate = (updateEvent) => {
  const { id } = updateEvent.item;
  sortOrder.value.loadingSubjectId = id;
  sortOrder.value.loading = true;
  SubjectsService.updateSubjectsDisplaySortOrder(projectId.value, id, updateEvent.newIndex)
      .finally(() => {
        sortOrder.value.loading = false;
        sortOrder.value.loadingSubjectId = '-1';
        SkillsReporter.reportSkill('ChangeSubjectDisplayOrder');
      });
};
</script>

<template>
  <div ref="mainFocus">
    <loading-container v-bind:is-loading="isLoading">
      <sub-page-header ref="subPageHeader" title="Subjects" :action="isReadOnlyProj ? null : 'Subject'" @add-action="openNewSubjectModal"
                       :disabled="addSubjectDisabled" :disabled-msg="addSubjectsDisabledMsg"
                       :aria-label="'new subject'"/>
      <jump-to-skill />
      <div v-if="subjectsState.subjects && subjectsState.subjects.length"
           class="grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-3 gap-4"
           id="subjectCards"
           data-cy="subjectCards">
        <div v-for="(subject) of subjectsState.subjects"
             :key="subject.subjectId"
             :id="subject.subjectId"
             :data-cy="`${subject.subjectId}_card`">
          <BlockUI :blocked="sortOrder.loading" class="h-full">
              <div class="absolute z-50 top-1/2 w-full text-center" v-if="sortOrder.loading" :data-cy="`${subject.subjectId}_overlayShown`">
                <div v-if="subject.subjectId===sortOrder.loadingSubjectId" data-cy="updatingSortMsg">
                  <div class="text-info uppercase mb-1">Updating sort order!</div>
                  <skills-spinner :is-loading="sortOrder.loading" label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                </div>
              </div>

            <subject :subject="subject"
                     class="h-full"
                     :ref="(el) => (subjRef[subject.subjectId] = el)"
                     @subject-deleted="deleteSubject"
                     @sort-changed-requested="updateSortAndReloadSubjects"
                     :disable-sort-control="subjectsState.subjects.length === 1"/>
          </BlockUI>
        </div>
      </div>

      <no-content2 v-else class="mt-6"
                   title="No Subjects Yet" message="Subjects are a way to group and organize skill definitions within a gameified training profile."></no-content2>
    </loading-container>

    <edit-subject
      v-if="subjectDialogInfo.show"
      v-model="subjectDialogInfo.show"
      :is-edit="subjectDialogInfo.isEdit"
      :subject="subjectDialogInfo.subject"
      @subject-saved="subjectAdded" />
  </div>
</template>

<style scoped>
@media screen and (min-width: 1600px) {
  .xlPlus\:col-4 {
    flex: 0 0 auto;
    padding: 0.5rem;
    width: 33.3333%;
  }
}

@media screen and (min-width: 2000px) {
  .xlPlusPlus\:col-3 {
    flex: 0 0 auto;
    padding: 0.5rem;
    width: 25%;
  }
}
</style>
