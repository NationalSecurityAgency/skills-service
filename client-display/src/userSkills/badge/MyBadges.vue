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
    <div class="card h-100" @click.stop="openMyBadges"
         :class="{ 'skills-navigable-item': !isSummaryOnly }" data-cy="myBadges">
        <div class="card-header">
            <h6 class="card-title mb-0 text-uppercase">My Badges</h6>
        </div>
        <div class="card-body">
            <span class="fa-stack skills-icon user-rank-stack">
                <i class="fa fa-award fa-stack-2x watermark-icon"/>
                <strong class="fa-stack-1x text-primary user-rank-text">
                  {{ numBadgesCompleted }} <span>Badge{{(numBadgesCompleted > 1 || numBadgesCompleted == 0) ? 's' : ''}}</span>
                </strong>
            </span>
        </div>
    </div>
</template>

<script>
  export default {
    name: 'MyBadges',
    props: {
      numBadgesCompleted: {
        type: Number,
        required: true,
      },
    },
    methods: {
      openMyBadges() {
        if (!this.isSummaryOnly) {
          this.$router.push('/badges');
        }
      },
    },
    computed: {
      isSummaryOnly() {
        return this.$store.state.isSummaryOnly;
      },
    },
  };
</script>

<style scoped>
    .skills-icon {
        display: inline-block;
        color: #b1b1b1;
        margin: 5px 0;
    }

    .skills-icon.user-rank-stack i {
        color: #0fcc15d1;
        opacity: 0.38;
    }

    .skills-icon.user-rank-stack {
        margin: 14px 0;
        font-size: 4.1rem;
        width: 100%;
        color: #0fcc15d1;
    }

    .user-rank-text {
        font-size: 0.5em;
        line-height: 1.2em;
        margin-top: 1.8em;
        background: rgba(255, 255, 255, 0.6);
    }

    .user-rank-text span {
        font-weight: normal;
    }
</style>
