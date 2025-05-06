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
import { useFocusState } from '@/stores/UseFocusState.js'
import CatalogService from '@/components/skills/catalog/CatalogService.js'
import { useRoute } from 'vue-router'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { SkillsReporter } from '@skilltree/skills-client-js'
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue'
import ScrollPanel from 'primevue/scrollpanel'
import {useDialogUtils} from "@/components/utils/inputForm/UseDialogUtils.js";

const model = defineModel()
const props = defineProps({
  skills: {
    type: Array,
    required: true
  },
  groupId: String,
  showInviteOnlyWarning: {
    type: Boolean,
    required: false,
    default: false
  }
})
const emit = defineEmits(['on-exported', 'on-nothing-to-export'])
const focusState = useFocusState()
const route = useRoute()
const appConfig = useAppConfig()

const handleOkBtn = () => {
  if (!state.value.exported) {
    emit('on-nothing-to-export')
  }
  handleClose()
}
const handleClose = () => {
  if (state.value.exported) {
    emit('on-exported', {groupId: props.groupId, exported: toRaw(skillsFiltered.value)})
  }
  model.value = false
  if (state.value.exported) {
    const focusOn = props.groupId ? `group-${props.groupId}_newSkillBtn` : 'newSkillBtn'
    focusState.setElementId(focusOn)
  }
  focusState.focusOnLastElement()
}
const onUpdateVisible = (newVal) => {
  if (!newVal) {
    handleClose()
  }
}

const loadingData = ref(true)
const selectedProject = ref(null)
const insufficientSubjectPoints = ref(false)
const isUserCommunityRestricted = ref(false)
const skillsFiltered = ref([])
const notExportableSkills = ref([])
const numAlreadyExported = ref(0)
const allSkillsExportedAlready = ref(false)
const isSingleId = ref(false)
const firstSkillId = ref(null)
const firstSkillName = ref(null)
const allSkillsAreDups = ref(false)
const state = ref({
  exporting: false,
  exported: false
})

onMounted(() => {
  prepSkillsForExport()
})
const prepSkillsForExport = () => {
  const skillIds = props.skills.map((skill) => skill.skillId)
  CatalogService.areSkillsExportable(route.params.projectId, skillIds)
    .then((res) => {
      insufficientSubjectPoints.value = !res.hasSufficientSubjectPoints
      isUserCommunityRestricted.value = res.isUserCommunityRestricted
      if (!insufficientSubjectPoints.value && !isUserCommunityRestricted.value) {
        let enrichedSkills = props.skills.map((skillToUpdate) => {
          const enhanceWith = res.skillsValidationRes[skillToUpdate.skillId]
          return ({
            ...skillToUpdate,
            hasDependencies: enhanceWith.hasDependencies,
            skillAlreadyInCatalog: enhanceWith.skillAlreadyInCatalog,
            skillIdConflictsWithExistingCatalogSkill: enhanceWith.skillIdConflictsWithExistingCatalogSkill,
            skillNameConflictsWithExistingCatalogSkill: enhanceWith.skillNameConflictsWithExistingCatalogSkill
          })
        })

        // re-filter if another user added to the filter or if the changes was made in another tab
        enrichedSkills = enrichedSkills.filter((skill) => !skill.skillAlreadyInCatalog)
        const isExportableSkill = (skill) => !skill.skillIdConflictsWithExistingCatalogSkill && !skill.skillNameConflictsWithExistingCatalogSkill && !skill.hasDependencies && skill.enabled

        notExportableSkills.value = enrichedSkills.filter((skill) => !isExportableSkill(skill))
        allSkillsAreDups.value = enrichedSkills.length === notExportableSkills.value.length
        skillsFiltered.value = enrichedSkills.filter((skill) => isExportableSkill(skill))
        numAlreadyExported.value = props.skills.length - skillsFiltered.value.length - notExportableSkills.value.length
        allSkillsExportedAlready.value = skillsFiltered.value.length === 0 && notExportableSkills.value.length === 0
        isSingleId.value = skillsFiltered.value.length === 1
        firstSkillId.value = skillsFiltered.value && skillsFiltered.value.length > 0 ? skillsFiltered.value[0].skillId : null
        firstSkillName.value = skillsFiltered.value && skillsFiltered.value.length > 0 ? skillsFiltered.value[0].name : null
      }
    }).finally(() => {
    loadingData.value = false
  })
}

const handleExport = () => {
  state.value.exporting = true
  CatalogService.bulkExport(route.params.projectId, skillsFiltered.value.map((skill) => skill.skillId))
    .then(() => {
      state.value.exported = true
      SkillsReporter.reportSkill('ExporttoCatalog')
    })
    .finally(() => {
      state.value.exporting = false
    })
}

const isExportable = computed(() => {
  return !allSkillsExportedAlready.value && !state.value.exported && !allSkillsAreDups.value && !insufficientSubjectPoints.value && !isUserCommunityRestricted.value
})
const dialogUtils = useDialogUtils()
</script>

<template>
  <Dialog
    modal
    header="Export Skill to the Catalog"
    :maximizable="true"
    :close-on-escape="true"
    class="w-11/12 xl:w-8/12"
    @update:visible="onUpdateVisible"
    v-model:visible="model"
    :pt="{ pcMaximizeButton: dialogUtils.getMaximizeButtonPassThrough() }"
  >
    <skills-spinner :is-loading="loadingData" />
    <div v-if="!loadingData">
      <Message
        v-if="isUserCommunityRestricted"
        :closable="false"
        severity="warn"
        data-cy="userCommunityRestrictedWarning">This project's access is restricted to <b
        class="text-primary">{{ appConfig.userCommunityRestrictedDescriptor }}</b> users only and its skills <b
        class="text-primary">cannot</b> be exported to the Skills Catalog.
      </Message>
      <div v-if="!isUserCommunityRestricted">
        <Message
          v-if="insufficientSubjectPoints"
          :closable="false"
          severity="warn">Export
          of skills is not allowed until the <b>subject</b> has sufficient points. Must have at least
          <Tag>{{ appConfig.minimumSubjectPoints }}</Tag>
          points!
        </Message>
        <div v-else>
          <Message :closable="false" severity="warn" v-if="allSkillsExportedAlready">
            All selected
            <Tag severity="info">{{ skills.length }}</Tag>
            skill(s) are already in the Skill Catalog.
          </Message>
          <Message v-else-if="showInviteOnlyWarning" severity="error">
            This project is defined as Invite Only, exporting skills will make those skills available to users who have
            not been invited to this project.
          </Message>

          <skills-overlay
            v-if="!allSkillsExportedAlready && !state.exported"
            :show="state.exporting"
            opacity="50">
            <p v-if="!allSkillsAreDups">
              This will export <span v-if="isSingleId">
            <b class="text-primary">[{{ firstSkillName }}]</b> Skill</span><span v-else><Tag
              severity="info">{{ skillsFiltered.length }}</Tag> Skills</span> to the
              <Tag>SkillTree Catalog</Tag>
              .
              Other project administrators will then be able to import a <b class="text-primary">read-only</b> version
              of this skill.
            </p>
            <p v-if="numAlreadyExported > 0">
              <span class="italic"><i class="fas fa-exclamation-triangle text-warning" /> Note:</span> The are
              already
              <Tag severity="info">{{ numAlreadyExported }}</Tag>
              skill(s) in the Skill Catalog from the provided selection.
            </p>

            <div v-if="notExportableSkills && notExportableSkills.length > 0">
              <Message severity="warn" :closable="false">
                Cannot export <Tag severity="primary">{{ notExportableSkills.length }}</Tag> skill(s):
              </Message>
              <ScrollPanel
                :pt="{
                      wrapper: {
                          style: { 'border-right': '10px solid var(--surface-ground)' }
                      },
                      bary: 'hover:bg-primary-400 bg-primary-300 opacity-100'
                  }"
                class="mb-4"
                style="width: 100%; max-height: 200px">
              <ul>
                <li v-for="dupSkill in notExportableSkills" :key="dupSkill.skillId"
                    :data-cy="`dupSkill-${dupSkill.skillId}`" class="leading-loose">
                  {{ dupSkill.name }}
                  <Tag severity="warn" v-if="dupSkill.skillNameConflictsWithExistingCatalogSkill" class="ml-1">
                    Name Conflict
                  </Tag>
                  <Tag severity="warn" v-if="dupSkill.skillIdConflictsWithExistingCatalogSkill" class="ml-1">ID
                    Conflict
                  </Tag>
                  <Tag severity="warn" v-if="dupSkill.hasDependencies" class="ml-1">Has Prerequisites
                  </Tag>
                  <Tag severity="warn" v-if="!dupSkill.enabled" class="ml-1">Is Disabled
                  </Tag>
                </li>
              </ul>
              </ScrollPanel>
            </div>
          </skills-overlay>

          <Message :closable="false" v-if="state.exported" icon="fas fa-check-circle">
            <span v-if="isSingleId"> Skill [<b class="text-primary">{{ firstSkillName }}</b>] was</span>
            <span v-else><Tag severity="info" class="ml-2">{{ skillsFiltered.length }}</Tag>
            Skills were</span> <span class="text-success font-weight-bold">successfully</span> exported to the catalog!
          </Message>

          <div v-if="isExportable" class="text-right mt-3">
            <SkillsButton
              label="Cancel"
              icon="far fa-times-circle"
              severity="warn"
              outlined
              class="mr-2"
              @click="handleClose"
              data-cy="closeButton" />
            <SkillsButton
              label="Export"
              icon="far fa-arrow-alt-circle-up"
              outlined
              severity="success"
              @click="handleExport"
              :disabled="loadingData"
              data-cy="exportToCatalogButton" />
          </div>
          <div v-if="!isExportable" class="text-right">
            <SkillsButton
              label="OK"
              icon="fas fa-check"
              severity="success"
              outlined
              size="small"
              @click="handleOkBtn"
              data-cy="okButton"
              :disabled="loadingData" />
          </div>
        </div>
      </div>
    </div>


  </Dialog>
</template>

<style scoped>

</style>