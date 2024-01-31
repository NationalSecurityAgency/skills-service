<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import Badge from 'primevue/badge';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillsService from '@/components/skills/SkillsService';
import PageHeader from '@/components/utils/pages/PageHeader.vue';
import Navigation from '@/components/utils/Navigation.vue';
import SkillReuseIdUtil from '@/components/utils/SkillReuseIdUtil';
import { useSkillsState } from '@/stores/UseSkillsState.js'

const route = useRoute();
const router = useRouter();
const store = useStore();
const announcer = useSkillsAnnouncer();

let isLoadingData = ref(true);
let subjectId = ref('');
let headerOptions = ref({});
let showEdit = ref(false);

let subject = ref(store.getters["subjects/subject"]);
// let skill = ref(store.getters["skills/skill"]);

const skillsState = useSkillsState();

onMounted(() => {
  loadData();
});

const isLoading = computed(() => {
  return isLoadingData.value; // || isLoadingProjConfig;
});

const navItems = computed(() => {
  if (isLoadingData.value) {
    return [];
  }
  const items = [];
  items.push({ name: 'Overview', iconClass: 'fa-info-circle skills-color-overview', page: 'SkillOverview' });
  items.push({ name: 'Video', iconClass: 'fa-video skills-color-video', page: 'ConfigureVideo' });
  items.push({ name: 'Expiration', iconClass: 'fa-hourglass-end skills-color-expiration', page: 'ConfigureExpiration' });
  items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SkillUsers' });
  const isReadOnlyNonSr = (skillsState.skill.readOnly === true && !skillsState.skill.selfReportType);
  const addEventDisabled = subject.totalPoints < store.getters.config.minimumSubjectPoints || isReadOnlyNonSr;

  let msg = addEventDisabled ? `Subject needs at least ${store.getters.config.minimumSubjectPoints} points before events can be added` : '';
  const disabledDueToGroupBeingDisabled = skillsState.skill.groupId && !skillsState.skill.enabled;
  if (disabledDueToGroupBeingDisabled) {
    msg = `CANNOT report skill events because this skill belongs to a group whose current status is disabled. ${msg}`;
  }
  if (isReadOnlyNonSr) {
    msg = 'Skills imported from the catalog can only have events added if they are configured for Self Reporting';
  }
  if (!isImported && !isReadOnlyProj) {
    items.push({
      name: 'Add Event', iconClass: 'fa-user-plus skills-color-events', page: 'AddSkillEvent', isDisabled: addEventDisabled || disabledDueToGroupBeingDisabled || isReadOnlyNonSr, msg,
    });
  }
  items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SkillMetrics' });
  return items;
});

const isImported = computed(() => {
  return skillsState.skill && skillsState.skill.copiedFromProjectId && skillsState.skill.copiedFromProjectId.length > 0;
});

// Methods
const displayEdit = () => {
  // should only enable edit button if dirty, isn't currently
  showEdit.value = true;
};

const loadData = () => {
  isLoadingData.value = true;
  const { projectId, subjectId } = route.params;
  skillsState.loadSkill({
    projectId: route.params.projectId,
    subjectId: route.params.subjectId,
    skillId: route.params.skillId,
  }).then(() => {
    headerOptions.value = buildHeaderOptions(skillsState.skill);
    if (subject.value) {
      isLoadingData.value = false;
    } else {
      store.dispatch('subjects/loadSubjectDetailsState', {
        projectId,
        subjectId,
      }).then(() => {
        // subject = store.getters["subjects/subject"];
        isLoadingData.value = false;
      });
    }
  });
};

const skillEdited = (editedSkil) => {
  isLoadingData.value = true;
  SkillsService.saveSkill(editedSkil).then((res) => {
    const origId = skill.skillId;
    const edited = Object.assign(res, { subjectId: route.params.subjectId });
    skillsState.skill = edited;
    if (origId !== skillsState.skill.skillId) {
      router.replace({ name: route.name, params: { ...route.params, skillId: skillsState.skill.skillId } });
    }
    headerOptions.value = buildHeaderOptions(res);
  }).then(() => {
    nextTick(() => announcer.polite(`Skill ${editedSkil.name} has been edited`));
  })
      .catch((err) => {
        if (err && err.response && err.response.data.errorCode === 'MaxSkillsThreshold') {
          // msgOk(err.response.data.explanation, 'Maximum Skills Reached');
        } else if (err?.response?.data?.errorCode === 'DbUpgradeInProgress') {
          router.push({ name: 'DbUpgradeInProgressPage' });
        } else {
          throw err;
        }
      })
      .finally(() => {
        isLoadingData.value = false;
        handleFocus();
      });
};

const handleHide = (e) => {
  showEdit.value = false;
  if (!e?.saved) {
    handleFocus();
  }
};

const handleFocus = () => {
  nextTick(() => {
    // const ref = $refs.editSkillInPlaceBtn;
    // if (ref) {
    //   ref.focus();
    // }
  });
};

const buildHeaderOptions = (skill) => {
  const skillId = skill?.skillId ? SkillReuseIdUtil.removeTag(skill.skillId) : '';
  return {
    icon: 'fas fa-graduation-cap skills-color-skills',
    title: `SKILL: ${skill?.name}`,
    subTitle: `ID: ${skillId} | GROUP ID: ${skill?.groupId}`,
    stats: [{
      label: 'Points',
      count: skill?.totalPoints,
      icon: 'far fa-arrow-alt-circle-up skills-color-points',
    }],
  };
};

const getSkillId = (skill) => {
  return skill ? `ID: ${SkillReuseIdUtil.removeTag(skill.skillId)}` : 'Loading...';
};
</script>

<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <div slot="subTitle" v-if="skillsState.skill">
        <div v-for="(tag) in skillsState.skill.tags" :key="tag.tagId" class="h6 mr-2 d-inline-block" :data-cy="`skillTag-${skillsState.skill.skillId}-${tag.tagId}`">
          <Badge variant="info">
            <span><i class="fas fa-tag"></i> {{ tag.tagValue }}</span>
          </Badge>
        </div>
        <div class="h5 text-muted" data-cy="skillId">
<!--          <show-more :limit="54" :text="getSkillId(skill)"></show-more>-->
        </div>
        <div class="h5 text-muted" v-if="skillsState.skill && skillsState.skill.groupId">
          <span style="font-size: 1rem">Group ID:</span> <span v-tooltip="`Name: ${ skillsState.skill.groupName }`">{{ skillsState.skill.groupId }}</span>
        </div>
      </div>
      <div slot="subSubTitle" v-if="!isImported">
        <div class="p-buttonset">
          <SkillsButton v-if="skillsState.skill && projConfig && !isReadOnlyProj" @click="displayEdit"
                    size="small" label="Edit" icon="fas fa-edit"
                    variant="outline-primary" :data-cy="`editSkillButton_${route.params.skillId}`"
                    :aria-label="'edit Skill '+skillsState.skill.name" ref="editSkillInPlaceBtn">
          </SkillsButton>
        </div>
      </div>
      <div slot="right-of-header" v-if="!isLoading && (skillsState.skill.sharedToCatalog || isImported)"
           class="d-inline h5">
        <Badge v-if="skillsState.skill.sharedToCatalog" class="ml-2" data-cy="exportedBadge"><i
            class="fas fa-book"></i> EXPORTED
        </Badge>
        <Badge v-if="isImported" class="ml-2" variant="success" data-cy="importedBadge">
          <span v-if="skillsState.skill.reusedSkill"><i class="fas fa-recycle"></i> Reused</span>
          <span v-else><i class="fas fa-book"></i> IMPORTED</span>
        </Badge>
        <Badge v-if="!skillsState.skill.enabled" class="ml-2" data-cy="disabledSkillBadge"> DISABLED</Badge>
      </div>
    </page-header>
    <navigation :nav-items="navItems">
    </navigation>
<!--    <edit-skill v-if="showEdit" v-model="showEdit" :skillId="skill.skillId" :is-copy="false" :is-edit="true"-->
<!--                :project-id="route.params.projectId" :subject-id="route.params.subjectId" @skill-saved="skillEdited" @hidden="handleHide"/>-->
  </div>
</template>

<style scoped></style>
