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
  console.log(inputItem);
  if (inputItem.value != null) {
    added(inputItem.value);
  } else {
    removed(null);
  }
};

const searchChanged = (query, loadingFunction) => {
  emit('search-change', query, loadingFunction);
};
</script>

<!--:filterable="internalSearch"-->
<!--label="name"-->
<!--v-on:search="searchChanged"-->
<template>
  <div id="project-selector">
    <Dropdown :options="projects"
              v-model="selectedInternal"
              placeholder="Select Project..."
              class="w-full"
              optionLabel="name"
              @change="inputChanged"
              :loading="isLoading">
<!--      <template #option="{ name, projectId }">-->
<!--        <div :data-cy="`${projectId}_option`">-->
<!--          <div class="h6">{{ name }}</div>-->
<!--          <div class="text-secondary">ID: {{ projectId }}</div>-->
<!--        </div>-->
<!--      </template>-->
<!--      <template v-if="afterListSlotText" #list-footer>-->
<!--        <li>-->
<!--          <div class="h6 ml-1" data-cy="projectSelectorCountMsg"> {{ afterListSlotText }}</div>-->
<!--        </li>-->
<!--      </template>-->
    </Dropdown>
  </div>
</template>

<style scoped>

</style>