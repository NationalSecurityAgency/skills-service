<template>
    <div class="mb-2">
        <span class="text-muted">Skill has <b>{{ userSkill.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
            Click <i class="fas fa-lock icon"></i> to see its dependencies.</span>
    </div>
</template>

<script>
    export default {
        name: 'SkillIsLockedMessage',
        props: {
            userSkill: {
                type: Object,
                required: true,
            },
        },
    };
</script>

<style scoped>
    .icon {
        color: #5d5d5d;
    }
</style>
