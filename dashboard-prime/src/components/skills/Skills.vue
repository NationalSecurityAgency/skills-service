<script setup>
import { computed, ref } from 'vue'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'

const projConfig = useProjConfig()
const isLoading = computed(() => {
  // return this.loadingSubjectSkills || this.isLoadingProjConfig;
  return false
})

const addSkillDisabled = computed(() => {
  // return this.skills && this.$store.getters.config && this.numSubjectSkills >= this.$store.getters.config.maxSkillsPerSubject;
  return false
})

const showImportDialog = ref(false)
const newGroup = () => {
  // this.editGroupInfo = {
  //   isEdit: false,
  //   show: true,
  //   group: {
  //     projectId: this.projectId,
  //     subjectId: this.subject.subjectId,
  //     type: 'SkillsGroup',
  //   },
  // };
}

const newSkill = () => {
  // this.editSkillInfo = {
  //   skill: {
  //     projectId: this.projectId,
  //     subjectId: this.subject.subjectId,
  //     type: 'Skill',
  //   },
  //   show: true,
  //   isEdit: false,
  //   isCopy: false,
  // };
}

</script>

<template>
  <div>
    <!--    :disabled="addSkillDisabled"-->
    <!--    :disabled-msg="addSkillsDisabledMsg"-->
    <sub-page-header
      ref="subPageHeader"
      title="Skills"
      :is-loading="isLoading"
      aria-label="new skill">
      <div v-if="!projConfig.isReadOnlyProj">
        <!--        <i v-if="addSkillDisabled" class="fas fa-exclamation-circle text-warning ml-1 mr-1"-->
        <!--           style="pointer-events: all; font-size: 1.5rem;"-->
        <!--           :aria-label="addSkillsDisabledMsg"-->
        <!--           v-b-tooltip.hover="addSkillsDisabledMsg"/>-->
        <SkillsButton
          id="importFromCatalogBtn"
          ref="importFromCatalogBtn"
          label="Import"
          outlined
          class="bg-primary-reverse"
          icon="fas fa-book"
          @click="showImportDialog=true"
          size="small"
          aria-label="import from catalog"
          data-cy="importFromCatalogBtn" />
        <SkillsButton
          id="newGroupBtn"
          ref="newGroupButton"
          label="Group"
          icon="fas fa-plus-circle"
          @click="newGroup"
          size="small"
          outlined
          class="bg-primary-reverse ml-1"
          aria-label="new skills group"
          aria-describedby="newGroupSrText"
          data-cy="newGroupButton"
          :aria-disabled="addSkillDisabled"
          :disabled="addSkillDisabled" />
        <SkillsButton
          id="newSkillBtn"
          ref="newSkillButton"
          label="Skill"
          icon="fas fa-plus-circle"
          @click="newSkill"
          variant="outline-primary"
          size="small"
          aria-label="new skill"
          aria-describedby="newSkillSrText"
          data-cy="newSkillButton"
          outlined
          class="bg-primary-reverse ml-1"
          :aria-disabled="addSkillDisabled"
          :disabled="addSkillDisabled"/>
      </div>
    </sub-page-header>


    <Card>
      <template #content>
        <no-content2
          title="No Skills Yet"
          class="my-5"
          message="Projects are composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework." />
      </template>
    </Card>

  </div>
</template>

<style scoped></style>
