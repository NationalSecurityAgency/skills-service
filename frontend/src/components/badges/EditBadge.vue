<template>
  <div class="modal-card" style="width: 900px;">
    <header class="modal-card-head">
      <p v-if="isEdit" class="modal-card-title">Editing Existing Badge</p>
      <p v-else class="modal-card-title">New Badge</p>
      <button class="delete" aria-label="close" v-on:click="$parent.close()"></button>
    </header>

    <b-loading :is-full-page="true" :active.sync="isLoadingAvailableBadgeSkills" :can-cancel="false">

    </b-loading>
    <div v-if="isLoadingAvailableBadgeSkills" class="modal-card-body" style="height: 400px;">
    </div>


    <section class="modal-card-body" v-if="!isLoadingAvailableBadgeSkills">
      <div class="field is-horizontal">
        <div class="field-body">
          <div class="field is-narrow">
            <icon-picker :startIcon="badge.iconClass" v-on:on-icon-selected="onSelectedIcons"></icon-picker>
          </div>
          <div class="field">
            <label class="label">Badge Name</label>
            <div class="control">
              <input class="input" type="text" v-model="badgeInternal.name" v-on:input="updateBadgeId"
                     v-validate="'required|min:3|max:50'" data-vv-delay="500" name="badgeName" v-focus/>
            </div>
            <p class="help is-danger" v-show="errors.has('badgeName')">{{ errors.first('badgeName')}}</p>
          </div>
        </div>
      </div>


      <div class="field skills-remove-bottom-margin">
        <label class="label">Badge ID</label>
        <div class="control">
          <input class="input" type="text" v-model="badgeInternal.badgeId" :disabled="!canEditBadgeId"
                 v-validate="'required|min:3|max:50|alpha_num'" data-vv-delay="500" name="badgeId"/>
        </div>
        <p class="help is-danger" v-show="errors.has('badgeId')">{{ errors.first('badgeId')}}</p>
      </div>
      <p class="control has-text-right">
        <b-tooltip label="Enable to override auto-generated ID."
                   position="is-left" animanted="true" type="is-light">
          <span><i class="fas fa-question-circle"></i></span>
        </b-tooltip>
        <span v-on:click="toggleEditId()">
          <a class="is-info" v-bind:class="{'disableControl': isEdit}" v-if="!canEditBadgeId">Enable</a>
          <a class="is-info" v-if="canEditBadgeId">Disable</a>
        </span>
      </p>

      <div class="field">
        <label class="label">Required Skills
          <b-tooltip label="Users must complete all listed skills in order to earn this badge."
                     position="is-right" animanted="true" type="is-light">
            <span><i class="fas fa-question-circle"></i></span>
          </b-tooltip>
        </label>
        <div class="control is-expanded">
          <skills-selector v-model="badgeInternal.requiredSkills" :available-to-select="availableBadgeSkills"
                           v-validate="'required'" name="requiredSkills"></skills-selector>
        </div>
        <p class="help is-danger" v-show="errors.has('requiredSkills')">{{ errors.first('requiredSkills')}}</p>
      </div>

      <div class="field">
        <label class="label">Description</label>
        <div class="control">
          <markdown-editor :value="badge.description" @value-updated="updateDescription"></markdown-editor>
        </div>
      </div>

      <div class="box">
        <div class="columns">
          <div class="column">
            <h2 class="title is-5" :class="{ 'has-text-grey-light': !limitTimeframe }">Gem Feature
              <b-tooltip label="The Gem feature allows for the badge to achievable during the specified time frame."
                         position="is-right" animanted="true" type="is-light">
                <span><i class="fas fa-question-circle"></i></span>
              </b-tooltip>
            </h2>
          </div>
          <div class="column has-text-right" style="font-size: 0.8rem; font-weight: lighter">
            <label class="checkbox" style="font-size: 0.8rem; font-weight: lighter">
              <input type="checkbox" v-model="limitTimeframe" @change="onEnableGemFeature"/>
                Enable Gem Feature
              <b-tooltip label="The Gem feature allows for the badge to only be achievable during the specified time frame."
                         position="is-left" animanted="true" type="is-light">
                <span><i class="fas fa-question-circle"></i></span>
              </b-tooltip>
            </label>
          </div>
        </div>

        <div class="field is-horizontal">
          <div class="field-body">
            <div class="field">
              <label class="label" :class="{ 'has-text-grey-light': !limitTimeframe }">Start Date</label>
              <div class="control is-expanded">
                <b-datepicker inline
                              placeholder="Click to select a date..."
                              v-model="badgeInternal.startDate"
                              ref="startDate"
                              icon="calendar-today"
                              :disabled="!limitTimeframe"
                              v-validate="'required|dateOrder'" name="startDate">
                </b-datepicker>
              </div>
              <p class="help is-danger" v-show="errors.has('startDate')">{{ errors.first('startDate')}}</p>
            </div>
            <div class="field">
              <label class="label" :class="{ 'has-text-grey-light': !limitTimeframe }">End Date</label>
              <div class="control ">
                <b-datepicker inline
                              placeholder="Click to select a date..."
                              v-model="badgeInternal.endDate"
                              ref="endDate"
                              icon="calendar-today"
                              :disabled="!limitTimeframe"
                              v-validate="'required|dateOrder'" name="endDate">
                </b-datepicker>
              </div>
              <p class="help is-danger" v-show="errors.has('endDate')">{{ errors.first('endDate')}}</p>
            </div>
          </div>
        </div>
      </div>

      <p v-if="errors.any() && overallErrMsg" class="help is-danger has-text-centered">***{{ overallErrMsg }}***</p>
    </section>

    <footer class="modal-card-foot skills-justify-content-right">
      <a class="button is-outlined" v-on:click="$parent.close()">
        <span class="icon is-small">
          <i class="fas fa-stop-circle"/>
        </span>
        <span>Cancel</span>
      </a>

      <a class="button is-primary is-outlined" v-on:click="updateBadge" :disabled="errors.any()">
        <span class="icon is-small">
          <i class="fas fa-arrow-circle-right"/>
        </span>
        <span>Save</span>
      </a>
    </footer>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import SkillsSelector from '../skills/SkillsSelector';
  import SkillsService from '../skills/SkillsService';

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
      IconPicker, SkillsSelector, MarkdownEditor,
    },
    props: ['badge', 'isEdit'],
    data() {
      // convert string to Date objects
      this.badge.startDate = this.toDate(this.badge.startDate);
      this.badge.endDate = this.toDate(this.badge.endDate);
      return {
        canEditBadgeId: false,
        limitTimeframe: this.badge.startDate && this.badge.endDate,
        isLoadingAvailableBadgeSkills: true,
        badgeInternal: Object.assign({}, this.badge),
        overallErrMsg: '',
        availableBadgeSkills: [],
      };
    },
    mounted() {
      self = this;
      this.loadAvailableBadgeSkills();
    },
    methods: {
      updateDescription(event) {
        this.badgeInternal.description = event.value;
      },
      updateBadge() {
        this.$validator.validateAll().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.$parent.close();
            this.$emit('badge-updated', this.badgeInternal);
          }
        });
      },
      updateBadgeId() {
        if (!this.isEdit && !this.canEditBadgeId) {
          this.badgeInternal.badgeId = this.badgeInternal.name.replace(/[^\w]/gi, '');
        }
      },
      onSelectedIcons(selectedIconCss) {
        this.badgeInternal.iconClass = selectedIconCss;
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
      loadAvailableBadgeSkills() {
        SkillsService.getProjectSkills(this.badgeInternal.projectId)
          .then((loadedSkills) => {
            this.availableBadgeSkills = loadedSkills.filter(item => item.type === 'Skill');
            this.isLoadingAvailableBadgeSkills = false;
          })
          .catch((e) => {
            this.serverErrors.push(e);
            this.isLoadingAvailableBadgeSkills = false;
            throw e;
        });
      },
      toggleEditId() {
        this.canEditBadgeId = !this.canEditBadgeId && !this.isEdit;
        this.updateBadgeId();
      },
    },
  };

</script>

<style scoped>
  .disableControl {
    pointer-events: none;
    color: #a8a8a8;
  }
</style>
