<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { useStore, createNamespacedHelpers } from 'vuex'
import { useRoute } from 'vue-router'
import { SkillsReporter } from '@skilltree/skills-client-js';
import Sortable from 'sortablejs';
import BlockUI from 'primevue/blockui';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SubjectsService from '@/components/subjects/SubjectsService';
import { projConfig } from '@/components/projects/ProjConfig.js';
import NoContent2 from "@/components/utils/NoContent2.vue";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import Subject from './Subject.vue';
import JumpToSkill from './JumpToSkill.vue';

const announcer = useSkillsAnnouncer()
const config = projConfig();
const props = defineProps(['subject']);
const emit = defineEmits(['subjects-changed']);
const store = useStore();
const route = useRoute();
const projects = createNamespacedHelpers('projects');

let subjects = store.getters["subjects/subjects"];

const subjRef = ref();
const mainFocus = ref();
const subPageHeader = ref();

watch(route.params.projectId, function projectIdParamUpdated() {
  projectId.value = route.params.projectId;
  doLoadSubjects();
});

let isReadOnlyProj = false;

let isLoadingData = ref(true);
let displayNewSubjectModal = ref(false);
let projectId = ref(null);
let sortOrder = ref({
  loading: false,
  loadingSubjectId: '-1',
});

onMounted(() => {
  projectId.value = route.params.projectId;
  doLoadSubjects();
});

// computed

// ...subjectsStore.mapGetters([
//   'subjects',
// ]),
const isLoading = computed(() => {
  return isLoadingData.value //|| config.isLoadingProjConfig;
});

const emptyNewSubject = computed(() => {
  return {
    projectId: route.params.projectId,
    name: '',
    subjectId: '',
    description: '',
    iconClass: 'fas fa-book',
  };
});

const addSubjectDisabled = computed(() => {
  return subjects && store.getters.config && subjects.length >= store.getters.config.maxSubjectsPerProject;
});

const addSubjectsDisabledMsg = computed(() => {
  if (store.getters.config) {
    return `The maximum number of Subjects allowed is ${store.getters.config.maxSubjectsPerProject}`;
  }
  return '';
});

// methods
// ...projects.mapActions([
//   'loadProjectDetailsState',
// ]),
// ...subjectsStore.mapActions([
//   'loadSubjects',
// ]),
const openNewSubjectModal = () => {
  displayNewSubjectModal.value = true;
};

const doLoadSubjects = () => {
  return SubjectsService.getSubjects(route.params.projectId).then((res) => {
    subjects = res;
  }).finally(() => {
    isLoadingData.value = false;
    enableDropAndDrop();
  });
};

const deleteSubject = (subject) => {
  isLoadingData.value = true;
  SubjectsService.deleteSubject(subject).then(() => {
    // loadProjectDetailsState({ projectId: projectId });
    doLoadSubjects().then(() => {
      isLoadingData.value = false;
      emit('subjects-changed', subject.subjectId);
      nextTick(() => {
        announcer.polite(`Subject ${subject.name} has been deleted`);
      });
    });
  });
};

const updateSortAndReloadSubjects = (updateInfo) => {
  const sortedSubjects = subjects.sort((a, b) => {
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
  if (newIndex >= 0 && (newIndex) < subjects.length) {
    isLoadingData.value = true;
    const { projectId } = route.params;
    SubjectsService.updateSubjectsDisplaySortOrder(projectId, updateInfo.id, newIndex)
        .finally(() => {
          doLoadSubjects()
              .then(() => {
                isLoadingData.value = false;
                const foundRef = subjRef.value[updateInfo.id];
                nextTick(() => {
                  foundRef[0].focusSortControl();
                });
              });
        });
  }
};

const subjectAdded = (subject) => {
  displayNewSubjectModal.value = false;
  isLoadingData.value = true;
  SubjectsService.saveSubject(subject)
      .then(() => {
        doLoadSubjects()
            .then(() => {
              handleFocus().then(() => {
                nextTick(() => {
                  announcer.polite(`Subject ${subject.name} has been saved`);
                });
              });
            });
        // loadProjectDetailsState({ projectId: projectId });
        emit('subjects-changed', subject.subjectId);
        SkillsReporter.reportSkill('CreateSubject');
      });
};

const handleHide = (e) => {
  if (!e || !e.update) {
    handleFocus();
  }
};

const handleFocus = () => {
  return new Promise((resolve) => {
    nextTick(() => {
      // this.$refs?.subPageHeader?.$refs?.actionButton?.focus();
      resolve();
    });
  });
};

const enableDropAndDrop = () => {
  if (subjects && subjects.length > 0) {
    nextTick(() => {
      const cards = document.getElementById('subjectCards');
      Sortable.create(cards, {
        handle: '.sort-control',
        animation: 150,
        ghostClass: 'skills-sort-order-ghost-class',
        onUpdate(event) {
          sortOrderUpdate(event);
        },
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
      <div v-if="subjects && subjects.length" class="flex flex-wrap align-items-center justify-content-center" id="subjectCards" data-cy="subjectCards">
        <div v-for="(subject) of subjects" :key="subject.subjectId" :id="subject.subjectId" class="lg:col-4 mb-3"
             style="min-width: 23rem;" :data-cy="`${subject.subjectId}_card`">
          <div>
            <BlockUI :blocked="sortOrder.loading">
                <div class="absolute z-5 top-50 w-full text-center" :data-cy="`${subject.subjectId}_overlayShown`">
                  <div v-if="subject.subjectId===sortOrder.loadingSubjectId" data-cy="updatingSortMsg">
                    <div class="text-info uppercase mb-1">Updating sort order!</div>
                    <skills-spinner :is-loading="sortOrder.loading" label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                  </div>
                </div>

              <subject :subject="subject"
                       :ref="`(el) => (subjRef[subject.subjectId] = el)`"
                       @subject-deleted="deleteSubject"
                       @sort-changed-requested="updateSortAndReloadSubjects"
                       :disable-sort-control="subjects.length === 1"/>
            </BlockUI>
          </div>
        </div>
      </div>

      <no-content2 v-else class="mt-4"
                   title="No Subjects Yet" message="Subjects are a way to group and organize skill definitions within a gameified training profile."></no-content2>
    </loading-container>

<!--    <edit-subject v-if="displayNewSubjectModal" v-model="displayNewSubjectModal"-->
<!--                  :subject="emptyNewSubject" @subject-saved="subjectAdded"-->
<!--                  @hidden="handleHide"/>-->
  </div>
</template>

<style scoped></style>