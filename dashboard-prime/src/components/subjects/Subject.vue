<script setup>
import { ref, computed, onMounted, nextTick, watch, inject, toRaw } from 'vue'
import { useStore } from 'vuex';
import Badge from 'primevue/badge';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SubjectsService from '@/components/subjects/SubjectsService';
import NavCardWithStatsAndControls from '@/components/utils/cards/NavCardWithStatsAndControls.vue';
import CardNavigateAndEditControls from '@/components/utils/cards/CardNavigateAndEditControls.vue';
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue';
import LoadingCard from '@/components/utils/LoadingCard.vue';

const announcer = useSkillsAnnouncer()
const store = useStore();
const emit = defineEmits(['sort-changed-requested', 'subject-deleted']);
const props = defineProps(['subject', 'disableSortControl']);
const subject = props.subject;

const subjectCardControls = ref();

let isLoading = ref(false);
let showDeleteDialog = ref(false);
let cardOptions = ref({ controls: {} });
let subjectInternal = ref({ ...subject });
let deleteSubjectDisabled = ref(false);
let deleteSubjectToolTip = ref('');

onMounted(() => {
  buildCardOptions();
})

// computed
const minimumPoints = computed(() => {
  return store.getters.config.minimumSubjectPoints;
});

const alreadyShared = computed(() => {
  return subjectInternal?.exported === true;
});

// methods
const buildCardOptions = () => {
  cardOptions.value = {
    navTo: buildManageNavLink(),
    icon: subjectInternal.value.iconClass,
    title: subjectInternal.value.name,
    subTitle: `ID: ${subjectInternal.value.subjectId}`,
    stats: [{
      label: '# Skills',
      count: subjectInternal.value.numSkills,
      icon: 'fas fa-graduation-cap skills-color-skills',
      secondaryStats: [{
        label: 'reused',
        count: subjectInternal.value.numSkillsReused,
        badgeVariant: 'info',
      }, {
        label: 'disabled',
        count: subjectInternal.value.numSkillsDisabled,
        badgeVariant: 'warning',
      }],
    }, {
      label: 'Points',
      count: subjectInternal.value.totalPoints,
      warn: (subjectInternal.value.totalPoints + subjectInternal.value.totalPointsReused) < minimumPoints,
      warnMsg: (subjectInternal.value.totalPoints + subjectInternal.value.totalPointsReused) < minimumPoints ? `Subject has insufficient points assigned. Skills cannot be achieved until subject has at least ${minimumPoints} points.` : null,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
      secondaryStats: [{
        label: 'reused',
        count: subjectInternal.value.totalPointsReused,
        badgeVariant: 'info',
      }],
    }],
    controls: {
      navTo: buildManageNavLink(),
      type: 'Subject',
      name: subjectInternal.value.name,
      id: subjectInternal.value.subjectId,
      deleteDisabledText: deleteSubjectToolTip,
      isDeleteDisabled: deleteSubjectDisabled,
      showShare: false,
      shareEnabled: !alreadyShared,
    },
    displayOrder: subject.displayOrder,
  };
};

const buildManageNavLink = () => {
  return { name: 'SubjectSkills', params: { projectId: props.subject.projectId, subjectId: props.subject.subjectId } };
};

const deleteSubject = () => {
  showDeleteDialog.value = true;
};

const doDeleteSubject = () => {
  SubjectsService.checkIfSubjectBelongsToGlobalBadge(subjectInternal.value.projectId, subjectInternal.value.subjectId)
      .then((belongsToGlobal) => {
        if (belongsToGlobal) {
          const msg = 'Cannot delete this subject as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
          // msgOk(msg, 'Unable to delete');
        } else {
          emit('subject-deleted', subjectInternal.value);
        }
      });
};

const shareSubject = () => {
  SubjectsService.shareSubject(subjectInternal.value.projectId, subjectInternal.value.subjectId)
      .then(() => {
        subjectInternal.value.exported = true;
      });
};

const unshareSubject = () => {
  SubjectsService.unshareSubject(subjectInternal.value.projectId, subjectInternal.value.subjectId)
      .then(() => {
        subjectInternal.value.exported = false;
      });
};

const sortRequested = (info) => {
  const withId = {
    ...info,
    id: subject.subjectId,
  };
  emit('sort-changed-requested', withId);
};


watch(
  () => props.subject,
  async (newVal) => {
    if (newVal) {
      subjectInternal.value = newVal
      buildCardOptions()
    }
  })

const createOrUpdateSubject = inject('createOrUpdateSubject')

</script>

<template>
  <div data-cy="subjectCard">
    <loading-card :loading="isLoading"/>
    <nav-card-with-stats-and-controls v-if="!isLoading"
                                      ref="navCardWithStatsAndControls"
                                      :disable-sort-control="disableSortControl"
                                      @sort-changed-requested="sortRequested"
                                      :options="cardOptions" :data-cy="`subjectCard-${subjectInternal.subjectId}`">
      <template #underTitle>
        <card-navigate-and-edit-controls
            ref="subjectCardControls"
            :to="buildManageNavLink()"
            :options="cardOptions.controls"
            :button-id-suffix="subjectInternal.subjectId"
            @edit="createOrUpdateSubject(props.subject, true)"
            @delete="deleteSubject"
            @share="shareSubject"
            @unshare="unshareSubject"
            :is-delete-disabled="deleteSubjectDisabled"
            :delete-disabled-text="deleteSubjectToolTip"/>
      </template>
      <template #footer>
        <div class="flex justify-content-end">
          <span class="small"><Badge style="font-size: 0.8rem;" variant="primary" data-cy="pointsPercent">{{ subjectInternal.pointsPercentage }}%</Badge> of the total points</span>
        </div>
      </template>
    </nav-card-with-stats-and-controls>


    <removal-validation
      v-if="showDeleteDialog"
      v-model="showDeleteDialog"
      :item-name="subjectInternal.name"
      item-type="subject"
      @do-remove="doDeleteSubject"
      :value="showDeleteDialog">
      Subject with id [{{subjectInternal.subjectId}}] will be removed. Deletion <b>cannot</b> be undone and permanently removes its skill definitions and users' performed skills.
    </removal-validation>
  </div>
</template>

<style scoped></style>
