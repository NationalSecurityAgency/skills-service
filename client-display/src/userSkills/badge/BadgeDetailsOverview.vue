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
    <div v-if=badge class="row skills-badge">
        <div class="col-lg-2">
            <div class="card mb-2 skills-badge-icon">
                <div class="card-body">
                    <i :class="iconCss" class="fa-4x" style="min-width: 3rem;, max-width: 4rem;"/>
                    <i v-if="badge.gem" class="fas fa-gem position-absolute" style="top: 5px; right: 5px; color: purple"></i>
                    <i v-if="badge.global" class="fas fa-globe position-absolute" style="top: 5px; right: 5px; color: blue"></i>
                    <div v-if="badge.gem" class="text-muted">
                        <small>Expires {{ badge.endDate | moment("from", "now") }}</small>
                    </div>
                    <div v-if="badge.global" class="text-muted">
                        <small><b>Global Badge</b></small>
                    </div>
                </div>
            </div>
        </div>

        <div class="text-sm-left text-secondary text-center skills-text-description col-lg-10">
            <div class="row">
                <h4 class="mb-1 col-md-8">{{ badge.badge }}</h4>
                <div class="col-md-4 text-right">
                    <small class=" float-right text-navy" :class="{ 'text-success': percent === 100 }">
                        <i v-if="percent === 100" class="fa fa-check"/> {{ percent }}% Complete
                    </small>
                </div>
            </div>


            <div class="mb-2">
                <progress-bar bar-color="lightgreen" :val="percent"></progress-bar>
            </div>

            <p v-if="badge && badge.description" class="">
               <markdown-text :text="badge.description"/>
            </p>


            <slot name="body-footer" v-bind:props="badge">

            </slot>
        </div>
    </div>
</template>

<script>
    import ProgressBar from 'vue-simple-progress';
    import MarkdownText from '@/common/utilities/MarkdownText.vue';

    export default {
        name: 'BadgeDetailsOverview',
        components: {
            ProgressBar,
            MarkdownText,
        },
        props: {
            badge: {
                type: Object,
            },
            iconColor: {
                type: String,
                default: 'text-success',
            },
        },
        computed: {
            percent() {
                if (this.badge.numTotalSkills === 0) {
                    return 0;
                }
                return Math.trunc((this.badge.numSkillsAchieved / this.badge.numTotalSkills) * 100);
            },
            iconCss() {
                return `${this.badge.iconClass} ${this.iconColor}`;
            },
        },
    };
</script>

<style scoped>

</style>
