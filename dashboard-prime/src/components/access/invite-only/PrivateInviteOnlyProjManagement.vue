<script setup>
import { ref } from 'vue'
import SkillsCardHeader from '@/components/utils/cards/SkillsCardHeader.vue'
import InviteUsersToProject from '@/components/access/invite-only/InviteUsersToProject.vue'
import InviteStatuses from '@/components/access/invite-only/InviteStatuses.vue'
import RevokeUserAccess from '@/components/access/invite-only/RevokeUserAccess.vue'


const privateProject = ref(true)

const inviteStatuses = ref(null)
const handleInviteSent = () => {
  inviteStatuses.value.loadData()
}

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
        <!--    <b-overlay :show="!isEmailEnabled">-->
        <!--      <div slot="overlay" class="alert alert-warning mt-2" data-cy="inviteUsers_emailServiceWarning">-->
        <!--        <i class="fa fa-exclamation-triangle" aria-hidden="true"/> Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.-->
        <!--      </div>-->
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

        <!--    </b-overlay>-->
      </template>
    </Card>

    <revoke-user-access v-if="privateProject" class="my-4" />
  </div>
</template>

<style scoped>

</style>
