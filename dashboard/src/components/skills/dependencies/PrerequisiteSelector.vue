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
import { ref, nextTick, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useForm } from 'vee-validate';
import { object, string } from 'yup'
import SkillsSelector from "@/components/skills/SkillsSelector.vue";
import SkillsService from '@/components/skills/SkillsService';
import SkillsShareService from '@/components/skills/crossProjects/SkillsShareService.js';
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

const props = defineProps(['selectedFromSkills']);
const emit = defineEmits(['updateSelectedFromSkills', 'clearSelectedFromSkills', 'update'])
const announcer = useSkillsAnnouncer();
const route = useRoute();

const { errors, defineField } = useForm({
  validationSchema: object({
    toSkillId: string().label('To Skill').required(false).test(validate)
  })
});
const [toSkillId, toSkillIdAttrs] = defineField('toSkillId');

const allSkills = ref([]);
const allPotentialSkills = ref([]);
const selectedToSkills = ref([]);
const toSkillName = ref(null);
const toProjectId = ref(null);
const loadingPotentialSkills = ref(false);
const loadingAllSkills = ref(false);
const projectId = route.params.projectId;

const fromSelector = ref();
const toSelector = ref();

onMounted(() => {
  loadAllSkills();
});

watch(() => props.selectedFromSkills, async () => {
    clearToData();
    updatePotentialSkills();
  },
);

const isLoading = computed(() => {
  return loadingPotentialSkills.value || loadingAllSkills.value;
});

const loadAllSkills = () => {
  loadingAllSkills.value = true;
  const getProjectSkillsAndBadges = SkillsService.getProjectSkillsAndBadgesWithImportedSkills(projectId);
  const getSharedSkills = SkillsShareService.getSharedWithmeSkills(projectId);

  Promise.all([getProjectSkillsAndBadges, getSharedSkills]).then((results) => {
    const mainSkills = results[0];
    const sharedSkills = results[1];
    if (sharedSkills && sharedSkills.length > 0) {
      sharedSkills.forEach((skill) => {
        const newSkill = {
          name: skill.skillName,
          type: 'Shared Skill',
          ...skill,
        };
        mainSkills.push(newSkill);
      });
    }
    allSkills.value = mainSkills;
    loadingAllSkills.value = false;
  });
};

const updatePotentialSkills = () => {
  loadingPotentialSkills.value = true;
  SkillsService.getProjectSkillsAndBadgesWithImportedSkills(projectId)
      .then((skills) => {
        if (props.selectedFromSkills && props.selectedFromSkills.skillId) {
          allPotentialSkills.value = skills.filter((skill) => (skill.skillId !== props.selectedFromSkills.skillId || (skill.skillId === props.selectedFromSkills.skillId && skill.projectId !== props.selectedFromSkills.projectId)));
        }
        loadingPotentialSkills.value = false;
      });
};

const onToSelected = (item) => {
  if(item) {
    toSkillId.value = item.skillId;
    toSkillName.value = item.name;
    toProjectId.value = item.projectId;
  } else {
    toSkillId.value = null;
    toSkillName.value = null;
    toProjectId.value = null;
  }
};

const onFromSelected = (item) => {
  clearToData();
  emit('updateSelectedFromSkills', item);
}

const onAddPath = () => {
  SkillsService.assignDependency(toProjectId.value, toSkillId.value, props.selectedFromSkills.skillId, props.selectedFromSkills.projectId)
    .then(() => {
      const from = props.selectedFromSkills.name;
      const to = toSkillName.value;
      if (toProjectId.value === props.selectedFromSkills.projectId) {
        SkillsReporter.reportSkill('CreateSkillDependencies');
      } else {
        SkillsReporter.reportSkill('CreateCrossProjectSkillDependencies');
      }
      nextTick(() => announcer.assertive(`Successfully added Learning Path from ${from} to ${to}`));
      clearData();
      emit('update');
  });
};

const clearData = () => {
  emit('clearSelectedFromSkills');
  fromSelector.value.clearValue();
  clearToData();
};

const clearToData = () => {
  allPotentialSkills.value = [];
  selectedToSkills.value = [];
  toSkillId.value = null;
  toSkillName.value = null;
  toProjectId.value = null;
  toSelector.value.clearValue();
};

function validate(value, ctx) {
  if (!toProjectId.value || !toSkillId.value || !props.selectedFromSkills.skillId || !props.selectedFromSkills.projectId) {
    return true;
  }
  return SkillsService.validateDependency(toProjectId.value, toSkillId.value, props.selectedFromSkills.skillId, props.selectedFromSkills.projectId).then((res) => {
    if (res.possible) {
      return true;
    }

    let reason = '';
    if (res.failureType && res.failureType === 'CircularLearningPath') {
      const additionalBadgeMsg = res.violatingSkillInBadgeName ? `under the badge <b>${res.violatingSkillInBadgeName}</b> ` : '';
      reason = `<b>${toSkillName.value}</b> already exists in the learning path ${additionalBadgeMsg}and adding it again will cause a <b>circular/infinite learning path</b>.`;
    } else if (res.failureType && res.failureType === 'BadgeOverlappingSkills') {
      reason = 'Multiple badges on the same Learning path cannot have overlapping skills. '
      + `Both <b>${res.violatingSkillInBadgeName}</b> badge and <b>${toSkillName.value}</b> badge have <b>${res.violatingSkillName}</b> skill.`;
    } else if (res.failureType && res.failureType === 'BadgeSkillIsAlreadyOnPath') {
      reason = res.reason.replace(/\[/g, "<b>").replace(/\]/g, "</b>");
    } else if (res.failureType && res.failureType === 'AlreadyExist') {
      reason = `Learning path from <b>${res.violatingSkillName}</b> to <b>${toSkillName.value}</b> already exists.`;
    } else if (res.failureType && res.failureType === 'SkillInCatalog') {
      reason = `Skill <b>${toSkillName.value}</b> was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path.`;
    } else if (res.failureType && res.failureType === 'ReusedSkill') {
      reason = `Skill <b>${toSkillName.value}</b> was reused in another subject or group and cannot have prerequisites in the learning path.`;
    } else {
      reason = res.reason;
    }

    const div = document.createElement('div');
    div.innerHTML = reason;
    const reasonWithoutHtmlTags = div.textContent || div.innerText || '';
    // nextTick(() => announcer.polite(`Learning Path item cannot be added. ${reasonWithoutHtmlTags}`));
    return ctx.createError({ message: reason });
  });
}
</script>

<template>
  <Card style="margin-bottom:10px;" data-cy="addPrerequisiteToLearningPath">
    <template #header>
      <SkillsCardHeader title="Add a new item to the learning path"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex gap-2 flex-wrap flex-col lg:flex-row lg:items-end">
        <div class="flex-1 field">
          <label for="learningItemFromInput">From:</label>
          <skills-selector :options="allSkills"
                           :is-loading="isLoading"
                           :selected="selectedFromSkills"
                           ref="fromSelector"
                           data-cy="learningPathFromSkillSelector"
                           id="learningItemFromInput"
                           inputId="learningItemFromInput"
                           placeholder="From Skill or Badge"
                           placeholder-icon="fas fa-search"
                           aria-label="Select a skill or a badge for the Learning Path's from step"
                           v-on:added="onFromSelected"
                           :showType=true />
        </div>
        <div class="flex-1 field">
          <label for="learningItemToInput">To:</label>
          <skills-selector :options="allPotentialSkills"
                           :is-loading="isLoading"
                           ref="toSelector"
                           data-cy="learningPathToSkillSelector"
                           id="learningItemToInput"
                           inputId="learningItemToInput"
                           placeholder="To Skill or Badge"
                           placeholder-icon="fas fa-search"
                           :selected="selectedToSkills"
                           v-on:added="onToSelected"
                           :disabled="!selectedFromSkills || !selectedFromSkills.skillId"
                           :showType=true />
        </div>
        <div class="field text-center">
          <SkillsButton @click="onAddPath"
                        class="mt-4"
                        icon="fas fa-plus-circle"
                        label="Add"
                        data-cy="addLearningPathItemBtn"
                        aria-label="Add item to the learning path"
                        :disabled="!selectedFromSkills || !selectedFromSkills.skillId || !toSkillId || !!errors.toSkillId">
          </SkillsButton>
        </div>
      </div>
      <div>
        <input v-model="toSkillId" v-bind="toSkillIdAttrs" class="hidden" aria-hidden="true" aria-label="Used to validate learning path route"/>
        <Message v-if="errors.toSkillId" severity="error" data-cy="learningPathError" :closable="false"><span v-html="errors.toSkillId" /></Message>
      </div>
    </template>
  </Card>
</template>

<style scoped></style>