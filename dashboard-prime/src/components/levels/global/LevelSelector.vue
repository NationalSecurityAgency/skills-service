<script setup>
import { ref, watch, onMounted } from 'vue';
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";

const emit = defineEmits(['input', 'removed'])
const props = defineProps({
  value: {
    type: Number,
  },
  projectId: {
    type: String,
  },
  disabled: {
    type: Boolean,
  },
  placeholder: {
    type: String,
  },
  loadImmediately: {
    type: Boolean,
    required: false,
    default: false,
  },
  selectedLevel: {
    required: false,
    type: Number,
  }
});

onMounted(() => {
  setSelectedInternal();
  if (props.loadImmediately && props.projectId) {
    loadProjectLevels(props.projectId);
  }
})

watch(() => props.projectId, (newProjectId) => {
  selectedInternal.value = null;
  if (!newProjectId) {
    projectLevels.value = [];
  } else {
    loadProjectLevels(newProjectId);
  }
})

const isLoading = ref(false);
const projectLevels = ref([]);
const selectedInternal = ref(null);

const loadProjectLevels = (projectId) => {
  isLoading.value = true;
  GlobalBadgeService.getProjectLevels(projectId)
      .then((response) => {
        response.forEach((entry) => {
          projectLevels.value.push({ level: entry.level, disabled: props.selectedLevel && props.selectedLevel === entry.level });
        })
      }).finally(() => {
    isLoading.value = false;
  });
};

const setSelectedInternal = () => {
  if (props.value) {
    selectedInternal.value = props.value;
  }
};

const removed = (removedItem) => {
  emit('removed', removedItem);
};

const selected = (selectedItem) => {
  emit('input', selectedItem);
};

const inputChanged = (inputItem) => {
  if (inputItem.value != null) {
    selected(inputItem.value);
  } else {
    removed(null);
  }
};
</script>

<template>
  <div id="level-selector">
    <Dropdown v-model="selectedInternal" :disabled="disabled" optionLabel="level" optionValue="level"
              optionDisabled="disabled" :options="projectLevels" :placeholder="placeholder"
              class="w-full" :loading="isLoading" @change="inputChanged">
    </Dropdown>
  </div>
</template>

<style scoped>

</style>