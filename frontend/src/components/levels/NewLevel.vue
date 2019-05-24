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
              <b-form-input id="editLevel-level" v-model="levelInternal.level" name="level" v-validate="'required|min:0|max:100|numeric'"></b-form-input>
              <small class="form-text text-danger" v-show="errors.has('level')">{{ errors.first('level')}}</small>

              <template v-if="!this.levelAsPoints">
                <label for="editLevel-percent">Percent</label>
                <b-form-input id="editLevel-percent" v-model="levelInternal.percent" name="percent" v-validate="'required|min:0|max:100|numeric|overlap'"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('percent')">{{ errors.first('percent')}}</small>
              </template>
              <template v-else>
                <label for="editLevel-pointsFrom">Points From</label>
                <b-form-input id="editlevel-pointsFrom" v-model="levelInternal.pointsFrom" name="pointsFrom" v-validate="'required|min:0|numeric|overlap'"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('pointsFrom')">{{ errors.first('pointsFrom')}}</small>

                <label for="editLevel-pointsTo">Points To</label>
                <b-form-input id="editLevel-pointsTo" v-model="levelInternal.pointsTo" name="pointsTo" v-validate="'required|min:0|numeric|overlap'"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('pointsTo')">{{ errors.first('pointsTo')}}</small>
              </template>

              <label for="editLevel-name">Name</label>
              <b-form-input id="editLevel-name" v-model="levelInternal.name" name="name" v-validate="'max:50'"></b-form-input>
              <small class="form-text text-danger" v-show="errors.has('name')">{{ errors.first('name')}}</small>
            </template>
            <template v-else>
              <template v-if="!this.levelAsPoints">
                <label for="newLevel-percent">Percent</label>
                <b-form-input id="newLevel-percent" v-model="levelInternal.percent" name="percent" v-validate="'required|min:0|max:100|numeric|overlap'"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('percent')">{{ errors.first('percent')}}</small>
              </template>
              <template v-else>
                <label for="newLevel-points">Points</label>
                <b-form-input id="newlevel-points" v-model="levelInternal.points" name="points" v-validate="'required|min:0|numeric|overlap'"></b-form-input>
                <small class="form-text text-danger" v-show="errors.has('points')">{{ errors.first('points')}}</small>
              </template>
              <label for="newLevel-name">Name</label>
              <b-form-input id="newLevel-name" v-model="levelInternal.name" name="name" v-validate="'max:50'"></b-form-input>
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
        <b-button variant="success" size="sm" class="float-right" @click="saveLevel">
          Save
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe">
          Cancel
        </b-button>
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
    },
    data() {
      return {
        levelInternal: this.level,
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

      Validator.extend('overlap', {
        getMessage: 'Value must not overlap with other levels',
        validate(value) {
          let valid = true;
          if (self.boundaries) {
            let previousValid = true;
            let nextValid = true;
            if (self.boundaries.previous !== null) {
              previousValid = value > self.boundaries.previous;
            }
            if (self.boundaries.next !== null) {
              nextValid = value < self.boundaries.next;
            }
            valid = nextValid && previousValid;
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
        return this.level.id ? `level-${this.level.id}` : 'newLevel';
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
                id: this.levelInternal.id,
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
