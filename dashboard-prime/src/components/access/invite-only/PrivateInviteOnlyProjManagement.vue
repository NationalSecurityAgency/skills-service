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
          class="my-4">
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
        <Card class="mt-3">
          <template #subtitle>Invites Pending Acceptance</template>
          <template #content>
            <invite-statuses ref="inviteStatuses" />
          </template>
        </Card>

        </BlockUI>
      </template>
    </Card>

    <revoke-user-access v-if="privateProject" class="my-4" />
  </div>
</template>

<style scoped>

</style>
