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
                    <div class="d-inline-block mt-2 mr-3">
                        <i class="fa-3x" :class="icon"/>
                    </div>
                    <div class="media-body text-truncate text-primary">
                        <h4 class="mb-0 text-truncate">
                            <span :title="title | number" data-cy="progressInfoCardTitle">
                              <animated-number v-if="animated" :num="title" />
                              <span v-else>{{ title | number }}</span>
                            </span>
                        </h4>
                        <div class="mt-0 pt-0">{{subTitle}}</div>
                    </div>
                </div>
                <div class="mt-2">{{label}}</div>
            </div>
    </div>
</template>

<script>
  import AnimatedNumber from './AnimatedNumber';

  export default {
    name: 'ProgressInfoCard',
    components: { AnimatedNumber },
    props: {
      title: Number,
      label: String,
      icon: String,
      subTitle: {
        type: String,
        default: 'Points',
      },
      animated: {
        type: Boolean,
        default: true,
      },
    },
  };
</script>
