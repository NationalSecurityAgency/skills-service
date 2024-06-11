<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import Badge from 'primevue/badge'
import Avatar from 'primevue/avatar'
import Card from 'primevue/card'
import ProjectService from '@/components/projects/ProjectService'
import SettingsService from '@/components/settings/SettingsService'
import ProjectCardFooter from '@/components/projects/ProjectCardFooter.vue'
import ProjectCardControls from '@/components/projects/ProjectCardControls.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import UserRolesUtil from '@/components/utils/UserRolesUtil'
import dayjs from '@/common-components/DayJsCustomizer.js'
import EditProject from '@/components/projects/EditProject.vue'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'
import { useAccessState } from '@/stores/UseAccessState.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import ReminderMessage from '@/components/utils/misc/ReminderMessage.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import ProjectExpirationWarning from '@/components/projects/ProjectExpirationWarning.vue'
import { useAdminProjectsState } from '@/stores/UseAdminProjectsState.js'

const props = defineProps(['project', 'disableSortControl'])
const appConfig = useAppConfig()
const accessState = useAccessState()
const emit = defineEmits(['project-deleted', 'copy-project', 'pin-removed', 'sort-changed-requested'])
const numberFormat = useNumberFormat()
const projectsState = useAdminProjectsState()
const announcer = useSkillsAnnouncer()

// data items
const pinned = ref(false);
const projectInternal = computed(() => props.project );
const stats = ref([]);
const showEditProjectModal = ref(false);
const showCopyProjectModal = ref(false);
const deleteProjectDisabled = ref(false);
const deleteProjectToolTip = ref('');
const cancellingExpiration = ref(false);
const showDeleteValidation = ref(false);
let overSortControl = ref(false);
const sortControl = ref();

let copyProjectInfo = ref({
  newProject: {},
});

onMounted(() => {
  pinned.value = projectInternal.value.pinned;
  createCardOptions();
});

// computed
const minimumPoints = computed(() => {
  return appConfig.minimumProjectPoints;
});
const isRootUser = computed(() => {
  return accessState.isRoot;
});
const isReadOnlyProj = computed(() => {
  return UserRolesUtil.isReadOnlyProjRole(projectInternal.value.userRole);
});

// methods
const createCardOptions = () => {
  stats.value = [{
    label: 'Subjects',
    count: projectInternal.value.numSubjects,
    icon: 'fas fa-cubes skills-color-subjects',
  }, {
    label: 'Skills',
    count: projectInternal.value.numSkills,
    icon: 'fas fa-graduation-cap skills-color-skills',
    secondaryStats: [{
      label: 'reused',
      count: projectInternal.value.numSkillsReused,
      badgeVariant: 'info',
    }, {
      label: 'disabled',
      count: projectInternal.value.numSkillsDisabled,
      badgeVariant: 'warning',
    }],
  }, {
    label: 'Points',
    count: projectInternal.value.totalPoints,
    icon: 'far fa-arrow-alt-circle-up skills-color-points',
    secondaryStats: [{
      label: 'reused',
      count: projectInternal.value.totalPointsReused,
      badgeVariant: 'info',
    }],
  }, {
    label: 'Badges',
    count: projectInternal.value.numBadges,
    icon: 'fas fa-award skills-color-badges',
  }];
};
const warningMsgAboutPoints = computed(() => {
  const shouldWarn = (projectInternal.value.totalPoints + projectInternal.value.totalPointsReused) < minimumPoints.value
  if (!shouldWarn) {
    return null
  }
  return `Project has insufficient points assigned. Skills cannot be achieved until project has at least ${minimumPoints.value} points.`
})

const checkIfProjectBelongsToGlobalBadge = () => {
  ProjectService.checkIfProjectBelongsToGlobalBadge(projectInternal.value.projectId)
      .then((res) => {
        if (res) {
          deleteProjectDisabled.value = true;
          deleteProjectToolTip.value = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
        }
      });
};
const doDeleteProject = () => {
  ProjectService.checkIfProjectBelongsToGlobalBadge(projectInternal.value.projectId)
      .then((belongsToGlobal) => {
        if (belongsToGlobal) {
          const msg = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
          // msgOk(msg, 'Unable to delete');
        } else {
          emit('project-deleted', projectInternal.value);
        }
      });
};

const copyProject = () => {
  copyProjectInfo.value.newProject = { userCommunity: props.project.userCommunity };
  showCopyProjectModal.value = true;
};
const projectCopied = (project) => {
  emit('copy-project', {
    originalProjectId: projectInternal.value.projectId,
    newProject: project,
  });
};

const createOrUpdateProject = inject('createOrUpdateProject')

const unpin = () => {
  SettingsService.unpinProject(projectInternal.value.projectId)
      .then(() => {
        projectInternal.value.pinned = false;
        pinned.value = false;
        emit('pin-removed', projectInternal);
      });
};
const moveDown = () => {
  emit('sort-changed-requested', {
    projectId: projectInternal.value.projectId,
    direction: 'down',
  });
};
const moveUp = () => {
  emit('sort-changed-requested', {
    projectId: projectInternal.value.projectId,
    direction: 'up',
  });
};

const focusSortControl = () => {
  sortControl.value.focus();
};

defineExpose({
  focusSortControl
});
</script>

<template>
  <div data-cy="projectCard" class="h-100">
    <Card :data-cy="`projectCard_${projectInternal.projectId}`" class="relative">
      <template #content>
        <div class="flex flex-wrap"
             :class="{
            'flex-column gap-1 justify-content-left': projectsState.shouldTileProjectsCards,
            '': !projectsState.shouldTileProjectsCards
          }">
          <div class="text-truncate">
            <router-link
                :to="{ name:'Subjects', params: { projectId: projectInternal.projectId }}"
                class="no-underline mb-0 pb-0" :title="`${projectInternal.name}`"
                :aria-label="`manage project ${projectInternal.name}`"
                role="link"
                :data-cy="`projCard_${projectInternal.projectId}_manageLink`">
              <Avatar
                class="uppercase"
                size="large"
                aria-hidden="true"
                shape="circle">
                {{ projectInternal.name.substring(0, 2) }}
              </Avatar>
              <span class="text-2xl font-bold ml-2">{{ projectInternal.name }}</span>
            </router-link>
            <div v-if="projectInternal.userCommunity" class="my-2" data-cy="userCommunity">
              <Avatar icon="fas fa-shield-alt" class="text-red-500"></Avatar>
              <span
                class="text-secondary font-italic ml-1">{{ appConfig.userCommunityBeforeLabel }}</span> <span
                class="font-weight-bold text-primary">{{ projectInternal.userCommunity }}</span> <span
                class="text-secondary font-italic">{{ appConfig.userCommunityAfterLabel }}</span>
            </div>
          </div>
          <div class="flex-1">
            <ProjectCardControls
                :class="{ 'mr-md-4': !disableSortControl}"
                ref="cardControls"
                :project="projectInternal"
                @edit-project="createOrUpdateProject(project, true)"
                @copy-project="copyProject"
                @delete-project="showDeleteValidation = true"
                @unpin-project="unpin"
                :read-only-project="isReadOnlyProj"
                :is-delete-disabled="deleteProjectDisabled"
                :delete-disabled-text="deleteProjectToolTip"/>
          </div>
        </div>

        <div class="grid text-center justify-content-center mt-2">
          <div v-for="(stat) in stats" :key="stat.label" class="col mt-1" style="min-width: 10rem;">
            <div :data-cy="`pagePreviewCardStat_${stat.label}`" class="h-full border-round border-1 border-300 stat-card surface-100">
              <i :class="stat.icon" aria-hidden="true" class="text-xl text-primary"/>
              <div class="uppercase">{{ stat.label }}</div>
              <div class="text-2xl mt-1 font-semibold" data-cy="statNum">{{ numberFormat.pretty(stat.count) }}</div>
              <i v-if="stat.warn" class="fas fa-exclamation-circle text-yellow-400 ml-1"
                 style="font-size: 1.5rem;"
                 v-tooltip.hover="stat.warnMsg"
                 data-cy="warning"
                 role="alert"
                 :aria-label="`Warning: ${stat.warnMsg}`"/>

              <div v-if="stat.secondaryStats">
                <div v-for="secCount in stat.secondaryStats" :key="secCount.label">
                  <div v-if="secCount.count > 0" style="font-size: 0.9rem">
                    <Badge :severity="`${secCount.badgeVariant}`"
                             :data-cy="`pagePreviewCardStat_${stat.label}_${secCount.label}`">
                      <span>{{ numberFormat.pretty(secCount.count) }}</span>
                    </Badge>
                    <span class="text-left uppercase ml-1"
                          style="font-size: 0.8rem">{{ secCount.label }}</span>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>

        <div class="text-center mt-1">
          <ProjectCardFooter class="mt-4" :project="projectInternal"/>
        </div>

        <project-expiration-warning :project="projectInternal" @extended="projectInternal.expiring = false" />
        <ReminderMessage
          v-if="warningMsgAboutPoints"
          :id="`projectCardWarning_${projectInternal.projectId}`"
          data-cy="projectCardWarning"
          severity="info">{{ warningMsgAboutPoints}}</ReminderMessage>

        <div v-if="!disableSortControl"
             :id="`sortControl_${project.projectId}`"
             ref="sortControl"
             @mouseover="overSortControl = true"
             @mouseleave="overSortControl = false"
             @keyup.down="moveDown"
             @keyup.up="moveUp"
             @click.prevent.self
             class="absolute text-secondary px-2 py-1 sort-control"
             tabindex="0"
             :aria-label="`Project Sort Control. Current position for ${project.name} project is ${project.displayOrder}. Press up or down to change the order of the project.`"
             role="button"
             data-cy="sortControlHandle"><i class="fas fa-arrows-alt"></i></div>
      </template>
    </Card>

    <edit-project id="copyProjectModal" v-if="showCopyProjectModal"
                  v-model="showCopyProjectModal"
                  :project="copyProjectInfo.newProject"
                  :is-edit="false"
                  :is-copy="true"
                  @project-saved="projectCopied"
                  :enable-return-focus="true" />

    <removal-validation
      v-if="showDeleteValidation"
      v-model="showDeleteValidation"
      :item-name="projectInternal.name"
      item-type="project"
      @do-remove="doDeleteProject">
        Deletion <b>cannot</b> be undone and permanently removes all skill subject definitions, skill
        definitions and users' performed skills for this Project.
    </removal-validation>
  </div>
</template>

<style scoped>
.project-settings {
  position: relative;
  display: inline-block;
  float: right;
}
.buttons i {
  font-size: 0.9rem;
}

.preview-card-subTitle {
  font-size: 0.8rem;
}


.stat-card {
  padding: 1rem;
}

.sort-control {
  font-size: 1.3rem !important;
  color: #b3b3b3 !important;
  top: 0rem;
  right: 0rem;
  border-bottom: 1px solid #e8e8e8;
  border-left: 1px solid #e8e8e8;
  background-color: #fbfbfb !important;
  border-bottom-left-radius:.25rem!important
}

.sort-control:hover, .sort-control i:hover {
  font-size: 1.5rem;
}
</style>
