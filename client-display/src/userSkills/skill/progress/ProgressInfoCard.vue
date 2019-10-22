<template>
    <div class="card h-100">
            <div class="card-body px-2 bg-light">
                <div class="media">
                    <div class="d-inline-block mt-2 mr-3">
                        <i class="fa-3x" :class="icon"/>
                    </div>
                    <div class="media-body text-truncate text-primary">
                        <h4 class="mb-0 text-truncate">
                            <span :title="title | number">{{title | number}}</span>
                        </h4>
                        <div class="mt-0 pt-0">{{subTitle}}</div>
                    </div>
                </div>
                <div class="mt-2">{{label}}</div>
            </div>
    </div>
</template>

<script>
    export default {
        name: 'ProgressInfoCard',
        props: {
            title: Number,
            label: String,
            icon: String,
            subTitle: {
                type: String,
                default: 'Points',
            },
        },
    };
</script>
