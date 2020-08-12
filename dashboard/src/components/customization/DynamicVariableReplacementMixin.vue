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
<script>
  const releaseVersion = /\{\{\s?release.version\s?\}\}/gi;
  const buildDate = /\{\{\s?build.date\s?\}\}/gi;
  export default {
    name: 'DynamicVariableReplacementMixin',
    methods: {
      populateDynamicVariables(text) {
        let result = text;
        if (result) {
          const version = this.$store.getters.config.dashboardVersion;
          if (version) {
            result = result.replace(releaseVersion, version);
          }
          const timestamp = this.$store.getters.config.buildTimestamp;
          if (timestamp) {
            const [date] = timestamp.split('T');
            result = result.replace(buildDate, date);
          }
        }
        return result;
      },
    },
  };
</script>
