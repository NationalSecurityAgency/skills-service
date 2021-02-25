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
    <div class="card h-100 skills-progress-info-card">
            <div class="card-body px-2 bg-light">
                <div class="media">
                    <div class="d-inline-block mr-3">
                        <i class="fa-3x fas fa-laptop text-success"/>
                    </div>
                    <div class="media-body text-truncate text-primary">
                        <div class="h3 mb-0 text-truncate text-uppercase">
                           {{ title }}
                        </div>
                    </div>
                </div>
                <div class="mt-2">{{description}}</div>
            </div>
    </div>
</template>

<script>

  export default {
    name: 'SelfReportInfoCard',
    data() {
      return {
        title: 'Approval is required',
        description: 'Once reported this skill will go into an approval queue.',
      };
    },
  };
</script>
