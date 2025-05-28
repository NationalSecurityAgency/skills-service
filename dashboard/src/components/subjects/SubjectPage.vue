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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useFocusState } from '@/stores/UseFocusState.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue'
import Navigation from '@/components/utils/Navigation.vue'
import EditSubject from '@/components/subjects/EditSubject.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import ImportFinalizeAlert from '@/components/skills/catalog/ImportFinalizeAlert.vue'
import CopySubjectOrSkillsDialog from "@/components/subjects/CopySubjectOrSkillsDialog.vue";
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'

const appConfig = useAppConfig()
const route = useRoute()
const router = useRouter()
const announcer = useSkillsAnnouncer()
const projConfig = useProjConfig()
const subjectState = useSubjectsState()
const subjectSkillsState = useSubjectSkillsState()
const finalizeInfoState = useFinalizeInfoState()
const focusState = useFocusState()

const showEditSubject = ref(false)
const showCopySubjectModal = ref(false)

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

onMounted(() => {
  loadSubject()
})

const isLoadingData = computed(() => {
  return subjectState.isLoadingSubject.value; // || projConfig.loadingProjConfig
})

const navItems = computed(() => {
  const items = [
    { name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'SubjectSkills' }
  ]

  if (!isReadOnlyProj.value) {
    items.push({ name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'SubjectLevels' })
  }
  items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SubjectUsers' })
  items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SubjectMetrics' })

  return items
})

const headerOptions = computed(() => {
  const subject = subjectState.subject
  return {
    icon: 'fas fa-cubes skills-color-subjects',
    title: `SUBJECT: ${subject.name}`,
    subTitle: `ID: ${subject.subjectId}`,
    stats: [{
      label: 'Groups',
      count: subject.numGroups,
      disabledCount: subject.numGroupsDisabled,
      icon: 'fas fa-layer-group skills-color-groups',
      secondaryStats: [{
        label: 'disabled',
        count: subject.numGroupsDisabled,
        badgeVariant: 'warning'
      }]
    }, {
      label: 'Skills',
      count: subject.numSkills,
      icon: 'fas fa-graduation-cap skills-color-skills',
      secondaryStats: [{
        label: 'reused',
        count: subject.numSkillsReused,
        badgeVariant: 'info'
      }, {
        label: 'disabled',
        count: subject.numSkillsDisabled,
        badgeVariant: 'warning'
      }]
    }, {
      label: 'Points',
      count: subject.totalPoints,
      warn: subject.totalPoints < minimumPoints.value,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
      secondaryStats: [{
        label: 'reused',
        count: subject.totalPointsReused,
        badgeVariant: 'info'
      }]
    }]
  }
})

const isInsufficientPoints = computed(() => {
  if (!subjectState.subject) {
    return false
  }
  return subjectState.subject?.totalPoints < appConfig.minimumSubjectPoints
})

const minimumPoints = computed(() => {
  return appConfig.minimumSubjectPoints
})

// watch(subject, (newVal, oldVal) => {
//   if (newVal && newVal.subjectId !== subjectId.value) {
//     subjectId.value = newVal.subjectId;
//   }
// });

const loadSubject = () => {
  subjectState.loadSubjectDetailsState()
}

const displayEditSubject = () => {
  showEditSubject.value = true
}

const displayCopySubjectModal = () => {
  showCopySubjectModal.value = true
}

const subjectEdited = (updatedSubject) => {
  // store.dispatch('subject/setSubject', resp);
  if (updatedSubject.subjectId !== subjectState.subject.subjectId) {
    router.replace({ name: route.name, params: { ...route.params, subjectId: updatedSubject.subjectId } })
      .then(() => focusState.focusOnLastElement())
  } else {
    focusState.focusOnLastElement()
  }
  const enabledStateChanged = updatedSubject.enabled !== subjectState.subject.enabled
  if (enabledStateChanged) {
    subjectSkillsState.loadSubjectSkills(updatedSubject.projectId, updatedSubject.subjectId)
    finalizeInfoState.loadInfo()
  }
  subjectState.subject = updatedSubject;
  announcer.polite(`Subject ${updatedSubject.name} has been edited`)
}

</script>

<template>
  <div>
    <page-header :loading="isLoadingData" :options="headerOptions">
      <template #subSubTitle v-if="!isLoadingData && !isReadOnlyProj">
        <SkillsButton
          id="editSubjectBtn"
          v-if="!isReadOnlyProj"
          @click="displayEditSubject"
          ref="editSubjectButton"
          label="Edit"
          icon="fas fa-edit"
          outlined
          class="btn btn-outline-primary mr-1"
          size="small"
          :track-for-focus="true"
          severity="info"
          data-cy="btn_edit-subject"
          :aria-label="`edit Subject ${subjectState.subject.name}`" />
        <SkillsButton
            id="copySubjectButton"
            v-if="!isReadOnlyProj"
            @click="displayCopySubjectModal"
            ref="copySubjectButton"
            label="Copy"
            icon="fas fa-copy"
            outlined
            class="btn btn-outline-primary"
            size="small"
            :track-for-focus="true"
            severity="info"
            data-cy="btn_copy-subject"
            :aria-label="`Copy Subject ${subjectState.subject.name} to another project`" />
      </template>
      <template #right-of-header
                v-if="!isLoadingData && (!subjectState.subject.enabled)">
        <Tag v-if="!subjectState.subject.enabled"
             severity="secondary"
             class="ml-2" data-cy="disabledSubjectBadge"><i
            class="fas fa-eye-slash mr-1" aria-hidden="true"></i> DISABLED</Tag>
      </template>
      <template #footer>
        <!--        <import-finalize-alert />-->
      </template>
    </page-header>

    <Message v-if="isInsufficientPoints" :closable="false" data-cy="subjInsufficientPoints">
      Subject has insufficient points assigned. Skills cannot be achieved until subject has at least <Tag>{{ appConfig.minimumSubjectPoints}}</Tag> points
    </Message>

    <import-finalize-alert />

    <navigation v-if="!isLoadingData" :nav-items="navItems">
    </navigation>

    <edit-subject v-if="showEditSubject" v-model="showEditSubject"
                  :subject="subjectState.subject" @subject-saved="subjectEdited"
                  :is-edit="true" />

    <copy-subject-or-skills-dialog
        v-if="showCopySubjectModal"
        v-model="showCopySubjectModal"
        copy-type="EntireSubject" />
  </div>
</template>

<style scoped></style>
