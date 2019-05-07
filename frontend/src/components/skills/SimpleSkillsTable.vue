<template>
  <div id="simple-skills-table" v-if="this.skills && this.skills.length">
    <v-client-table :data="skills" :columns="columns" :options="options">
      <div slot="edit" slot-scope="props">
        <div class="field has-addons has-text-right">
          <span class="field has-addons">
            <p class="">
              <button v-on:click="onDeleteEvent(props.row)" class="btn btn-sm btn-outline-primary">
                      <i class="fas fa-trash"/>
              </button>
            </p>
            <p class="skills-pad-left-1-rem">
              <b-tooltip :label="getManagedBtnDisabledMsg(props.row)" :active="isManagedBtnDisabled(props.row)"
                         position="is-left" animanted="true" type="is-light">
                <router-link :to="{ name:'SkillPage',
                params: { projectId: props.row.projectId, subjectId: props.row.subjectId, skillId: props.row.skillId }}"
                             class="btn btn-sm btn-outline-primary"
                             v-bind:class="{ notactive: isManagedBtnDisabled(props.row) }">
                  <span>Manage</span>
                  <span class="icon is-small">
                    <i class="fas fa-arrow-circle-right"/>
                  </span>
                </router-link>
              </b-tooltip>
            </p>
          </span>
        </div>
      </div>


      <div slot="name" slot-scope="props">
        <!-- allow to override how name field is rendered-->
        <slot name="name-cell" v-bind:props="props.row">
          {{ props.row.name }}
        </slot>
      </div>
    </v-client-table>
  </div>
</template>

<script>
  export default {
    name: 'SimpleSkillsTable',
    props: ['skills'],
    data() {
      return {
        columns: ['name', 'skillId', 'pointIncrement', 'totalPoints', 'edit'],
        options: {
          headings: {
            name: 'Skill Name',
            skillId: 'Skill ID',
            pointIncrement: 'Point Increment',
            totalPoints: 'Total Points',
            edit: '',
          },
          perPage: 15,
          columnsClasses: {
            edit: 'control-column',
          },
          columnsDisplay: {
            skillId: 'not_mobile',
            pointIncrement: 'not_mobile',
            totalPoints: 'not_mobile',
          },
          pagination: { dropdown: false, edge: false },
          sortable: ['name', 'skillId', 'pointIncrement', 'totalPoints'],
          sortIcon: {
            base: 'fa fa-sort', up: 'fa fa-sort-up', down: 'fa fa-sort-down', is: 'fa fa-sort',
          },
          // highlightMatches: true,
          skin: 'table is-striped is-fullwidth',
          filterable: false,
        },
      };
    },
    methods: {
      isManagedBtnDisabled(row) {
        return row && row.disabledStatus && row.disabledStatus.manageBtn && row.disabledStatus.manageBtn.disable;
      },
      getManagedBtnDisabledMsg(row) {
        let msg = '';
        if (this.isManagedBtnDisabled(row)) {
          ({ msg } = row.disabledStatus.manageBtn);
        }
        return msg;
      },
      onDeleteEvent(skill) {
        this.$emit('skill-removed', skill);
      },
    },
  };
</script>

<style>
  #simple-skills-table .VueTables__limit-field {
    display: none;
  }

  #simple-skills-table .control-column {
    width: 11rem;
  }

  /* on the mobile platform some of the columns will be removed
     so let's allow the table to size on its own*/
  @media (max-width: 576px) {
    #simple-skills-table .control-column {
      width: unset;
    }
  }


  #simple-skills-table .notactive {
    cursor: not-allowed;
    pointer-events: none;
    color: #c0c0c0;
    background-color: #ffffff;
    border-color: gray;
  }

</style>
