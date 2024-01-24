<script setup>
import { ref, computed, nextTick, onMounted } from 'vue';
import { useStore, createNamespacedHelpers } from 'vuex';
import { useRoute } from 'vue-router'
import PageHeader from '@/components/utils/pages/PageHeader.vue';
import Navigation from '@/components/utils/Navigation.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import ProjectService from '@/components/projects/ProjectService';
import ProjectDates from '@/components/projects/ProjectDates.vue';
import dayjs from "../../../../common-components/src/common/DayJsCustomizer.js";

// const props = defineProps(['project'])
const store = useStore();
const route = useRoute();
const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('projects');

let isLoadingData = ref(true);
let cancellingExpiration = ref(false);
let editProject = ref(false);
let shareProjModal = ref(false);
let shareUrl = ref('');
let project = ref({});
let projectId = '';
let isReadOnlyProj = false;

// ...mapGetters([
//   'project',
// ]),
const isLoading = computed(() => {
  return isLoadingData.value; // || isLoadingProjConfig;
});

onMounted(() => {
  loadProjects();
});

const navItems = computed(() => {
  const items = [
    { name: 'Subjects', iconClass: 'fa-cubes skills-color-subjects', page: 'Subjects' },
    { name: 'Badges', iconClass: 'fa-award skills-color-badges', page: 'Badges' },
    { name: 'Self Report', iconClass: 'fa-laptop skills-color-selfreport', page: 'SelfReport' },
    { name: 'Learning Path', iconClass: 'fa-project-diagram skills-color-dependencies', page: 'FullDependencyGraph' },
  ];

  if (!isReadOnlyProj) {
    items.push({ name: 'Skill Catalog', iconClass: 'fa-book skills-color-skill-catalog', page: 'SkillsCatalog' });
    items.push({ name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'ProjectLevels' });
  }

  items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'ProjectUsers' });
  items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'ProjectMetrics' });

  if (!isReadOnlyProj) {
    items.push({ name: 'Contact Users', iconClass: 'fas fa-mail-bulk', page: 'EmailUsers' });
    items.push({ name: 'Issues', iconClass: 'fas fa-exclamation-triangle', page: 'ProjectErrorsPage' });
    items.push({ name: 'Access', iconClass: 'fa-shield-alt skills-color-access', page: 'ProjectAccess' });
    items.push({ name: 'Admin Activity', iconClass: 'fa-users-cog text-success', page: 'ProjectActivityHistory' });
    items.push({ name: 'Skill Expiration History', iconClass: 'fa-clock skills-color-expiration', page: 'ExpirationHistory' });
    items.push({ name: 'Settings', iconClass: 'fa-cogs skills-color-settings', page: 'ProjectSettings' });
  }

  return items;
});

const headerOptions = computed(() => {
  if (!project) { // || !projConfig
    return {};
  }
  let visibilityLabel = 'Project Catalog';
  let visibilityIcon = 'fas fa-eye-slash text-warning';
  let visibilityDescription = '';
  let visibilityType = 'Hidden';
  // if (isProjConfigInviteOnly) {
  //   visibilityLabel = 'Protection';
  //   visibilityDescription = 'Invite Only';
  //   visibilityIcon = 'fas fa-user-lock text-danger';
  //   visibilityType = 'PRIVATE';
  // } else if (isProjConfigDiscoverable) {
  //   visibilityType = 'Discoverable';
  //   visibilityIcon = 'fas fa-search-plus text-success';
  //   visibilityDescription = '';
  // }

  const stats = [];
  stats.push({
    label: visibilityLabel,
    preformatted: `<div class="h5 font-weight-bold mb-0">${visibilityType}</div>`,
    secondaryPreformatted: `<div class="text-secondary text-uppercase text-truncate" style="font-size:0.8rem;margin-top:0.1em;">${visibilityDescription}</div>`,
    icon: `${visibilityIcon} skills-color-visibility`,
  });
  stats.push({
    label: 'Skills',
    count: project.value.numSkills,
    secondaryStats: [{
      label: 'reused',
      count: project.value.numSkillsReused,
      badgeVariant: 'info',
    }, {
      label: 'disabled',
      count: project.value.numSkillsDisabled,
      badgeVariant: 'warning',
    }],
    icon: 'fas fa-graduation-cap skills-color-skills',
  });
  stats.push({
    label: 'Points',
    count: project.value.totalPoints,
    warnMsg: project.value.totalPoints < minimumPoints ? 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.' : null,
    icon: 'far fa-arrow-alt-circle-up skills-color-points',
    secondaryStats: [{
      label: 'reused',
      count: project.value.totalPointsReused,
      badgeVariant: 'info',
    }],
  });
  stats.push({
    label: 'Badges',
    count: project.value.numBadges,
    icon: 'fas fa-award skills-color-badges',
  });

  if (!isReadOnlyProj) {
    stats.push({
      label: 'Issues',
      count: project.value.numErrors,
      icon: 'fas fa-exclamation-triangle',
    });
  }

  return {
    icon: 'fas fa-list-alt skills-color-projects',
    title: `PROJECT: ${project.value.name}`,
    subTitle: `ID: ${project.value.projectId}`,
    stats,
  };
});

const minimumPoints = computed(() => {
  return store.getters.config.minimumProjectPoints;
});
const expirationDate = computed(() => {
  if (!project.value.expiring) {
    return '';
  }
  const gracePeriodInDays = store.getters.config.expirationGracePeriod;
  const expires = dayjs(project.value.expirationTriggered).add(gracePeriodInDays, 'day').startOf('day');
  return expires.format('YYYY-MM-DD HH:mm');
});

// ...mapActions([
//   'loadProjectDetailsState',
// ]),
// ...mapMutations([
//   'setProject',
// ]),

const loadProjectDetailsState = (payload) => {
  return new Promise((resolve, reject) => {
    ProjectService.getProjectDetails(payload.projectId)
        .then((response) => {
          // commit('setProject', response);
          setProject(response);
          resolve(response);
        })
        .catch((error) => reject(error));
  });
};

const copyAndDisplayShareProjInfo = () => {
  const host = window.location.origin;
  shareUrl = `${host}/progress-and-rankings/projects/${project.value.projectId}?invited=true`;
  navigator.clipboard.writeText(shareUrl).then(() => {
    shareProjModal = true;
  });
};

const fromExpirationDate = () => {
  return dayjs().startOf('day').to(dayjs(expirationDate));
};
const displayEditProject = () => {
  editProject = true;
};
const loadProjects = () => {
  isLoadingData.value = true;
  if (route.params.project) {
    setProject(route.params.project);
    isLoadingData.value = false;
  } else {
    loadProjectDetailsState({ projectId: route.params.projectId })
        .finally(() => {
          isLoadingData.value = false;
        });
  }
};
const editProjectHidden = () => {
  editProject = false;
  nextTick(() => {
    const ref = this.$refs.editProjectButton;
    if (ref) {
      ref.focus();
    }
  });
};
const focusOnShareButton = () => {
  nextTick(() => {
    const ref = this.$refs.shareProjectButton;
    if (ref) {
      ref.focus();
    }
  });
};
const projectSaved = (updatedProject) => {
  ProjectService.saveProject(updatedProject).then((resp) => {
    const origProjId = project.value.projectId;
    setProject(resp);
    if (resp.projectId !== origProjId) {
      this.$router.replace({ name: route.name, params: { ...route.params, projectId: resp.projectId } });
      projectId = resp.projectId;
    }
    store.dispatch('loadProjConfigState', { projectId: resp.projectId, updateLoadingVar: false });
    nextTick(() => {
      // this.$announcer.polite(`Project ${updatedproject.value.name} has been edited`);
    });
  });
};
const keepIt = () => {
  cancellingExpiration = true;
  ProjectService.cancelUnusedProjectDeletion(route.params.projectId).then(() => {
    loadProjects();
  }).finally(() => {
    cancellingExpiration = false;
  });
};

const setProject = (newProject) => {
  project.value = newProject;
};

</script>

<template>
  <div ref="mainFocus">
    <PageHeader :loading="isLoading" :options="headerOptions">
      <div slot="banner" v-if="project && project.expiring && !isReadOnlyProj" data-cy="projectExpiration"
           class="w-100 text-center alert-danger p-2 mb-3">
          <span class="mr-2"
                aria-label="This Project has not been used recently, it will  be deleted unless you explicitly retain it"
                v-tooltip="'This Project has not been used recently, it will  be deleted unless you explicitly retain it'">
            Project has not been used in over <b>{{ store.getters.config.expireUnusedProjectsOlderThan }} days</b> and will be deleted <b>{{
              fromExpirationDate()
            }}</b>.
          </span>
        <Button @click="keepIt" data-cy="keepIt" size="small" variant="alert"
                  :aria-label="'Keep Project '+ project.value.name">
          <span class="d-none d-sm-inline">Keep It</span>
          <SkillsSpinner v-if="cancellingExpiration" small style="font-size:1rem"/>
          <i v-if="!cancellingExpiration" :class="'fas fa-shield-alt'" style="font-size: 1rem;" aria-hidden="true"/>
        </Button>
      </div>
      <template #subTitle v-if="project">
        <div v-if="project.userCommunity" class="mb-3" data-cy="userCommunity">
          <span class="border p-1 border-danger rounded"><i
              class="fas fa-shield-alt text-danger" aria-hidden="true"/></span> <span
            class="text-secondary font-italic ml-1">{{ beforeCommunityLabel }}</span> <span
            class="font-weight-bold text-primary">{{ project.userCommunity }}</span> <span
            class="text-secondary font-italic">{{ afterCommunityLabel }}</span>
        </div>
        <div class="h6"><span class="border p-1 border-info rounded mr-1 mb-2">
          <i class="fas fa-fingerprint text-info" aria-hidden="true"/></span> <span class="font-italic text-muted">Project ID</span>: {{ project.projectId }}</div>
      </template>
      <template #subSubTitle v-if="project">
        <span class="p-buttonset mr-2" v-if="!isReadOnlyProj">
          <Button @click="displayEditProject"
                    ref="editProjectButton"
                    class="border-1 border-black-alpha-90"
                    size="small"
                    data-cy="btn_edit-project"
                    :aria-label="'edit Project '+project.projectId">
            <span>Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
          </Button>
          <Button target="_blank" v-if="project" :to="{ name:'MyProjectSkills', params: { projectId: project.projectId } }"
                    data-cy="projectPreview" size="small"
                   class="border-1 border-black-alpha-90" :aria-label="'preview client display for project'+project.name">
<!--            v-skills="'PreviewProjectClientDisplay'" -->
            <span>Preview</span> <i class="fas fa-eye" style="font-size:1rem;" aria-hidden="true"/>
          </Button>
          <Button v-if="isProjConfigDiscoverable"
                    ref="shareProjectButton"
                    size="small"
                    @click="copyAndDisplayShareProjInfo"
                    data-cy="shareProjBtn"
                    class="border-1 border-black-alpha-90"
                    :aria-label="`Share ${project.name} with new users`">
<!--            v-skills="'ShareProject'" -->
            <span>Share</span> <i class="fas fa-share-alt" style="font-size:1rem;" aria-hidden="true"/>
          </Button>
        </span>
        <div data-cy="projectCreated">
          <i class="fas fa-clock text-success header-status-icon" aria-hidden="true" /> <ProjectDates :created="project.created" :load-last-reported-date="true"/>
        </div>
<!--        <div v-if="userProjRole">-->
<!--          <i class="fas fa-user-shield text-success header-status-icon" aria-hidden="true" /> <span class="text-secondary font-italic small">Role:</span> <span class="small text-primary" data-cy="userRole">{{ userProjRole | userRole }}</span>-->
<!--        </div>-->
      </template>
<!--      <div slot="footer">-->
<!--        <import-finalize-alert />-->
<!--      </div>-->
    </PageHeader>

    <Navigation v-if="!isLoading" :nav-items="navItems">
    </Navigation>

<!--    <edit-project v-if="editProject" v-model="editProject" :project="project" :is-edit="true"-->
<!--                  @project-saved="projectSaved" @hidden="editProjectHidden"/>-->
<!--    <project-share-modal v-if="shareProjModal" v-model="shareProjModal"-->
<!--                         :share-url="shareUrl"-->
<!--                         @hidden="focusOnShareButton"/>-->
  </div>

</template>

<style scoped></style>
