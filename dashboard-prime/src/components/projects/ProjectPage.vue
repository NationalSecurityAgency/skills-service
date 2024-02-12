<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { createNamespacedHelpers, useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue'
import Navigation from '@/components/utils/Navigation.vue'
import ProjectService from '@/components/projects/ProjectService'
import ProjectDates from '@/components/projects/ProjectDates.vue'
import dayjs from '@/common-components/DayJsCustomizer.js'
import EditProject from '@/components/projects/EditProject.vue'

// const props = defineProps(['project'])
const store = useStore();
const router = useRouter()
const route = useRoute();
const projConfig = useProjConfig()
const announcer = useSkillsAnnouncer()
const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('projects');

const isLoadingData = ref(true);
const cancellingExpiration = ref(false);
const editProject = ref(false);
const shareProjModal = ref(false);
const shareUrl = ref('');
const project = ref({});
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
  if (!project.value || !projConfig) {
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
const loadProjects = () => {
  isLoadingData.value = true
  if (route.params.project) {
    setProject(route.params.project)
    isLoadingData.value = false
  } else {
    loadProjectDetailsState({ projectId: route.params.projectId })
      .finally(() => {
        isLoadingData.value = false
      })
  }
}
const projectSaved = (updatedProject) => {
    const origProjId = project.value.projectId;
    setProject(updatedProject);
    if (updatedProject.projectId !== origProjId) {
      router.replace({ name: route.name, params: { ...route.params, projectId: updatedProject.projectId } })
        .then(() =>{
          // projConfig.loadProjConfigState({ projectId: updatedProject.projectId, updateLoadingVar: false })
        });
    }
    announcer.polite(`Project ${updatedProject.name} has been edited`);
};
const keepIt = () => {
  cancellingExpiration.value = true;
  ProjectService.cancelUnusedProjectDeletion(route.params.projectId).then(() => {
    loadProjects();
  }).finally(() => {
    cancellingExpiration.value= false;
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
        <SkillsButton @click="keepIt" data-cy="keepIt" size="small" variant="alert"
                  :aria-label="'Keep Project '+ project.value.name" label="Keep It" :icon="!cancellingExpiration ? 'fas fa-shield-alt' : ''">
        </SkillsButton>
      </div>
      <template #subTitle v-if="project">
        <div v-if="project.userCommunity" class="mb-3" data-cy="userCommunity">
          <span class="border p-1 border-danger rounded"><i
              class="fas fa-shield-alt text-danger" aria-hidden="true"/></span> <span
            class="text-secondary font-italic ml-1">{{ beforeCommunityLabel }}</span> <span
            class="font-weight-bold text-primary">{{ project.userCommunity }}</span> <span
            class="text-secondary font-italic">{{ afterCommunityLabel }}</span>
        </div>
        <div class="">
          <span class="border-1 border-round px-1 mr-2">
           <i class="fas fa-fingerprint" aria-hidden="true"/>
          </span>
            <span class="font-italic text-color-secondary">Project ID</span>: {{ project.projectId }}
        </div>
      </template>
      <template #subSubTitle v-if="project">
        <div v-if="!isReadOnlyProj">
          <SkillsButton
            id="editProjectBtn"
            @click="editProject = true"
            ref="editProjectButton"
            size="small"
            outlined
            severity="info"
            :track-for-focus="true"
            data-cy="btn_edit-project"
            label="Edit"
            icon="fas fa-edit"
            :aria-label="`edit Project ${project.name}`">
          </SkillsButton>
          <router-link
            class="ml-1"
            data-cy="projectPreview"
            :to="{ name:'MyProjectSkills', params: { projectId: project.projectId } }"
            target="_blank" rel="noopener">
            <SkillsButton
              target="_blank"
              v-if="project"
              outlined
              severity="info"

              size="small"
              label="Preview"
              icon="fas fa-eye"
              :aria-label="`preview client display for project ${project.name}`">
              <!--            v-skills="'PreviewProjectClientDisplay'" -->
            </SkillsButton>
          </router-link>
<!--          <SkillsButton v-if="isProjConfigDiscoverable"-->
<!--                    ref="shareProjectButton"-->
<!--                    size="small"-->
<!--                    @click="copyAndDisplayShareProjInfo"-->
<!--                    data-cy="shareProjBtn"-->
<!--                    class="border-1 border-black-alpha-90"-->
<!--                    label="Share" icon="fas fa-share-alt"-->
<!--                    :aria-label="`Share ${project.name} with new users`">-->
<!--&lt;!&ndash;            v-skills="'ShareProject'" &ndash;&gt;-->
<!--          </SkillsButton>-->
        </div>
        <div data-cy="projectCreated" class="mt-3">
          <i class="fas fa-clock text-success mr-1" aria-hidden="true" />
          <ProjectDates :created="project.created" :load-last-reported-date="true"/>
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

    <edit-project
      v-if="editProject"
      v-model="editProject"
      :is-edit="true"
      :id="`editProjectModal${project.projectId}`"
      :project="project"
      :enable-return-focus="true"
      @project-saved="projectSaved"/>

<!--    <edit-project v-if="editProject" v-model="editProject" :project="project" :is-edit="true"-->
<!--                  @project-saved="projectSaved" @hidden="editProjectHidden"/>-->
<!--    <project-share-modal v-if="shareProjModal" v-model="shareProjModal"-->
<!--                         :share-url="shareUrl"-->
<!--                         @hidden="focusOnShareButton"/>-->
  </div>

</template>

<style scoped></style>
