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
import {computed, onMounted, ref} from 'vue'
import {useTimeoutFn} from "@vueuse/core";
import Popover from "primevue/popover";
import ScrollPanel from "primevue/scrollpanel";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import WebNotificationsService from "@/components/header/WebNotificationsService.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {useStorage} from '@vueuse/core'

const timeUtils = useTimeUtils()
const colors = useColors()

const lastViewedNotificationDate = useStorage('lastViewedNotificationDate', null)

const notificationsPopover = ref();
const toggle = (event) => {
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

const acknowledgeNotification = (notification) => {
  WebNotificationsService.ackNotification(notification.id)
      .then(() => {
        notifications.value = notifications.value.filter(n => n.id !== notification.id)
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
</script>

<template>
  <div class="d-inline relative">
    <div v-if="!loadingNotifications && notificationCount > 0" class="absolute -top-2 -right-1 z-10"
         :class="{'animate-bounce': alertNewNotif && hasNonViewedNotifications}">
      <span class="text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center"
            :class="{
              'bg-orange-700 text-white': hasNonViewedNotifications,
              'bg-green-700 text-white': !hasNonViewedNotifications
            }">
        {{ notificationCount > 9 ? '9+' : notificationCount }}
      </span>
    </div>
    <Button
        icon="fa-solid fa-bell"
        severity="warn"
        outlined
        raised
        @click="toggle"
        aria-label="User Settings Button"
        aria-haspopup="true"
        aria-controls="user_settings_menu">
    </Button>
    <Popover ref="notificationsPopover" aria-label="Notifications">
      <div class="flex flex-col w-[25rem] skills-notification">
        <div class="flex gap-2 items-center border-b-1 border-b-gray-200 mb-2 pb-2">
          <div class="flex-1 text-orange-800 dark:text-orange-400 uppercase">Notifications</div>
          <div>
            <SkillsButton label="Clear All" severity="danger" icon="fa-solid fa-trash" size="small"/>
          </div>
        </div>

        <ScrollPanel :style="`width: 100%; height: ${loadingOrNotNotifications? '200': '400'}px`">



          <div v-if="loadingNotifications" class="flex flex-col justify-center items-center pt-5">
            <skills-spinner :is-loading="true"/>
            Loading Notifications...
          </div>

          <div v-if="!loadingNotifications && notifications.length === 0" class="py-8 text-center">
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
               :class="notifClasses(index)">
            <div class="flex gap-2 items-center">
              <div class="font-bold flex-1 ">{{ notification.title }}</div>
              <div class="text-sm text-gray-600 dark:text-gray-200">
                {{ timeUtils.formatDate(notification.notifiedOn, 'YYYY-MM-DD') }}
              </div>
            </div>
            <div class="flex">
              <div class="flex-1 ">
                <markdown-text :text="notification.notification" data-cy="questionsText"
                               :instance-id="`${notification.id}`"/>
              </div>
              <div class="pt-2">
                <SkillsButton severity="warn" icon="fa-solid fa-trash" size="small"
                              @click="acknowledgeNotification(notification); notification.updating = true"
                              :loading="notification.updating"
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
.skills-notification .toastui-editor-contents a {
  //text-decoration: underline !important;
}
</style>