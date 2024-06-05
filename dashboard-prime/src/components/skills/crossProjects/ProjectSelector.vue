<script setup>
import { ref, onMounted, watch } from 'vue';
import ProjectService from '@/components/projects/ProjectService'
import Dropdown from "primevue/dropdown";

const props = defineProps({
  projectId: String,
  selected: Object,
  disabled: {
    type: Boolean,
    default: false,
  },
  onlySingleSelectedValue: {
    type: Boolean,
    default: false,
  },
});
const emit = defineEmits(['selected', 'unselected']);

const isLoading = ref(false);
const projects = ref([]);
const selectedValue = ref({});

watch(() => props.selected, () => {
  selectedValue.value = props.selected;
})

onMounted(() => {
  selectedValue.value = props.selected;
  search('');
});

const onSelected = (selectedItem) => {
  emit('selected', selectedItem);
};

const onRemoved = (item) => {
  emit('unselected', item);
};

const inputChanged = (inputItem) => {
  if (inputItem != null) {
    onSelected(inputItem);
  } else {
    onRemoved(null);
  }
};

const search = (query) => {
  isLoading.value = true;

  ProjectService.queryOtherProjectsByName(props.projectId, query).then((response) => {
    isLoading.value = false;
    projects.value = response;
  });
};
</script>

<template>
  <div id="project-selector" data-cy="projectSelector" class="w-full">
    <Dropdown :options="projects" placeholder="Search for a project..." v-model="selectedValue" label="name" class="w-full" filter
              :disabled="disabled" :loading="isLoading" :filterFields="['name']" @update:model-value="inputChanged">
      <template #value="slotProps" v-if="selectedValue">
        <div>{{slotProps.value?.name}}</div>
      </template>
      <template #option="slotProps">
        <div>
          <div class="h6 project-name" data-cy="projectSelector-projectName">{{ slotProps.option.name }}</div>
          <div class="text-secondary project-id">ID: {{ slotProps.option.projectId }}</div>
        </div>
      </template>
    </Dropdown>
  </div>
</template>

<style scoped>

</style>