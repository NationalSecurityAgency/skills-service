<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import AccessService from '@/components/access/AccessService.js'
import { useRoute } from 'vue-router'
import Column from 'primevue/column'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import NoContent2 from '@/components/utils/NoContent2.vue'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'
import RemovalValidation from '@/components/utils/modal/RemovalValidation.vue'

const route = useRoute()
const responsive = useResponsiveBreakpoints()
const timeUtils = useTimeUtils()
const colors = useColors()
const announcer = useSkillsAnnouncer()
const dialogMessages = useDialogMessages()

const recipientFilter = ref('')
const data = ref([])
const loadingData = ref(true)
const busy = ref(false)
const pageSize = ref(5)
const possiblePageSizes = [5, 10, 15, 20]
const sortInfo = ref({ sortOrder: -1, sortBy: 'expires' })
const totalRows = ref(0)
const currentPage = ref(1)

const showNotificationSending = ref(false)
const showNotificationSuccess = ref(false)

const showDeleteDialog = ref(false)
const deleteRecipientRef = ref(null)
const recipientToDelete = ref('')

const hasData = computed(() => data.value && data.value.length > 0)


const inviteExtensionMenu = ref()
const inviteExtensionMenuItems = ref([
  {
    label: 'Extend expiration by',
    items: [
      {
        label: '30 minutes',
        value: 'PT30M',
        command: (event, second) => {
          console.log(event)
          console.log(second)
        }
      },
      {
        label: '8 hours',
        value: 'PT8H'
      },
      {
        label: '24 hours',
        value: 'PT24H'
      },
      {
        label: '7 days',
        value: 'P7D'
      },
      {
        label: '30 days',
        value: 'P30D'
      }
    ]
  }
])
const extendExpirationRecipientEmail = ref('')
const toggleInviteExtensionMenu = (event, recipientEmail) => {
  extendExpirationRecipientEmail.value = recipientEmail
  inviteExtensionMenu.value.toggle(event)
}
const extendExpiration = (extension) => {
  AccessService.extendInvite(route.params.projectId, extendExpirationRecipientEmail.value, extension).then(() => {
    announcer.polite(`the expiration of project invite for ${extendExpirationRecipientEmail.value} has been extended`)
    loadData()
  })
}

const loadData = () => {
  busy.value = true
  const pageParams = {
    limit: pageSize.value,
    ascending: sortInfo.value.sortOrder === 1,
    page: currentPage.value,
    orderBy: sortInfo.value.sortBy
  }
  AccessService.getInviteStatuses(route.params.projectId, recipientFilter.value, pageParams)
    .then((result) => {
      data.value = result.data
      totalRows.value = result.totalCount
      busy.value = false
    }).finally(() => {
    loadingData.value = false
  })
}

onMounted(() => {
  loadData()
})


const isExpired = (expirationDate) => {
  return dayjs(expirationDate).isBefore(dayjs())
}

const MESSAGE_DURATION = 8000
const remindUser = (recipientEmail) => {
  showNotificationSuccess.value = false
  showNotificationSending.value = true
  AccessService.remindInvitedUser(route.params.projectId, recipientEmail).then(() => {
    announcer.polite(`Invite reminder sent to ${recipientEmail}`)
    showNotificationSending.value = false
    showNotificationSuccess.value = true
    setTimeout(() => {
      showNotificationSuccess.value = false
    }, MESSAGE_DURATION)
  }).catch((err) => {
    if (err.response.data && err.response.data.errorCode && err.response.data.errorCode === 'ExpiredProjectInvite') {
      dialogMessages.msgOk(`The project invite for ${recipientEmail} has expired, reminders cannot be sent for expired invites, please extend the expiration for this invite and try again.`, 'Expired Invite')
      loadData()
    } else {
      throw err
    }
  }).finally(() => {
    showNotificationSending.value = false
  })
}

const deletePendingInvite = (recipient, deleteBtnRef) => {
  recipientToDelete.value = recipient
  deleteRecipientRef.value = deleteBtnRef
  showDeleteDialog.value = true
}
const doDeletePendingInvite = () => {
  busy.value = true
  AccessService.deleteInvite(route.params.projectId, recipientToDelete.value).then(() => {
    const email = recipientToDelete.value
    deleteRecipientRef.value = null
    recipientToDelete.value = null
    announcer.polite(`the project invite for ${email} has been deleted`)
    loadData()
  })
}

defineExpose({
  loadData
})
</script>

<template>
  <div>
    <skills-spinner :is-loading="loadingData" />
    <div v-if="!loadingData">
      <no-content2 v-if="!hasData" title="No Project Invites"
                   message="Once Project Invites have been sent to users, any invites that have not yet been accepted or invites that have recently expired can be managed here" />
      <SkillsDataTable
        v-if="hasData"
        tableStoredStateId="pendingInviteTable"
        :value="data"
        :loading="busy"
        data-cy="projectInviteStatusTable"
        paginator
        :rows="pageSize"
        :rowsPerPageOptions="possiblePageSizes"
        v-model:sort-field="sortInfo.sortBy"
        v-model:sort-order="sortInfo.sortOrder">
        <template #empty>
          There are not pending invites
        </template>

        <Column field="recipientEmail" header="Recipient" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-envelope-open-text mr-1" :class="colors.getTextClass(0)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            {{ slotProps.data.recipientEmail }}
          </template>
        </Column>

        <Column field="created" header="Created" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-user-clock mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <span
              :title="`${timeUtils.formatDate(slotProps.data.created)}`">{{ timeUtils.relativeTime(slotProps.data.created)
              }}</span>
          </template>
        </Column>

        <Column field="expires" header="Expires" :sortable="true" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-hourglass-half mr-1" :class="colors.getTextClass(2)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <div class="flex align-items-center">
              <InlineMessage
                v-if="isExpired(slotProps.data.expires)"
                icon="fas fa-exclamation-triangle"
                class="mr-1"
                severity="error">
                expired
              </InlineMessage>
              <span
                :title="`${timeUtils.formatDate(slotProps.data.expires)}`">{{ timeUtils.timeFromNow(slotProps.data.expires)
                }}</span>
            </div>
          </template>
        </Column>
        <Column field="controls" header="Controls" :sortable="false" :class="{'flex': responsive.md.value }">
          <template #header>
            <i class="fas fa-tools mr-1" :class="colors.getTextClass(3)" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <div>
              <ButtonGroup>
                <SkillsButton
                  icon="fas fa-hourglass-half"
                  :aria-label="`Extend invite expiration for ${slotProps.data.recipientEmail}`"
                  :id="`extend-${slotProps.index}`"
                  data-cy="extendInvite"
                  :title="`Extend invite expiration for ${slotProps.data.recipientEmail}`"
                  @click="toggleInviteExtensionMenu($event, slotProps.data.recipientEmail)" />
                <Menu ref="inviteExtensionMenu"
                      :id="`extendMenuSelection-${slotProps.index}`"
                      :model="inviteExtensionMenuItems" :popup="true">
                  <template #item="{ item }">
                    <div class="pb-2 pl-4">
                      <a
                        :data-cy="`invite-${slotProps.index}-extension`"
                        @click="extendExpiration(item.value)">{{ item.label }}</a>
                    </div>
                  </template>
                </Menu>

                <SkillsButton
                  icon="fas fa-paper-plane"
                  aria-label="remind user"
                  data-cy="remindUser"
                  :disabled="isExpired(slotProps.data.expires)"
                  :title="`Send ${slotProps.data.recipientEmail} a reminder`"
                  @click="remindUser(slotProps.data.recipientEmail)" />

                <SkillsButton
                  :id="`deleteInviteBtn-${slotProps.index}`"
                  :track-for-focus="true"
                  icon="fas fa-trash"
                  severity="warn"
                  aria-label="delete project invite"
                  data-cy="deleteInvite"
                  :title="`Delete ${slotProps.data.recipientEmail} invite`"
                  :ref="`${slotProps.data.recipientEmail}_delete`"
                  @click="deletePendingInvite(slotProps.data.recipientEmail, `${slotProps.data.recipientEmail}_delete`)" />

              </ButtonGroup>
            </div>
          </template>
        </Column>
      </SkillsDataTable>
      <Message
        v-if="showNotificationSending || showNotificationSuccess"
      >
        <div id="accessNotificationPanel">
          <div v-if="showNotificationSending">
            <skills-spinner :is-loading="true"></skills-spinner>
            Sending notification reminder
          </div>
          <div v-if="showNotificationSuccess">
            Invite reminder sent!
          </div>
        </div>
      </Message>

    </div>

    <removal-validation v-if="showDeleteDialog"
                        v-model="showDeleteDialog"
                        :item-name="recipientToDelete"
                        item-type="invite"
                        @do-remove="doDeletePendingInvite" />
  </div>

</template>

<style scoped>

</style>