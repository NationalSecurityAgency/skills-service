<script setup>
import { nextTick, ref } from 'vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

const model = defineModel()
const props = defineProps({
  shareUrl: {
    type: String,
    required: true
  }
})

const announcer = useSkillsAnnouncer()
const visualEffectUrlWasCopied = ref(false);

const copyUrl = () =>  {
  navigator.clipboard.writeText(props.shareUrl)
    .then(() => {
      letUserKnowUrlWasCopied();
    });
}
const letUserKnowUrlWasCopied = () => {
  nextTick(() => {
    visualEffectUrlWasCopied.value = true;
    setTimeout(() => {
      visualEffectUrlWasCopied.value = false;
    }, 2000);
    announcer.polite('Copied project\'s share url');
  });
}
</script>

<template>
  <SkillsDialog
    v-model="model"
    header="Share Discoverable Project"
    @on-cancel="model = false"
    :show-ok-button="false"
    :enable-return-focus="true"
    cancel-button-label="Done"
    cancel-button-icon=""
    cancel-button-severity="success">
    <div class="text-xl text-primary text-center">To share your project, the following URL was copied.</div>

    <div class="flex border-1 border-round surface-border mt-2">
      <div class="flex-1 p-2" data-cy="projShareUrl">{{ shareUrl }}</div>
      <div class="border-left-1 surface-border">
        <SkillsButton
          text
          severity="info"
          @click="copyUrl"
          data-cy="copySharedUrl"
          aria-label="Copy project share url"
          icon="fas fa-copy" />
      </div>
    </div>

    <div class="flex justify-content-center">
      <InlineMessage
        severity="success"
        class="mt-2" :class="{ 'fadein animation-duration-1000 animation-iteration-infinite': visualEffectUrlWasCopied }">
        URL was copied!
      </InlineMessage>
    </div>
    <div class="text-primary mt-2 text-center mb-3">
      Please feel free to paste and share it with new users.
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>