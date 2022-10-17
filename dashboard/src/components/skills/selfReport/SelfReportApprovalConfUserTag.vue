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
  <b-card body-class="p-0 mt-3">
    <template #header>
      <div>
        <i class="fas fa-user-tag text-primary" aria-hidden="true"/> Split Workload <span class="font-italic text-primary">By {{ tagLabel }}</span>
      </div>
    </template>
    <ValidationProvider ref="validationProvider" :name="`${tagLabel}`" v-slot="{errors}" rules="maxTagValueLengthInApprovalWorkloadConfig|alpha_num|uniqueTagConf">
    <div class="row mx-2 no-gutters">
      <div class="col px-1">
          <b-form-input id="tagValueInput"
                        v-model="enteredTag"
                        v-on:keydown.enter="addTagConf"
                        :placeholder="`Enter ${tagLabel} to add to the config!`"
                        data-cy="tagValueInput"
                        :aria-invalid="errors && errors.length > 0"
                        aria-describedby="tagValueInputError"
                        aria-errormessage="tagValueInputError"></b-form-input>
          <small role="alert" class="form-text text-danger" v-show="errors[0]" data-cy="tagValueInputError" id="tagValueInputError">{{ errors[0] }}</small>
      </div>
      <div class="col-auto px-1">
        <b-button
          aria-label="Add Tag Value"
          @click="addTagConf"
          :disabled="!enteredTag || (errors && errors.length > 0)"
          variant="outline-primary">Add <i class="fas fa-plus-circle" aria-hidden="true" />
        </b-button>
      </div>
    </div>
    </ValidationProvider>

    <skills-b-table v-if="hadData" class="mt-3"
                    :options="table.options" :items="table.items"
                    tableStoredStateId="skillApprovalConfSpecificUsersTable"
                    data-cy="skillApprovalConfSpecificUsersTable">
      <template v-slot:cell(userTagValue)="data">
        <div class="row">
          <div class="col">
            {{ data.value }}
          </div>
          <div class="col-auto">
            <b-button title="Delete Skill"
                      variant="outline-danger"
                      :aria-label="`Remove ${data.value} tag.`"
                      @click="removeTagConf(data.item)"
                      :disabled="data.item.deleteInProgress"
                      size="sm">
              <b-spinner v-if="data.item.deleteInProgress" small></b-spinner>
              <i v-else class="fas fa-trash" aria-hidden="true"/>
            </b-button>
          </div>
        </div>

      </template>
      <template v-slot:cell(updated)="data">
        <date-cell :value="data.value" />
      </template>
    </skills-b-table>

    <no-content2 v-if="!hadData" title="Not Configured Yet..."
                 class="my-5"
                 icon-size="fa-2x"
                 :message="`You can split approval workload by ${tagLabel}.`" />

  </b-card>
</template>

<script>
  import { extend } from 'vee-validate';
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import DateCell from '@/components/utils/table/DateCell';
  import NoContent2 from '@/components/utils/NoContent2';
  import SelfReportService from '@/components/skills/selfReport/SelfReportService';

  export default {
    name: 'SelfReportApprovalConfUserTag',
    components: {
      NoContent2, DateCell, SkillsBTable,
    },
    props: {
      userInfo: Object,
      tagLabel: String,
      tagKey: String,
    },
    created() {
      this.assignCustomValidation();
    },
    mounted() {
      this.table.options.fields = [
        {
          key: 'userTagValue',
          label: this.tagLabel,
          sortable: true,
        },
        {
          key: 'updated',
          label: 'Configured On',
          sortable: true,
        },
      ];

      const hasTagConf = this.userInfo.tagConf && this.userInfo.tagConf.length > 0;
      if (hasTagConf) {
        this.table.items = this.userInfo.tagConf.map((u) => ({ ...u }));
      }
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        enteredTag: '',
        table: {
          items: [],
          options: {
            busy: false,
            bordered: true,
            outlined: true,
            stacked: 'md',
            sortBy: 'updated',
            sortDesc: true,
            emptyText: 'You are the only user',
            tableDescription: 'Configure Approval Workload',
            fields: null,
            pagination: {
              remove: false,
              server: false,
              currentPage: 1,
              totalRows: 1,
              pageSize: 4,
              possiblePageSizes: [4, 10, 15, 20],
            },
          },
        },
      };
    },
    computed: {
      pkiAuthenticated() {
        return this.$store.getters.isPkiAuthenticated;
      },
      hadData() {
        return this.table.options.fields && this.table.items && this.table.items.length > 0;
      },
    },
    methods: {
      addTagConf() {
        this.$refs.validationProvider.validate().then((validationRes) => {
          if (validationRes.valid) {
            SelfReportService.configureApproverForUserTag(this.projectId, this.userInfo.userId, this.tagKey, this.enteredTag)
              .then((res) => {
                this.table.items.push(res);
                this.enteredTag = '';
                this.$emit('conf-added', res);
              });
          }
        });
      },
      removeTagConf(removedIem) {
        this.table.items = this.table.items.map((i) => ({ ...i, deleteInProgress: i.id === removedIem.id }));
        SelfReportService.removeApproverConfig(this.projectId, removedIem.id)
          .then(() => {
            this.table.items = this.table.items.filter((i) => i.id !== removedIem.id);
            this.$emit('conf-removed', removedIem);
          });
      },
      assignCustomValidation() {
        const self = this;
        extend('uniqueTagConf', {
          message: (field) => `${field} value is already taken.`,
          validate(value) {
            return !self.table.items.find((i) => value.toLowerCase() === i.userTagValue);
          },
        });
      },
    },
  };
</script>

<style scoped>

</style>
