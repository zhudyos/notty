<template>
    <div class="row mt-2" :class="rowBorderClass">
        <div class="d-flex">
            <div class="container">
                <div class="row">
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">ID:</div>
                        <div class="text-secondary ml-1">{{ id }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Service Name:</div>
                        <div class="text-secondary ml-1">{{ serviceName }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">SID:</div>
                        <div class="text-secondary ml-1">{{ sid }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Created At:</div>
                        <div class="text-secondary ml-1">{{ createdAt }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">URL:</div>
                        <div class="text-secondary ml-1">{{ url }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Last Call At:</div>
                        <div class="text-secondary ml-1">{{ lastCallAt }}</div>
                    </div>
                    <template v-if="showDetails">
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Retry Count:</div>
                            <div class="text-secondary ml-1">{{ retryCount }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Retry Max Count:</div>
                            <div class="text-secondary ml-1">{{ retryMaxCount }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Method:</div>
                            <div class="text-secondary ml-1">{{ method }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Content-Type:</div>
                            <div class="text-secondary ml-1">{{ contentType }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Body:</div>
                            <div class="ml-1">
                                <pre class="text-secondary">{{ prettyBody }}</pre>
                            </div>
                        </div>
                        <div class="col-12">
                            <task-details></task-details>
                        </div>
                    </template>
                </div>
            </div>
            <div class="d-flex flex-column justify-content-center align-items-center task-item-right-option"
                 :class="taskItemRightOptionBorderClass"
                 @click="toggleDetails()"
            >
                <i class="fas fa-ellipsis-v"></i>
            </div>
        </div>
    </div>
</template>
<script>
    import TaskDetails from "./TaskDetails.vue"

    export default {
        components: {TaskDetails},
        name: "TaskItem",
        props: {
            id: String,
            serviceName: String,
            sid: String,
            createdAt: String,
            url: String,
            method: String,
            contentType: String,
            body: String,
            lastCallAt: String,
            retryCount: Number,
            retryMaxCount: Number,
            status: Number
        },
        data: () => ({
            showDetails: false
        }),
        computed: {
            rowBorderClass() {
                let s = this.status
                if (s == 1) {
                    return "border border-primary"
                } else if (s == 7) {
                    return "border border-warning"
                } else if (s == 8) {
                    return "border border-success"
                }
                return "border border-danger"
            },
            taskItemRightOptionBorderClass() {
                let s = this.status
                if (s == 1) {
                    return "border-left border-primary"
                } else if (s == 7) {
                    return "border-left border-warning"
                } else if (s == 8) {
                    return "border-left border-success"
                }
                return "border-left border-danger"
            },
            prettyBody() {
                if (!this.body) {
                    return ''
                }

                try {
                    let b = JSON.parse(this.body)
                    return JSON.stringify(b, null, 2)
                } catch (e) {
                    return this.body
                }
            }
        },
        methods: {
            toggleDetails() {
                this.showDetails = !this.showDetails
            }
        }
    }
</script>
<style lang="stylus" scoped>
    .task-item-right-option
        padding 0 5px
</style>