<template>
    <div>
        <div class="popover-title">
            <span class="progress-popup-label">Unfulfilled Dependencies</span>
        </div>

        <div class="popover-content-row">
            <span>Skill has <b>{{ userSkill.dependencyInfo.numDirectDependents}}</b> direct dependent(s). <br/> Click <i
                    class="fas fa-lock"></i> to see its dependencies.</span>
        </div>
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
    .progress-popup-label {
        font-weight: bold;
    }

    .popover-title {
        background-color: transparent;
        padding-bottom: 5px;
        margin-bottom: 5px;
        border-bottom: 1px solid #cccccc;
    }

    .popover-content-row {
        text-align: center;
        padding: 10px;
    }

    .popover-content-row i {
        color: #5d5d5d;
    }
</style>
