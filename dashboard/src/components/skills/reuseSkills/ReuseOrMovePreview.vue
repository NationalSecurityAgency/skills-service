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
import { useRoute } from 'vue-router'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import { SkillsReporter } from '@skilltree/skills-client-js'
import NoContent2 from '@/components/utils/NoContent2.vue'

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
const pluralSupport = useLanguagePluralSupport()

const loadingReusedSkills = ref(true)
const skillsForReuse = ref({
  allAlreadyExist: [],
  alreadyExist: [],
  available: [],
  skillsWithDeps: []
})

const buildSummaryInfo = () => {
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
const doMoveOrReuse = () => {
  reuseInProgress.value = true
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
      })
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
const isReuseBtnDisabled = computed(() => {
  return reuseInProgress.value || (skillsForReuse.value.available && skillsForReuse.value.available.length === 0)
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

          <div v-if="skillsForReuse.available.length > 0">
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