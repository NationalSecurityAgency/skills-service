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
    <div v-if="isPartiallyAchievedAndLocked || isFullyAchievedAndLocked">
        <div v-if="isPartiallyAchievedAndLocked" class="alert alert-info mt-2 text-center">
            <p>
                <i class="fas fa-info-circle mr-1"></i> You were able to earn partial points before the dependencies were added. Don't worry you get to keep the points!!!
            </p>
            <hr>
            <div class="text-center">Accomplish all of the dependencies to unlock the rest of the skill's points!</div>
        </div>

        <div v-if="isFullyAchievedAndLocked" class="alert alert-success mt-2 text-center">
            <p>
                <i class="fas fa-info-circle mr-1"></i> Congrats! You completed this skill before the dependencies were added. Don't worry you get to keep the points!!!
            </p>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'PartialPointsAlert',
        props: {
            skill: Object,
            isLocked: Boolean,
        },
        computed: {
            isPartiallyAchievedAndLocked() {
                return this.isLocked && this.skill.points > 0 && this.skill.points !== this.skill.totalPoints;
            },
            isFullyAchievedAndLocked() {
                return this.isLocked && this.skill.points === this.skill.totalPoints;
            },
        },
    };
</script>

<style scoped>

</style>
