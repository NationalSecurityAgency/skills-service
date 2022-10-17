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
    <metrics-card title="Project Administrators" data-cy="projectAdmins" :no-padding="true">
      <role-manager :project-id="projectId" :add-user-label="'Add Administrator'"
                    :add-role-confirmation="addUserConfirmationConfig"/>
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
  import MetricsCard from '../metrics/utils/MetricsCard';
  import RoleManager from './RoleManager';
  import TrustedClientProps from './TrustedClientProps';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import LoadingContainer from '../utils/LoadingContainer';
  import SettingsService from '../settings/SettingsService';

  export default {
    name: 'AccessSettings',
    components: {
      MetricsCard,
      LoadingContainer,
      SubPageHeader,
      RoleManager,
      TrustedClientProps,
      InviteUsersToProject,
      RevokeUserAccess,
      InviteStatuses,
    },
    data() {
      return {
        isLoading: true,
        privateProject: false,
        projectId: this.$route.params.projectId,
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
        if (this.privateProject) {
          return {
            msgText: 'The selected user will be added as an Administrator for this project and will be able to edit/add/delete all aspects of the Project.',
            titleText: 'Add Project Administrator?',
            okBtnText: 'Add Administrator!',
          };
        }
        return null;
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
      SettingsService.getProjectSetting(this.$route.params.projectId, 'invite_only')
        .then((setting) => {
          this.privateProject = setting?.enabled;
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
    methods: {
      handleInviteSent() {
        this.$refs.inviteStatuses.loadData();
      },
    },
  };
</script>

<style scoped>

</style>
