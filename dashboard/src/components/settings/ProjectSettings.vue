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
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import Card from 'primevue/card'
import RadioButton from 'primevue/radiobutton'
import Checkbox from 'primevue/checkbox'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import LoadingContainer from '@/components/utils/LoadingContainer.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SettingService from '@/components/settings/SettingsService'
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import SkillsSettingTextInput from '@/components/settings/SkillsSettingTextInput.vue'
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import SettingsItem from "@/components/settings/SettingsItem.vue";
import ProjectService from '@/components/projects/ProjectService.js'

const dialogMessages = useDialogMessages()
const announcer = useSkillsAnnouncer();
const route = useRoute();
const appConfig = useAppConfig()
const projConfig = useProjConfig()

const publicNotDiscoverable = 'pnd';
const discoverableProgressAndRanking = 'dpr';
const privateInviteOnly = 'pio';

const schema = yup.object({
  helpUrlHost: yup.string().max(appConfig.maxHostLength).label('Root Help Url').urlValidator(),
  projectDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Project Display Text'),
  subjectDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Subject Display Text'),
  groupDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Group Display Text'),
  skillDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Skill Display Text'),
  levelDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Level Display Text'),
  pointDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Point Display Text'),
});

const { values, defineField, errors, meta, handleSubmit, setFieldValue, resetForm } = useForm({
  validationSchema: schema,
  initialValues: {
    levelDisplayName: 'Level',
    skillDisplayName: 'Skill',
    groupDisplayName: 'Group',
    subjectDisplayName: 'Subject',
    projectDisplayName: 'Project',
    pointDisplayName: 'Point',
  }
});

onMounted(() => {
  loadSettings();
})

const labelsHaveBeenCleared = ref(false);
let isLoading = ref(true);
let showCustomLabelsConfigToggle = ref(false);
let selfReport = ref({
  enabled: false,
  justificationRequired: false,
  selected: 'Approval',
  options: [
    // { text: 'Approval Queue (reviewed by project admins first)', value: 'Approval', disabled: true },
    { text: 'Honor System (applied right away)', value: 'HonorSystem', disabled: true },
  ],
});
let settings = ref({
  projectVisibility: {
    value: 'pnd',
    setting: 'synthetic.project_visibility',
    lastLoadedValue: 'pnd',
    dirty: false,
    projectId: route.params.projectId,
  },
  productionModeEnabled: {
    value: 'false',
    setting: 'production.mode.enabled',
    lastLoadedValue: 'false',
    dirty: false,
    projectId: route.params.projectId,
  },
  inviteOnlyProject: {
    value: 'false',
    setting: 'invite_only',
    lastLoadedValue: 'false',
    dirty: false,
    projectId: route.params.projectId,
  },
  levelPointsEnabled: {
    value: 'false',
    setting: 'level.points.enabled',
    lastLoadedValue: 'false',
    dirty: false,
    projectId: route.params.projectId,
  },
  selfReportType: {
    value: '',
    setting: 'selfReport.type',
    lastLoadedValue: '',
    dirty: false,
    projectId: route.params.projectId,
  },
  selfReportJustificationRequired: {
    value: 'false',
    setting: 'selfReport.justificationRequired',
    lastLoadedValue: 'false',
    dirty: false,
    projectId: route.params.projectId,
  },
  helpUrlHost: {
    value: '',
    setting: 'help.url.root',
    lastLoadedValue: '',
    dirty: false,
    projectId: route.params.projectId,
  },
  rankAndLeaderboardOptOut: {
    value: false,
    setting: 'project-admins_rank_and_leaderboard_optOut',
    lastLoadedValue: false,
    dirty: false,
    projectId: route.params.projectId,
  },
  groupDescriptions: {
    value: false,
    setting: 'group-descriptions',
    lastLoadedValue: false,
    dirty: false,
    projectId: route.params.projectId,
  },
  projectDeletionProtection: {
    value: false,
    setting: 'project-deletion-protection',
    lastLoadedValue: false,
    dirty: false,
    projectId: route.params.projectId,
  },
  groupInfoOnSkillPage: {
    value: false,
    setting: 'group-info-on-skill-page',
    lastLoadedValue: false,
    dirty: false,
    projectId: route.params.projectId,
  },
  disableAchievementsCelebration: {
    value: false,
    setting: 'skills-display-achievement-celebration-disabled',
    lastLoadedValue: false,
    dirty: false,
    projectId: route.params.projectId,
  },
  projectDisplayName: {
    value: 'Project',
    setting: 'project.displayName',
    lastLoadedValue: 'Project',
    dirty: false,
    projectId: route.params.projectId,
  },
  subjectDisplayName: {
    value: 'Subject',
    setting: 'subject.displayName',
    lastLoadedValue: 'Subject',
    dirty: false,
    projectId: route.params.projectId,
  },
  groupDisplayName: {
    value: 'Group',
    setting: 'group.displayName',
    lastLoadedValue: 'Group',
    dirty: false,
    projectId: route.params.projectId,
  },
  skillDisplayName: {
    value: 'Skill',
    setting: 'skill.displayName',
    lastLoadedValue: 'Skill',
    dirty: false,
    projectId: route.params.projectId,
  },
  levelDisplayName: {
    value: 'Level',
    setting: 'level.displayName',
    lastLoadedValue: 'Level',
    dirty: false,
    projectId: route.params.projectId,
  },
  pointDisplayName: {
    value: 'Point',
    setting: 'point.displayName',
    lastLoadedValue: 'Point',
    dirty: false,
    projectId: route.params.projectId,
  },
  hideProjectDescription: {
    value: false,
    setting: 'show_project_description_everywhere',
    lastLoadedValue: false,
    dirty: false,
    projectId: route.params.projectId,
  },
});
const errMsg = ref(null);
const showSavedMsg = ref(false);

// computed
const isDirty = computed(() => {
  const foundDirty = Object.values(settings.value).find((item) => item.dirty) || labelsHaveBeenCleared.value;
  return !!foundDirty;
});

const isProgressAndRankingEnabled = computed(() =>  appConfig.rankingAndProgressViewsEnabled)

const approvalSelected = computed(() => {
  return selfReport.value.selected === 'Approval';
});

const shouldShowCustomLabelsConfig = computed(() => {
  return settings.value.projectDisplayName.value !== 'Project' || settings.value.projectDisplayName.dirty
      || settings.value.subjectDisplayName.value !== 'Subject' || settings.value.subjectDisplayName.dirty
      || settings.value.groupDisplayName.value !== 'Group' || settings.value.groupDisplayName.dirty
      || settings.value.skillDisplayName.value !== 'Skill' || settings.value.skillDisplayName.dirty
      || settings.value.levelDisplayName.value !== 'Level' || settings.value.levelDisplayName.dirty
      || settings.value.pointDisplayName.value !== 'Point' || settings.value.pointDisplayName.dirty;
});

const showCustomLabelsConfigLabel = computed(() => {
  return formatToggleLabel(showCustomLabelsConfigToggle.value);
});

const selfReportingEnabledLabel = computed(() => {
  return formatToggleLabel(selfReport.value.enabled);
});

const groupDescriptionsLabel = computed(() => {
  return formatToggleLabel(settings.value.groupDescriptions.value);
});

const disableAchievementCelebrationLabel = computed(() => {
  return formatToggleLabel(settings.value.disableAchievementsCelebration.value);
});

const projectDeletionProtectionLabel = computed(() => {
  return formatToggleLabel(settings.value.projectDeletionProtection.value);
});

const groupInfoOnSkillPageLabel = computed(() => {
  return formatToggleLabel(settings.value.groupInfoOnSkillPage.value);
})

const rankOptOutLabel = computed(() => {
  return formatToggleLabel(settings.value.rankAndLeaderboardOptOut.value);
});

const usePointsForLevelsLabel = computed(() => {
  return formatToggleLabel(settings.value.levelPointsEnabled.value);
});

const projectVisibilityOptions = computed(() => {
  const opts = [];
  if (isProgressAndRankingEnabled.value) {
    opts.push({ value: discoverableProgressAndRanking, text: 'Add to the Project Catalog' });
    opts.push({ value: publicNotDiscoverable, text: 'Not in the Project Catalog' });
  } else {
    opts.push({ value: publicNotDiscoverable, text: 'Not Enabled' });
  }
  opts.push({ value: privateInviteOnly, text: 'Private Invite Only' });
  return opts;
});

// functions
const updateApprovalType = ((disabled) => {
  selfReport.value.options = selfReport.value.options.map((item) => {
    const copy = { ...item };
    copy.disabled = disabled;
    return copy;
  });
});

const formatToggleLabel = ((value) => {
  if (value === true || value === 'true') {
    return 'Enabled';
  }
  return 'Disabled';
});

const selfReportingControl = ((checked) => {
  updateApprovalType(!checked);
  if (checked) {
    settings.value.selfReportType.value = selfReport.value.selected;
  } else {
    settings.value.selfReportType.value = '';
  }
  settings.value.selfReportType.dirty = `${settings.value.selfReportType.value}` !== `${settings.value.selfReportType.lastLoadedValue}`;
});

const productionModeEnabledChanged = ((value) => {
  settings.value.productionModeEnabled.dirty = `${value}` !== `${settings.value.productionModeEnabled.lastLoadedValue}`;
});

const rankAndLeaderboardOptOutChanged = ((value) => {
  settings.value.rankAndLeaderboardOptOut.dirty = `${value}` !== `${settings.value.rankAndLeaderboardOptOut.lastLoadedValue}`;
});

const groupDescriptionsChanged = ((value) => {
  settings.value.groupDescriptions.dirty = `${value}` !== `${settings.value.groupDescriptions.lastLoadedValue}`;
});

const projectDeletionProtectionChanged = ((value) => {
  settings.value.projectDeletionProtection.dirty = `${value}` !== `${settings.value.projectDeletionProtection.lastLoadedValue}`;
});

const groupInfoOnSkillPageChanged = ((value) => {
  settings.value.groupInfoOnSkillPage.dirty = `${value}` !== `${settings.value.groupInfoOnSkillPage.lastLoadedValue}`;
})

const disableAchievementsCelebrationChanged = ((value) => {
  settings.value.disableAchievementsCelebration.dirty = `${value}` !== `${settings.value.disableAchievementsCelebration.lastLoadedValue}`;
})

const inviteOnlyProjectChanged = ((value) => {
  settings.value.inviteOnlyProject.dirty = `${value}` !== `${settings.value.inviteOnlyProject.lastLoadedValue}`;
});

const selfReportingTypeChanged = ((value) => {
  settings.value.selfReportType.value = value;
  settings.value.selfReportType.dirty = `${settings.value.selfReportType.value}` !== `${settings.value.selfReportType.lastLoadedValue}`;
});

const justificationRequiredChanged = ((value) => {
  settings.value.selfReportJustificationRequired.dirty = `${value}` !== `${settings.value.selfReportJustificationRequired.lastLoadedValue}`;
});

const levelPointsEnabledChanged = ((value) => {
  settings.value.levelPointsEnabled.dirty = `${value}` !== `${settings.value.levelPointsEnabled.lastLoadedValue}`;
});

const updateSettingsField = ((value) => {
  const [field, val] = value;
  settings.value[field].value = val;
  settings.value[field].dirty = `${val}` !== `${settings.value.projectDisplayName.lastLoadedValue}`;
});

const hideProjectDescriptionChanged = ((value) => {
  settings.value.hideProjectDescription.dirty = `${value}` !== `${settings.value.hideProjectDescription.lastLoadedValue}`;
});

const projectVisibilityChanged = ((value) => {
  const dirty = `${value.value}` !== `${settings.value.projectVisibility.lastLoadedValue}`;
  settings.value.projectVisibility.dirty = dirty;
  if (dirty) {
    if (value.value === publicNotDiscoverable) {
      settings.value.inviteOnlyProject.value = 'false';
      settings.value.productionModeEnabled.value = 'false';
    } else if (value.value === privateInviteOnly) {
      ProjectService.checkIfProjectBelongsToGlobalBadge(route.params.projectId)
          .then((belongsToGlobal) => {
            if (belongsToGlobal) {
              settings.value.projectVisibility.value = settings.value.projectVisibility.lastLoadedValue
              settings.value.projectVisibility.dirty = false;
              dialogMessages.msgOk({
                message: 'This project participates in one or more global badges.  All project levels and skills must be removed from all global badges before this project can be made invite only.',
                header: 'Unable to change to Invite Only',
              });
            } else {
              settings.value.inviteOnlyProject.value = 'true';
              settings.value.productionModeEnabled.value = 'false';
              dialogMessages.msgOk({
                message: 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.',
                header: 'Changing to Invite Only',
              })
              inviteOnlyProjectChanged(settings.value.inviteOnlyProject.value);
              productionModeEnabledChanged(settings.value.productionModeEnabled.value);
            }
          });
    } else if (value.value === discoverableProgressAndRanking) {
      settings.value.inviteOnlyProject.value = 'false';
      settings.value.productionModeEnabled.value = 'true';
    }
  } else {
    settings.value.inviteOnlyProject.value = settings.value.inviteOnlyProject.lastLoadedValue;
    settings.value.productionModeEnabled.value = settings.value.productionModeEnabled.lastLoadedValue;
  }
  inviteOnlyProjectChanged(settings.value.inviteOnlyProject.value);
  productionModeEnabledChanged(settings.value.productionModeEnabled.value);
});

const loadSettings = (() => {
  SettingService.getSettingsForProject(route.params.projectId)
      .then((response) => {
        if (response) {
          setSyntheticSetting(response);
          const entries = Object.entries(settings.value);
          entries.forEach((entry) => {
            const [key, value] = entry;
            const found = response.find((item) => item.setting === value.setting);

            if (found) {
              found.value = (found.value === 'true' || found.value === true) ? true : found.value;
              settings.value[key] = { dirty: false, lastLoadedValue: found.value, ...found };

              setFieldValue(key, found.value);

              // special handling of self reporting as it's not just a simple key-value prop control
              if (found.setting === settings.value.selfReportType.setting) {
                selfReport.value.enabled = true;
                selfReport.value.selected = settings.value.selfReportType.value;
                updateApprovalType(false);
              }
            }
          });
        }
        showCustomLabelsConfigToggle.value = shouldShowCustomLabelsConfig.value;
      })
      .finally(() => {
        isLoading.value = false;
      });
});

const setSyntheticSetting = ((settingsResponse) => {
  const productionMode = settingsResponse.find((setting) => setting.setting === 'production.mode.enabled');
  const inviteOnly = settingsResponse.find((setting) => setting.setting === 'invite_only');

  let selectedValue = publicNotDiscoverable;
  if (inviteOnly?.value === 'true') {
    selectedValue = privateInviteOnly;
  } else if (productionMode?.value === 'true') {
    selectedValue = discoverableProgressAndRanking;
  }
  settings.value.projectVisibility.lastLoadedValue = selectedValue;
  settings.value.projectVisibility.value = selectedValue;
  settings.value.projectVisibility.dirty = false;
});

const save = (() => {
  const dirtyChanges = Object.values(settings.value).filter((item) => item.dirty && !item.setting.startsWith('synthetic.'));
  if (dirtyChanges) {
    SettingService.checkSettingsValidity(route.params.projectId, dirtyChanges)
    .then((res) => {
      if (res.valid) {
        saveSettings(dirtyChanges);
      } else {
        errMsg.value = res.explanation;
      }
    });
  }
});

const saveSettings = ((dirtyChanges) => {
  SettingService.saveSettings(route.params.projectId, dirtyChanges)
      .then(() => {
        showSavedMsg.value = true;
        announcer.polite('Project Settings have been successfully saved');
        setTimeout(() => {
          showSavedMsg.value = false;
        }, 4000);
        const entries = Object.entries(settings.value);
        entries.forEach((entry) => {
          const [key, value] = entry;
          settings.value[key] = Object.assign(value, {
            dirty: false,
            lastLoadedValue: value.value,
          });
          if (value.setting === settings.value.helpUrlHost.setting && value.value && value.value.length > 0) {
            SkillsReporter.reportSkill('ConfigureProjectRootHelpUrl');
          }
        });
        labelsHaveBeenCleared.value = false;
        projConfig.loadProjConfigState({ projectId: route.params.projectId, updateLoadingVar: false })
      })
      .finally(() => {
        // isLoading.value = false;
      });
});

const toggleCustomLabelConfig = () => {
  resetForm()
  if(!showCustomLabelsConfigToggle.value) {
    labelsHaveBeenCleared.value = true;
    settings.value.projectDisplayName.value = 'Project'
    settings.value.projectDisplayName.dirty = true
    settings.value.subjectDisplayName.value = 'Subject'
    settings.value.subjectDisplayName.dirty = true
    settings.value.groupDisplayName.value = 'Group'
    settings.value.groupDisplayName.dirty = true
    settings.value.skillDisplayName.value = 'Skill'
    settings.value.skillDisplayName.dirty = true
    settings.value.levelDisplayName.value = 'Level'
    settings.value.levelDisplayName.dirty = true
    settings.value.pointDisplayName.value = 'Point'
    settings.value.pointDisplayName.dirty = true
  }
}
</script>

<template>
  <div>
    <sub-page-header title="Project Settings"/>
    <Card>
      <template #content>
        <loading-container :is-loading="isLoading">
          <settings-item label="Project Discoverability" input-id="projectVisibilityDropdown">
            <Select v-model="settings.projectVisibility.value"
                      inputId="projectVisibilityDropdown"
                      :options="projectVisibilityOptions"
                      @change="projectVisibilityChanged"
                      aria-labelledby="projectVisibilityLabel"
                      optionLabel="text" optionValue="value"
                      class="w-full"
                      data-cy="projectVisibilitySelector" required />
          </settings-item>

          <settings-item label="Project Description" input-id="hideProjectDescription">
            <Select v-model="settings.hideProjectDescription.value"
                      inputId="hideProjectDescription"
                      :options="[{value: true, label: 'Show Project Description everywhere'}, {value: false, label: 'Only show Description in Project Catalog'}]"
                      optionLabel="label" optionValue="value"
                      @change="hideProjectDescriptionChanged"
                      aria-labelledby="hideProjectDescriptionLabel"
                      class="w-full"
                      data-cy="showProjectDescriptionSelector" />
          </settings-item>

          <settings-item label="Point-Based Level Management" input-id="levelPointsEnabled">
            <ToggleSwitch v-model="settings.levelPointsEnabled.value"
                         inputId="levelPointsEnabled"
                         v-on:update:modelValue="levelPointsEnabledChanged"
                         name="check-button"
                         aria-labelledby="pointsForLevelsLabel"
                         data-cy="usePointsForLevelsSwitch" />
            <span class="ml-1">{{ usePointsForLevelsLabel }}</span>
          </settings-item>
          <Message v-if="settings.levelPointsEnabled.value === 'true' || settings.levelPointsEnabled.value === true" class="ml-3" severity="error" :closable="false">
            <div>You now responsible for defining point ranges for each level. As new skills increase the total available points, update <router-link class="underline" :to="{ name:'ProjectLevels', params: { quizId: route.params.projectId }}">level thresholds</router-link> to prevent users from reaching max level prematurely.</div>
          </Message>

          <SkillsSettingTextInput name="helpUrlHost"
                                  label="Root Help Url"
                                  :label-width-in-rem="16"
                                  @input="updateSettingsField"
                                  placeholder="http://www.STarticle.com" />

          <settings-item label="Self Report Default" input-id="selfReportingControl">
            <div class="flex flex-col flex-1">
              <div class="flex items-center">
                <ToggleSwitch v-model="selfReport.enabled"
                             name="check-button"
                             inputId="selfReportingControl"
                             v-on:update:modelValue="selfReportingControl"
                             aria-labelledby="selfReportLabel"
                             data-cy="selfReportSwitch"/>
                <span class="ml-1">{{ selfReportingEnabledLabel }}</span>
              </div>
              <Card v-if="selfReport.enabled" class="mt-2" Card :pt="{  content: { class: 'py-0' } }" data-cy="selfReportTypeSelector">
                <template #content>
                  <div class="flex flex-col">
                    <div>
                      <RadioButton class="mr-2"
                                   inputId="approval"
                                   value="Approval"
                                   v-model="selfReport.selected"
                                   @change="selfReportingTypeChanged('Approval')"
                                   :disabled="!selfReport.enabled"/>
                      <label for="approval">Approval Queue (reviewed by project admins first)</label>
                    </div>
                    <div class="ml-6 mt-1">
                      <Checkbox data-cy="justificationRequiredCheckbox"
                                inputId="justificationRequiredCheckbox"
                                id="justification-required-checkbox" :binary="true"
                                class="d-inline mr-2"
                                v-model="settings.selfReportJustificationRequired.value"
                                :disabled="!approvalSelected || !selfReport.enabled"
                                @update:modelValue="justificationRequiredChanged"/>
                      <label for="justificationRequiredCheckbox" class="m-0 italic"
                             :class="{ 'text-secondary': !approvalSelected || !selfReport.enabled}">
                        Justification Required
                      </label>
                    </div>
                  </div>
                  <div class="flex mt-2">
                    <RadioButton class="mr-2"
                                 inputId="honorSystem"
                                 @change="selfReportingTypeChanged('HonorSystem')"
                                 value="HonorSystem"
                                 v-model="selfReport.selected"
                                 :disabled="!selfReport.enabled"/>
                    <label for="honorSystem">Honor System (applied right away)</label>
                  </div>
                </template>
              </Card>
            </div>
          </settings-item>

          <settings-item label="Rank Opt-Out for ALL Admins" input-id="rankAndLeaderboardOptOut">
            <ToggleSwitch v-model="settings.rankAndLeaderboardOptOut.value"
                         inputId="rankAndLeaderboardOptOut"
                         name="check-button"
                         v-on:update:modelValue="rankAndLeaderboardOptOutChanged"
                         aria-labelledby="rankAndLeaderboardOptOutLabel"
                         data-cy="rankAndLeaderboardOptOutSwitch" />
            <span class="ml-1">{{ rankOptOutLabel }}</span>
          </settings-item>

          <settings-item label="Custom Labels" input-id="showCustomLabelsConfigToggle">
            <div class="flex flex-col flex-1">
              <div class="flex items-center">
                <ToggleSwitch v-model="showCustomLabelsConfigToggle"
                             inputId="showCustomLabelsConfigToggle"
                             name="check-button"
                             aria-labelledby="customLabelsLabel"
                             @change="toggleCustomLabelConfig"
                             data-cy="customLabelsSwitch"/>
                <span class="ml-1">{{ showCustomLabelsConfigLabel }}</span>
              </div>
              <Card class="mt-4" v-if="showCustomLabelsConfigToggle">
                <template #content>
                  <SkillsSettingTextInput name="projectDisplayName"
                                          label="Project Display Text"
                                          @input="updateSettingsField"
                                          help-message='The word "Project" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                  <SkillsSettingTextInput name="subjectDisplayName"
                                          label="Subject Display Text"
                                          @input="updateSettingsField"
                                          help-message='The word "Subject" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                  <SkillsSettingTextInput name="groupDisplayName"
                                          label="Group Display Text"
                                          @input="updateSettingsField"
                                          help-message='The word "Group" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                  <SkillsSettingTextInput name="skillDisplayName"
                                          label="Skill Display Text"
                                          @input="updateSettingsField"
                                          help-message='The word "Skill" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                  <SkillsSettingTextInput name="levelDisplayName"
                                          label="Level Display Text"
                                          @input="updateSettingsField"
                                          help-message='The word "Level" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                  <SkillsSettingTextInput name="pointDisplayName"
                                          label="Point Display Text"
                                          @input="updateSettingsField"
                                          help-message='The word "Point" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                </template>
              </Card>
            </div>
          </settings-item>

          <settings-item label="Always Show Group Descriptions" input-id="groupDescriptionsSwitchInput">
            <ToggleSwitch v-model="settings.groupDescriptions.value"
                         inputId="groupDescriptionsSwitchInput"
                         name="check-button"
                         v-on:update:modelValue="groupDescriptionsChanged"
                         aria-labelledby="groupDescriptionsLabel"
                         data-cy="groupDescriptionsSwitch" />
            <span class="ml-1">{{ groupDescriptionsLabel }}</span>
          </settings-item>
          <settings-item label="Hide Group Info On Skill Pages" input-id="groupInfoOnSkillPageSwitchInput">
            <ToggleSwitch v-model="settings.groupInfoOnSkillPage.value"
                         inputId="groupInfoOnSkillPageSwitchInput"
                         name="check-button"
                         v-on:update:modelValue="groupInfoOnSkillPageChanged"
                         aria-labelledby="groupInfoOnSkillPageLabel"
                         data-cy="groupInfoOnSkillPageSwitch" />
            <span class="ml-1">{{ groupInfoOnSkillPageLabel }}</span>
          </settings-item>

          <settings-item label="Hide Achievement Celebration" input-id="disableAchievementsCelebrationSwitchInput">
            <ToggleSwitch v-model="settings.disableAchievementsCelebration.value"
                         inputId="disableAchievementsCelebrationSwitchInput"
                         name="check-button"
                         v-on:update:modelValue="disableAchievementsCelebrationChanged"
                         aria-labelledby="disableAchievementsCelebrationLabel"
                         data-cy="disableAchievementsCelebrationSwitch" />
            <span class="ml-1">{{ disableAchievementCelebrationLabel }}</span>
          </settings-item>

          <settings-item label="Project Deletion Protection" input-id="projectDeletionProtectionSwitchInput">
              <ToggleSwitch v-model="settings.projectDeletionProtection.value"
                           inputId="projectDeletionProtectionSwitchInput"
                           name="check-button"
                           v-on:update:modelValue="projectDeletionProtectionChanged"
                           aria-labelledby="projectDeletionProtectionLabel"
                           data-cy="projectDeletionProtectionSwitch" />
              <span class="ml-1">{{ projectDeletionProtectionLabel }}</span>
          </settings-item>

          <hr/>

          <Message v-if="errMsg" severity="error" :closable="false">{{ errMsg }}</Message>

          <div class="flex flex-row mt-2">
            <SkillsButton variant="outline-success" @click="save" :disabled="!meta.valid || !isDirty"
                          data-cy="saveSettingsBtn" icon="fas fa-arrow-circle-right" label="Save">
            </SkillsButton>

            <InlineMessage v-if="isDirty" class="ml-2" data-cy="unsavedChangesAlert" severity="warn">
              Unsaved Changes
            </InlineMessage>
            <InlineMessage v-if="!isDirty && showSavedMsg" class="ml-2" data-cy="settingsSavedAlert" severity="success">
              Settings Updated!
            </InlineMessage>
          </div>
        </loading-container>
      </template>
    </Card>

  </div>
</template>

<style>
.p-dropdown-label.p-inputtext {
  text-wrap: wrap;
}
</style>
