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
      <role-manager :project="project"/>
    </metrics-card>

    <metrics-card v-if="privateProject && emailEnabled" title="Invite Users" data-cy="inviteUser" :no-padding="true" class="my-4">
      <invite-users-to-project ref="inviteUsers" :project-id="project.projectId"/>
    </metrics-card>
    <metrics-card v-if="privateProject" title="Revoke Access" data-cy="revokeAccess" :no-padding="true" class="my-4">
      <revoke-user-access />
    </metrics-card>

    <trusted-client-props v-if="showTrustedClientProps" :project-id="project.projectId" class="my-4"/>
  </loading-container>
</template>

<script>
  import InviteUsersToProject from '@/components/access/InviteUsersToProject';
  import RevokeUserAccess from '@/components/access/RevokeUserAccess';
  import MetricsCard from '../metrics/utils/MetricsCard';
  import RoleManager from './RoleManager';
  import TrustedClientProps from './TrustedClientProps';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ProjectService from '../projects/ProjectService';
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
    },
    data() {
      return {
        isLoading: true,
        project: {},
        privateProject: false,
        emailEnabled: false,
      };
    },
    computed: {
      showTrustedClientProps() {
        return (!this.$store.getters.isPkiAuthenticated);
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
      ProjectService.getProjectDetails(this.$route.params.projectId)
        .then((res) => {
          this.project = res;
        })
        .then(() => SettingsService.getProjectSetting(this.$route.params.projectId, 'invite_only'))
        .then((setting) => {
          this.privateProject = setting?.enabled;
        })
        .then(() => ProjectService.isEmailServiceSupported())
        .then((emailEnabled) => {
          this.emailEnabled = emailEnabled;
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
  };
</script>

<style scoped>

</style>
