<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import Card from 'primevue/card'
import InputSwitch from 'primevue/inputswitch'
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
import { useConfirm } from 'primevue/useconfirm'

const announcer = useSkillsAnnouncer();
const route = useRoute();
const appConfig = useAppConfig()
const projConfig = useProjConfig()
const confirm = useConfirm();

const publicNotDiscoverable = 'pnd';
const discoverableProgressAndRanking = 'dpr';
const privateInviteOnly = 'pio';

const schema = yup.object({
  helpUrlHost: yup.string().max(appConfig.maxCustomLabelLength).label('Root Help Url').urlValidator(),
  projectDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Project Display Text'),
  subjectDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Subject Display Text'),
  groupDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Group Display Text'),
  skillDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Skill Display Text'),
  levelDisplayName: yup.string().max(appConfig.maxCustomLabelLength).label('Level Display Text'),
});

const { values, defineField, errors, meta, handleSubmit, setFieldValue } = useForm({
  validationSchema: schema,
  initialValues: {
    levelDisplayName: 'Level',
    skillDisplayName: 'Skill',
    groupDisplayName: 'Group',
    subjectDisplayName: 'Subject',
    projectDisplayName: 'Project',
  }
});

onMounted(() => {
  loadSettings();
})

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
  hideProjectDescription: {
    value: false,
    setting: 'show_project_description_everywhere',
    lastLoadedValue: false,
    dirty: false,
    projectId: route.params.projectId,
  },
});
let errMsg = ref(null);
let showSavedMsg = ref(false);

// computed
const isDirty = computed(() => {
  const foundDirty = Object.values(settings.value).find((item) => item.dirty);
  return !!foundDirty;
});

const isProgressAndRankingEnabled = computed(() =>  appConfig.rankingAndProgressViewsEnabled)

const approvalSelected = computed(() => {
  return selfReport.value.selected === 'Approval';
});

const shouldShowCustomLabelsConfig = computed(() => {
  return showCustomLabelsConfigToggle.value
      || settings.value.projectDisplayName.value !== 'Project' || settings.value.projectDisplayName.dirty
      || settings.value.subjectDisplayName.value !== 'Subject' || settings.value.subjectDisplayName.dirty
      || settings.value.groupDisplayName.value !== 'Group' || settings.value.groupDisplayName.dirty
      || settings.value.skillDisplayName.value !== 'Skill' || settings.value.skillDisplayName.dirty
      || settings.value.levelDisplayName.value !== 'Level' || settings.value.levelDisplayName.dirty;
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

const projectVisibilityHelpMsg = computed(() => {
  if (isProgressAndRankingEnabled.value) {
    return '<b>Not in the Project Catalog</b> (default) projects can be accessed directly but are not discoverable via Project Catalog page. <br/><br/>'
        + '<b>Add to the Project Catalog</b> projects can be accessed directly and can be found in the Project Catalog. <br/><br/>'
        + '<b>Private Invite Only</b> projects may ONLY be accessed by users who have been issued a specific project invite. ';
  }
  return '<b>Not in the Project Catalog</b> (default) projects can be accessed directly but are not discoverable via Project Catalog page. <br/><br/>'
      + '<b>Private Invite Only</b> projects may ONLY be accessed by users who have been issued a specific project invite.';
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

const inviteOnlyProjectChanged = ((value) => {
  settings.value.inviteOnlyProject.dirty = `${value}` !== `${settings.value.inviteOnlyProject.lastLoadedValue}`;
});

const selfReportingTypeChanged = ((value) => {
  settings.value.selfReportType.value = value;
  settings.value.selfReportType.dirty = `${settings.value.selfReportType.value}` !== `${settings.value.selfReportType.lastLoadedValue}`;
});

const justificationRequiredChanged = ((value) => {
  console.log(value);
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
      settings.value.inviteOnlyProject.value = 'true';
      settings.value.productionModeEnabled.value = 'false';
      confirm.require({
        message: 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.',
        header: 'Changing to Invite Only',
        rejectClass: 'hidden',
        acceptLabel: 'OK',
      })
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
  // $refs.observer.validate()
  //     .then((res1) => {
  //       if (!res1) {
  //         errMsg.value = 'Form did NOT pass validation, please fix and try to Save again';
  //       } else {
          const dirtyChanges = Object.values(settings.value).filter((item) => item.dirty && !item.setting.startsWith('synthetic.'));
          if (dirtyChanges) {
            // isLoading.value = true;
            SettingService.checkSettingsValidity(route.params.projectId, dirtyChanges)
                .then((res) => {
                  if (res.valid) {
                    saveSettings(dirtyChanges);
                  } else {
                    errMsg.value = res.explanation;
                    // isLoading.value = false;
                  }
                });
          }
      //   }
      // });
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
        projConfig.loadProjConfigState({ projectId: route.params.projectId, updateLoadingVar: false })
      })
      .finally(() => {
        // isLoading.value = false;
      });
});
</script>

<template>
  <div>
    <sub-page-header title="Project Settings"/>
    <Card>
      <template #content>
        <loading-container :is-loading="isLoading">
          <div class="flex flex-row" data-cy="projectVisibility">
            <div class="md:col-5 xl:col-3 text-secondary" id="projectVisibilityLabel">
              Project Discoverability:
            </div>
            <div class="md:col-7 xl:col-9">
              <Dropdown v-model="settings.projectVisibility.value"
                        :options="projectVisibilityOptions"
                        @change="projectVisibilityChanged"
                        aria-labelledby="projectVisibilityLabel"
                        optionLabel="text" optionValue="value"
                        class="w-full"
                        data-cy="projectVisibilitySelector" required/>
            </div>
          </div>

          <div class="flex flex-row">
            <div class="md:col-5 xl:col-3 text-secondary" id="hideProjectDescriptionLabel">
              Project Description:
            </div>
            <div class="md:col-7 xl:col-9">
              <Dropdown v-model="settings.hideProjectDescription.value"
                        :options="[{value: true, label: 'Show Project Description everywhere'}, {value: false, label: 'Only show Project Description in Manage My Projects'}]"
                        optionLabel="label" optionValue="value"
                        @change="hideProjectDescriptionChanged"
                        aria-labelledby="hideProjectDescriptionLabel"
                        class="w-full"
                        data-cy="showProjectDescriptionSelector" />
            </div>
          </div>

          <div class="flex flex-row">
            <div class="md:col-5 xl:col-3 text-secondary" id="pointsForLevelsLabel">
              Use Points For Levels:
            </div>
            <div class="md:col-7 xl:col-9">
              <div class="flex align-items-center">
                <InputSwitch v-model="settings.levelPointsEnabled.value"
                             v-on:update:modelValue="levelPointsEnabledChanged"
                             name="check-button"
                             aria-labelledby="pointsForLevelsLabel"
                             data-cy="usePointsForLevelsSwitch" />
                <span class="ml-1">{{ usePointsForLevelsLabel }}</span>
              </div>
            </div>
          </div>

          <SkillsSettingTextInput name="helpUrlHost"
                                  label="Root Help Url"
                                  @input="updateSettingsField"
                                  placeholder="http://www.HelpArticlesHost.com" />

          <div class="flex flex-row">
            <div class="md:col-5 xl:col-3 text-secondary" id="selfReportLabel">
              Self Report Default:
            </div>
            <div class="md:col-7 xl:col-9">
              <div class="flex align-items-center">
              <InputSwitch v-model="selfReport.enabled"
                               name="check-button"
                               v-on:update:modelValue="selfReportingControl"
                               aria-labelledby="selfReportLabel"
                               data-cy="selfReportSwitch" />
                <spand class="ml-1">{{ selfReportingEnabledLabel }}</spand>
              </div>
              <Card class="mt-2" Card :pt="{  content: { class: 'py-0' } }" data-cy="selfReportTypeSelector">
                <template #content>
                      <div class="flex">
                        <RadioButton class="mr-2"
                                     inputId="approval"
                                     value="Approval"
                                     v-model="selfReport.selected"
                                     @change="selfReportingTypeChanged('Approval')"
                                     :disabled="!selfReport.enabled" />
                        <label for="approval">Approval Queue (reviewed by project admins first)</label>
                        <span class="text-muted mr-3 ml-2">|</span>
                        <label for="self-report-checkbox" class="m-0">
                          <Checkbox data-cy="justificationRequiredCheckbox"
                                    id="justification-required-checkbox" :binary="true"
                                    class="d-inline mr-2"
                                    v-model="settings.selfReportJustificationRequired.value"
                                    :disabled="!approvalSelected || !selfReport.enabled"
                                    @update:modelValue="justificationRequiredChanged" />
                          <span class="font-italic"
                                :class="{ 'text-secondary': !approvalSelected || !selfReport.enabled}">Justification Required </span>
                        </label>
                      </div>
                      <div class="flex mt-2">
                        <RadioButton class="mr-2"
                                     inputId="honorSystem"
                                     @change="selfReportingTypeChanged('HonorSystem')"
                                     value="HonorSystem"
                                     v-model="selfReport.selected"
                                     :disabled="!selfReport.enabled" />
                        <label for="honorSystem">Honor System (applied right away)</label>
                      </div>
                </template>
              </Card>
            </div>
          </div>

          <div class="flex flex-row">
            <div class="md:col-5 xl:col-3 text-secondary" id="rankAndLeaderboardOptOutLabel">
              Rank Opt-Out for ALL Admins:
            </div>
            <div class="md:col-7 xl:col-9">
              <div class="flex align-items-center">
              <InputSwitch v-model="settings.rankAndLeaderboardOptOut.value"
                           name="check-button"
                           v-on:update:modelValue="rankAndLeaderboardOptOutChanged"
                           aria-labelledby="rankAndLeaderboardOptOutLabel"
                           data-cy="rankAndLeaderboardOptOutSwitch"/>
                <span class="ml-1">{{ rankOptOutLabel }}</span>
              </div>
            </div>
          </div>

          <div class="flex flex-row">
            <div class="md:col-5 xl:col-3 text-secondary" id="customLabelsLabel">
              Custom Labels:
            </div>
            <div class="md:col-7 xl:col-9">
              <div class="flex align-items-center">
                <InputSwitch v-model="showCustomLabelsConfigToggle"
                               name="check-button"
                               aria-labelledby="customLabelsLabel"
                               data-cy="customLabelsSwitch"/>
                <span class="ml-1">{{ showCustomLabelsConfigLabel }}</span>
              </div>

  <!--            <b-collapse id="customLabelsCollapse" :visible="showCustomLabelsConfigToggle">-->
                <Card class="mt-1" v-if="shouldShowCustomLabelsConfig">
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
<!--                    projectDisplayNameChanged-->
                  </template>
                </Card>
  <!--            </b-collapse>-->
            </div>
          </div>

          <div class="flex flex-row">
            <div class="md:col-5 xl:col-3 text-secondary" id="groupDescriptions">
              <span id="groupDescriptionsLabel">Always Show Group Descriptions:</span>
            </div>
            <div class="md:col-7 xl:col-9">
              <div class="flex align-items-center">
                <InputSwitch v-model="settings.groupDescriptions.value"
                             name="check-button"
                             v-on:update:modelValue="groupDescriptionsChanged"
                             aria-labelledby="groupDescriptionsLabel"
                             data-cy="groupDescriptionsSwitch" />
                <span class="ml-1">{{ groupDescriptionsLabel }}</span>
              </div>
            </div>
          </div>

          <hr/>

          <p v-if="errMsg" class="text-center text-danger" role="alert">***{{ errMsg }}***</p>

          <div class="flex flex-row">
            <div class="col">
              <SkillsButton variant="outline-success" @click="save" :disabled="!meta.valid || !isDirty" data-cy="saveSettingsBtn" icon="fas fa-arrow-circle-right" label="Save">
              </SkillsButton>

              <span v-if="isDirty" class="text-warning ml-2" data-cy="unsavedChangesAlert">
                  <i class="fa fa-exclamation-circle"
                     aria-label="Settings have been changed, do not forget to save"
                     v-tooltip="'Settings have been changed, do not forget to save'"/> Unsaved Changes
                </span>
              <span v-if="!isDirty && showSavedMsg" class="text-success ml-2" data-cy="settingsSavedAlert">
                  <i class="fa fa-check" />
                  Settings Updated!
                </span>
            </div>
          </div>
        </loading-container>
      </template>
    </Card>

  </div>
</template>

<style scoped></style>
