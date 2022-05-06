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
  <div class="form-group">
    <label for="skillHelpUrl">Help URL/Path
      <inline-help v-if="$route.params.projectId"
        msg="If project level 'Root Help Url' is specified then this path will be relative to 'Root Help Url'"/>
    </label>
      <ValidationProvider rules="help_url|customUrlValidator" v-slot="{errors}"
                          name="Help URL/Path">
          <b-input-group>
            <template #prepend v-if="rootHelpUrlSetting">
              <b-input-group-text><i class="fas fa-cogs mr-1"></i>
                <span class="text-primary" :class="{ 'stikethrough' : overrideRootHelpUrl}" data-cy="rootHelpUrlSetting"
                      :aria-label="`Root Help URL was configured in the project's settings. Root Help URL is ${rootHelpUrl}. URLs starting with http or https will not use Root Help URL.`"
                      v-b-tooltip.hover="'Root Help URL was configured in the project\'s settings. URLs starting with http(s) will not use Root Help URL.'">{{ rootHelpUrl }}</span></b-input-group-text>
            </template>
            <b-form-input
              id="skillHelpUrl"
              v-model="internalValue" data-cy="skillHelpUrl"
              aria-describedby="skillHelpUrlError"
              aria-errormessage="skillHelpUrlError"
              :aria-invalid="(errors && errors.length > 0)"></b-form-input>
          </b-input-group>
        <small class="form-text text-danger" id="skillHelpUrlError"
               data-cy="skillHelpUrlError">{{ errors[0] }}</small>
      </ValidationProvider>
  </div>
</template>

<script>
  import { extend, ValidationProvider } from 'vee-validate';
  import InlineHelp from './InlineHelp';

  extend('help_url', {
    message: (field) => `${field} must use http://, https://, or be a relative url.`,
    validate(value) {
      if (!value) {
        return true;
      }
      return value.startsWith('http') || value.startsWith('https') || value.startsWith('/');
    },
  });

  export default {
    name: 'HelpUrlInput',
    components: {
      InlineHelp,
      ValidationProvider,
    },
    props: {
      value: String,
    },
    data() {
      return {
        internalValue: this.value,
      };
    },
    computed: {
      overrideRootHelpUrl() {
        return this.rootHelpUrlSetting && this.internalValue && (this.internalValue.startsWith('http://') || this.internalValue.startsWith('https://'));
      },
      rootHelpUrlSetting() {
        return this.$store.getters.projConfig && this.$store.getters.projConfig['help.url.root'];
      },
      rootHelpUrl() {
        if (!this.rootHelpUrlSetting) {
          return this.rootHelpUrlSetting;
        }
        if (this.rootHelpUrlSetting.endsWith('/')) {
          return this.rootHelpUrlSetting.substring(0, this.rootHelpUrlSetting.length - 1);
        }
        return this.rootHelpUrlSetting;
      },
    },
    watch: {
      internalValue(newVal) {
        this.$emit('input', newVal);
      },
    },
  };
</script>

<style scoped>
.stikethrough {
  text-decoration: line-through;
}
</style>
