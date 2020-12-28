/*
Copyright 2020 SkillTree

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
<template>
  <ValidationObserver ref="observer" v-slot="{ invalid, handleSubmit }" slim>
    <b-modal :id="levelId" size="xl" :title="title" v-model="show" :no-close-on-backdrop="true"
             header-bg-variant="info" header-text-variant="light" no-fade
             @hide="publishHidden">
      <b-container fluid>
        <div v-if="displayIconManager === false">
          <div class="media">
            <icon-picker :startIcon="levelInternal.iconClass" @select-icon="toggleIconDisplay(true)"
                         class="mr-3"></icon-picker>
            <div class="media-body">
                <template v-if="isEdit">
                  <label for="editLevel-level">* Level</label>
                  <ValidationProvider name="Level" :debounce=500 v-slot="{errors}" rules="optionalNumeric|required|min_value:0|max_value:100">
                    <b-form-input v-focus id="editLevel-level" v-model="levelInternal.level" name="level" :disabled="isEdit"
                    aria-required="true" data-cy="levelId" v-on:keyup.enter="handleSubmit(saveLevel)"
                                  :aria-invalid="errors && errors.length > 0"
                                  aria-errormessage="levelIdError"
                                  aria-describedby="levelIdError"></b-form-input>
                    <small class="form-text text-danger" v-show="errors[0]" data-cy="levelIdError" id="levelIdError">{{ errors[0] }}</small>
                  </ValidationProvider>
                  <template v-if="!levelAsPoints">
                    <label for="editLevel-percent" class="mt-3">* Percent</label>
                    <ValidationProvider name="Percent" :debounce=500 v-slot="{errors}" rules="optionalNumeric|required|min_value:0|max_value:100|overlap">
                      <b-form-input id="editLevel-percent" v-model="levelInternal.percent" name="percent" aria-required="true" data-cy="levelPercent"
                                    v-on:keyup.enter="handleSubmit(saveLevel)"
                                    :aria-invalid="errors && errors.length > 0"
                                    aria-errormessage="levelPercentError"
                                    aria-describedby="levelPercentError">
                      </b-form-input>
                      <small class="form-text text-danger" v-show="errors[0]" data-cy="levelPercentError" id="levelPercentError">{{ errors[0] }}</small>
                    </ValidationProvider>
                  </template>
                  <template v-else>
                    <label for="editLevel-pointsFrom" class="mt-3">* Points From</label>
                    <ValidationProvider name="Points From" :debounce=500 v-slot="{errors}" rules="optionalNumeric|required|min_value:0|overlap">
                      <b-form-input id="editlevel-pointsFrom" v-model="levelInternal.pointsFrom" name="pointsFrom" aria-required="true"
                                    v-on:keyup.enter="handleSubmit(saveLevel)" data-cy="editLevelPoints"
                                    :aria-invalid="errors && errors.length > 0"
                                    aria-errormessage="levelPointsFromError" aria-describedby="levelPointsFromError"></b-form-input>
                      <small class="form-text text-danger" v-show="errors[0]" id="levelPointsFromError">{{ errors[0] }}</small>
                    </ValidationProvider>
                    <div v-if="!levelInternal.isLast">
                      <label for="editLevel-pointsTo" class="mt-3">* Points To</label>
                      <ValidationProvider name="Points To" :debounce=500 v-slot="{errors}" rules="optionalNumeric|required|min_value:0|overlap">
                        <b-form-input id="editLevel-pointsTo" v-model="levelInternal.pointsTo" name="pointsTo" aria-required="true"
                                      v-on:keyup.enter="handleSubmit(saveLevel)"
                                      :aria-invalid="errors && errors.length > 0"
                                      aria-errormessage="levelPointsToError" aria-describedby="levelPointsToError"></b-form-input>
                        <small class="form-text text-danger" v-show="errors[0]" id="levelPointsToError">{{ errors[0] }}</small>
                      </ValidationProvider>
                    </div>
                  </template>

                  <label for="editLevel-name" class="mt-3">Name <span class="text-muted">(optional)</span></label>
                  <ValidationProvider name="Name" :debounce=500 v-slot="{errors}" rules="maxLevelNameLength|uniqueName">
                    <b-form-input id="editLevel-name" v-model="levelInternal.name" name="name" data-cy="levelName"
                                  v-on:keyup.enter="handleSubmit(saveLevel)"
                                  :aria-invalid="errors && errors.length > 0"
                                  aria-errormessage="levelNameError" aria-describedby="levelNameError"></b-form-input>
                    <small class="form-text text-danger" v-show="errors[0]" data-cy="levelNameError" id="levelNameError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </template>
                <template v-else>
                  <template v-if="!levelAsPoints">
                    <label for="newLevel-percent">* Percent %</label>
                    <ValidationProvider name="Percent %" :debounce=500 v-slot="{errors}" rules="optionalNumeric|required|min_value:0|max_value:100|overlap">
                      <b-form-input v-focus id="newLevel-percent" v-model="levelInternal.percent"
                                    name="percent" aria-required="true" data-cy="levelPercent"
                                    v-on:keyup.enter="handleSubmit(saveLevel)"
                                    :aria-invalid="errors && errors.length > 0"
                                    aria-errormessage="levelPercentError" aria-describedby="levelPercentError"></b-form-input>
                      <small class="form-text text-danger" v-show="errors[0]" data-cy="levelPercentError" id="levelPercentError">{{ errors[0] }}</small>
                    </ValidationProvider>
                  </template>
                  <template v-else>
                    <label for="newLevel-points" class="mt-3">* Points</label>
                    <ValidationProvider name="Points" :debounce=500 v-slot="{errors}" rules="optionalNumeric|required|min_value:0|overlap">
                      <b-form-input id="newlevel-points" v-model="levelInternal.points" name="points" aria-required="true"
                                    v-on:keyup.enter="handleSubmit(saveLevel)" data-cy="newLevelPoints"
                                    :aria-invalid="errors && errors.length > 0"
                                    aria-errormessage="levelPointsError" aria-describedby="levelPointsError"></b-form-input>
                      <small class="form-text text-danger" v-show="errors[0]" id="levelPointsError">{{ errors[0] }}</small>
                    </ValidationProvider>
                  </template>
                  <label for="newLevel-name" class="mt-3">Name <span class="text-muted">(optional)</span></label>
                  <ValidationProvider name="Name" :debounce=500 v-slot="{errors}" rules="maxLevelNameLength|uniqueName">
                    <b-form-input id="newLevel-name" v-model="levelInternal.name" name="name" data-cy="levelName"
                                  v-on:keyup.enter="handleSubmit(saveLevel)"
                                  :aria-invalid="errors && errors.length > 0"
                                  aria-errormessage="levelNameError" aria-describedby="levelNameError"></b-form-input>
                    <small class="form-text text-danger" v-show="errors[0]" data-cy="levelNameError" id="levelNameError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </template>
            </div>
          </div>
        </div>
        <div v-else>
          <icon-manager @selected-icon="onSelectedIcon"></icon-manager>
          <div class="text-right mr-2">
            <b-button variant="secondary" @click="toggleIconDisplay(false)" class="mt-4">Cancel Icon Selection</b-button>
          </div>
        </div>
      </b-container>
      <div slot="modal-footer" class="w-100">
        <div v-if="displayIconManager === false">
          <b-button variant="success"
                    size="sm"
                    class="float-right"
                    @click="handleSubmit(saveLevel)"
                    :disabled="invalid"
                    v-skills="'AddOrModifyLevels'"
                    data-cy="saveLevelButton">
            Save
          </b-button>
          <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe" data-cy="cancelLevel">
            Cancel
          </b-button>
        </div>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import { extend } from 'vee-validate';
  // eslint-disable-next-line camelcase
  import { min_value, max_value } from 'vee-validate/dist/rules';
  import IconPicker from '../utils/iconPicker/IconPicker';
  // import IconManager from '../utils/iconPicker/IconManager';
  import InputSanitizer from '../utils/InputSanitizer';

  extend('min_value', {
    // eslint-disable-next-line camelcase
    ...min_value,
    message: (fieldname, placeholders) => `${fieldname} must be ${placeholders.min} or greater`,
  });
  extend('max_value', {
    // eslint-disable-next-line camelcase
    ...max_value,
    message: (fieldname, placeholders) => `${fieldname} must be ${placeholders.max} or less`,
  });

  export default {
    name: 'NewLevel',
    components: {
      IconPicker,
      'icon-manager': () => import(/* webpackChunkName: 'iconManager' */'../utils/iconPicker/IconManager'),
    },
    props: {
      levelAsPoints: Boolean,
      iconClass: String,
      level: Object,
      boundaries: Object,
      isEdit: Boolean,
      value: Boolean,
      allLevels: Array,
    },
    data() {
      return {
        levelInternal: { ...this.level },
        displayIconManager: false,
        show: this.value,
      };
    },
    mounted() {
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    created() {
      const self = this;

      const gte = (value, compareTo) => value >= compareTo;
      const lte = (value, compareTo) => value <= compareTo;
      const gt = (value, compareTo) => value > compareTo;
      const lt = (value, compareTo) => value < compareTo;

      extend('overlap', {
        message: '{_field_} must not overlap with other levels',
        validate(value) {
          let valid = true;
          if (self.boundaries) {
            let previousValid = true;
            let nextValid = true;
            let gtOp = self.levelAsPoints ? gte : gt;
            const ltOp = self.levelAsPoints ? lte : lt;

            if (self.boundaries.previous !== null) {
              if (self.boundaries.next === null) {
                // use gt regardless of points configuration if it's the last level
                gtOp = gt;
              }
              previousValid = gtOp(value, self.boundaries.previous);
            }
            if (self.boundaries.next !== null) {
              nextValid = ltOp(value, self.boundaries.next);
            }

            valid = nextValid && previousValid;
          }
          return valid;
        },
      });

      extend('uniqueName', {
        message: (field) => `${field} is already taken.`,
        validate(value) {
          let valid = true;
          if (self.allLevels && value && value.localeCompare(self.level.name, 'en', { sensitivity: 'base' }) !== 0) {
            const lcVal = value.toLowerCase();
            const existingLevelWithName = self.allLevels.find((elem) => elem.name && elem.name.toLowerCase() === lcVal);
            if (existingLevelWithName) {
              valid = false;
            }
          }
          return valid;
        },
      });
    },
    computed: {
      title() {
        return this.isEdit ? 'Edit Level' : 'New Level';
      },
      levelId() {
        return this.level.level ? `level-${this.level.level}` : 'newLevel';
      },
    },
    methods: {
      closeMe(e) {
        this.show = false;
        this.publishHidden(e);
      },
      saveLevel() {
        this.levelInternal.name = InputSanitizer.sanitize(this.levelInternal.name);
        const closeArg = {};
        if (this.isEdit === true) {
          closeArg.saved = true;
          this.$emit('edited-level', {
            percent: this.levelInternal.percent,
            pointsFrom: this.levelInternal.pointsFrom,
            pointsTo: this.levelInternal.pointsTo,
            name: this.levelInternal.name,
            iconClass: this.levelInternal.iconClass,
            id: this.levelInternal.level,
            level: this.levelInternal.level,
          });
        } else {
          this.$emit('new-level', {
            percent: this.levelInternal.percent,
            points: this.levelInternal.points,
            name: this.levelInternal.name,
            iconClass: this.levelInternal.iconClass,
          });
        }
        this.closeMe(closeArg);
      },
      toggleIconDisplay(shouldDisplay) {
        this.displayIconManager = shouldDisplay;
      },
      onSelectedIcon(selectedIcon) {
        this.levelInternal.iconClass = `${selectedIcon.css}`;
        this.displayIconManager = false;
      },
      publishHidden(e) {
        this.$emit('hidden', { edit: this.isEdit, ...e });
      },
    },
  };
</script>

<style scoped>
  .level-title{
    padding-left: 1rem;
  }
</style>
