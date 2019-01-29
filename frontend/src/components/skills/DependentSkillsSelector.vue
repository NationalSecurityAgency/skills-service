<template>
  <div class="field skills-pad-top-1-rem" v-if="!isLoadingAvailable">
    <label class="label">
      <b-loading :is-full-page="false" :active.sync="isReLoadingAvailable" :can-cancel="false"></b-loading>
      <div class="columns">
        <div class="column">
          {{ title }}
          <b-tooltip label="Users must first complete these before given a chance to start on this skill."
                     position="is-right" animanted="true" type="is-light">
            <span><i class="fas fa-question-circle"></i></span>
          </b-tooltip>
        </div>
        <div class="column has-text-right skills-enable-control">
          <search-all-skills-checkbox v-model="shouldOfferAllAvailableSkills" v-on:selection-changed="reLoadAvailableDependentSkills"></search-all-skills-checkbox>
        </div>
      </div>
    </label>
    <div class="control is-expanded">
      <skills-selector name="dependentSkills" v-validate="`checkSkillsGraph-${this.validationType}`"
                       v-model="skills" :available-to-select="available" v-on:selection-changed="selectionChanged"></skills-selector>
      <p class="help is-danger" v-show="errors.has('dependentSkills')">{{
        errors.first('dependentSkills')}}</p>
    </div>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SkillsSelector from './SkillsSelector';
  import SearchAllSkillsCheckbox from './SearchAllSkillsCheckbox';
  import SkillsService from './SkillsService';

  export default {
    name: 'DependentSkillsSelector',
    components: { SkillsSelector, SearchAllSkillsCheckbox },
    // skillId is optional and indicates that we are editing existing skill rather than creating a new one
    props: ['title', 'tooltip', 'projectId', 'subjectId', 'skillId', 'value', 'validationType'],
    data() {
      return {
        skills: [],
        copy: [],
        available: [],
        errorMsg: '',
        skillsWhenLoaded: [],
        skillIdsWithoutIssues: [],
        isLoadingAvailable: true,
        shouldOfferAllAvailableSkills: false,
        isReLoadingAvailable: false,
        serverErrors: [],
      };
    },
    mounted() {
      this.skills = this.value.map(item => Object.assign({}, item));
      this.copy = this.value;

      const self = this;
      Validator.extend(`checkSkillsGraph-${this.validationType}`, {
        getMessage: () => self.errorMsg,
        validate(value) {
          return self.checkGraphViaRMI(value);
        },
      });

      this.loadAvailableDependentSkills();
    },
    methods: {
      selectionChanged() {
        this.$emit('input', this.skills);
      },

      reLoadAvailableDependentSkills() {
        this.isReLoadingAvailable = true;
        this.loadAvailableDependentSkills();
      },
      loadAvailableDependentSkills() {
        this.loadAvailableSkills(this.shouldOfferAllAvailableSkills)
          .then((loadedSkills) => {
            this.available = loadedSkills;
            this.completeLoading();
          }).catch((e) => {
            this.completeLoading();
            throw e;
        });
      },

      completeLoading() {
        this.isReLoadingAvailable = false;
        this.isLoadingAvailable = false;
        this.$emit('done-loading', true);
      },

      loadAvailableSkills(isAllSkills) {
        const self = this;
        return this.serviceCallForAvailableSkills(isAllSkills)
          .then((loadedSkills) => {
            let res = loadedSkills;
            if (self.skillId) {
              res = loadedSkills.filter(item => item.skillId !== self.skillId);
            }
            return res;
          })
          .catch((e) => {
            this.serverErrors.push(e);
            throw e;
        });
      },

      serviceCallForAvailableSkills(isAllSkills) {
        if (isAllSkills) {
          return SkillsService.getProjectSkills(this.projectId);
        }

        return SkillsService.getSubjectSkills(this.projectId, this.subjectId);
      },

      checkGraphViaRMI(value) {
        // custom validation makes sure that there are not circular dependencies
        // which only applicable if the skill is being edited
        if (this.skillId) {
          let skillIds = value.map(item => item.skillId);
          // no reason to check for skills that we know already work
          // filter any of the skills that were assigned at the time component was created
          skillIds = skillIds.filter(item => !this.skillsWhenLoaded.find(item1 => item1.skillId === item));
          skillIds = skillIds.filter(item => !this.skillIdsWithoutIssues.find(item1 => item1 === item));

          if (skillIds && skillIds.length > 0) {
            return SkillsService.checkSkillsGraph(this.projectId, this.skillId, this.validationType, skillIds)
              .then((remoteRes) => {
                const successSkills = remoteRes.filter(item => item.possible);
                for (let i = 0; i < successSkills.length; i += 1) {
                  const depSkillId = successSkills[i].dependentSkillId;
                  if (this.skillIdsWithoutIssues.indexOf(depSkillId) === -1) {
                    this.skillIdsWithoutIssues.push(depSkillId);
                  }
                }

                const failedSkills = remoteRes.filter(item => !item.possible);
                if (failedSkills && failedSkills.length > 0) {
                  this.errorMsg = failedSkills[0].reason;
                  return false;
                }
                return true;
              })
              .catch(e => this.serverErrors.push(e));
          }
        }
        return true;
      },
    },
  };
</script>

<style scoped>

</style>
