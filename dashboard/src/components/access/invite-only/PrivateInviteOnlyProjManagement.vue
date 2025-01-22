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
import { computed, ref } from 'vue'
import SkillsCardHeader from '@/components/utils/cards/SkillsCardHeader.vue'
import InviteUsersToProject from '@/components/access/invite-only/InviteUsersToProject.vue'
import InviteStatuses from '@/components/access/invite-only/InviteStatuses.vue'
import RevokeUserAccess from '@/components/access/invite-only/RevokeUserAccess.vue'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'

const appInfo = useAppInfoState()

const privateProject = ref(true)

const inviteStatuses = ref(null)
const handleInviteSent = () => {
  inviteStatuses.value.loadData()
}

const emailFeatureConfigured = computed(() => { return appInfo.emailEnabled });

</script>

<template>
  <div>
    <Card v-if="privateProject"
          :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }"
          data-cy="inviteUser"
          class="my-6">
      <template #header>
        <SkillsCardHeader title="Project User: Invite">
          <template #headerIcon><i class="fas fa-user-lock mr-2 text-red-500"
                                   aria-hidden="true"/></template>
        </SkillsCardHeader>
      </template>
      <template #content>
        <Message severity="warn"
                 class="mx-2"
                 data-cy="contactUsers_emailServiceWarning" v-if="!emailFeatureConfigured" :closable="false">
          Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.
        </Message>

        <BlockUI :blocked="!emailFeatureConfigured" :class="{'p-3': !emailFeatureConfigured}">
        <Card>
          <template #subtitle>Invite Users</template>
          <template #content>
            <invite-users-to-project
              ref="inviteUsers"
              @invites-sent="handleInviteSent"
            />
          </template>
        </Card>
        <Card class="mt-4">
          <template #subtitle>Invites Pending Acceptance</template>
          <template #content>
            <invite-statuses ref="inviteStatuses" />
          </template>
        </Card>

        </BlockUI>
      </template>
    </Card>

    <revoke-user-access v-if="privateProject" class="my-6" />
  </div>
</template>

<style scoped>

</style>
