<template>
  <b-modal :id="badgeInternal.badgeId" size="xl" :title="title" v-model="show"
           header-bg-variant="info" header-text-variant="light" no-fade >
    <b-container fluid>
      <div v-if="displayIconManager === false" class="text-left">
        <div class="media">
          <icon-picker :startIcon="badgeInternal.iconClass" @select-icon="toggleIconDisplay(true)" class="mr-3"></icon-picker>
          <div class="media-body">
            <div class="form-group">
              <label for="badgeName">Badge Name</label>
              <input class="form-control" id="badgeName" type="text" v-model="badgeInternal.name"
                     @input="updateBadgeId"
                     v-validate="'required|min:3|max:50'" data-vv-delay="500" data-vv-name="badgeName" v-focus/>
              <small class="form-text text-danger" v-show="errors.has('badgeName')">{{ errors.first('badgeName')}}
              </small>
            </div>
          </div>
        </div>

        <id-input type="text" label="Badge ID" v-model="badgeInternal.badgeId" @input="canAutoGenerateId=false"
                  v-validate="'required|min:3|max:50|alpha_num'" data-vv-name="badgeId"/>
        <small class="form-text text-danger">{{ errors.first('badgeId')}}</small>

        <div class="mt-2">
          <label>Description</label>
          <markdown-editor :value="badge.description" @input="updateDescription"></markdown-editor>
        </div>

        <b-form-checkbox v-model="limitTimeframe" class="mt-4"
                         @change="onEnableGemFeature">
            Enable Gem Feature <inline-help msg="The Gem feature allows for the badge to only be achievable during the specified time frame."/>
        </b-form-checkbox>


        <b-collapse id="gemCollapse" v-model="limitTimeframe">
            <b-row v-if="limitTimeframe" no-gutters class="justify-content-md-center mt-3">
              <b-col cols="12" md="4" style="min-width: 20rem;">
                <label class="label mt-2">Start Date</label>
                <datepicker :inline="true" v-model="badgeInternal.startDate" name="startDate"
                            v-validate="'required|dateOrder'"></datepicker>
                <small class="form-text text-danger" v-show="errors.has('startDate')">{{ errors.first('startDate')}}
                </small>
              </b-col>
              <b-col cols="12" md="4"  style="min-width: 20rem;">
                <label class="label mt-2">End Date</label>
                <datepicker :inline="true" v-model="badgeInternal.endDate" name="endDate"
                            v-validate="'required|dateOrder'"></datepicker>
                <small class="form-text text-danger" v-show="errors.has('endDate')">{{ errors.first('endDate')}}</small>
              </b-col>
            </b-row>
        </b-collapse>
        <p v-if="overallErrMsg" class="text-center text-danger mt-3">***{{ overallErrMsg }}***</p>
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
        <b-button variant="success" size="sm" class="float-right" @click="updateBadge">
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
  import Datepicker from 'vuejs-datepicker';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import IconManager from '../utils/iconPicker/IconManager';
  import IdInput from '../utils/inputForm/IdInput';
  import InlineHelp from '../utils/InlineHelp';

  let self;
  const dictionary = {
    en: {
      attributes: {
        badgeName: 'Badge Name',
        badgeId: 'Badge ID',
        requiredSkills: 'Required Skills',
        startDate: 'Start Date',
        endDate: 'End Date',
      },
    },
  };
  Validator.localize(dictionary);
  Validator.extend('dateOrder', {
    getMessage: 'Start Date must come before End Date',
    validate() {
      let valid = true;
      if (self.limitTimeframe) {
        if (self.badgeInternal.startDate && self.badgeInternal.endDate) {
          valid = self.badgeInternal.startDate < self.badgeInternal.endDate;
          if (valid) {
            // manually clear errors in case the orig error occurred when setting startDate,
            // but was fixed by updating endDate (or vise-versa)
            self.errors.remove('startDate');
            self.errors.remove('endDate');
          }
        }
      }
      return valid;
    },
  }, {
    immediate: false,
  });

  export default {
    name: 'EditBadge',
    components: {
      InlineHelp,
      IconPicker,
      MarkdownEditor,
      Datepicker,
      IconManager,
      IdInput,
    },
    props: {
      badge: Object,
      isEdit: Boolean,
      value: Boolean,
    },
    data() {
      // convert string to Date objects
      this.badge.startDate = this.toDate(this.badge.startDate);
      this.badge.endDate = this.toDate(this.badge.endDate);

      const timeframe = !!(this.badge.startDate && this.badge.endDate);
      return {
        canAutoGenerateId: true,
        canEditBadgeId: false,
        limitTimeframe: timeframe,
        badgeInternal: Object.assign({}, this.badge),
        overallErrMsg: '',
        show: this.value,
        displayIconManager: false,
      };
    },
    mounted() {
      self = this;
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Badge' : 'New Badge';
      },
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      closeMe() {
        this.show = false;
      },
      updateDescription(event) {
        this.badgeInternal.description = event;
      },
      updateBadge() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.show = false;
            this.$emit('badge-updated', this.badgeInternal);
          }
        });
      },
      updateBadgeId() {
        if (!this.isEdit && this.canAutoGenerateId) {
          this.badgeInternal.badgeId = this.badgeInternal.name.replace(/[^\w]/gi, '');
        }
      },
      onSelectedIcon(selectedIcon) {
        this.badgeInternal.iconClass = `${selectedIcon.css}`;
        this.displayIconManager = false;
      },
      onEnableGemFeature() {
        if (!this.limitTimeframe) {
          this.badgeInternal.startDate = null;
          this.badgeInternal.endDate = null;
        }
      },
      toDate(value) {
        let dateVal = value;
        if (value && !(value instanceof Date)) {
          dateVal = new Date(Date.parse(value));
        }
        return dateVal;
      },
      toggleEditId() {
        this.canEditBadgeId = !this.canEditBadgeId && !this.isEdit;
        this.updateBadgeId();
      },
      toggleIconDisplay(shouldDisplay) {
        this.displayIconManager = shouldDisplay;
      },
    },
  };

</script>

<style scoped>
</style>
