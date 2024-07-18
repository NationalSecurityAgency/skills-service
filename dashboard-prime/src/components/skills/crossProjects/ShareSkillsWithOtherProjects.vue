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
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import ProjectService from '@/components/projects/ProjectService'
import SkillsService from '@/components/skills/SkillsService.js'
import SkillsShareService from '@/components/skills/crossProjects/SkillsShareService'
import NoContent2 from '@/components/utils/NoContent2.vue'
import SkillsSelector from '@/components/skills/SkillsSelector.vue'
import ProjectSelector from '@/components/skills/crossProjects/ProjectSelector.vue'
import SharedSkillsTable from '@/components/skills/crossProjects/SharedSkillsTable.vue'

const route = useRoute()
const projectId = route.params.projectId
const announcer = useSkillsAnnouncer()
const communityLabels = useCommunityLabels()
const appConfig = useAppConfig()

const allProjectsConstant = 'ALL_SKILLS_PROJECTS'
const loading = ref({
  allSkills: true,
  sharedSkillsInit: true,
  sharedSkills: false,
  projInfo: true
})
const restrictedUserCommunity = ref(false)
const allSkills = ref([])
const selectedSkill = ref(null)
const sharedSkills = ref([])
const selectedProject = ref(null)
const errorMessage = ref('')
const shareWithAllProjects = ref(false)

const shareButtonEnabled = computed(() => {
  return (selectedProject.value || shareWithAllProjects.value) && selectedSkill.value && !loading.value.sharedSkills
})

onMounted(() => {
  loadProjectInfo()
  loadAllSkills()
  loadSharedSkills()
})

const loadProjectInfo = () => {
  loading.value.projInfo = true
  ProjectService.getProject(projectId)
    .then((projRes) => {
      restrictedUserCommunity.value = communityLabels.isRestrictedUserCommunity(projRes.userCommunity)
    }).finally(() => {
    loading.value.projInfo = false
  })
}

const loadAllSkills = () => {
  loading.value.allSkills = true
  SkillsService.getProjectSkillsWithoutImportedSkills(projectId)
    .then((skills) => {
      allSkills.value = skills
      loading.value.allSkills = false
    })
}

const loadSharedSkills = () => {
  loading.value.sharedSkills = true
  return SkillsShareService.getSharedSkills(projectId)
    .then((data) => {
      sharedSkills.value = data
      loading.value.sharedSkillsInit = false
      loading.value.sharedSkills = false
    })
}

const shareSkill = () => {
  if (!doesShareAlreadyExist.value) {
    loading.value.sharedSkills = true
    let sharedProjectId = allProjectsConstant
    if (!shareWithAllProjects.value) {
      sharedProjectId = selectedProject.value.projectId
    }
    SkillsShareService.shareSkillToAnotherProject(projectId, selectedSkill.value.skillId, sharedProjectId)
      .then(() => {
        loading.value.sharedSkills = true
        selectedProject.value = null
        const skillId = selectedSkill.value.skillId
        selectedSkill.value = null
        loadSharedSkills().then(() => {
          const sharedWith = sharedProjectId === allProjectsConstant ? 'All Projects' : sharedProjectId
          announcer.assertive(`Skill with id of ${skillId} was shared with ${sharedWith}`)
        })
      })
  }
}

const doesShareAlreadyExist = computed(() => {
  if(!selectedSkill.value || (!selectedProject.value && !shareWithAllProjects.value)) {
    return false;
  }
  const alreadyExist = sharedSkills.value.find((entry) => entry.skillId === selectedSkill.value.skillId && (!entry.projectId || shareWithAllProjects.value || entry.projectId === selectedProject.value.projectId))
  if (alreadyExist) {
    if (alreadyExist.sharedWithAllProjects) {
      errorMessage.value = `Skill <strong>[${selectedSkill.value.name}]</strong> is already shared to <strong>[All Projects]</strong>.`
    } else {
      errorMessage.value = `Skill <strong>[${selectedSkill.value.name}]</strong> is already shared to project <strong>[${alreadyExist.projectName}]</strong>.`
    }
  }
  return alreadyExist
});

const deleteSharedSkill = (itemToRemove) => {
  loading.value.sharedSkills = true
  let sharedProjectId = allProjectsConstant
  if (!itemToRemove.sharedWithAllProjects) {
    sharedProjectId = itemToRemove.projectId
  }
  SkillsShareService.deleteSkillShare(projectId, itemToRemove.skillId, sharedProjectId)
    .then(() => {
      loadSharedSkills()
    }).finally(() => {
    const sharedWith = sharedProjectId === allProjectsConstant ? 'All Projects' : sharedProjectId
    announcer.assertive(`Removed shared skill with id ${itemToRemove.skillId} from ${sharedWith}`)
  })
}

const onSelectedProject = (item) => {
  selectedProject.value = item
}

const onUnSelectedProject = () => {
  selectedProject.value = null
}

const onSelectedSkill = (item) => {
  selectedSkill.value = item
}

const onDeselectedSkill = () => {
  selectedSkill.value = null
}

const onShareWithAllProjects = (checked) => {
  if (checked) {
    selectedProject.value = null
  }
}
</script>

<template>
  <Card class="mb-3"
        :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }"
        data-cy="shareSkillsWithOtherProjectsCard">
    <template #header>
      <SkillsCardHeader title="Share skills from this project with other projects"></SkillsCardHeader>
    </template>
    <template #content>
      <no-content2 v-if="restrictedUserCommunity" title="Cannot Be Added" icon="fas fa-shield-alt"
                   class="my-5 mx-4" data-cy="restrictedUserCommunityWarning">
        This project's access is
        restricted to <b class="text-primary">{{ appConfig.userCommunityRestrictedDescriptor }}</b> users
        only and its skills <b class="text-primary">cannot</b> be added as dependencies in other Projects.
      </no-content2>
      <div v-if="!restrictedUserCommunity">
        <div class="p-3">
          <div class="flex gap-4 flex-wrap flex-column lg:flex-row">
            <div class="flex flex-1 ">
              <skills-selector :options="allSkills"
                               v-on:removed="onDeselectedSkill"
                               v-on:added="onSelectedSkill"
                               placeholder="Select Skill"
                               placeholder-icon="fas fa-search"
                               :selected="selectedSkill"
                               data-cy="skillSelector"
                               :onlySingleSelectedValue="true" />
            </div>
            <div class="flex flex-1">
              <project-selector :project-id="projectId" :selected="selectedProject"
                                v-on:selected="onSelectedProject"
                                v-on:unselected="onUnSelectedProject"
                                :only-single-selected-value="true"
                                :showClear="true"
                                :disabled="shareWithAllProjects">

              </project-selector>
            </div>
          </div>

          <div class="flex gap-4 mt-2">
            <div class="flex flex-1 justify-content-end">
              <Checkbox v-model="shareWithAllProjects" inputId="shareToggle" @change="onShareWithAllProjects" :disabled="selectedProject !== null"
                        :binary="true" data-cy="shareWithAllProjectsCheckbox"></Checkbox>
              <label for="shareToggle" class="ml-1">Share With All Projects</label>

              <Button size="small" v-on:click="shareSkill" class="ml-4"
                      aria-label="Share skill with another project"
                      :disabled="!shareButtonEnabled || doesShareAlreadyExist" data-cy="shareButton">
                <i class="fas fa-share-alt mr-1"></i><span class="text-truncate">Share</span>
              </Button>
            </div>
          </div>
        </div>
        <Message v-if="doesShareAlreadyExist" severity="error">
          <span v-html="errorMessage"></span>
        </Message>

        <div v-if="sharedSkills && sharedSkills.length > 0" class="my-4">
          <shared-skills-table :shared-skills="sharedSkills"
                               v-on:skill-removed="deleteSharedSkill"></shared-skills-table>
        </div>
        <div v-else>
          <no-content2 title="Not Selected Yet..." icon="fas fa-share-alt" class="p-5"
                       message="To make your project's skills eligible please select a skill and then the project that you want to share this skill with." />
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>