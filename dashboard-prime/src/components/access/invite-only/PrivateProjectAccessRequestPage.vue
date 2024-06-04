<script setup>
import { ref, computed } from 'vue'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useRoute } from 'vue-router'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import ContactOwnersDialog from '@/components/myProgress/ContactOwnersDialog.vue'

const appInfo = useAppInfoState()
const route = useRoute()
const colors = useColors()

const showContact = ref(false)

const projectId = computed(() => route.params.projectId)
</script>

<template>
  <div class="mt-8">
    <div class="text-center">
      <span class="fa-stack fa-3x" style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x" :class="colors.getTextClass(1)"></i>
                      <i class="fas fa-shield-alt fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-center">
      <div class="text-2xl text-primary">Invite Only Project</div>
    </div>

    <div class="row justify-content-center text-danger mt-3">
      <div class="col col-sm-8 col-md-6 col-lg-4 text-center" data-cy="notAuthorizedExplanation">
        <p>
          This Project is configured for Invite Only access. You can concat project's administrators to request access.
        </p>
        <p v-if="appInfo.emailEnabled">
          <SkillsButton
            label="Contact Project"
            icon="fas fa-mail-bulk"
            @click="showContact=true"
            data-cy="contactOwnerBtn" />
        </p>
      </div>
    </div>
    <contact-owners-dialog v-if="showContact"
                           v-model="showContact"
                           :project-id="projectId"
                           :project-name="projectId"
                            />
  </div>
</template>

<style scoped>

</style>