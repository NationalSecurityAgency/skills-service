<template>
  <div class="modal-card" style="width:400px">
    <header class="modal-card-head">
      <icon-picker :startIcon="iconClassInternal"
                   v-on:on-icon-selected="onSelectedIcons"
                   custom-icon-height="24px"
                   custom-icon-width="24px"></icon-picker>
      <span v-if="isEdit" class="level-title">Edit Level</span>
      <span v-else class="level-title">New Level</span>
    </header>

    <section class="modal-card-body">
      <template v-if="isEdit">
        <b-field label="Level" :type="{'help is-danger': errors.has('level')}"
                 :message="errors.first('level')">
          <b-input v-model="levelInternal" name="level" v-validate="'required|min:0|max:100|numeric'"></b-input>
        </b-field>
        <b-field v-if="!this.levelAsPoints" label="Percent" :type="{'help is-danger': errors.has('percent')}"
                 :message="errors.first('percent')">
          <b-input v-model="percentInternal" name="percent" v-validate="'required|min:0|max:100|numeric'"></b-input>
        </b-field>
        <template v-else>
          <b-field label="Points From" :type="{'help is-danger': errors.has('pointsFrom')}"
                   :message="errors.first('pointsFrom')">
            <b-input v-model="pointsFromInternal" name="pointsFrom" v-validate="'required|min:0|numeric'"></b-input>
          </b-field>
          <b-field label="Points To" :type="{'help is-danger': errors.has('pointsTo')}"
                   :message="errors.first('pointsTo')">
            <b-input v-model="pointsToInternal" name="pointsTo" v-validate="'required|min:0|numeric'"></b-input>
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
          <b-input v-model="percentInternal" name="percent" v-validate="'required|min:0|max:100|numeric'"></b-input>
        </b-field>
        <b-field v-else label="Points" :type="{'help is-danger': errors.has('points')}"
                 :message="errors.first('points')">
          <b-input v-model="pointsInternal" name="points" v-validate="'required|min:0|numeric'"></b-input>
        </b-field>
        <b-field label="Name" :type="{'help is-danger': errors.has('name')}"
                 :message="errors.first('name')">
          <b-input v-model="nameInternal" name="name" v-validate="'max:50'"></b-input>
        </b-field>
      </template>
    </section>

    <footer class="modal-card-foot">
      <button class="button is-link is-outlined" v-on:click="$parent.close()">
        <span>Cancel</span>
        <span class="icon is-small">
              <i class="fas fa-stop-circle"/>
            </span>
      </button>

      <button class="button is-primary is-outlined" v-on:click="saveLevel" :disabled="errors.any()">
        <span>Save</span>
        <span class="icon is-small">
              <i class="fas fa-arrow-circle-right"/>
            </span>
      </button>
    </footer>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import IconPicker from '../utils/iconPicker/IconPicker';

  export default {
    name: 'NewLevel',
    components: { IconPicker },
    props: ['levelAsPoints', 'percent', 'points', 'pointsFrom', 'pointsTo', 'name', 'iconClass', 'isEdit', 'levelId', 'level'],
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
    created() {
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
      Validator.localize(dictionary);
    },
    methods: {
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
