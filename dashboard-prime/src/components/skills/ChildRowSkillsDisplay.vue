<script setup>
import { computed, onMounted, ref } from 'vue'
import Badge from 'primevue/badge'
import Card from 'primevue/card'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import LoadingContainer from '@/components/utils/LoadingContainer.vue'
import SkillsService from '@/components/skills/SkillsService'
import SkillReuseIdUtil from '@/components/utils/SkillReuseIdUtil'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import { useTimeWindowFormatter} from '@/components/skills/UseTimeWindowFormatter.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'

const config = useProjConfig()
const timeWindowFormatter = useTimeWindowFormatter()
const props = defineProps({
  projectId: {
    type: String
  },
  subjectId: {
    type: String
  },
  parentSkillId: {
    type: String
  },
  skill: {
    type: Object
  },
  // increment this counter to force component to reload data from the server
  refreshCounter: {
    type: Number,
    default: 0
  }
})

let loading = ref({
  skills: true
})
let skillInfo = ref({})

onMounted(() => {
  loadSkills()
})
//
// refreshCounter() {
//   this.loadSkills();
// },
// skill(val) {
//   this.skillInfo = val;
// },

// computed
const isLoading = computed(() => {
  return loading.skills // || config.isLoadingProjConfig.value;
})

const skillId = computed(() => {
  return SkillReuseIdUtil.removeTag(skillInfo.value.skillId)
})

const totalPoints = computed(() => {
  return skillInfo.value.totalPoints
  // return NumberFilter(skillInfo.value.totalPoints);
})

const description = computed(function markDownDescription() {
  if (skillInfo.value && skillInfo.value.description) {
    return skillInfo.value.description
  }
  return null
})

const selfReportingTitle = computed(() => {
  if (!skillInfo.value.selfReportingType) {
    return 'Disabled'
  }

  if (skillInfo.value.selfReportingType === 'Quiz') {
    return skillInfo.value.quizType
  }

  if (skillInfo.value.selfReportingType === 'HonorSystem') {
    return 'Honor System'
  }

  return skillInfo.value.selfReportingType
})

const rootHelpUrl = computed(() => {
  if (!config.projConfigRootHelpUrl.value || skillInfo.value?.helpUrl?.toLowerCase()?.startsWith('http')) {
    return null
  }
  if (config.projConfigRootHelpUrl.value.endsWith('/')) {
    return config.projConfigRootHelpUrl.value.substring(0, config.projConfigRootHelpUrl.value.length - 1)
  }
  return config.projConfigRootHelpUrl.value
})

const helpUrl = computed(() => {
  if (!skillInfo.value?.helpUrl) {
    return null
  }
  const rootHelpUrlSetting = rootHelpUrl
  if (rootHelpUrlSetting) {
    return `${rootHelpUrlSetting.value}${skillInfo.value.helpUrl}`;
  }
  return skillInfo.value.helpUrl
})

const isImported = computed(() => {
  return skillInfo.value && skillInfo.value.copiedFromProjectId && skillInfo.value.copiedFromProjectId.length > 0 && !skillInfo.value.reusedSkill
})

const isReused = computed(() => {
  return skillInfo.value && skillInfo.value.reusedSkill
})

const isDisabled = computed(() => {
  return skillInfo.value && !skillInfo.value.enabled
})

// methods
const loadSkills = () => {
  loading.skills = true
  if (props.skill) {
    skillInfo.value = props.skill
    loading.value.skills = false
  } else {
    SkillsService.getSkillDetails(props.projectId, props.subjectId, props.parentSkillId)
      .then((response) => {
        skillInfo.value = response
      })
      .finally(() => {
        loading.value.skills = false
      })
  }
}
</script>

<template>
  <loading-container class="child-row" v-bind:is-loading="isLoading" :data-cy="`childRowDisplay_${skillInfo.skillId}`">
    <div v-if="isImported" class="mt-3 alert alert-info" header="Skill Catalog">
      This skill was <b>imported</b> from the
      <Badge class=""><i class="fas fa-book"></i> CATALOG</Badge>
      and was initially
      defined in the <b class="text-primary">{{ skillInfo.copiedFromProjectName }}</b> project.
      This skill is
      <Badge>Read-Only</Badge>
      and can only be edited in the <b class="text-primary">{{ skillInfo.copiedFromProjectName }}</b> project
    </div>
    <div v-if="isReused" class="mt-3 alert alert-info" header="Skill Catalog" data-cy="reusedAlert">
      This skill is a
      <Badge class="text-uppercase"><i class="fas fa-recycle"></i> reused</Badge>
      copy
      of another skill in this project and can only be edited from the
      <!--      <link-to-skill-page v-if="skillId" :project-id="skillInfo.projectId" :skill-id="skillId"-->
      <!--                          link-label="Original Skill" data-cy="linkToTheOriginalSkill"/>-->
      .
    </div>
    <div v-if="isImported && isDisabled" class="mt-3 alert alert-warning" header="Skill Catalog">
      <i class="fas fa-exclamation-circle"></i> This skill is <b>disabled</b> because import was not
      finalized yet.
    </div>
    <div class="sm:flex">
      <div class="flex-1 sm:mr-2 mb-2">
        <media-info-card
          :title="`${totalPoints} Points`"
          class="h-full"
          icon-class="fas fa-calculator text-success"
          data-cy="skillOverviewTotalpoints">
          <strong>{{ skillInfo.pointIncrement }}</strong> points <i
          class="fa fa-times text-muted" aria-hidden="true" />
          <strong> {{ skillInfo.numPerformToCompletion }}</strong> repetition<span
          v-if="skillInfo.numPerformToCompletion>1">s</span> to Completion
        </media-info-card>
      </div>
      <div class="flex-1 mb-2">
        <media-info-card
          :title="timeWindowFormatter.timeWindowTitle(skillInfo)"
          class="h-full"
          icon-class="fas fa-hourglass-half text-info"
          data-cy="skillOverviewTimewindow">
          {{ timeWindowFormatter.timeWindowDescription(skillInfo) }}
        </media-info-card>
      </div>
    </div>
    <div class="sm:flex">
      <div class="flex-1 sm:mr-2 mb-2">
        <media-info-card
          :title="`Version # ${skillInfo.version}`"
          class="h-full"
          icon-class="fas fa-code-branch text-warning"
          data-cy="skillOverviewVersion">
          Mechanism of adding new skills without affecting existing software running.
        </media-info-card>
      </div>
      <div class="flex-1 mb-2">
        <media-info-card
          :title="`Self Report: ${selfReportingTitle}`"
          class="h-full"
          icon-class="fas fa-laptop skills-color-selfreport"
          data-cy="selfReportMediaCard">
          <div v-if="skillInfo.selfReportingType">Users can <i>self report</i> this skill
            <span v-if="skillInfo.selfReportingType === 'Approval'">and will go into an <b
              class="text-primary">approval</b> queue.</span>
            <span v-if="skillInfo.selfReportingType === 'HonorSystem'">and will apply <b class="text-primary">immediately</b>.</span>
            <span v-if="skillInfo.selfReportingType === 'Quiz'">and points will be awarded after the
              <!--              <router-link-->
              <!--                  :to="{ name:'Questions', params: { quizId: skillInfo.quizId } }"-->
              <!--                  tag="a">{{ skillInfo.quizName }}</router-link> {{ skillInfo.quizType }} is {{ skillInfo.quizType === 'Survey' ? 'completed' : 'passed' }}!-->
            </span>
          </div>
          <div v-else>
            Self reporting is <b class="text-primary">disabled</b> for this skill.
          </div>
        </media-info-card>
      </div>
    </div>

    <Card class="mt-2">
      <template #title>
        Description
      </template>
      <template #content>
        <!--        <markdown-text v-if="description" :text="description" data-cy="skillOverviewDescription"/>-->
        <!--        <p v-else class="text-muted">-->
        <!--          Not Specified-->
        <!--        </p>-->
      </template>
    </Card>

    <InputGroup class="mt-3">
      <InputGroupAddon>
        <div class="input-group-text"><i class="fas fa-link mr-1"></i> Help URL:</div>
      </InputGroupAddon>
      <div class="p-inputtext p-component">
        <a v-if="skillInfo.helpUrl" :href="helpUrl" target="_blank" rel="noopener" class="skill-url" data-cy="skillOverviewHelpUrl">
          <span v-if="rootHelpUrl" class="surface-200 border-50 border-x-2 border-round"
                aria-label="Root Help URL was configured in the project's settings."
                v-tooltip.top="'Root Help URL was configured in the project\'s settings.'">
            <i class="fas fa-cogs"></i> {{ rootHelpUrl }}</span>{{ skillInfo.helpUrl }}
        </a>
      </div>
    </InputGroup>

    <Card v-if="skillInfo.sharedToCatalog" class="mt-3" header="Skill Catalog" data-cy="exportedToCatalogCard">
      This skill was exported to the
      <Badge class=""><i class="fas fa-book"></i> CATALOG</Badge>
      .
      Please visit
      <SkillsButton data-cy="navigateToSkillCatalog" variant="outline-info" size="sm"
                    :to="{ name:'SkillsCatalog', params: { projectId: projectId} }"
                    aria-label="View Skill Catalog">Skill Catalog
      </SkillsButton>
      page to manage exported skills.
    </Card>

  </loading-container>
</template>

<style scoped></style>