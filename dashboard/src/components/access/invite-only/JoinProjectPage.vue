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

const route = useRoute()
const router = useRouter()
const announcer = useSkillsAnnouncer()
const colors = useColors()

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
    <div class="text-center">
      <h1 class="text-2xl text-primary"><span v-if="joined">Enrolled!</span><span v-else>Enroll in Training</span></h1>
    </div>
    <div class="max-w-lg lg:max-w-xl mx-auto text-center mt-4">
      <Card class="mt-4">
        <template #content>
      <skills-spinner :is-loading="loading" />
      <div v-if="!loading">
        <div class="text-center">
          <div v-if="!joined">
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
                :disabled="inviteInvalid"
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
          <Message v-if="inviteInvalid" :closable="false" severity="error">{{ invalidMsg }}</Message>
        </div>
      </div>
    </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>

</style>