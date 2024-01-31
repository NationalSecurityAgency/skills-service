<script setup>
import Card from 'primevue/card';

const props = defineProps({
  title: String,
  subTitle: String,
  iconClass: String,
});
</script>

<template>
  <Card>
    <template #content>
      <div class="flex flex-row">
        <div style="min-width: 5rem;" class="text-center">
          <i class="fa-3x" :class="iconClass"/>
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
