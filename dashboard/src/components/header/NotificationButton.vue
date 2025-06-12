/*
Copyright 2025 SkillTree

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
<script setup lang="ts">
import {computed, nextTick, onMounted, ref} from 'vue'
import {useTimeoutFn} from "@vueuse/core";
import Popover from "primevue/popover";
import ScrollPanel from "primevue/scrollpanel";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import WebNotificationsService from "@/components/header/WebNotificationsService.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useStorage} from '@vueuse/core'
import {useConfirm} from "primevue";
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'

const timeUtils = useTimeUtils()
const colors = useColors()
const dialogMessages = useDialogMessages()

const lastViewedNotificationDate = useStorage('lastViewedNotificationDate', null)

const confirm = useConfirm()

const notificationsPopover = ref();
const toggle = (event=null) => {
  notificationsPopover.value.toggle(event);
  afterNotificationsLoaded().then(() => {
    if (latestNotification.value) {
      lastViewedNotificationDate.value = latestNotification.value.notifiedOn
    }
  })
}

const latestNotification = computed(() => {
  if (notifications.value && notifications.value.length > 0) {
    return notifications.value.reduce((latest, current) => {
      const currentDate = new Date(current.notifiedOn);
      const latestDate = latest ? new Date(latest.notifiedOn) : null;

      if (!latestDate || currentDate > latestDate) {
        return current;
      }
      return latest;
    }, null)
  }

  return null
})
const hasNonViewedNotifications = computed(() => {
  const latestNotif = latestNotification.value
  if (!latestNotif) {
    return false
  }

  if (!lastViewedNotificationDate.value) {
    return true
  }

  return latestNotif.notifiedOn > lastViewedNotificationDate.value
})

const alertNewNotif = ref(false)

onMounted(() => {
  alertNewNotif.value = true
  const {isPending, start, stop} = useTimeoutFn(() => {
    alertNewNotif.value = false
  }, 7000)
})


const notifications = ref([])
const loadingNotifications = ref(true)
const afterNotificationsLoaded = () => {
  return new Promise((resolve) => {
    (function waitNotificationsLoaded() {
      if (!loadingNotifications.value) return resolve(notifications.value)
      setTimeout(waitNotificationsLoaded, 100)
      return notifications.value
    }())
  })
}

onMounted(() => {
  WebNotificationsService.getNotifications()
      .then((res) => {
        notifications.value = res;
      }).finally(() => {
    loadingNotifications.value = false
  })
})
const notificationCount = computed(() => notifications.value ? notifications.value.length : 0)
const hasNotifications = computed(() => notificationCount.value > 0)

const dismissNotification = (notification) => {
  WebNotificationsService.dismissNotification(notification.id)
      .then(() => {
        notifications.value = notifications.value.filter(n => n.id !== notification.id)
        if (notifications.value.length === 0) {
          toggle()
        }
      })
}
const confirmDismissAllNotifications = (event) => {
  dialogMessages.msgConfirm({
    message: 'Clear your notification history? This action is permanent.',
    header: 'Dismiss All Notifications',
    acceptLabel: 'Proceed',
    acceptIcon: 'fa-solid fa-trash-can',
    acceptClass: 'p-button-danger p-button-outlined',
    rejectClass: 'p-button-secondary p-button-outlined',
    accept: () => {
      dismissAllNotifications()
    }
  })
};

const dismissAllNotifications = () => {
  loadingNotifications.value = true
  WebNotificationsService.dismissAllNotifications()
      .then(() => {
        notifications.value = []
      }).finally(() => {
    loadingNotifications.value = false
    nextTick(() => {
      console.log('focus')
      document.getElementById('viewNotifBtn').focus()
    })
  })
}
const loadingOrNotNotifications = computed(() => {
  return loadingNotifications.value || notifications.value.length === 0
})

const notifClasses = (index) => {
  let res = colors.getLeftBorderClass(index)
  if (index % 2 !== 0) {
    return `bg-gray-100 dark:bg-gray-800 ${res}`
  }

  return res
}

const onPopoverShow = () => {
  if (hasNotifications.value) {
    nextTick(() => {
      document.getElementById('dismissAllNotifBtn').focus()
    })
  }
}
</script>

<template>
  <div class="d-inline relative">
    <div v-if="!loadingNotifications && hasNotifications"
         class="absolute -top-2 -right-1 z-10"
         :aria-label="`You have ${notificationCount} new notifications`"
         :class="{'animate-bounce': alertNewNotif && hasNonViewedNotifications}">
      <span class="text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center"
            :class="{
              'bg-orange-700 text-white': hasNonViewedNotifications,
              'bg-green-700 text-white': !hasNonViewedNotifications
            }"
            data-cy="notifCount">
        {{ notificationCount > 9 ? '9+' : notificationCount }}
      </span>
    </div>
    <Button
        id="viewNotifBtn"
        icon="fa-solid fa-bell"
        severity="warn"
        outlined
        raised
        @click="toggle"
        data-cy="notifBtn"
        :aria-label="`View ${notificationCount} Notifications`"
        aria-haspopup="true"
        aria-controls="user_settings_menu">
    </Button>
    <Popover ref="notificationsPopover" aria-label="Notifications" @show="onPopoverShow">
      <div class="flex flex-col w-max-[50rem]" data-cy="notifPanel">
        <div class="flex gap-2 items-center border-b-1 border-b-gray-200 mb-2 pb-2">
          <div class="flex-1 text-orange-800 dark:text-orange-400 uppercase">Notifications</div>
          <div>
            <SkillsButton
                v-if="hasNotifications"
                id="dismissAllNotifBtn"
                label="Dismiss All"
                severity="danger"
                icon="fa-solid fa-trash"
                size="small"
                data-cy="dismissAllNotifBtn"
                @click="confirmDismissAllNotifications"/>
          </div>
        </div>

        <ScrollPanel :style="`width: 100%; height: ${loadingOrNotNotifications? '200': '400'}px`">

          <div v-if="loadingNotifications" class="flex flex-col justify-center items-center pt-5">
            <skills-spinner :is-loading="true"/>
            Loading Notifications...
          </div>

          <div v-if="!loadingNotifications && !hasNotifications" class="py-8 text-center w-[18rem]">
            <div class="text-gray-400 mb-2">
              <i class="fa-regular fa-bell-slash text-4xl"></i>
            </div>
            <div class="text-gray-600 dark:text-gray-300" role="alert">
              No new notifications
            </div>
            <div class="text-sm text-gray-500 mt-1">
              Check back later for updates
            </div>
          </div>

          <div v-if="!loadingNotifications"
               v-for="(notification, index) in notifications"
               :key="notification.id"
               class="mb-2 pl-2 pr-3 border-l-2 pt-1"
               :class="notifClasses(index)"
               :data-cy="`notif-${index}`">
            <div class="flex gap-2 items-center">
              <div class="font-bold flex-1" data-cy="notifTitle">{{ notification.title }}</div>
              <div class="text-sm text-gray-600 dark:text-gray-200" :title="timeUtils.formatDate(notification.notifiedOn, 'dddd, MMMM D, YYYY')">
                {{ timeUtils.relativeTime(notification.notifiedOn) }}
              </div>
            </div>
            <div class="flex">
              <div class="flex-1 ">
                <markdown-text :text="notification.notification" data-cy="notifText"
                               :instance-id="`${notification.id}`"/>
              </div>
              <div class="pt-2">
                <SkillsButton severity="warn" icon="fa-solid fa-trash" size="small"
                              @click="dismissNotification(notification); notification.updating = true"
                              :loading="notification.updating"
                              data-cy="dismissNotifBtn"
                              aria-label="Dismiss Notification"/>
              </div>
            </div>
          </div>

        </ScrollPanel>
      </div>
    </Popover>
  </div>
</template>

<style>
</style>