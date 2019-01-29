<template>
    <div id="skills-selector">
      <v-select multiple label="name" :options="available"
                v-model="value" style="width: 100%;" :onChange="changed">
        <template slot="no-options">
          No skills available at this time. You can always add/create skills after this skill is created.
        </template>
        <template slot="option" slot-scope="option">
          <div class="columns">
            <div class="column handle-overflow" style="flex:none; width:45%;" :title="option.name"><span class="selector-skill-name">{{ option.name }}</span></div>
            <div class="column is-one-fifth handle-overflow" style="flex:none; width:30%;" :title="option.skillId"><span class="selector-other-label">ID:</span> <span class="selector-other-value">{{option.skillId}},</span></div>
            <div class="column is-one-fifth" style="flex:none; width:20%;"><span class="selector-other-label">Total Points:</span> <span class="selector-other-value">{{ option.totalPoints}}</span></div>
          </div>
        </template>
      </v-select>
    </div>
</template>

<script>

  export default {
    name: 'ChildSkillsSelector',
    props: ['value', 'availableToSelect'],
    data() {
      return {
        options: [],
        serverErrors: [],
        available: [],
      };
    },
    mounted() {
      this.updateAvailable();
    },
    watch: {
      availableToSelect: function watchAvailable() {
        this.updateAvailable();
      },
    },
    methods: {
      changed() {
        this.updateAvailable();
        this.$emit('input', this.value);
        this.$emit('selection-changed', this.value);
      },
      updateAvailable() {
        this.available = this.availableToSelect.filter(item => !this.value.find(item1 => item.skillId === item1.skillId));
      },
    },
  };
</script>

<style>
  #skills-selector .v-select.searchable .dropdown-toggle {
    width: 100%;
  }

  #skills-selector  .selector-skill-name {
    font-size: 1.2rem;
    font-weight: bold;
  }

  #skills-selector .selector-other-value {
    /*font-weight: bold;*/
  }
  #skills-selector .selector-other-label {
    color: lightgray;
    font-style: italic;
  }

  #skills-selector .handle-overflow {
    overflow: hidden;
    text-overflow: ellipsis;
    /*background-color: blue;*/
  }

  #skills-selector .handle-overflow {
    overflow: hidden;
    text-overflow: ellipsis;
  }

</style>
