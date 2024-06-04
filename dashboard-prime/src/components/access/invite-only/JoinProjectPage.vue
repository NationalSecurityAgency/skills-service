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
  <Card class="mt-3">
    <template #content>
      <skills-spinner :is-loading="loading" />
      <div v-if="!loading">
        <div class="text-center mt-5">
          <i class="fa fa-users fa-4x text-secondary" :class="colors.getTextClass(1)"></i>
          <h2 class="mt-3 text-primary">
            <span>Join Project</span>
          </h2>
          <div v-if="!joined">
            <div class="mb-2">
              <p>Great News! You have been invited to join the <span
                class="text-primary font-bold">{{ projectName }}</span> Project.</p>
              <p><span class="text-primary font-bold">{{ projectName }}</span> is a gamified micro-learning
                experience
                built using the SkillTree platform.
                Click 'Join Now' button to explore <span class="text-primary font-bold">{{ projectName
                  }}</span> and see how you can earn points and achievements!
              </p>
            </div>
            <div>
              <SkillsButton
                label="Join Now"
                :icon="joinIcon"
                @click="join"
                :disabled="inviteInvalid"
                data-cy="joinProject" />
            </div>
          </div>
          <div v-if="joined">
            You have successfully joined Project <span class="text-primary font-bold">{{ projectName }}</span>!
            <div class="flex justify-content-center mt-3">
            <router-link :to="{ path: `/progress-and-rankings/projects/${route.params.pid}` }">
              <Button
                label="View Project Now"
                :aria-label="`Click to navigate to ${projectName} project page.`"
                :data-cy="`project-link-${route.params.pid}`"
                icon="far fa-eye"
                outlined class="w-full" size="small"/>
            </router-link>
            </div>
            <p>You will be forwarded to in <Tag>{{ timer }}</Tag> seconds.</p>

          </div>
          <div v-if="inviteInvalid" class="mt-3 text-danger" role="alert">{{ invalidMsg }}</div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>