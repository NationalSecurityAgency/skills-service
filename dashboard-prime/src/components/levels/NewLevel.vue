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
import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import SkillsNumberInput from '@/components/utils/inputForm/SkillsNumberInput.vue'
import {number} from "yup";
import LevelService from "@/components/levels/LevelService.js";
import {nextTick} from "vue";
import { useRoute } from 'vue-router';

const route = useRoute();
const props = defineProps({
  levelAsPoints: Boolean,
  level: Object,
  boundaries: Object,
  isEdit: Boolean,
  value: Boolean,
  allLevels: Array,
});

const emit = defineEmits(['load-levels']);

const model = defineModel()
const isDisabled = false;
const saveLevel = (values) => {
  if (props.isEdit === true) {
    return doEditLevel({
      percent: values.percent,
      pointsFrom: values.pointsFrom,
      pointsTo: values.pointsTo,
      id: values.level,
      level: values.level,
    });
  } else {
    return doCreateNewLevel({
      percent: values.percent,
      points: values.points,
    });
  }
};

const doCreateNewLevel = (nextLevelObj) => {
  if (route.params.subjectId) {
    return LevelService.createNewLevelForSubject(route.params.projectId, route.params.subjectId, nextLevelObj)
  } else {
    return LevelService.createNewLevelForProject(route.params.projectId, nextLevelObj)
  }
};

const doEditLevel = (editedLevelObj) => {
  if (route.params.subjectId) {
    return LevelService.editLevelForSubject(route.params.projectId, route.params.subjectId, editedLevelObj)
  } else {
    return LevelService.editLevelForProject(route.params.projectId, editedLevelObj)
  }
};

const saved = () => {
  const msg = props.isEdit ? `Level ${props.level} has been saved` : 'New Level has been created';
  // announcer.polite(msg);
  emit('load-levels');
  close();
}
const close = () => {
  model.value = false
}
let formId = props.isEdit ? 'editLevelDialog' : 'newLevelDialog';
const modalTitle = props.isEdit ? 'Edit Level' : 'New Level';

const initialLevelData = {
  level: props.level.level,
  percent: props.level.percent,
  pointsFrom: props.level.pointsFrom,
  pointsTo: props.level.pointsTo,
  points: props.level.points,
};

if (!props.isEdit) {
  initialLevelData.percent = props.allLevels.reduce((max, level) => Math.max(max, level.percent), 0) +1;
}

const boundsValidator = (value) => {
  const gte = (value, compareTo) => value >= compareTo;
  const lte = (value, compareTo) => value <= compareTo;
  const gt = (value, compareTo) => value > compareTo;
  const lt = (value, compareTo) => value < compareTo;

  let valid = true;
  if (props.boundaries) {
    let previousValid = true;
    let nextValid = true;
    let gtOp = props.levelAsPoints ? gte : gt;
    const ltOp = props.levelAsPoints ? lte : lt;

    if (props.boundaries.previous !== null) {
      if (props.boundaries.next === null) {
        // use gt regardless of points configuration if it's the last level
        gtOp = gt;
      }
      previousValid = gtOp(value, props.boundaries.previous);
    }
    if (props.boundaries.next !== null) {
      nextValid = ltOp(value, props.boundaries.next);
    }

    valid = nextValid && previousValid;
  }
  return valid;
}

// levelAsPoints

let schema = {};

if (props.isEdit) {
  schema = {
    'level': number().required().min(1).label('Level'),
  }
  if (props.levelAsPoints) {
    schema = {
      ...schema,
      'pointsFrom': number().required().min(0).test('overlap', ({ label }) => `${label} must not overlap with other levels`, boundsValidator).label('Points From'),
      'pointsTo': number().required().min(0).test('overlap', ({ label }) => `${label} must not overlap with other levels`, boundsValidator).label('Points To'),
    }
  } else {
    schema = {
      ...schema,
      'percent': number().required().min(0).max(100).label('Percent').test('overlap', ({ label }) => `${label} must not overlap with other levels`, boundsValidator),
    }
  }
} else {
  if (props.levelAsPoints) {
    schema = {
      ...schema,
      'points': number().required().min(0).test('overlap', ({ label }) => `${label} must not overlap with other levels`, boundsValidator).label('Points'),
    }
  } else {
    schema = {
      ...schema,
      'percent': number().required().min(0).max(100).label('Percent').test('overlap', ({ label }) => `${label} must not overlap with other levels`, boundsValidator),
    }
  }
}

</script>

<template>
  <SkillsInputFormDialog
      :id="formId"
      v-model="model"
      :header="modalTitle"
      saveButtonLabel="Save"
      :enable-return-focus="true"
      @saved="saved"
      @close="close"
      :validation-schema="schema"
      :save-data-function="saveLevel"
      :initial-values="initialLevelData"
      :style="{ width: '40rem !important' }">

    <template #default>
      <template v-if="isEdit">
        <div class="w-full">
          <SkillsNumberInput isRequired :min="1" label="Level" name="level" disabled />
        </div>
        <template v-if="!levelAsPoints">
          <div class="w-full">
            <SkillsNumberInput showButtons isRequired :min="0" :max="100" suffix="%" label="Percent" name="percent" />
          </div>
        </template>
        <template v-else>
          <div class="w-full">
            <SkillsNumberInput showButtons isRequired :min="0" label="Points From" name="pointsFrom" />
          </div>
          <div class="w-full">
            <SkillsNumberInput showButtons isRequired :min="0" label="Points To" name="pointsTo" />
          </div>
        </template>
      </template>
      <template v-else>
        <template v-if="!levelAsPoints">
          <div class="w-full">
            <SkillsNumberInput showButtons isRequired :min="0" :max="100" suffix="%" label="Percent" name="percent" />
          </div>
        </template>
        <template v-else>
          <div class="w-full">
            <SkillsNumberInput showButtons isRequired :min="0" data-cy="pointsInput" label="Points" name="points" />
          </div>
        </template>
      </template>
    </template>
  </SkillsInputFormDialog>
</template>