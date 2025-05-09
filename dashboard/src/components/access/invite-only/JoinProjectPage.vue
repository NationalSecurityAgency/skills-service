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
import { computed, onMounted, ref, watch } from 'vue'
import AccessService from '@/components/access/AccessService.js'
import { useRoute, useRouter } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import MyProgressService from "@/components/myProgress/MyProgressService.js";
import {useAppInfoState} from "@/stores/UseAppInfoState.js";

const route = useRoute()
const router = useRouter()
const announcer = useSkillsAnnouncer()
const colors = useColors()
const appInfoState = useAppInfoState()

const inviteInvalid = ref(false)
const loading = ref(false)
const invalidMsg = ref('')
const joining = ref(false)
const joined = ref(false)
const timer = ref(-1)

const loadData = () => {
  loading.value = true
  AccessService.isInviteValid(route.params.pid, route.params.inviteToken).then((resp) => {
    inviteInvalid.value = !resp.valid
    invalidMsg.value = resp.message
  }).finally(() => {
    loading.value = false
  })
}

const projectName = computed(() => route.query.pn)
const joinIcon = computed(() => joining.value ? 'fas fa-spinner' : 'fas fa-unlock')

onMounted(() => {
  loadData()
})

const join = () => {
  if (!inviteInvalid.value) {
    joining.value = true
    AccessService.joinProject(route.params.pid, route.params.inviteToken).then(() => {
      joined.value = true
      announcer.polite(`Successfully joined project ${projectName.value}, loading project profile`)
      timer.value = 10;
    }).finally(() => {
      joining.value = false
    })
  }
}

const sendingRequestForNewInvite = ref(false)
const requestForNewInviteSent = ref(false)
const requestNewInvite = () => {
  sendingRequestForNewInvite.value = true
  const projId = route.params.pid
  MyProgressService.requestNewInvite(projId)
      .then(() => {
        requestForNewInviteSent.value = true
      })
      .finally(() => {
        sendingRequestForNewInvite.value = false
      })
}

watch(() => timer.value, (value) => {
  if (value > 0) {
    setTimeout(() => {
      timer.value -= 1;
    }, 1000);
  } else {
    router.replace(`/progress-and-rankings/projects/${route.params.pid}`);
  }
})
</script>

<template>
  <div class="mt-10 mb-20">
    <div class="flex justify-center">
      <div class="rounded-lg w-24 h-24 p-2 m-2 bg-green-600 font-bold flex items-center justify-center">
        <i class="text-surface-0 dark:text-surface-900 text-6xl fa fa-users" aria-hidden="true"></i>
      </div>
    </div>
    <div class="text-center text-2xl">
      <h1 class="text-primary" v-if="!inviteInvalid"><span v-if="joined">Enrolled!</span><span v-else>Enroll in Training</span></h1>
      <h1 class="text-red-800 dark:text-red-200" v-if="inviteInvalid">Invalid Invite</h1>
    </div>
    <div class="max-w-lg lg:max-w-xl mx-auto text-center mt-4">
      <Card class="mt-4">
        <template #content>
      <skills-spinner :is-loading="loading" />
      <div v-if="!loading">
        <div class="text-center">
          <div v-if="!joined && !inviteInvalid">
            <div class="mb-2">
              <p>
                Exciting News! You're Invited to <span class="text-primary font-bold">{{ projectName }}</span>!
              </p>
              <p class="mt-2">
                Discover a unique gamified learning experience, powered by SkillTree. Join <span
                  class="text-primary font-bold">{{ projectName }}</span>
                to embark on a journey of bite-sized learning, earning rewards and achievements along the way.
              </p>

            </div>
            <SkillsButton
                class="mt-3"
                label="Join Now"
                :icon="joinIcon"
                @click="join"
                data-cy="joinProject" />
          </div>
          <div v-if="joined">
            Congratulations! You're now a member of <span class="text-primary font-bold">{{ projectName }}</span>!
            <div class="flex justify-center mt-4 mb-2">
            <router-link :to="{ path: `/progress-and-rankings/projects/${route.params.pid}` }" tabindex="-1">
              <Button
                label="Get Started"
                :aria-label="`Click to navigate to ${projectName} project page.`"
                :data-cy="`project-link-${route.params.pid}`"
                icon="far fa-eye"
                outlined class="w-full" size="small"/>
            </router-link>
            </div>
            <Message :closable="false" class="mt-3">Click 'Get Started' to proceed immediately, or wait and you'll be redirected automatically in <Tag>{{ timer }}</Tag> seconds.</Message>

          </div>
          <div v-if="inviteInvalid && !requestForNewInviteSent" data-cy="invalidInvite">
            <p>Unfortunately, this invite to <span class="text-primary font-bold">{{ projectName }}</span> training is no longer valid.</p>
            <p class="mt-3">Common reasons for this include expiration of the invite.</p>

            <SkillsButton
                v-if="appInfoState.emailEnabled"
                class="mt-3"
                label="Request New Invite"
                :icon="joinIcon"
                @click="requestNewInvite"
                :loading="sendingRequestForNewInvite"
                data-cy="requestNewInvite" />
          </div>
          <div v-if="requestForNewInviteSent" data-cy="inviteRequestSent">
            <Message :closable="false">Request was sent. Thank your for your patience!</Message>

            <router-link to="/" data-cy="takeMeHome">
              <SkillsButton
                  label="Take Me Home"
                  icon="fas fa-home"
                  outlined
                  size="medium"
                  severity="info"
                  class="mt-8" />
            </router-link>
          </div>
        </div>
      </div>
    </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>

</style>