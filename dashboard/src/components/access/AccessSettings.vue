/*
Copyright 2020 SkillTree

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
<template>
  <loading-container v-bind:is-loading="isLoading">
    <sub-page-header title="Access Management"/>
    <metrics-card title="Project Management Users" data-cy="projectAdmins" :no-padding="true">
      <role-manager :project-id="projectId"
                    :add-role-confirmation="addUserConfirmationConfig"/>
    </metrics-card>

    <metrics-card v-if="showManageUserCommunity" title="User Community Management" data-cy="userCommunityManagement" :no-padding="true" class="my-4">
      <b-card>
            <div class="row" data-cy="projectVisibility">
              <div class="d-inline-block pt-2 col" id="userCommunityLabel">
                <span class="text-secondary">{{ userCommunityLabel }}: <inline-help target-id="userCommunityHelp"
                  :msg="`Restrict Access to users with ${currentUserCommunity} access`" />
                </span>
                <span class="ml-2">{{userCommunitySwitchLabel}}</span>
              </div>
              <div class="col-auto">
                <b-button variant="outline-info" @click="userCommunityChanged" data-cy="userCommunityBtn"> {{userCommunityButtonLabel}}</b-button>
              </div>
            </div>
        <div v-if="errMsg" class="row">
          <div class="col">
            <p v-if="errMsg" class="text-center text-danger mt-3" role="alert">***{{ errMsg }}***</p>
          </div>
        </div>
      </b-card>
    </metrics-card>

    <metrics-card v-if="privateProject" title="Project User: Invite" data-cy="inviteUser" :no-padding="true" class="my-4">
      <b-overlay :show="!isEmailEnabled">
        <div slot="overlay" class="alert alert-warning mt-2" data-cy="inviteUsers_emailServiceWarning">
          <i class="fa fa-exclamation-triangle" aria-hidden="true"/> Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
        </div>
        <div class="card h-100">
          <h5 class="card-title mt-4 ml-4">Invite Users</h5>
          <div class="card-body">
            <div class="media w-100">
              <invite-users-to-project ref="inviteUsers" :project-id="projectId" @invites-sent="handleInviteSent"/>
            </div>
          </div>
        </div>
        <div class="card h-100">
          <div class="card-body">
            <h5 class="card-title">Invites Pending Acceptance</h5>
            <div class="media w-100">
              <invite-statuses ref="inviteStatuses" :project-id="projectId"/>
            </div>
          </div>
        </div>
      </b-overlay>
    </metrics-card>
    <metrics-card v-if="privateProject" title="Project User: Revoke" data-cy="revokeAccess" :no-padding="true" class="my-4">
      <revoke-user-access />
    </metrics-card>

    <trusted-client-props v-if="showTrustedClientProps" :project-id="projectId" class="my-4"/>
  </loading-container>
</template>

<script>
  import { mapGetters } from 'vuex';
  import InviteUsersToProject from '@/components/access/InviteUsersToProject';
  import InviteStatuses from '@/components/access/InviteStatuses';
  import RevokeUserAccess from '@/components/access/RevokeUserAccess';
  import InlineHelp from '@/components/utils/InlineHelp';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import MetricsCard from '../metrics/utils/MetricsCard';
  import RoleManager from './RoleManager';
  import TrustedClientProps from './TrustedClientProps';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import LoadingContainer from '../utils/LoadingContainer';
  import SettingsService from '../settings/SettingsService';

  export default {
    name: 'AccessSettings',
    mixins: [MsgBoxMixin],
    components: {
      MetricsCard,
      LoadingContainer,
      SubPageHeader,
      RoleManager,
      TrustedClientProps,
      InviteUsersToProject,
      RevokeUserAccess,
      InviteStatuses,
      InlineHelp,
    },
    data() {
      return {
        isLoading: true,
        privateProject: false,
        projectId: this.$route.params.projectId,
        userCommunityRestrictedSetting: {
          value: false,
          setting: 'user_community',
          projectId: this.$route.params.projectId,
        },
        errMsg: null,
      };
    },
    computed: {
      ...mapGetters([
        'isEmailEnabled',
      ]),
      showTrustedClientProps() {
        return (!this.$store.getters.isPkiAuthenticated);
      },
      addUserConfirmationConfig() {
        return this.privateProject;
      },
      showManageUserCommunity() {
        return Boolean(this.currentUserCommunity);
      },
      userCommunityLabel() {
        return this.$store.getters.config.userCommunityLabel;
      },
      userCommunityRestrictedValue() {
        return this.userCommunityRestrictedSetting.value;
      },
      userCommunitySwitchLabel() {
        if (this.userCommunityRestrictedValue) {
          return 'Enabled';
        }
        return 'Disabled';
      },
      userCommunityButtonLabel() {
        if (this.userCommunityRestrictedValue) {
          return 'Disable';
        }
        return 'Enable';
      },
      currentUserCommunity() {
        return this.$store.getters.userInfo?.userCommunity;
      },
    },
    beforeRouteLeave(to, from, next) {
      if (this.$refs.inviteUsers && this.$refs.inviteUsers.canDiscard) {
        this.$refs.inviteUsers.canDiscard().then((discard) => {
          if (discard) {
            next();
          } else {
            next(false);
          }
        });
      } else {
        next();
      }
    },
    mounted() {
      SettingsService.getSettingsForProject(this.$route.params.projectId)
        .then((settingsResponse) => {
          this.privateProject = settingsResponse.find((setting) => setting.setting === 'invite_only')?.enabled;
          this.userCommunityRestrictedSetting.value = Boolean(settingsResponse.find((setting) => setting.setting === 'user_community')?.enabled);
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
    methods: {
      handleInviteSent() {
        this.$refs.inviteStatuses.loadData();
      },
      userCommunityChanged() {
        if (!this.userCommunityRestrictedValue) {
          this.msgConfirm(`Warning: User's that do not belong to the ${this.currentUserCommunity} community will no longer have access to this project. Are you sure you want to continue?`, 'Please Confirm!', 'YES, Restrict Access!')
            .then((res) => {
              if (res) {
                this.doToggleUserCommunitySetting();
              }
            });
        } else {
          this.msgConfirm(`Warning: This will remove the ${this.currentUserCommunity} community restriction from this project, allowing any user to access this project. `
            + `You are responsible for ensuring that all data is releasable and does not require restricting access to the ${this.currentUserCommunity} community! `
            + 'Are you sure you want to continue?', 'Please Confirm!', `YES, Remove ${this.currentUserCommunity} Access Restriction!`)
            .then((res) => {
              if (res) {
                this.doToggleUserCommunitySetting();
              }
            });
        }
      },
      doToggleUserCommunitySetting() {
        const updatedUserCommunitySetting = {
          value: !this.userCommunityRestrictedSetting.value,
          setting: 'user_community',
          projectId: this.$route.params.projectId,
        };

        this.isLoading = true;
        SettingsService.checkSettingsValidity(this.$route.params.projectId, [updatedUserCommunitySetting])
          .then((res) => {
            if (res.valid) {
              SettingsService.saveSettings(this.$route.params.projectId, [updatedUserCommunitySetting])
                .then(() => {
                  this.$announcer.polite('User Community Settings have been successfully saved');
                  this.userCommunityRestrictedSetting = updatedUserCommunitySetting;
                  this.$store.dispatch('loadProjConfigState', { projectId: this.$route.params.projectId, updateLoadingVar: false });
                })
                .finally(() => {
                  this.isLoading = false;
                });
            } else {
              this.errMsg = res.explanation;
              this.isLoading = false;
            }
          });
      },
    },
  };
</script>

<style scoped>

</style>
