<script setup>
import { reactive} from 'vue'
import Card from 'primevue/card'

const props = defineProps({
  title: [String, Number],
  subTitle: String,
  iconClass: String,
  iconColor: {
    type: String,
    require: false,
  }
});

const styleObject = {
  color: props.iconColor,
}
</script>

<template>
  <Card :pt="{content: { class: 'p-0' } }">
    <template #content>
      <div class="flex flex-row">
        <div style="min-width: 5rem;" class="text-center">
          <i class="fa-3x" :class="iconClass" :style="styleObject"/>
        </div>
        <div>
          <div class="text-2xl mb-2 uppercase" data-cy="mediaInfoCardTitle">{{ title }}</div>
          <div class="text-sm" data-cy="mediaInfoCardSubTitle">
            <slot>
              {{ subTitle }}
            </slot>
          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped></style>
