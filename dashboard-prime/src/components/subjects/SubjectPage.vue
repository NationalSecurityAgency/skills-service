<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue';
import Navigation from '@/components/utils/Navigation.vue';
import SubjectsService from '@/components/subjects/SubjectsService';

const store = useStore();
const route = useRoute();
const router = useRouter();
const announcer = useSkillsAnnouncer();

let subject = ref(store.getters["subjects/subject"]);

let isLoadingSubjects = ref(true);
let projectId = ref('');
let subjectId = ref('');
let showEditSubject = ref(false);

let isLoadingProjConfig = false;
let isReadOnlyProj = false;

onMounted(() => {
  projectId.value = route.params.projectId;
  subjectId.value = route.params.subjectId;
  loadSubject();
});

const isLoadingData = computed(() => {
  return isLoadingSubjects.value || isLoadingProjConfig;
});

const navItems = computed(() => {
  const items = [
    { name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'SubjectSkills' },
  ];

  if (!isReadOnlyProj) {
    items.push({ name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'SubjectLevels' });
  }
  items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SubjectUsers' });
  items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SubjectMetrics' });

  return items;
});

const headerOptions = computed(() => {
  if (!subject) {
    return {};
  }
  return {
    icon: 'fas fa-cubes skills-color-subjects',
    title: `SUBJECT: ${subject.name}`,
    subTitle: `ID: ${subjectId.value}`,
    stats: [{
      label: 'Groups',
      count: subject.numGroups,
      disabledCount: subject.numGroupsDisabled,
      icon: 'fas fa-layer-group skills-color-groups',
    }, {
      label: 'Skills',
      count: subject.numSkills,
      icon: 'fas fa-graduation-cap skills-color-skills',
      secondaryStats: [{
        label: 'reused',
        count: subject.numSkillsReused,
        badgeVariant: 'info',
      }, {
        label: 'disabled',
        count: subject.numSkillsDisabled,
        badgeVariant: 'warning',
      }],
    }, {
      label: 'Points',
      count: subject.totalPoints,
      warn: subject.totalPoints < minimumPoints,
      warnMsg: (subject.totalPoints + subject.totalPointsReused) < minimumPoints ? `Subject has insufficient points assigned. Skills cannot be achieved until subject has at least ${minimumPoints} points.` : null,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
      secondaryStats: [{
        label: 'reused',
        count: subject.totalPointsReused,
        badgeVariant: 'info',
      }],
    }],
  };
});

const minimumPoints = computed(() => {
  return store.getters.config.minimumSubjectPoints;
});

// watch(subject, (newVal, oldVal) => {
//   if (newVal && newVal.subjectId !== subjectId.value) {
//     subjectId.value = newVal.subjectId;
//   }
// });

const loadSubject = () => {
  isLoadingSubjects.value = true;
  if (route.params.subject) {
    store.dispatch('subjects/setSubject', route.params.subject);
    isLoadingSubjects.value = false;
  } else {
    store.dispatch('subjects/loadSubjectDetailsState', { projectId: projectId.value, subjectId: subjectId.value }).finally(() => {
      isLoadingSubjects.value = false;
    });
  }
};

const displayEditSubject = () => {
  showEditSubject.value = true;
};

const subjectEdited = (subject) => {
  SubjectsService.saveSubject(subject).then((resp) => {
    const origId = subject.subjectId;
    store.dispatch('subjects/setSubject', resp);
    if (resp.subjectId !== origId) {
      router.replace({ name: route.name, params: { ...route.params, subjectId: resp.subjectId } });
    }
    nextTick(() => {
      announcer.polite(`Subject ${subject.name} has been edited`);
    });
  });
};

const handleHideSubjectEdit = () => {
  showEditSubject.value = false;
  // if the id is edited, the route is reloaded which causes the focus to be moved to the container element
  // as such, if we want the edit button receive focus after the id has been altered, we need to double
  // the nextTick wait.
  nextTick(() => {
    nextTick(() => {
      // const ref = this.$refs?.editSubjectButton;
      // if (ref) {
      //   ref.focus();
      // }
    });
  });
};
</script>

<template>
  <div>
    <page-header :loading="isLoadingData" :options="headerOptions">
      <div slot="subSubTitle" v-if="subject && !isReadOnlyProj">
        <SkillsButton @click="displayEditSubject"
                  ref="editSubjectButton"
                  class="btn btn-outline-primary mr-1"
                  size="small"
                  variant="outline-primary"
                  data-cy="btn_edit-subject"
                  :aria-label="'edit Subject '+subject.subjectId">
          <span class="">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
        </SkillsButton>
      </div>
      <div slot="footer">
<!--        <import-finalize-alert />-->
      </div>
    </page-header>

    <navigation v-if="!isLoadingData" :nav-items="navItems">
    </navigation>

<!--    <edit-subject v-if="showEditSubject" v-model="showEditSubject"-->
<!--                  :subject="subject" @subject-saved="subjectEdited"-->
<!--                  :is-edit="true"-->
<!--                  @hidden="handleHideSubjectEdit"/>-->
  </div>
</template>

<style scoped></style>
