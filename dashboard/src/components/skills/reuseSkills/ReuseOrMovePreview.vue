/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { computed, onMounted, ref, toRaw } from 'vue'
import SkillsService from '@/components/skills/SkillsService.js'
import SubjectsService from '@/components/subjects/SubjectsService.js'
import {useRoute, useRouter} from 'vue-router'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import { SkillsReporter } from '@skilltree/skills-client-js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import {useAnnouncer} from "@vue-a11y/announcer";
import LinkToSkillPage from "@/components/utils/LinkToSkillPage.vue";

const props = defineProps({
  skills: {
    type: Array,
    required: true
  },
  destination: {
    type: Object,
    required: true
  },
  isReuseType: {
    type: Boolean,
    default: false
  },
  nextStepNavFunction: {
    type: Function,
    required: true
  },
  actionName: {
    type: String,
    required: true
  },
  actionDirection: {
    type: String,
    required: true
  }
})
const emits = defineEmits(['on-cancel', 'on-changed'])
const route = useRoute()
const router = useRouter()
const announcer = useAnnouncer()
const pluralSupport = useLanguagePluralSupport()
const appConfig = useAppConfig()

const loadingReusedSkills = ref(true)
const skillsForReuse = ref({
  allAlreadyExist: [],
  alreadyExist: [],
  available: [],
  skillsWithDeps: []
})

const buildSummaryInfo = () => {
  loadDestinationSubject()
  const parentId = props.destination.groupId || props.destination.subjectId
  SkillsService.getReusedSkills(route.params.projectId, parentId)
    .then((res) => {
      skillsForReuse.value.allAlreadyExist = res
      skillsForReuse.value.alreadyExist = props.skills.filter((skill) => res.find((e) => e.name === skill.name))
      skillsForReuse.value.available = props.skills.filter((skill) => !res.find((e) => e.name === skill.name))
      loadDependencyInfo()
    })
    .finally(() => {
      loadingReusedSkills.value = false
    })
}

const destinationSubject = ref(null)
const loadDestinationSubject = () => {
  if (props.destination.groupId) {
    SubjectsService.getSubjectDetailsForGroup(route.params.projectId, props.destination.groupId).then((res) => {
      destinationSubject.value = res
    });
  } else {
    destinationSubject.value = SubjectsService.getSubjectDetails(route.params.projectId, props.destination.subjectId).then((res) => {
      destinationSubject.value = res
    });
  }
}

const loadingDependencyInfo = ref(true)
const loadDependencyInfo = () => {
  if (props.isReuseType && skillsForReuse.value.available.length > 0) {
    SkillsService.checkSkillsForDeps(route.params.projectId, skillsForReuse.value.available.map((item) => item.skillId))
      .then((res) => {
        const withDeps = res.filter((item) => item.hasDependency)
        skillsForReuse.value.skillsWithDeps = skillsForReuse.value.available.filter((skill) => withDeps.find((e) => e.skillId === skill.skillId))
        skillsForReuse.value.available = skillsForReuse.value.available.filter((skill) => !withDeps.find((e) => e.skillId === skill.skillId))
      })
      .finally(() => {
        loadingDependencyInfo.value = false
      })
  } else {
    loadingDependencyInfo.value = false
  }
}
const isLoading = computed(() => loadingReusedSkills.value || loadingDependencyInfo.value || reuseInProgress.value)
onMounted(() => {
  buildSummaryInfo()
})

const skillsWereMovedOrReusedAlready = ref(false)
const reuseInProgress = ref(false)
const reuseFailedInfo = ref({
  failed: false,
  skillId: false,
  isTranscript: false,
})

const doMoveOrReuse = () => {
  reuseInProgress.value = true
  reuseFailedInfo.value = { failed: false, skillId: false, isTranscript: false }
  const skillIds = skillsForReuse.value.available.map((sk) => sk.skillId)
  if (!props.isReuseType) {
    SkillsService.moveSkills(route.params.projectId, skillIds, props.destination.subjectId, props.destination.groupId)
      .then(() => {
        handleActionCompleting()
      })
  } else {
    SkillsService.reuseSkillInAnotherSubject(route.params.projectId, skillIds, props.destination.subjectId, props.destination.groupId)
      .then(() => {
        handleActionCompleting()
      }).catch((err) => {
        const isParagraphValidationFailed = err && err.response && err.response.data && err.response.data.errorCode === 'ParagraphValidationFailed'
        if (isParagraphValidationFailed) {
          const explanation = err.response.data.explanation
          reuseFailedInfo.value = {
            failed: true,
            skillId: err.response.data.skillId,
            isTranscript: explanation.includes('Video transcript validation failed'),
          }
          reuseInProgress.value = false
          announcer.polite(`Failed to reuse skills due to validation issue for skill with id ${reuseFailedInfo.value.skillId}`)
        } else {
          router.push({ name: 'ErrorPage', query: { err } });
        }
      });
  }
}

const handleActionCompleting = () => {
  // this.state.reUseComplete = true;
  if (props.isReuseType) {
    SkillsReporter.reportSkill('ReuseSkill')
  } else {
    SkillsReporter.reportSkill('MoveSkill')
  }
  reuseInProgress.value = false
  props.nextStepNavFunction()
  emits('on-changed', toRaw(skillsForReuse.value.available))
}

const textCustomization = props.isReuseType ?
    { actionName: 'Reuse', actionDirection: 'in' } :
    { actionName: 'Move', actionDirection: 'to' }

const actionName = computed(() => props.actionName.toLocaleLowerCase())
const actionNameInPast = computed(() => `${actionName.value}d`)
const destinationSubjectTotalSkills = computed(() => (destinationSubject.value?.numSkills || 0) + (destinationSubject.value?.numSkillsReused || 0))
const exceedsMaxDestinationSkills = computed(() => {
  return (destinationSubjectTotalSkills.value + skillsForReuse.value.available?.length) > appConfig.maxSkillsPerSubject
})
const isReuseBtnDisabled = computed(() => {
  return reuseInProgress.value || (skillsForReuse.value.available && skillsForReuse.value.available.length === 0) || exceedsMaxDestinationSkills.value || reuseFailedInfo.value?.failed
})
</script>

<template>
  <div role="alert">
    <skills-spinner :is-loading="isLoading" class="my-8" />
    <div v-if="!isLoading">
      <no-content2
        v-if="skillsWereMovedOrReusedAlready"
        class="mt-4"
        title="Please Refresh"
        :show-refresh-action="true"
        message="Skills were moved or reused in another browser tab OR modified by another project administrator." />

      <div v-if="!skillsWereMovedOrReusedAlready" class="flex flex-col h-48">
        <div
          class="p-6 border-2 border-dashed border-surface rounded-border bg-surface-50 dark:bg-surface-950 flex-auto flex flex-col gap-2 justify-center items-center font-medium">

          <div v-if="!reuseFailedInfo.failed">
            <div v-if="skillsForReuse.available.length > 0 && !exceedsMaxDestinationSkills">
              <Tag severity="info">{{ skillsForReuse.available.length }}</Tag>
              skill{{ pluralSupport.plural(skillsForReuse.available) }} will be {{ actionNameInPast }}
              {{ actionDirection }} the
              <span v-if="destination.groupName">
                  <span class="text-primary font-weight-bold">[{{
                      destination.groupName
                    }}]</span>
                  group.
                </span>
              <span v-else>
                  <span class="text-primary font-weight-bold">[{{
                      destination.subjectName
                    }}]</span>
                  subject.
                </span>
            </div>
            <div v-else>
              <Message severity="warn" :closable="false">
                Selected skills can NOT be {{ actionNameInPast }} {{ actionDirection }} the
                <span v-if="destination.groupName"><span
                  class="text-primary font-weight-bold">{{ destination.groupName }} </span> group</span>
                <span v-else><span
                  class="text-primary font-weight-bold">{{ destination.subjectName }} </span> subject</span>.
                Please cancel and select different skills.
              </Message>
            </div>
            <div v-if="skillsForReuse.alreadyExist.length > 0">
              <Tag severity="warn">{{ skillsForReuse.alreadyExist.length }}</Tag>
              selected skill{{ pluralSupport.pluralWithHave(skillsForReuse.alreadyExist) }} <span
              class="text-primary font-weight-bold">already</span> been reused in that <span
              v-if="destination.groupName">group</span><span v-else>subject</span>!
            </div>
            <div v-if="skillsForReuse.skillsWithDeps.length > 0">
              <Tag severity="warn">{{ skillsForReuse.skillsWithDeps.length }}</Tag>
              selected skill{{ pluralSupport.pluralWithHave(skillsForReuse.skillsWithDeps) }} other skill
              dependencies, reusing skills with dependencies is not allowed!
            </div>
            <div v-if="exceedsMaxDestinationSkills">
            <Tag severity="warn">{{ skillsForReuse.available.length }}</Tag>
            selected skill{{ pluralSupport.plural(skillsForReuse.available) }} will exceed the maximum number of skills allowed in the destination subject!
          </div>
          </div>
          <div v-if="reuseFailedInfo.failed">
            <Message severity="error" :closable="false">
              <div data-cy="failedReuseMessage">
                <div>The skill with ID
                  <link-to-skill-page
                      :skill-id="reuseFailedInfo.skillId"
                      :project-id="route.params.projectId"
                      :link-label="reuseFailedInfo.skillId"
                      data-cy="failedSkillLink"
                  />
                  has a <span v-if="!reuseFailedInfo.isTranscript">description</span><span v-else>video/audio transcript</span> that doesn't meet the validation requirements.
                </div>
                <div>
                  Please update the skill's <span v-if="!reuseFailedInfo.isTranscript">description</span><span v-else>video/audio transcript</span> to resolve the issue, then try reusing again.
                </div>
              </div>
            </Message>
          </div>
        </div>
      </div>

      <div class="flex pt-6 justify-end">
        <SkillsButton
          label="Cancel"
          icon="far fa-times-circle"
          outlined
          class="mr-2"
          severity="warn"
          data-cy="closeButton"
          @click="emits('on-cancel')" />
        <SkillsButton
          :label="textCustomization.actionName"
          icon="fas fa-shipping-fast"
          @click="doMoveOrReuse"
          data-cy="reuseButton"
          :disabled="isReuseBtnDisabled"
          outlined />
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>