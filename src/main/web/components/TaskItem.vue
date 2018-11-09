<template>
    <div class="row mt-2 bg-transparent" :class="rowBorderClass">
        <div class="d-flex">
            <div class="container">
                <div class="row">
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">ID:</div>
                        <div class="ml-1" title="任务ID">{{ id }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Service Name:</div>
                        <div class="ml-1" title="提交任务服务名">{{ serviceName }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">SID:</div>
                        <div class="ml-1" title="业务ID">{{ sid }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Created At:</div>
                        <div class="ml-1" title="任务提交时间">{{ $dayjs(createdAt).format("YYYY-MM-DD hh:mm:ss") }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">URL:</div>
                        <div class="ml-1" title="回调地址">{{ url }}</div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Last Call At:</div>
                        <div class="ml-1" title="最后回调时间" v-if="lastCallAt">{{ $dayjs(lastCallAt).format("YYYY-MM-DD hh:mm:ss") }}</div>
                    </div>
                    <template v-if="showDetails">
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Retry Count:</div>
                            <div class="ml-1" title="已回调次数">{{ retryCount }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Retry Max Count:</div>
                            <div class="ml-1" title="最大回调次数">{{ retryMaxCount }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Method:</div>
                            <div class="ml-1" title="回调方法">{{ method }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Content-Type:</div>
                            <div class="ml-1" title="回调内容类型">{{ contentType }}</div>
                        </div>
                        <div class="col-md-6 d-flex">
                            <div class="t-data-item-title">Body:</div>
                            <div class="ml-1">
                                <read-more :max-chars="20" :text="body"></read-more>
                            </div>
                        </div>
                        <div class="col-12">
                            <task-details :id="id"></task-details>
                        </div>
                    </template>
                </div>
            </div>
            <div class="d-flex flex-column justify-content-center align-items-center task-item-right-option"
                 :class="taskItemRightOptionBorderClass"
            >
                <a href="#"
                   class="text-white"
                   title="取消任务"
                   v-if="status == 1"
                ><i class="fas fa-ban"></i></a>
                <a href="#"
                   class="text-white"
                   title="重新执行"
                   v-if="status == 7 || status == 9"
                ><i class="fas fa-recycle"></i></a>
                <a href="#"
                   class="text-white"
                   title="手动回调"
                   v-if="status != 8"
                ><i class="fas fa-sync"></i></a>
                <a href="#"
                   class="text-white"
                   title="任务详情"
                   @click="toggleDetails()"
                ><i class="fas fa-ellipsis-h"></i></a>
            </div>
        </div>
    </div>
</template>
<script>
    import TaskDetails from "./TaskDetails.vue"
    import ReadMore from "./ReadMore.vue"

    export default {
        components: {
            ReadMore,
            TaskDetails
        },
        name: "TaskItem",
        props: {
            id: String,
            serviceName: String,
            sid: String,
            createdAt: Number,
            url: String,
            method: String,
            contentType: String,
            body: String,
            lastCallAt: Number,
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
        width 18px
</style>