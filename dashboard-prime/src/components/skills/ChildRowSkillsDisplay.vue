<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import Badge from 'primevue/badge'
import Card from 'primevue/card'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import LoadingContainer from '@/components/utils/LoadingContainer.vue'
import SkillsService from '@/components/skills/SkillsService'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import SkillReuseIdUtil from '@/components/utils/SkillReuseIdUtil'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import { useTimeWindowFormatter } from '@/components/skills/UseTimeWindowFormatter.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import LinkToSkillPage from '@/components/utils/LinkToSkillPage.vue'
import { useRoute } from 'vue-router'

const config = useProjConfig()
const timeWindowFormatter = useTimeWindowFormatter()
const numberFormat = useNumberFormat()
const route = useRoute()
const props = defineProps({
  skill: {
    type: Object,
    required: true
  },
  loadSkillAsync: {
    type: Boolean,
    default: false
  }
})

const loading = ref(true)
const skillInfo = ref({})
const projectId = computed(() => route.params.projectId)

onMounted(() => {
  loadSkill()
})


watch(() => props.skill,
  () => {
    loadSkill()
  },
  { deep: true })

// computed
// const skillId = computed(() => {
//   return SkillReuseIdUtil.removeTag(skillInfo.value.skillId)
// })

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
  if (!config.projConfigRootHelpUrl || skillInfo.value?.helpUrl?.toLowerCase()?.startsWith('http')) {
    return null
  }
  if (config.projConfigRootHelpUrl.endsWith('/')) {
    return config.projConfigRootHelpUrl.substring(0, config.projConfigRootHelpUrl.length - 1)
  }
  return config.projConfigRootHelpUrl
})

const helpUrl = computed(() => {
  if (!skillInfo.value?.helpUrl) {
    return null
  }
  const rootHelpUrlSetting = rootHelpUrl.value
  if (rootHelpUrlSetting) {
    return `${rootHelpUrlSetting}${skillInfo.value.helpUrl}`
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
const loadSkill = () => {
  loading.value = true
  if (!props.loadSkillAsync) {
    skillInfo.value = props.skill
    loading.value = false
  } else {
    SkillsService.getSkillDetails(props.skill.projectId, props.skill.subjectId, props.skill.skillId)
      .then((response) => {
        skillInfo.value = response
      })
      .finally(() => {
        loading.value = false
      })
  }
}

const skillIdOfTheOriginalSkill = computed(() => SkillReuseIdUtil.removeTag(skillInfo.value.skillId))
</script>

<template>
  <loading-container class="child-row" v-bind:is-loading="loading" :data-cy="`childRowDisplay_${skillInfo.skillId}`">
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
      <link-to-skill-page
        :project-id="skillInfo.projectId"
        :skillId="skillIdOfTheOriginalSkill"
        link-label="Original Skill" data-cy="linkToTheOriginalSkill" />
      <!--      <link-to-skill-page v-if="skillId" :project-id="skillInfo.projectId" :skill-id="skillId"-->
      <!--                          link-label="Original Skill" data-cy="linkToTheOriginalSkill"/>-->
      .
    </div>
    <div v-if="isImported && isDisabled" class="mt-3 alert alert-warning" header="Skill Catalog">
      <i class="fas fa-exclamation-circle"></i> This skill is <b>disabled</b> because import was not
      finalized yet.
    </div>
    <div class="md:flex">
      <div class="flex-1 md:mr-2 mb-2">
        <media-info-card
          :title="`${numberFormat.pretty(totalPoints)} Points`"
          class="h-full"
          icon-class="fas fa-calculator text-success"
          data-cy="skillOverviewTotalpoints">
          <strong>{{ numberFormat.pretty(skillInfo.pointIncrement) }}</strong> points <i
          class="fa fa-times text-muted mr-2" aria-hidden="true" />
          <strong> {{ numberFormat.pretty(skillInfo.numPerformToCompletion) }}</strong> repetition<span
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
    <div class="md:flex">
      <div class="flex-1 md:mr-2 mb-2">
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
          <div v-if="skillInfo.selfReportingType && skillInfo.selfReportingType !== 'Disabled'">Users can <i>self report</i> this skill
            <span v-if="skillInfo.selfReportingType === 'Approval'">and will go into an <b
              class="text-primary">approval</b> queue.</span>
            <span v-if="skillInfo.selfReportingType === 'HonorSystem'">and will apply <b class="text-primary">immediately</b>.</span>
            <span v-if="skillInfo.selfReportingType === 'Quiz'">and points will be awarded after the
              <router-link
                :to="{ name:'Questions', params: { quizId: skillInfo.quizId } }"
              >{{ skillInfo.quizName }}</router-link> {{ skillInfo.quizType
              }} is {{ skillInfo.quizType === 'Survey' ? 'completed' : 'passed' }}!
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
        <markdown-text
          v-if="description"
          :text="description"
          :instance-id="skill.skillId"
          data-cy="skillOverviewDescription" />
        <p v-else class="text-muted">
          Not Specified
        </p>
      </template>
    </Card>

    <InputGroup class="mt-3">
      <InputGroupAddon>
        <div class="input-group-text text-700"><i class="fas fa-link mr-1" aria-hidden="true" /> Help URL:</div>
      </InputGroupAddon>
      <div class="p-inputtext p-component">
        <div v-if="helpUrl">
          <a :href="helpUrl" target="_blank" rel="noopener" class="skill-url" data-cy="skillOverviewHelpUrl">
            <span v-if="rootHelpUrl" class="surface-200 border-50 border-x-2 border-round"
                  aria-label="Root Help URL was configured in the project's settings.">
              <i class="fas fa-cogs"></i> {{ rootHelpUrl }}</span>{{ skillInfo.helpUrl }}
          </a>
          <div v-if="rootHelpUrl" class="mt-1 text-xs">
            ** Root Help URL was configured in the project's settings.
          </div>
        </div>
        <div v-else>Not Specified</div>
      </div>
    </InputGroup>

    <Message
      v-if="skillInfo.sharedToCatalog"
      icon="fas fa-book"
      :closable="false"
      data-cy="exportedToCatalogCard">
        This skill was exported to the <strong>CATALOG</strong>.
        Please visit
        <router-link :to="{ name:'SkillsCatalog', params: { projectId: skill.projectId} }" data-cy="navigateToSkillCatalog">Skill Catalog</router-link>
        page to manage exported skills.
    </Message>

  </loading-container>
</template>

<style scoped></style>
