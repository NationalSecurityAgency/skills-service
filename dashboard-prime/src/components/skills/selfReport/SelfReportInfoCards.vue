<script setup>
import { ref, onMounted } from 'vue';

const props = defineProps({
  selfReportStats: Array,
});

const cards = ref([{
  id: 'Approval',
  label: 'Approval Required',
  count: 0,
  icon: 'far fa-thumbs-up',
}, {
  id: 'HonorSystem',
  label: 'Honor System',
  count: 0,
  icon: 'far fa-meh-rolling-eyes',
}, {
  id: 'Disabled',
  label: 'Disabled',
  count: 0,
  icon: 'far fa-times-circle',
}]);

onMounted(() => {
cards.value = cards.value.map((c) => {
  const found = props.selfReportStats.find((s) => c.id === s.value);
  if (found) {
    // eslint-disable-next-line
    c.count = found.count;
  }
  return c;
});
});
</script>

<template>
  <div class="flex gap-2">
    <div class="flex flex-1" v-for="card in cards" :key="card.label">
      <Card class="h-full w-full" :pt="{ body: { class: 'p-3' }, content: { class: 'p-0' } }">
        <template #content>
          <div class="flex">
            <div class="flex-1 text-left">
              <div class="h5 card-title uppercase text-muted mb-0 small">{{card.label}}</div>
              <span class="h5 font-bold mb-0" :data-cy="`selfReportInfoCardCount_${card.id}`">{{ card.count }}</span> skills
            </div>
            <div class="">
              <i :class="card.icon" style="font-size: 2.2rem;"></i>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
</style>