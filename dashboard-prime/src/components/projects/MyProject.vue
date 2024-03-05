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

const props = defineProps(['project', 'disableSortControl'])
const appConfig = useAppConfig()
const accessState = useAccessState()
const emit = defineEmits(['project-deleted', 'copy-project', 'pin-removed', 'sort-changed-requested'])
const announcer = useSkillsAnnouncer()

// data items
let pinned = ref(false);
let projectInternal = ref({ ...props.project });
let stats = ref([]);
const showEditProjectModal = ref(false);
let deleteProjectDisabled = ref(false);
let deleteProjectToolTip = ref('');
let cancellingExpiration = ref(false);
const showDeleteValidation = ref(false)
let copyProjectInfo = {
  showModal: false,
  newProject: {},
};

onMounted(() => {
  pinned.value = projectInternal.pinned;
  createCardOptions();
});

// computed
const minimumPoints = computed(() => {
  return appConfig.minimumProjectPoints;
});
const isRootUser = computed(() => {
  return accessState.isRoot;
});
const expirationDate = computed(() => {
  if (!projectInternal.expiring) {
    return '';
  }
  const gracePeriodInDays = appConfig.expirationGracePeriod;
  const expires = dayjs(projectInternal.expirationTriggered).add(gracePeriodInDays, 'day').startOf('day');
  return expires.format('YYYY-MM-DD HH:mm');
});
const isReadOnlyProj = computed(() => {
  return UserRolesUtil.isReadOnlyProjRole(projectInternal.userRole);
});

// methods
const fromExpirationDate = () => {
  return dayjs()
      .startOf('day')
      .to(dayjs(expirationDate));
};
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
    warn: (projectInternal.value.totalPoints + projectInternal.value.totalPointsReused) < minimumPoints.value,
    warnMsg: 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.',
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
const checkIfProjectBelongsToGlobalBadge = () => {
  ProjectService.checkIfProjectBelongsToGlobalBadge(projectInternal.value.projectId)
      .then((res) => {
        if (res) {
          deleteProjectDisabled = true;
          deleteProjectToolTip = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
        }
      });
};
const doDeleteProject = () => {
  ProjectService.checkIfProjectBelongsToGlobalBadge(projectInternal.value.projectId)
      .then((belongsToGlobal) => {
        if (belongsToGlobal) {
          const msg = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
          msgOk(msg, 'Unable to delete');
        } else {
          emit('project-deleted', projectInternal.value);
        }
      });
};

const copyProject = () => {
  copyProjectInfo.newProject = { userCommunity: props.project.userCommunity };
  copyProjectInfo.showModal = true;
};
const projectCopied = (project) => {
  emit('copy-project', {
    originalProjectId: projectInternal.projectId,
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
const keepIt = () => {
  cancellingExpiration = true;
  ProjectService.cancelUnusedProjectDeletion(projectInternal.value.projectId)
      .then(() => {
        projectInternal.value.expiring = false;
      })
      .finally(() => {
        cancellingExpiration = false;
      });
};
const moveDown = () => {
  emit('sort-changed-requested', {
    projectId: project.projectId,
    direction: 'down',
  });
};
const moveUp = () => {
  emit('sort-changed-requested', {
    projectId: project.projectId,
    direction: 'up',
  });
};

</script>

<template>
  <div data-cy="projectCard" class="h-100">
    <Card :data-cy="`projectCard_${projectInternal.projectId}`">
      <template #content class="p-0">
        <div class="flex flex-wrap">
          <div class="text-truncate">
            <router-link
                :to="{ name:'Subjects', params: { projectId: projectInternal.projectId, project: projectInternal }}"
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
              <span class="border p-1 border-danger rounded"><i
                  class="fas fa-shield-alt text-danger" aria-hidden="true"/></span> <span
                class="text-secondary font-italic ml-1">{{ beforeCommunityLabel }}</span> <span
                class="font-weight-bold text-primary">{{ projectInternal.userCommunity }}</span> <span
                class="text-secondary font-italic">{{ afterCommunityLabel }}</span>
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
            <div :data-cy="`pagePreviewCardStat_${stat.label}`" class="border-round border-1 border-300 stat-card surface-100">
              <i :class="stat.icon"></i>
              <div class="uppercase text-muted count-label">{{ stat.label }}</div>
              <strong class="text-2xl font-normal" data-cy="statNum">{{ stat.count ? stat.count : '0' }}</strong>
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
                      <span>{{ secCount.count }}</span>
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

        <div v-if="projectInternal.expiring" data-cy="projectExpiration" class="w-100 text-center alert-danger p-2 mt-2">
              <span class="mr-2" v-tooltip="'This Project has not been used recently, ' +
               'it will  be deleted unless you explicitly retain it'">Project has not been used in over <b>{{appConfig.expireUnusedProjectsOlderThan}} days</b> and will be deleted <b>{{ fromExpirationDate() }}</b>.</span>
          <Button @click="keepIt" data-cy="keepIt" size="sm" variant="alert" :aria-label="'Keep Project '+ projectInternal.name">
            <span class="d-none d-sm-inline">Keep It</span> <SkillsSpinner v-if="cancellingExpiration" small style="font-size:1rem"/><i v-if="!cancellingExpiration" :class="'fas fa-shield-alt'" style="font-size: 1rem;" aria-hidden="true"/>
          </Button>
        </div>
      </template>

      <div v-if="!disableSortControl"
           :id="`sortControl_${project.projectId}`"
           ref="sortControl"
           @mouseover="overSortControl = true"
           @mouseleave="overSortControl = false"
           @keyup.down="moveDown"
           @keyup.up="moveUp"
           @click.prevent.self
           class="position-absolute text-secondary px-2 py-1 sort-control"
           tabindex="0"
           :aria-label="`Project Sort Control. Current position for ${project.name} project is ${project.displayOrder}. Press up or down to change the order of the project.`"
           role="button"
           data-cy="sortControlHandle"><i class="fas fa-arrows-alt"></i></div>
    </Card>

    <edit-project
      v-if="showEditProjectModal"
      v-model="showEditProjectModal"
      :is-edit="true"
      :id="`editProjectModal${projectInternal.projectId}`"
      :project="projectInternal"
      @project-saved="projectSaved"
      :enable-return-focus="true"/>

<!--    <edit-project id="editProjectModal" v-if="showEditProjectModal" v-model="showEditProjectModal"-->
<!--                  :project="projectInternal" :is-edit="true"-->
<!--                  @project-saved="projectSaved" @hidden="handleHidden"/>-->
<!--    <edit-project id="copyProjectModal" v-if="copyProjectInfo.showModal"-->
<!--                  v-model="copyProjectInfo.showModal"-->
<!--                  :project="copyProjectInfo.newProject"-->
<!--                  :is-edit="false"-->
<!--                  :is-copy="true"-->
<!--                  @project-saved="projectCopied"-->
<!--                  @hidden="handleCopyModalIsHidden"/>-->

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

.count-label {
  font-size: 0.9rem;
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
  cursor: grab !important;
  color: $info !important;
  font-size: 1.5rem;
}
</style>
