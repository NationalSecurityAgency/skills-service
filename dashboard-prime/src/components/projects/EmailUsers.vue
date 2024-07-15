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
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import ProjectService from '@/components/projects/ProjectService';
import SkillsService from '@/components/skills/SkillsService.js';
import BadgesService from '@/components/badges/BadgesService.js';
import SubjectsService from '@/components/subjects/SubjectsService'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import SkillsDropDown from "@/components/utils/inputForm/SkillsDropDown.vue";
import SkillsInputSwitch from "@/components/utils/inputForm/SkillsInputSwitch.vue";
import LevelService from "@/components/levels/LevelService.js";
import {object, string} from "yup";
import {useForm} from "vee-validate";
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";

const dialogMessages = useDialogMessages()
const route = useRoute();
const announcer = useSkillsAnnouncer();
const appConfig = useAppConfig();
const appInfo = useAppInfoState()
const emailFeatureConfigured = computed(() => { return appInfo.emailEnabled });

const schema = object({
  subjectLine: string().required().max(appConfig.descriptionMaxLength).customDescriptionValidator('Subject Line', false).label('Subject Line'),
  emailBody: string().required().max(appConfig.descriptionMaxLength).customDescriptionValidator('Email Body', false).label('Email Body')
});

const { defineField, meta } = useForm({
  validationSchema: schema,
})

const [emailBody] = defineField('emailBody');
const [subjectLine] = defineField('subjectLine');

const sentMsg = ref('');
const maxCriteria = ref(15);
const alreadyApplied = ref(false);
const currentCount = ref(0);
const tags = ref([]);
const selectedItem = ref('');
const emailSent = ref(false);
const emailing = ref(false);
const loading = ref({
  subjects: false,
  skills: false,
  levels: false,
  badges: false,
});
const currentFilterType = ref('');
const levels = ref({
  selected: '',
  available: [
    { value: '', text: 'Optionally select level' },
    { value: 1, text: 'Level 1' },
    { value: 2, text: 'Level 2' },
    { value: 3, text: 'Level 3' },
    { value: 4, text: 'Level 4' },
    { value: 5, text: 'Level 5' },
  ],
});
const skills = ref({
  achieved: true,
  selected: '',
  available: [],
});
const subjects = ref({
  selected: '',
  available: [],
});
const badges = ref({
  selected: '',
  available: [],
});
const criteria = ref({
  projectId: '',
  projectLevel: '',
  subjectLevels: [],
  badgeIds: [],
  achievedSkillIds: [],
  notAchievedSkillIds: [],
  allProjectUsers: false,
});

const filterOptions = [
  {
    text: 'Project',
    value: 'project',
  },
  {
    text: 'Badge',
    value: 'badge',
  },
  {
    text: 'Subject',
    value: 'subject',
  },
  {
    text: 'Skill',
    value: 'skill',
  }
]

const nameSort = (one, two) => {
  const nameOne = one.name.toUpperCase();
  const nameTwo = two.name.toUpperCase();
  if (nameOne < nameTwo) {
    return -1;
  }
  if (nameOne > nameTwo) {
    return 1;
  }
  return 0;
};

onMounted(() => {
  loading.value.skills = true;
  SkillsService.getProjectSkills(route.params.projectId).then((newSkills) => {
    skills.value.available = newSkills;
    skills.value.available.sort(nameSort);
  }).finally(() => {
    loading.value.skills = false;
  });

  loading.value.badges = true;

  BadgesService.getBadges(route.params.projectId).then((newBadges) => {
    badges.value.available = newBadges;
    badges.value.available.sort(nameSort);
  }).finally(() => {
    loading.value.badges = false;
  });

  loading.value.subjects = true;
  SubjectsService.getSubjects(route.params.projectId).then((newSubjects) => {
    subjects.value.available = newSubjects;
    subjects.value.available.sort(nameSort);
  }).finally(() => {
    loading.value.subjects = false;
  });
})

watch(currentFilterType, (newVal) => {
  levels.value.available = [];
  levels.value.selected = '';
  subjects.value.selected = null;
  skills.value.selected = null;
  badges.value.selected = null;
  selectedItem.value = '';

  if (newVal === 'project') {
    loading.value.levels = true;
    LevelService.getLevelsForProject(route.params.projectId).then((newLevels) => {
      levels.value.available = newLevels?.map((level) => ({ value: level.level, text: level.level }));
      levels.value.available.unshift({ value: null, text: 'Any Level' });
      levels.value.selected = null;
    }).finally(() => {
      loading.value.levels = false;
    });
  }
});

watch(() => subjects.value.selected, (subject) => {
  if (subject) {
    loading.value.levels = true;
    levels.value.selected = '';
    LevelService.getLevelsForSubject(route.params.projectId, subject.subjectId).then((newLevels) => {
      levels.value.available = newLevels?.map((level) => ({ value: level.level, text: level.level }));
    }).finally(() => {
      loading.value.levels = false;
    });
  }
});

watch(selectedItem, (newVal) => {
  if (!newVal) {
    return;
  }

  switch (currentFilterType.value) {
    case 'subject':
      subjects.value.selected = newVal;
      break;
    case 'badge':
      badges.value.selected = newVal;
      break;
    case 'skill':
      skills.value.selected = newVal;
      break;
    default:
      // eslint-disable-next-line no-console
      console.error(`selectedItem does not support filter type ${currentFilterType.value}, value [${newVal}]`);
  }
});

const levelsDisabled = computed(() => {
  let disabled = true;
  if (currentFilterType.value === 'project') {
    disabled = false;
  } else if (currentFilterType.value === 'subject' && subjects.value.selected) {
    disabled = false;
  }
  return disabled;
});

const ids = computed(() => {
  let ids = [];
  switch (currentFilterType.value) {
    case 'subject':
      ids = subjects.value.available;
      break;
    case 'badge':
      ids = badges.value.available;
      break;
    case 'skill':
      ids = skills.value.available;
      break;
    default:
      // eslint-disable-next-line no-console
      console.error(`ids does not support filter type ${currentFilterType.value}`);
  }
  return ids;
});

const isAddDisabled = computed(() => {
  if (criteria.value.allProjectUsers) {
    return true;
  }
  let disabled = true;
  if (currentFilterType.value === 'project') {
    // can add just the project id to contact all users in the project
    disabled = false;
  } else if (currentFilterType.value === 'subject') {
    disabled = !subjects.value.selected || !levels.value.selected;
  } else if (currentFilterType.value === 'skill') {
    disabled = !skills.value.selected;
  } else if (currentFilterType.value === 'badge') {
    disabled = !badges.value.selected;
  }
  return disabled;
});

const maxTagsReached = computed(() => {
  return tags.value.length === maxCriteria.value;
});

const isEmailDisabled = computed(() => {
  return !emailBody.value || !subjectLine.value || emailing.value || emailSent.value || tags.value.length < 1 || currentCount.value < 1;
});

const isPreviewDisabled = computed(() => {
  return !emailBody.value || !subjectLine.value;
});

const selectText = computed(() => {
  let text = '';
  if (currentFilterType.value === 'skill') {
    text = 'Select Skill';
  } else if (currentFilterType.value === 'badge') {
    text = 'Select Badge';
  } else if (currentFilterType.value === 'subject') {
    text = 'Select Subject';
  }
  return text;
});

const addCriteria = () => {
  let tag = null;
  const c = criteria.value;
  switch (currentFilterType.value) {
    case 'project':
      if (levels.value.selected === '' || levels.value.selected === null) {
        tag = {
          display: 'All Users',
          type: currentFilterType.value,
        };
        criteria.value.allProjectUsers = true;
      } else {
        tag = {
          display: `Level ${levels.value.selected} or greater`,
          type: currentFilterType.value,
          projectLevel: levels.value.selected,
        };
        c.projectLevel = levels.value.selected;
      }
      tag.projectId = route.params.projectId;
      c.projectId = tag.projectId;
      break;
    case 'subject':
      tag = {
        display: `Level ${levels.value.selected} or greater in Subject ${subjects.value.selected.name}`,
        type: currentFilterType.value,
        subjectId: subjects.value.selected.subjectId,
      };
      // eslint-disable-next-line no-case-declarations
      const slC = { subjectId: subjects.value.selected.subjectId, level: levels.value.selected };
      if (!arrayContainsClosure(c.subjectLevels, (arrEl) => arrEl.subjectId && arrEl.subjectId === slC.subjectId)) {
        c.subjectLevels.push(slC);
      }
      break;
    case 'skill':
      // eslint-disable-next-line no-case-declarations
      const selectedId = skills.value.selected.skillId;
      tag = {
        display: `${skills.value.achieved ? '' : 'Not '}Achieved Skill ${skills.value.selected.name}`,
        type: currentFilterType.value,
        skillId: selectedId,
        achieved: skills.value.achieved,
      };
      if (skills.value.achieved && c.achievedSkillIds.indexOf(selectedId) < 0) {
        c.achievedSkillIds.push(selectedId);
      } else if (c.notAchievedSkillIds.indexOf(selectedId) < 0) {
        c.notAchievedSkillIds.push(selectedId);
      }
      break;
    case 'badge':
      // eslint-disable-next-line no-case-declarations
      const selectedBadgeId = badges.value.selected.badgeId;
      tag = {
        display: `Achieved Badge ${badges.value.selected.name}`,
        badgeId: selectedBadgeId,
        type: currentFilterType.value,
      };
      if (c.badgeIds.indexOf(selectedBadgeId) < 0) {
        c.badgeIds.push(selectedBadgeId);
      }
      break;
    default:
      // eslint-disable-next-line no-console
      console.error(`unrecognized filter type ${currentFilterType.value}`);
  }
  handleTagAdd(tag);
};

const handleTagAdd = (tag) => {
  if (!tag) {
    return;
  }
  const contained = tagAlreadyExists(tag);
  if (contained) {
    alreadyApplied.value = true;
    setTimeout(() => { alreadyApplied.value = false; }, 2000);
    return;
  }

  const addTagAndUpdate = () => {
    updateCount(tag);
    tags.value.push(tag);
    resetSelections();
  };
  if (criteria.value.allProjectUsers) {
    if (tags.value.length > 0) {
      dialogMessages.msgConfirm({
        message: 'Adding the All Users filter will remove all other filters',
        header: 'Remove Other Filters?',
        acceptLabel: 'YES, Remove Them!',
        rejectLabel: 'Cancel',
        accept: () => {
          resetCriteria(true);
          resetTags();
          addTagAndUpdate();
        },
        reject: () => {
          criteria.value.allProjectUsers = false;
        }
      })
    } else {
      resetCriteria(true);
      resetTags();
      addTagAndUpdate();
    }
  } else {
    addTagAndUpdate();
  }
};

const resetSelections = () => {
  badges.value.selected = null;
  levels.value.selected = null;
  subjects.value.selected = null;
  skills.value.selected = null;
  skills.value.achieved = true;
  selectedItem.value = null;
};

const resetCriteria = (allProjects) => {
  criteria.value.projectLevel = '';
  criteria.value.subjectLevels = [];
  criteria.value.badgeIds = [];
  criteria.value.achievedSkillIds = [];
  criteria.value.notAchievedSkillIds = [];
  criteria.value.allProjectUsers = allProjects;
};

const resetTags = () => {
  tags.value.splice(0, tags.value.length);
};

const tagAlreadyExists = (tag) => {
  const isProjectLevelTag = tag.type === 'project' && Object.prototype.hasOwnProperty.call(tag, 'projectLevel');
  const isSubjectLevelTag = tag.type === 'subject' && Object.prototype.hasOwnProperty.call(tag, 'subjectId');
  const searchObj = JSON.stringify(tag);

  return arrayContainsClosure(tags.value, (arrEl) => {
    if (isProjectLevelTag) {
      return arrEl.type === 'project' && Object.prototype.hasOwnProperty.call(arrEl, 'projectLevel');
    }
    if (isSubjectLevelTag) {
      return arrEl.type === 'subject' && arrEl.subjectId === tag.subjectId;
    }
    return JSON.stringify((arrEl)) === searchObj;
  });
};

const arrayContainsClosure = (array, closure) => {
  let exists = false;
  // eslint-disable-next-line no-plusplus
  for (let i = 0; i < array.length; i++) {
    const t = array[i];
    if (closure(t)) {
      exists = true;
      break;
    }
  }
  return exists;
};

const deleteCriteria = (tag) => {
  switch (tag.type) {
    case 'project':
      if (tag.projectLevel) {
        criteria.value.projectLevel = null;
      } else {
        criteria.value.allProjectUsers = false;
      }
      break;
    case 'subject':
      // eslint-disable-next-line no-unused-expressions
      removeFromArray(criteria.value.subjectLevels, (el) => el.subjectId === tag.subjectId);
      break;
    case 'badge':
      // eslint-disable-next-line no-unused-expressions
      removeFromArray(criteria.value.badgeIds, (el) => el === tag.badgeId);
      break;
    case 'skill':
      // eslint-disable-next-line no-case-declarations
      const arrToUse = tag.achieved ? criteria.value.achievedSkillIds : criteria.value.notAchievedSkillIds;
      // eslint-disable-next-line no-unused-expressions
      removeFromArray(arrToUse, (el) => el === tag.skillId);
      break;
    default:
      // eslint-disable-next-line no-console
      console.error(`unrecognized user criteria type ${tag.type}`);
  }
  removeFromArray(tags.value, (el) => el === tag);
  nextTick(() => announcer.polite(`${tag.display} criteria has been removed`));
  updateCount();
};

const removeFromArray = (array, findCallback) => {
  const element = array.find(findCallback);
  if (element) {
    const idx = array.indexOf(element);
    array.splice(idx, 1);
  }
};

const updateCount = (latestTag) => {
  ProjectService.countUsersMatchingCriteria(route.params.projectId, criteria.value).then((count) => {
    let msg = `There are ${count} Project Users matching your specified criteria`;
    if (latestTag) {
      msg = `Added criteria ${latestTag.display}, there are ${count} matching Project Users`;
    }
    setTimeout(() => announcer.polite(msg), 750);
    currentCount.value = count;
  });
};

const emailUsers = () => {
  emailing.value = true;
  sentMsg.value = `Email${currentCount.value > 1 ? 's' : ''} sent!`;
  ProjectService.contactUsers(route.params.projectId, {
    queryCriteria: criteria.value,
    emailBody: emailBody.value,
    emailSubject: subjectLine.value,
  }).then(() => {
    emailSent.value = true;
    nextTick(() => {
      resetTags();
      resetCriteria();
      emailBody.value = '';
      subjectLine.value = '';
      currentCount.value = 0;
    });
    setTimeout(() => { emailSent.value = false; }, 8000);
  }).finally(() => {
    emailing.value = false;
  });
};

const previewEmail = () => {
  emailing.value = true;
  sentMsg.value = 'Preview email sent!';
  ProjectService.previewEmail(route.params.projectId, {
    emailBody: emailBody.value,
    emailSubject: subjectLine.value,
  }).then(() => {
    emailSent.value = true;
    announcer.polite('Email has been sent to selected users');
    setTimeout(() => { emailSent.value = false; }, 8000);
  }).finally(() => {
    emailing.value = false;
  });
};

</script>

<template>
  <div id="contact-users-panel">
    <sub-page-header title="Contact Users" />

    <Card>
      <template #content>
        <Message severity="warn" data-cy="contactUsers_emailServiceWarning" v-if="!emailFeatureConfigured" :closable="false">
          Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
        </Message>

        <BlockUI :blocked="!emailFeatureConfigured">
          <div class="font-bold text-lg  uppercase mb-3">Filters</div>
          <div class="">
            <SkillsDropDown
              label="Type"
              name="filterSelector"
              id="filterSelector"
              :disabled="criteria.allProjectUsers || !emailFeatureConfigured"
              data-cy="filterSelector"
              class="w-full"
              placeholder="Select a filter to add"
              optionLabel="text"
              optionValue="value"
              v-model="currentFilterType"
              :options="filterOptions" />
            <div class="flex flex-1 gap-2 flex-column ml-4">
              <SkillsDropDown
                  v-if="currentFilterType && currentFilterType !== 'project'"
                  :disabled="(currentFilterType && currentFilterType === 'project') || !emailFeatureConfigured"
                  label="Name"
                  name="name-selector"
                  id="name-selector"
                  data-cy="name-selector"
                  :placeholder="selectText"
                  optionLabel="name"
                  v-model="selectedItem"
                  :options="ids" />

              <SkillsDropDown
                  v-if="currentFilterType === 'project' || currentFilterType === 'subject'"
                  label="Minimum Level"
                  name="emailUsers-levelsInput"
                  id="emailUsers-levelsInput"
                  data-cy="emailUsers-levelsInput"
                  optionLabel="text"
                  optionValue="value"
                  placeholder="Any Level"
                  :disabled="(levelsDisabled || criteria.allProjectUsers) || !emailFeatureConfigured"
                  v-model="levels.selected"
                  :options="levels.available" />

              <div class="flex mb-3" v-if="currentFilterType && currentFilterType==='skill'">
                <SkillsInputSwitch
                    v-model="skills.achieved"
                    inputId="skillAchieved"
                    name="achieved-button"
                    label="test"
                    :disabled="!emailFeatureConfigured"
                    data-cy=skillAchievedSwitch />
                <label for="skillAchieved" class="ml-2"> {{ skills.achieved ? 'Achieved' : 'Not Achieved' }} </label>
              </div>
            </div>
            <div>
              <SkillsButton class="ml-4" @click="addCriteria" data-cy="emailUsers-addBtn" :track-for-focus="true" id="addCriteriaButton"
                            :disabled="isAddDisabled || maxTagsReached || !emailFeatureConfigured" label="Add" icon="fas fa-plus-circle" />
              <transition name="fade">
                <span v-if="alreadyApplied" data-cy="filterExists" class="pt-2 pl-1" role="alert">Filter already exists</span>
              </transition>
              <span v-if="maxTagsReached" data-cy="maxFiltersReached" class="text-warning pt-2 pl-1">Only {{maxCriteria}} filters are allowed</span>
            </div>
          </div>

          <div class="pr-3 pb-3 pt-4 mr-3 ml-1">
            <div class="flex flex-wrap gap-2 pb-3 mt-1 ml-3">
              <Chip v-for="(tag) of tags" :key="tag.display" :label="tag.display" data-cy="filterBadge" removable @remove="deleteCriteria(tag)" />
            </div>
            <div class="h5 uppercase" data-cy="usersMatchingFilters"><Badge variant="info">{{currentCount}}</Badge> Users Selected</div>
          </div>

          <hr />

          <div class="py-2 font-bold text-lg uppercase">Email Content</div>
          <div class="mt-2">
            <SkillsTextInput name="subjectLine" label="Subject Line" data-cy="emailUsers_subject" class="w-full" :disabled="!emailFeatureConfigured" />
          </div>
          <div class="flex w-full">
              <markdown-editor class="w-full"
                               data-cy="emailUsers_body"
                               label="Email Body"
                               name="emailBody"
                               v-if="emailFeatureConfigured"
                               :resizable="false"
                               :allow-attachments="false"
                               :use-html="true"/>
            <SkillsTextarea v-else data-cy="emailUsers_body"
                            label="Email Body"
                            class="w-full"
                            disabled
                            rows="5"
                            name="emailBody">
            </SkillsTextarea>
          </div>

          <div class="flex ">
            <SkillsButton class="mr-3" data-cy="previewUsersEmail"
                          :disabled="isPreviewDisabled || !meta.valid || !emailFeatureConfigured"
                          @click="previewEmail"
                          label="Preview"
                          :icon="emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-eye'"
                          aria-label="preview email to project users" />
            <SkillsButton class="mr-1" @click="emailUsers" data-cy="emailUsers-submitBtn"
                          label="Email"
                          :icon="emailing ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fas fa-mail-bulk'"
                          :disabled="isEmailDisabled || !meta.valid || !emailFeatureConfigured" />
            <transition name="fade">
              <span v-if="emailSent" class="pt-2 pl-1" data-cy="emailSent"><i class="far fa-check-square text-success"/> {{ sentMsg }}</span>
            </transition>
          </div>
        </BlockUI>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
