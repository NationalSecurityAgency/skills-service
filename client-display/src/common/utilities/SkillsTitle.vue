<template>
    <div class="py-2 mt-2 mb-3 card-body skills-page-title-text-color card rounded bg-white text-info text-uppercase">
        <h2>
            <slot/>
        </h2>
    </div>
</template>

<script>
    export default {
        name: 'SkillsTitle',
    };
</script>
