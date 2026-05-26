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
import SkillType from "@/common-components/utilities/SkillType.js";

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
  appendTo: {
    type: String,
    default: 'body',
    required: false,
  },
  showIcon: {
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

const getIconClass = (skill) => {
  const { type } = skill
  if (SkillType.isSubject(type)) {
    return 'fa-solid fa-cubes skills-color-subjects text-slate-500'
  }
  if (SkillType.isBadge(type)) {
    return 'fas fa-award skills-color-badges text-indigo-500'
  }
  if (SkillType.isSkillsGroup(type)) {
    return 'fa-solid fa-layer-group text-purple-500'
  }
  return 'fas fa-graduation-cap skills-color-skills text-sky-500'
}

const subTypesToShow = (item) => {
  const res = []

  if (props.showType) {
    res.push({
      label: 'Type',
      value: SkillType.isSkillsGroup(item.type) ? 'Skills Group' : item.type,
      dataCy: 'skillsSelector-type'
    })
  }

  if (props.showProject) {
    res.push({
      label: 'Project ID',
      value: item.projectId,
      dataCy: 'skillsSelector-projectId'
    })
  }

  if (SkillType.isSkill(item.type) || SkillType.isSkillsGroup(item.type)) {
    res.push({
      label: 'Subject',
      value: item.subjectName,
      dataCy: 'skillsSelector-subjectName'
    })
  }

  if (item.type === 'Shared Skill') {
    res.push({
      label: 'Project',
      value: item.projectName,
      dataCy: 'skillsSelector-projectName'
    })
  }

  if (item.groupName) {
    res.push({
      label: 'Group',
      value: item.groupName,
      dataCy: 'skillsSelector-groupName'
    })
  }

  return res
}
</script>

<template>
  <!--  <Fluid>-->
    <div class="p-fluid">
      <AutoComplete
          v-model="currentSearch"
          :suggestions="internalSearch ? optionsInternal : options"
          :placeholder="placeholder"
          class="st-skills-selector"
          :pt="{ dropdown: { 'aria-label': 'click to select a skill' } }"
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
          :appendTo="appendTo"
          :delay="500">

        <template #option="slotProps">
          <slot name="dropdown-item" :option="slotProps">
            <div class="flex gap-2 items-center w-full">
              <div v-if="showIcon" class="border rounded p-2 min-w-6 text-primary">
                <i class="text-2xl" :class="getIconClass(slotProps.option)" aria-hidden="true"/>
              </div>
              <div :data-cy="`skillsSelectionItem-${slotProps.option.projectId}-${slotProps.option.skillId}`">
                <div class="text-xl text-info skills-option-name flex gap-2 items-center">
                  <div data-cy="skillsSelector-skillName" class="text-primary">{{ slotProps.option.name }}</div>
                  <Tag v-if="slotProps.option.isReused" aria-label="Reused" class="text-sm" data-cy="reusedBadge">
                    <i class="fas fa-recycle" aria-hidden="true"></i><span class="uppercase">reused</span>
                  </Tag>
                </div>
                <div class="text-sm flex gap-2 items-center">
                  <div v-for="(subType, index) in subTypesToShow(slotProps.option)" :key="subType.dataCy" class="flex gap-1 items-center">
                    <div v-if="index > 0" class="text-gray-500 mx-1">|</div>
                    <div class="italic">{{ subType.label }}:</div>
                    <div class="font-bold" :data-cy="subType.dataCy">{{ subType.value }}</div>
                  </div>
                </div>
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