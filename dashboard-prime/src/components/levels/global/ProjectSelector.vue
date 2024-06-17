<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const emit = defineEmits(['input', 'added', 'removed', 'search-change'])
const props = defineProps({
  value: {
    type: Object,
  },
  projects: {
    type: Array,
    required: true,
  },
  internalSearch: {
    type: Boolean,
    default: true,
  },
  afterListSlotText: {
    type: String,
    default: '',
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
});

const selectedInternal = ref(null);
const badgeId = ref(null);

onMounted(() => {
  badgeId.value = route.params.badgeId;
  setSelectedInternal();
});

watch(() => props.value, () => {
  setSelectedInternal();
})

const setSelectedInternal = () => {
  if (props.value) {
    selectedInternal.value = { ...props.value };
  } else {
    selectedInternal.value = null;
  }
};

const removed = (removedItem) => {
  emit('removed', removedItem);
};

const added = (addedItem) => {
  emit('input', addedItem);
  emit('added', addedItem);
};

const inputChanged = (inputItem) => {
  if (inputItem.value != null) {
    added(inputItem.value);
  } else {
    removed(null);
  }
};

const searchChanged = (query) => {
  emit('search-change', query.value);
};
</script>

<template>
  <div id="project-selector">
    <Dropdown :options="projects"
              v-model="selectedInternal"
              :filter="internalSearch"
              @filter="searchChanged"
              placeholder="Select Project..."
              class="w-full"
              optionLabel="name"
              @change="inputChanged"
              :loading="isLoading">
      <template #option="slotProps">
        <div :data-cy="`${slotProps.option.projectId}_option`">
          <div class="h6">{{ slotProps.option.name }}</div>
          <div class="text-400 text-sm">ID: {{ slotProps.option.projectId }}</div>
        </div>
      </template>
      <template #footer v-if="afterListSlotText">
        <div class="h6 ml-1" data-cy="projectSelectorCountMsg">{{ afterListSlotText }}</div>
      </template>
    </Dropdown>
  </div>
</template>

<style scoped>

</style>