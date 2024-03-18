<script setup>
import { ref, nextTick, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import SkillsSelector from "@/components/skills/SkillsSelector.vue";
import SkillsService from '@/components/skills/SkillsService';
import SkillsShareService from '@/components/skills/crossProjects/SkillsShareService.js';
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

const props = defineProps(['selectedFromSkills']);
const emit = defineEmits(['updateSelectedFromSkills', 'clearSelectedFromSkills', 'update'])
const announcer = useSkillsAnnouncer();
const route = useRoute();

const allSkills = ref([]);
const allPotentialSkills = ref([]);
const selectedToSkills = ref([]);
const toSkillId = ref(null);
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
        if (props.selectedFromSkills.length > 0 && props.selectedFromSkills[0].skillId) {
          allPotentialSkills.value = skills.filter((skill) => (skill.skillId !== props.selectedFromSkills[0].skillId || (skill.skillId === props.selectedFromSkills[0].skillId && skill.projectId !== props.selectedFromSkills[0].projectId)));
        }
        if (selectedToSkills.value.length > 0) {
          selectedToSkills.value.forEach((skill) => {
            allPotentialSkills.value = allPotentialSkills.value.filter((potentialSkill) => (potentialSkill.skillId !== skill.skillId || (potentialSkill.skillId === skill.skillId && potentialSkill.projectId !== skill.projectId)));
          });
        }
        loadingPotentialSkills.value = false;
      });
};

const onToSelected = (item) => {
  toSkillId.value = item.skillId;
  toSkillName.value = item.name;
  toProjectId.value = item.projectId;
};

const onToDeselected = () => {
  selectedToSkills.value = [];
  updatePotentialSkills();
};

const onFromSelectionRemoved = () => {
  // if ($refs && $refs.learningPathValidator) {
  //   clearData();
  //   $refs.learningPathValidator.reset();
  // }
};

const onToSelectionRemoved = () => {
  // if ($refs && $refs.learningPathValidator) {
  //   clearToData();
  //   $refs.learningPathValidator.reset();
  //   updatePotentialSkills();
  // }
};

const onFromSelected = (item) => {
  clearToData();
  emit('updateSelectedFromSkills', item);
}

const onFromDeselected = () => {
};

const onAddPath = () => {
  // $refs.validationObserver.validate()
  //     .then((res) => {
  //       if (res) {
          SkillsService.assignDependency(toProjectId.value, toSkillId.value, props.selectedFromSkills[0].skillId, props.selectedFromSkills[0].projectId)
              .then(() => {
                const from = props.selectedFromSkills[0].name;
                const to = toSkillName.value;
                if (toProjectId.value === props.selectedFromSkills[0].projectId) {
                  SkillsReporter.reportSkill('CreateSkillDependencies');
                } else {
                  SkillsReporter.reportSkill('CreateCrossProjectSkillDependencies');
                }
                nextTick(() => announcer.assertive(`Successfully added Learning Path from ${from} to ${to}`));
                clearData();
                emit('update');
              });
        // }
      // });
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

// registerValidation() {
//   const self = this;
//   extend('validLearningPath', {
//     validate() {
//       if (!self || !self.toProjectId || !self.toSkillId || !self.selectedFromSkills[0].skillId || !self.selectedFromSkills[0].projectId) {
//         return true;
//       }
//       return SkillsService.validateDependency(self.toProjectId, self.toSkillId, self.selectedFromSkills[0].skillId, self.selectedFromSkills[0].projectId)
//           .then((res) => {
//             if (res.possible) {
//               return true;
//             }
//
//             let reason = '';
//             if (res.failureType && res.failureType === 'CircularLearningPath') {
//               const additionalBadgeMsg = res.violatingSkillInBadgeName ? `under the badge <b>${res.violatingSkillInBadgeName}</b> ` : '';
//               reason = `<b>${self.toSkillName}</b> already exists in the learning path ${additionalBadgeMsg}and adding it again will cause a <b>circular/infinite learning path</b>.`;
//             } else if (res.failureType && res.failureType === 'BadgeOverlappingSkills') {
//               reason = 'Multiple badges on the same Learning path cannot have overlapping skills. '
//                   + `Both <b>${res.violatingSkillInBadgeName}</b> badge and <b>${self.toSkillName}</b> badge have <b>${res.violatingSkillName}</b> skill.`;
//             } else if (res.failureType && res.failureType === 'BadgeSkillIsAlreadyOnPath') {
//               reason = `Provided badge <b>${self.toSkillName}</b> has skill <b>${res.violatingSkillName}</b> which already exists on the learning path.`;
//             } else if (res.failureType && res.failureType === 'AlreadyExist') {
//               reason = `Learning path from <b>${res.violatingSkillName}</b> to <b>${self.toSkillName}</b> already exists.`;
//             } else if (res.failureType && res.failureType === 'SkillInCatalog') {
//               reason = `Skill <b>${self.toSkillName}</b> was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path.`;
//             } else if (res.failureType && res.failureType === 'ReusedSkill') {
//               reason = `Skill <b>${self.toSkillName}</b> was reused in another subject or group and cannot have prerequisites in the learning path.`;
//             } else {
//               reason = res.reason;
//             }
//
//             const div = document.createElement('div');
//             div.innerHTML = reason;
//             const reasonWithoutHtmlTags = div.textContent || div.innerText || '';
//             self.$nextTick(() => self.$announcer.polite(`Learning Path item cannot be added. ${reasonWithoutHtmlTags}`));
//             return `${reason}`;
//           });
//     },
//   });
// },

</script>

<template>
  <Card style="margin-bottom:10px;">
    <template #header>
      <div class="border-bottom-1 p-3 surface-border surface-100" data-cy="metricsCard-header">
        <span class="font-bold">Add a new item to the learning path</span>
      </div>
    </template>
    <template #content>
      <div class="flex gap-2">
        <div class="flex-1">
          <label for="learningItemFromInput">From:</label>
          <skills-selector :options="allSkills"
                           ref="fromSelector"
                           id="learningItemFromInput"
                           v-on:removed="onFromDeselected"
                           v-on:added="onFromSelected"
                           @selection-removed="onFromSelectionRemoved"
                           placeholder="From Skill or Badge"
                           placeholder-icon="fas fa-search"
                           aria-label="Select a skill or a badge for the Learning Path's from step"
                           :selected="selectedFromSkills"
                           data-cy="learningPathFromSkillSelector"
                           :showType=true
                           :onlySingleSelectedValue="true" />
        </div>
        <div class="flex-1">
          <label for="learningItemToInput">To:</label>
          <skills-selector :options="allPotentialSkills"
                           ref="toSelector"
                           id="learningItemToInput"
                           v-on:removed="onToDeselected"
                           v-on:added="onToSelected"
                           @selection-removed="onToSelectionRemoved"
                           :disabled="selectedFromSkills?.length === 0"
                           placeholder="To Skill or Badge"
                           placeholder-icon="fas fa-search"
                           :selected="selectedToSkills"
                           data-cy="learningPathToSkillSelector"
                           :showType=true
                           :onlySingleSelectedValue="true" />
        </div>
        <div>
          <Button @click="onAddPath"
                  class="mt-3"
                  data-cy="addLearningPathItemBtn"
                  aria-label="Add item to the learning path"
                  :disabled="selectedFromSkills?.length === 0 || !toSkillId">Add <i class="fas fa-plus-circle" aria-hidden="true"/></Button>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped></style>