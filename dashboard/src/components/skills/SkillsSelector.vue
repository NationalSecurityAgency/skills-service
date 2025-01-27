/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import AutoComplete from 'primevue/autocomplete'
import Badge from 'primevue/badge';
// import Fluid from 'primevue/fluid';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SkillReuseIdUtil from '@/components/utils/SkillReuseIdUtil';

const announcer = useSkillsAnnouncer();
const emit = defineEmits(['added', 'search-change']);
const props = defineProps({
  options: {
    type: Array,
    required: true,
  },
  selected: {
    type: Object,
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
  internalSearch: {
    type: Boolean,
    default: true,
  },
  emptyWithoutSearch: {
    type: Boolean,
    default: false,
  },
  afterListSlotText: {
    type: String,
    default: '',
  },
  showProject: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: 'Select skill(s)...',
  },
  placeholderIcon: {
    type: String,
    default: null,
  },
  selectLabel: {
    type: String,
    default: 'Press enter to select',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  showType: {
    type: Boolean,
    default: false,
  },
  showClear: {
    type: Boolean,
    default: true,
  },
  showDropdown: {
    type: Boolean,
    default: true,
  },
});

const selectedInternal = ref([]);
const optionsInternal = ref([]);
const currentSearch = ref('');
const isMounted = ref(false);

watch(() => props.selected, async () => {
  setSelectedInternal();
})
// methods
const removeReuseTag = (val) => {
  return SkillReuseIdUtil.removeTag(val);
};

const setSelectedInternal = () => {
  if (props.selected && (props.selected.length || props.selected.name)) {
    selectedInternal.value = {entryId: `${props.selected.projectId}_${props.selected.skillId}`, ...props.selected}; //props.selected.map((entry) => ({ entryId: `${entry.projectId}_${entry.skillId}`, ...entry }));
    currentSearch.value = selectedInternal.value.name
  } else {
    clearValue();
  }
};

const setOptionsInternal = () => {
  if (props.options) {
    optionsInternal.value = props.options.map((entry) => ({entryId: `${entry.projectId}_${entry.skillId}`, ...entry}));
    if (props.selected && props.selected.length) {
      // removed already selected items
      optionsInternal.value = optionsInternal.value.filter((el) => !props.selected?.some((sel) => `${sel.projectId}_${sel.skillId}` === el.entryId));
    }
    if (optionsInternal.value && optionsInternal.value.length === 0) {
      announcer.polite('No results found. Consider changing the search query');
    }
  }
};

const added = (addedItem) => {
  selectedInternal.value = addedItem?.value;
  emit('added', addedItem?.value);
};

const searchChanged = (event) => {
  currentSearch.value = event?.query || '';
  emit('search-change', currentSearch.value);
  if (props.internalSearch) {
    if(currentSearch.value) {
      optionsInternal.value = props.options.filter((item) => {
        return item.name.toLowerCase().includes(currentSearch.value.toLowerCase());
      })
    }
    else {
      optionsInternal.value = props.options.map((entry) => ({ name: entry.name, ...entry }));
    }
  }
};

const isString = (variable) => typeof variable === 'string';
const isObject = (variable) => typeof variable === 'object' && variable !== null;

const handleBlur = () => {
  if (currentSearch.value && selectedInternal.value) {
    if (isString(currentSearch.value) && currentSearch.value !== selectedInternal.value.name) {
      currentSearch.value = selectedInternal.value.name;
    } else if (isObject(currentSearch.value) && currentSearch.value.name !== selectedInternal.value.name) {
      currentSearch.value = selectedInternal.value.name;
    }
  }
}

const clearValue = () => {
  selectedInternal.value = null
  currentSearch.value = ''
}
const focus = () => {
  const skillsInput = document.querySelector('[data-cy="skillsSelector"] button')
  if (skillsInput) {
    skillsInput.focus()
  }
}

onMounted(() => {
  setSelectedInternal();
  setOptionsInternal();
  isMounted.value = true;
})

defineExpose({
  clearValue,
  focus
})
</script>

<template>
  <!--  <Fluid>-->
    <div class="p-fluid">
      <AutoComplete
          v-model="currentSearch"
          :suggestions="internalSearch ? optionsInternal : options"
          :placeholder="placeholder"
          class="st-skills-selector"
          :class="props.class"
          :fluid="true"
          :dropdown="showDropdown"
          data-cy="skillsSelector"
          :disabled="disabled"
          :loading="isLoading"
          @complete="searchChanged"
          @option-select="added"
          @clear="added"
          @blur="handleBlur"
          optionLabel="name"
          :completeOnFocus="true"
          :delay="500">

        <template #option="slotProps">
          <slot name="dropdown-item" :option="slotProps">
            <div :data-cy="`skillsSelectionItem-${slotProps.option.projectId}-${slotProps.option.skillId}`">
              <div class="text-xl text-info skills-option-name" data-cy="skillsSelector-skillName"><span
                  v-if="showType">{{
                  slotProps.option.type
                }}:</span> {{ slotProps.option.name }}
                <Tag v-if="slotProps.option.isReused" variant="success" size="sm" class="uppercase"
                     data-cy="reusedBadge"
                     style="font-size: 0.85rem !important;"><i class="fas fa-recycle"></i> reused
                </Tag>
              </div>
              <div style="font-size: 0.8rem;">
              <span class="skills-option-id">
                <span v-if="showProject" data-cy="skillsSelectionItem-projectId"><span
                    class="uppercase mr-1 italic">Project ID:</span><span
                    class="font-bold"
                    data-cy="skillsSelector-projectId">{{ slotProps.option.projectId }}</span></span>
                <span v-if="!showProject" data-cy="skillsSelectionItem-skillId">
                  <span class="uppercase mr-1 italic">ID:</span>
                  <span class="font-bold" data-cy="skillsSelector-skillId">
                  {{ removeReuseTag(slotProps.option.skillId) }}
                  </span>
                </span>
              </span>
                <span class="mx-2" v-if="slotProps.option.type !== 'Badge'">|</span>
                <span v-if="slotProps.option.type === 'Skill'" class="uppercase mr-1 italic"
                      data-cy="skillsSelectionItem-subjectId">Subject:</span>
                <span v-if="slotProps.option.type === 'Skill'"
                      class="font-bold skills-option-subject-name"
                      data-cy="skillsSelector-subjectName">{{ slotProps.option.subjectName }}</span>
                <span v-if="slotProps.option.type === 'Shared Skill'" class="uppercase mr-1 italic"
                      data-cy="skillsSelectionItem-projectName">Project:</span>
                <span v-if="slotProps.option.type === 'Shared Skill'"
                      class="font-bold skills-option-subject-name"
                      data-cy="skillsSelector-projectName">{{ slotProps.option.projectName }}</span>
                <span v-if="slotProps.option.groupName">
                <span class="mx-2">|</span>
                <span class="uppercase mr-1 italic skills-option-group-name" data-cy="skillsSelectionItem-group">Group:</span><span
                    class="font-bold skills-id"
                    data-cy="skillsSelector-groupName">{{ slotProps.option.groupName }}</span>
              </span>
              </div>
            </div>
          </slot>
        </template>
        <template #footer v-if="afterListSlotText">
          <li>
            <div class="h6 ml-1"> {{ afterListSlotText }}</div>
          </li>
        </template>
        <template #empty>
          <span v-if="emptyWithoutSearch && !internalSearch && !currentSearch" class="px-4"><i class="fas fa-search"/> Type to <span
            class="font-bold">search</span> for skills...</span>
          <span class="px-4" v-else>No results found. Consider changing the search query</span>
        </template>
      </AutoComplete>
    </div>
  <!--  </Fluid>-->
</template>

<style>
.st-skills-selector .p-placeholder {
  text-wrap: wrap;
}
</style>