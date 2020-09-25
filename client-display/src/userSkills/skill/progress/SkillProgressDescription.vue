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
    <div v-if="skill" class="text-left text-muted pb-md-2">
        <div v-if="locked" class="row justify-content-center pb-3 locked-text">
            <div class="col-12 col-md-9 text-muted">
                *** Skill has <b>{{ skill.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
                Click <i class="fas fa-lock icon"></i> to see its dependencies. ***
            </div>
        </div>
        <small v-else>
            <achievement-date v-if="skill.achievedOn" :date="skill.achievedOn" class="mb-3"/>
            <skill-summary-cards class="mb-3" :skill="skill" :short-sub-titles="true"/>
        </small>

        <div v-if="skill.description">
            <p v-if="skill.description.description" class="text-primary skills-text-description">
                <markdown-text :text="skill.description.description"/>
            </p>
            <ul v-if="skill.description.examples">
                Examples:
                <li v-for="(example, index) in skill.description.examples" :key="`unique-example-${index}`"
                    v-html="example"/>
            </ul>

            <div v-if="skill.description.href" class="user-skill-description-href mb-3 text-center text-md-left">
                <strong>Need help?</strong>
                <a :href="skill.description.href" target="_blank" rel="noopener">
                    Click here!
                </a>
            </div>
        </div>

        <hr class="mb-2"/>
    </div>
</template>

<script>
  import SkillSummaryCards from '@/userSkills/skill/progress/SkillSummaryCards';
  import MarkdownText from '@/common/utilities/MarkdownText';
  import AchievementDate from '@/userSkills/skill/AchievementDate';

  export default {
    name: 'SkillProgressDescription',
    components: { AchievementDate, SkillSummaryCards, MarkdownText },
    props: {
      skill: Object,
    },
    computed: {
      locked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
    },
  };
</script>

<style scoped>
    .locked-text {
        font-size: 0.8rem;
        color: #383838;
    }
</style>
