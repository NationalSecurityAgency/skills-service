<script setup>
import { onMounted, computed, ref } from 'vue';
import Badge from 'primevue/badge';
import Avatar from 'primevue/avatar';
import Card from 'primevue/card';
import ProjectService from '@/components/projects/ProjectService';
import ProjectCardFooter from '@/components/projects/ProjectCardFooter.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import UserRolesUtil from '@/components/utils/UserRolesUtil';
import dayjs from "../../../../common-components/src/common/DayJsCustomizer.js";

const props = defineProps(['project', 'disableSortControl'])

// data items
let isLoading = ref(false);
let pinned = ref(false);
let projectInternal = ref({ ...props.project });
let stats = ref([]);
let showEditProjectModal = ref(false);
let deleteProjectDisabled = ref(false);
let deleteProjectToolTip = ref('');
let cancellingExpiration = ref(false);
let deleteProjectInfo = {
  showDialog: false,
  project: {},
};
let copyProjectInfo = {
  showModal: false,
  newProject: {},
};

onMounted(() => {
  pinned = projectInternal.pinned;
  createCardOptions();
});

// computed
const minimumPoints = computed(() => {
  return store.getters.config.minimumProjectPoints;
});
const isRootUser = computed(() => {
  return store.getters['access/isRoot'];
});
const expirationDate = computed(() => {
  if (!projectInternal.expiring) {
    return '';
  }
  const gracePeriodInDays = store.getters.config.expirationGracePeriod;
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
const handleHidden = () => {
  this.$nextTick(() => {
    this.$refs.cardControls.focusOnEdit();
  });
};
const handleCopyModalIsHidden = () => {
  this.$nextTick(() => {
    if (this.$refs && this.$refs.cardControls) {
      this.$refs.cardControls.focusOnCopy();
    }
  });
};
const createCardOptions = () => {
  stats = [{
    label: 'Subjects',
    count: projectInternal.numSubjects,
    icon: 'fas fa-cubes skills-color-subjects',
  }, {
    label: 'Skills',
    count: projectInternal.numSkills,
    icon: 'fas fa-graduation-cap skills-color-skills',
    secondaryStats: [{
      label: 'reused',
      count: projectInternal.numSkillsReused,
      badgeVariant: 'info',
    }, {
      label: 'disabled',
      count: projectInternal.numSkillsDisabled,
      badgeVariant: 'warning',
    }],
  }, {
    label: 'Points',
    count: projectInternal.totalPoints,
    warn: (projectInternal.totalPoints + projectInternal.totalPointsReused) < minimumPoints,
    warnMsg: 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.',
    icon: 'far fa-arrow-alt-circle-up skills-color-points',
    secondaryStats: [{
      label: 'reused',
      count: projectInternal.totalPointsReused,
      badgeVariant: 'info',
    }],
  }, {
    label: 'Badges',
    count: projectInternal.numBadges,
    icon: 'fas fa-award skills-color-badges',
  }];
};
const checkIfProjectBelongsToGlobalBadge = () => {
  ProjectService.checkIfProjectBelongsToGlobalBadge(projectInternal.projectId)
      .then((res) => {
        if (res) {
          deleteProjectDisabled = true;
          deleteProjectToolTip = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
        }
      });
};
const doDeleteProject = () => {
  ProjectService.checkIfProjectBelongsToGlobalBadge(deleteProjectInfo.project.projectId)
      .then((belongsToGlobal) => {
        if (belongsToGlobal) {
          const msg = 'Cannot delete this project as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
          msgOk(msg, 'Unable to delete');
        } else {
          this.$emit('project-deleted', deleteProjectInfo.project);
        }
      });
};
const deleteProject = () => {
  deleteProjectInfo.project = projectInternal;
  deleteProjectInfo.showDialog = true;
};
const editProject = () => {
  showEditProjectModal = true;
};
const copyProject = () => {
  copyProjectInfo.newProject = { userCommunity: project.userCommunity };
  copyProjectInfo.showModal = true;
};
const projectCopied = (project) => {
  this.$emit('copy-project', {
    originalProjectId: projectInternal.projectId,
    newProject: project,
  });
};
const projectSaved = (project) => {
  isLoading = true;
  ProjectService.saveProject(project)
      .then((res) => {
        projectInternal = res;
        pinned = projectInternal.pinned;
        createCardOptions();
        this.$announcer.polite(`Project ${project.name} has been successfully edited`);
      })
      .finally(() => {
        isLoading = false;
      });
};
const unpin = () => {
  SettingsService.unpinProject(projectInternal.projectId)
      .then(() => {
        projectInternal.pinned = false;
        pinned = false;
        this.$emit('pin-removed', projectInternal);
      });
};
const keepIt = () => {
  cancellingExpiration = true;
  ProjectService.cancelUnusedProjectDeletion(projectInternal.projectId)
      .then(() => {
        projectInternal.expiring = false;
      })
      .finally(() => {
        cancellingExpiration = false;
      });
};
const moveDown = () => {
  this.$emit('sort-changed-requested', {
    projectId: project.projectId,
    direction: 'down',
  });
};
const moveUp = () => {
  this.$emit('sort-changed-requested', {
    projectId: project.projectId,
    direction: 'up',
  });
};
const focusSortControl = () => {
  this.$refs.sortControl.focus();
};
const handleDeleteCancelled = () => {
  this.$refs.cardControls.focusOnDelete();
};
</script>

<template>
  <div data-cy="projectCard" class="h-100">
    <Card :data-cy="`projectCard_${projectInternal.projectId}`">
      <template #content>
        <div class="grid mb-2">
          <div class="col-md text-truncate">
            <router-link
                :to="{ label:'Subjects', params: { projectId: projectInternal.projectId, project: projectInternal }, route: '/'}"
                class="text-green-800 no-underline mb-0 pb-0 preview-card-title" :title="`${projectInternal.name}`"
                :aria-label="`manage project ${projectInternal.name}`"
                role="link"
                :data-cy="`projCard_${projectInternal.projectId}_manageLink`">
              <Avatar variant="info" icon="people-fill" class="uppercase avatar-link" aria-hidden="true" shape="circle">
                {{ projectInternal.name.substring(0, 2) }}
              </Avatar>
              {{ projectInternal.name }}
            </router-link>
            <div v-if="projectInternal.userCommunity" class="my-2" data-cy="userCommunity">
              <span class="border p-1 border-danger rounded"><i
                  class="fas fa-shield-alt text-danger" aria-hidden="true"/></span> <span
                class="text-secondary font-italic ml-1">{{ beforeCommunityLabel }}</span> <span
                class="font-weight-bold text-primary">{{ projectInternal.userCommunity }}</span> <span
                class="text-secondary font-italic">{{ afterCommunityLabel }}</span>
            </div>
          </div>
          <div class="col-md-auto mt-3 mt-md-0">
<!--            <project-card-controls-->
<!--                :class="{ 'mr-md-4': !disableSortControl}"-->
<!--                ref="cardControls"-->
<!--                :project="projectInternal"-->
<!--                @edit-project="editProject"-->
<!--                @copy-project="copyProject"-->
<!--                @delete-project="deleteProject"-->
<!--                @unpin-project="unpin"-->
<!--                :read-only-project="isReadOnlyProj"-->
<!--                :is-delete-disabled="deleteProjectDisabled"-->
<!--                :delete-disabled-text="deleteProjectToolTip"/>-->
          </div>
        </div>

        <div class="grid text-center justify-content-center">
          <div v-for="(stat) in stats" :key="stat.label" class="col mt-1" style="min-width: 10rem;">
            <div :data-cy="`pagePreviewCardStat_${stat.label}`" class="border rounded stat-card h-100">
              <i :class="stat.icon"></i>
              <div class="uppercase text-muted count-label">{{ stat.label }}</div>
              <strong class="text-2xl font-normal" data-cy="statNum">{{ stat.count | number }}</strong>
              <i v-if="stat.warn" class="fas fa-exclamation-circle text-warning ml-1"
                 style="font-size: 1.5rem;"
                 v-tooltip.hover="stat.warnMsg"
                 data-cy="warning"
                 role="alert"
                 :aria-label="`Warning: ${stat.warnMsg}`"/>

              <div v-if="stat.secondaryStats">
                <div v-for="secCount in stat.secondaryStats" :key="secCount.label">
                  <div v-if="secCount.count > 0" style="font-size: 0.9rem">
                    <Badge :variant="`${secCount.badgeVariant}`"
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
               'it will  be deleted unless you explicitly retain it'">Project has not been used in over <b>{{store.getters.config.expireUnusedProjectsOlderThan}} days</b> and will be deleted <b>{{ fromExpirationDate() }}</b>.</span>
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

<!--    <removal-validation v-if="deleteProjectInfo.showDialog" v-model="deleteProjectInfo.showDialog"-->
<!--                        @do-remove="doDeleteProject" @hidden="handleDeleteCancelled">-->
<!--      <p>-->
<!--        This will remove <span-->
<!--          class="text-primary font-weight-bold">{{ deleteProjectInfo.project.name }}</span>.-->
<!--      </p>-->
<!--      <div>-->
<!--        Deletion can not be undone and permanently removes all skill subject definitions, skill-->
<!--        definitions and users' performed skills for this Project.-->
<!--      </div>-->
<!--    </removal-validation>-->
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

.preview-card-title {
  font-size: 1.4rem;
  font-weight: bold;
}

.preview-card-subTitle {
  font-size: 0.8rem;
}

.count-label {
  font-size: 0.9rem;
}

.avatar-link i {
  font-size: 2.5rem;
  display: inline-block;
}

.stat-card {
  background-color: #f8f9fa;
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