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
import { onMounted, ref, computed, inject, watch } from 'vue'
import SkillsService from '@/components/skills/SkillsService.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import SkillsTable from '@/components/skills/SkillsTable.vue'
import EditNumRequiredSkills from '@/components/skills/skillsGroup/EditNumRequiredSkills.vue'

const props = defineProps({
  skill: {
    type: Object,
    required: true
  }
})
const appConfig = useAppConfig()
const subjectState = useSubjectsState()
const skillsState = useSubjectSkillsState()

const loading = ref(true)
const skillInfo = ref({})
// const numSkills = computed(() => {
//   const groupSkills = skillsState.getGroupSkills(skillInfo.value.skillId)
//   return groupSkills ? groupSkills.length : 0
// })
const hasSkills = computed(() => {
  return skillsState.getGroupSkills(skillInfo.value.skillId).length > 0
})
onMounted(() => {
  loadSkillsInfo()
})

const loadSkillsInfo = () => {
  loading.value = true
  SkillsService.getSkillDetails(props.skill.projectId, props.skill.subjectId, props.skill.skillId)
    .then((response) => {
      skillInfo.value = response
      return skillsState.loadGroupSkills(props.skill.projectId, props.skill.skillId)
        .finally(() => {
          loading.value = false
        })
    })
}

const allSkillsRequired = computed(() => {
  // -1 == all skills required
  return (skillInfo.value.numSkillsRequired < 0)
})
const requiredSkillsNum = computed(() => {
  // -1 == all skills required
  return (skillInfo.value.numSkillsRequired === -1) ? skillsState.getGroupSkills(skillInfo.value.skillId).value.length : skillInfo.value.numSkillsRequired
})
const lessThanTwoSkills = computed(() => {
  return skillsState.getGroupSkills(props.skill.skillId).length < 2;
})

const addDisabled = computed(() => {
  if (props.skill.enabled) {
    if (subjectState.subject.numSkills >= appConfig.maxSkillsPerSubject) {
      return true
    }
  }
  return false
})
const addDisabledMessage = computed(() => {
  return `No more Skills can be added to this group, the maximum number of Skills allowed per subject is ${appConfig.maxSkillsPerSubject}`
})

const createOrUpdateSkill = inject('createOrUpdateSkill')
const initiateImport = inject('initiateImport')
const showEditRequiredSkillsDialog = ref(false)

watch(() => skillsState.getGroupSkills(props.skill.skillId).length,
  (newLength) => {
    if (skillInfo.value.numSkillsRequired >= newLength) {
      skillInfo.value.numSkillsRequired = -1
    }
  }
)
watch(
  () => props.skill.description,
  (newDescripton) => {
    if (newDescripton) {
      skillInfo.value.description = newDescripton
    }
  }
)

const groupChanged = (updatedGroup) => {
  skillInfo.value.numSkillsRequired = updatedGroup.numSkillsRequired
}
</script>

<template>
  <div>
    <skills-spinner :is-loading="loading" />
    <div v-if="!loading" :data-cy="`ChildRowSkillGroupDisplay_${skillInfo.skillId}`">

      <Fieldset v-if="skillInfo.description" legend="Group's Description" class="mb-4">
        <markdown-text
          :text="skillInfo.description"
          :instance-id="skillInfo.skillId"
          data-cy="description" />
      </Fieldset>

      <Fieldset
        legend="Group's Skills"
        :pt="{ content: { class: 'p-0' }, root: { class: 'm-0' }, toggler: { class: 'm-2'} }"
      >
        <div class="flex mx-4 my-6" data-cy="requiredSkillsSection">
          <div>
            <span class="mr-1 italic">Required: </span>
            <span v-if="!allSkillsRequired">
              <Tag severity="info" data-cy="requiredSkillsNum">{{ requiredSkillsNum }}</Tag>
              <span class="ml-1">out of <Tag data-cy="numSkillsInGroup">{{ skill.numSkillsInGroup
                }}</Tag> skills</span>
            </span>
            <span v-if="allSkillsRequired" data-cy="requiredAllSkills">
              <Tag severity="info" class="uppercase">all skills</Tag>
            </span>
            <SkillsButton
              :id="`editNumSkillsReq${skillInfo.skillId}`"
              icon="far fa-edit"
              severity="outline"
              size="small"
              outlined
              @click="showEditRequiredSkillsDialog=true"
              :track-for-focus="true"
              :disabled="lessThanTwoSkills"
              :aria-label="'Edit Number of Required skills for '+ skillInfo.name + ' group'"
              data-cy="editRequired" class="ml-2" />
            <div v-if="lessThanTwoSkills"
              class="italic text-small mt-1 font-light">** Must have at least 2 skills to modify</div>

          </div>
          <div class="flex-1 text-right">
            <ButtonGroup>
              <SkillsButton
                :id="`group-${skillInfo.skillId}_importSkillBtn`"
                :ref="`group-${skillInfo.skillId}_importSkillBtn`"
                variant="outline-info"
                size="small"
                outlined
                severity="info"
                @click="initiateImport(skillInfo.skillId)"
                label="Import Skills"
                icon="fas fa-book"
                :track-for-focus="true"
                :data-cy="`importSkillToGroupBtn-${skillInfo.skillId}`"
                :aria-label="`Import skill to ${skillInfo.name} group`"
                class="ml-1" />
              <SkillsButton
                :id="`group-${skillInfo.skillId}_newSkillBtn`"
                :ref="`group-${skillInfo.skillId}_newSkillBtn`"
                label="Add Skill"
                icon="fas fa-plus-circle"
                variant="outline-info"
                size="small"
                outlined
                severity="info"
                :track-for-focus="true"
                @click="createOrUpdateSkill({}, false, false, skillInfo.skillId)"
                :disabled="addDisabled"
                :aria-label="`Add skill to ${skillInfo.name} group`"
                :data-cy="`addSkillToGroupBtn-${skillInfo.skillId}`" />
            </ButtonGroup>
            <div v-if="addDisabled">
              {{ addDisabledMessage }}
            </div>
          </div>
        </div>
        <div>

          <!--        :subject-name="subjectName"-->
          <!--        @skill-removed="skillRemoved"-->
          <!--        @skills-change="skillChanged"-->
          <!--        @skills-reused="skillReused"-->
          <!--        :disableDeleteButtonsInfo="disableDeleteButtonInfo"-->
          <!--        :page-size="this.maxSkillsToShow"-->
          <!--        :table-id="`groupSkills_${skillInfo.skillId}`"-->
          <!--        :ref="`groupSkills_${skillInfo.skillId}`"-->
          <!--        :skills-prop="skills"-->
          <!--        :is-top-level="true"-->
          <!--        :project-id="this.$route.params.projectId"-->
          <!--        :subject-id="this.$route.params.subjectId"-->
          <!--        actions-btn-size="sm"-->
          <!--        :show-search="false"-->
          <!--        :show-header="false"-->
          <!--        :show-paging="false"-->
          <skills-table v-if="hasSkills" :group-id="skillInfo.skillId" />
          <no-content2
            v-if="!hasSkills"
            title="Group has no Skills"
            class="py-20"
            message="Click 'Add Skill' button to add skills to this group" />
        </div>
      </Fieldset>
    </div>

    <edit-num-required-skills
      v-if="showEditRequiredSkillsDialog"
      v-model="showEditRequiredSkillsDialog"
      @group-changed="groupChanged"
      :group="skillInfo" />
  </div>
</template>

<style scoped>

</style>