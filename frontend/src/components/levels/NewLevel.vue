<template>
  <b-modal :id="levelId" size="xl" :title="title" v-model="show" :no-close-on-backdrop="true"
           header-bg-variant="info" header-text-variant="light" no-fade >
    <b-container fluid>
      <div v-if="displayIconManager === false">
        <div class="media">
          <icon-picker :startIcon="levelInternal.iconClass" @select-icon="toggleIconDisplay(true)"
                       class="mr-3"></icon-picker>
          <div class="media-body">
            <template v-if="isEdit">
              <label for="editLevel-level">Level</label>
              <b-form-input v-focus id="editLevel-level" v-model="levelInternal.level" name="level" :disabled="isEdit"
                            v-validate="'optionalNumeric|required|min_value:0|max_value:100'" data-vv-delay="500"></b-form-input>
              <small class="form-text text-danger" v-show="errors.has('level')">{{ errors.first('level')}}</small>

              <template v-if="!this.levelAsPoints">
                <label for="editLevel-percent" class="mt-3">Percent</label>
                <b-form-input id="editLevel-percent" v-model="levelInternal.percent" name="percent" v-validate="'optionalNumeric|required|min_value:0|max_value:100|overlap'" data-vv-delay="500"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('percent')">{{ errors.first('percent')}}</small>
              </template>
              <template v-else>
                <label for="editLevel-pointsFrom" class="mt-3">Points From</label>
                <b-form-input id="editlevel-pointsFrom" v-model="levelInternal.pointsFrom" name="pointsFrom" v-validate="'optionalNumeric|required|min_value:0|overlap'" data-vv-delay="500"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('pointsFrom')">{{ errors.first('pointsFrom')}}</small>

                <div v-if="!levelInternal.isLast">
                  <label for="editLevel-pointsTo" class="mt-3">Points To</label>
                  <b-form-input id="editLevel-pointsTo" v-model="levelInternal.pointsTo" name="pointsTo" v-validate="'optionalNumeric|required|min_value:0|overlap'" data-vv-delay="500"></b-form-input>
                  <small class="form-text text-danger" v-show="errors.has('pointsTo')">{{ errors.first('pointsTo')}}</small>
                </div>
              </template>

              <label for="editLevel-name" class="mt-3">Name <span class="text-muted">(optional)</span></label>
              <b-form-input id="editLevel-name" v-model="levelInternal.name" name="name" v-validate="'max:50|uniqueName'" data-vv-delay="500"></b-form-input>
              <small class="form-text text-danger" v-show="errors.has('name')">{{ errors.first('name')}}</small>
            </template>
            <template v-else>
              <template v-if="!this.levelAsPoints">
                <label for="newLevel-percent">Percent %</label>
                <b-form-input v-focus id="newLevel-percent" v-model="levelInternal.percent" name="percent"
                              v-validate="'optionalNumeric|required|min_value:0|max_value:100|overlap'" data-vv-delay="500"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('percent')">{{ errors.first('percent')}}</small>
              </template>
              <template v-else>
                <label for="newLevel-points" class="mt-3">Points</label>
                <b-form-input id="newlevel-points" v-model="levelInternal.points" name="points" v-validate="'optionalNumeric|required|min_value:0|overlap'" data-vv-delay="500"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('points')">{{ errors.first('points')}}</small>
              </template>
              <label for="newLevel-name" class="mt-3">Name <span class="text-muted">(optional)</span></label>
              <b-form-input id="newLevel-name" v-model="levelInternal.name" name="name" v-validate="'max:50|uniqueName'" data-vv-delay="500"></b-form-input>
              <small class="form-text text-danger" v-show="errors.has('name')">{{ errors.first('name')}}</small>
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
        <b-button variant="success" size="sm" class="float-right" @click="saveLevel" v-skills="'AddOrModifyLevels'">
          Save
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe">
          Cancel
        </b-button>.
      </div>
    </div>
  </b-modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import IconManager from '../utils/iconPicker/IconManager';

  export default {
    name: 'NewLevel',
    components: { IconPicker, IconManager },
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
        levelInternal: Object.assign({}, this.level),
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
      const dictionary = {
        en: {
          attributes: {
            percent: 'Percent',
            points: 'Points',
            pointsFrom: 'Points From',
            pointsTo: 'Points To',
            name: 'Name',
            level: 'Level',
          },
        },
      };

      const gte = (value, compareTo) => value >= compareTo;
      const lte = (value, compareTo) => value <= compareTo;
      const gt = (value, compareTo) => value > compareTo;
      const lt = (value, compareTo) => value < compareTo;

      Validator.extend('overlap', {
        getMessage: 'Value must not overlap with other levels',
        validate(value) {
          let valid = true;
          if (self.boundaries) {
            let previousValid = true;
            let nextValid = true;
            const gtOp = self.levelAsPoints ? gte : gt;
            const ltOp = self.levelAsPoints ? lte : lt;

            if (self.boundaries.previous !== null) {
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

      Validator.extend('uniqueName', {
        getMessage: field => `${field} is already taken.`,
        validate(value) {
          let valid = true;
          if (self.allLevels && value !== self.level.name) {
            const lcVal = value.toLowerCase();
            const existingLevelWithName = self.allLevels.find(elem => elem.name.toLowerCase() === lcVal);
            if (existingLevelWithName) {
              valid = false;
            }
          }
          return valid;
        },
      });

      Validator.localize(dictionary);
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
      closeMe() {
        this.show = false;
      },
      saveLevel() {
        this.$validator.validateAll().then((res) => {
          if (res) {
            if (this.isEdit === true) {
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
            this.closeMe();
          }
        });
      },
      toggleIconDisplay(shouldDisplay) {
        this.displayIconManager = shouldDisplay;
      },
      onSelectedIcon(selectedIcon) {
        this.levelInternal.iconClass = `${selectedIcon.css}`;
        this.displayIconManager = false;
      },
    },
  };
</script>

<style scoped>
  .level-title{
    padding-left: 1rem;
  }
</style>
