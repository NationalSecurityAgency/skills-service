<template>
  <modal :title="title" @cancel-clicked="closeMe" @save-clicked="saveLevel">
    <template slot="content">
      <div class="field is-horizontal">
        <div class="field-body">
          <div class="field is-narrow">
            <icon-picker :startIcon="iconClassInternal"
                         v-on:on-icon-selected="onSelectedIcons"></icon-picker>
          </div>
        </div>
      </div>

      <template v-if="isEdit">
        <b-field label="Level" :type="{'help is-danger': errors.has('level')}"
                 :message="errors.first('level')">
          <b-input v-model="levelInternal" name="level" v-validate="'required|min:0|max:100|numeric'"></b-input>
        </b-field>
        <b-field v-if="!this.levelAsPoints" label="Percent" :type="{'help is-danger': errors.has('percent')}"
                 :message="errors.first('percent')">
          <b-input v-model="percentInternal" name="percent" v-validate="'required|min:0|max:100|numeric|overlap'"></b-input>
        </b-field>
        <template v-else>
          <b-field label="Points From" :type="{'help is-danger': errors.has('pointsFrom')}"
                   :message="errors.first('pointsFrom')">
            <b-input v-model="pointsFromInternal" name="pointsFrom" v-validate="'required|min:0|numeric|overlap'"></b-input>
          </b-field>
          <b-field label="Points To" :type="{'help is-danger': errors.has('pointsTo')}"
                   :message="errors.first('pointsTo')">
            <b-input v-model="pointsToInternal" name="pointsTo" v-validate="'required|min:0|numeric|overlap'"></b-input>
          </b-field>
        </template>
        <b-field label="Name" :type="{'help is-danger': errors.has('name')}"
                 :message="errors.first('name')">
          <b-input v-model="nameInternal" name="name" v-validate="'max:50'"></b-input>
        </b-field>
      </template>
      <template v-else>
        <b-field v-if="!this.levelAsPoints" label="Percent" :type="{'help is-danger': errors.has('percent')}"
                 :message="errors.first('percent')">
          <b-input v-model="percentInternal" name="percent" v-validate="'required|min:0|max:100|numeric|overlap'"></b-input>
        </b-field>
        <b-field v-else label="Points" :type="{'help is-danger': errors.has('points')}"
                 :message="errors.first('points')">
          <b-input v-model="pointsInternal" name="points" v-validate="'required|min:0|numeric|overlap'"></b-input>
        </b-field>
        <b-field label="Name" :type="{'help is-danger': errors.has('name')}"
                 :message="errors.first('name')">
          <b-input v-model="nameInternal" name="name" v-validate="'max:50'"></b-input>
        </b-field>
      </template>

    </template>
  </modal>
</template>

<script>
  import { Validator } from 'vee-validate';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import Modal from '../utils/modal/Modal';

  export default {
    name: 'NewLevel',
    components: { IconPicker, Modal },
    props: ['levelAsPoints', 'percent', 'points', 'pointsFrom', 'pointsTo', 'name', 'iconClass', 'isEdit', 'levelId', 'level', 'boundaries'],
    data() {
      return {
        percentInternal: this.percent,
        pointsInternal: this.points,
        nameInternal: this.name,
        iconClassInternal: this.iconClass,
        pointsFromInternal: this.pointsFrom,
        pointsToInternal: this.pointsTo,
        levelInternal: this.level,
      };
    },
    mounted() {
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
    },
    methods: {
      closeMe() {
        this.$parent.close();
      },
      saveLevel() {
        this.$validator.validateAll().then((res) => {
          if (res) {
            if (this.isEdit === true) {
              this.$emit('edited-level', {
                percent: this.percentInternal,
                pointsFrom: this.pointsFromInternal,
                pointsTo: this.pointsToInternal,
                name: this.nameInternal,
                iconClass: this.iconClassInternal,
                id: this.levelId,
                level: this.levelInternal,
              });
            } else {
              this.$emit('new-level', {
                percent: this.percentInternal,
                points: this.pointsInternal,
                name: this.nameInternal,
                iconClass: this.iconClassInternal,
              });
            }
            this.$parent.close();
          }
        });
      },
      onSelectedIcons(selectedIconCss) {
        this.iconClassInternal = selectedIconCss;
      },
    },
  };
</script>

<style scoped>
  .level-title{
    padding-left: 1rem;
  }
</style>
