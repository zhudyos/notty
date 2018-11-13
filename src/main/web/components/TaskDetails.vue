<template>
    <div>
        <div class="d-block p-2 bg-dark text-white">Logs</div>
        <div class="container-fluid my-2">
            <div v-for="item in logs" class="row" :class="rowBorderClass(item)">
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">N:</div>
                    <div class="ml-1" title="第N次回调">{{item.n}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Created At:</div>
                    <div class="ml-1" title="回调时间">{{$dayjs(item.created_at).format("YYYY-MM-DD HH:mm:ss")}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Status:</div>
                    <div class="ml-1" title="回调响应HTTP状态">{{item.http_res_status}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Reason:</div>
                    <div class="ml-1" title="失败原因">{{item.reason}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Headers:</div>
                    <div class="ml-1" title="回调响应HTTP Header">
                        <read-more :max-chars="20" :text="item.http_res_headers"></read-more>
                    </div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Body:</div>
                    <div class="ml-1" title="回调响应HTTP Body">
                        <read-more :max-chars="20" :text="item.http_res_body"></read-more>
                    </div>
                </div>
            </div>
            <p class="text-center" v-if="logs.length == 0">无</p>
            <div v-if="enough" class="d-flex justify-content-center mt-1">
                <a href="#" @click="loadMore">加载更多...</a>
            </div>
        </div>
    </div>
</template>
<script>
    import axios from "axios"
    import ReadMore from "./ReadMore.vue"

    export default {
        components: {ReadMore},
        name: 'TaskDetails',
        props: {
            id: String
        },
        data: () => ({
            logs: [],
            page: 1,
            size: 20,
            enough: true
        }),
        mounted() {
            this.loadMore()
        },
        methods: {
            rowBorderClass(log) {
                if (log.success) {
                    return "border border-success"
                }
                return "border border-danger"
            },
            loadMore() {
                if (!this.enough) {
                    return
                }

                axios.get(`/api/v1/tasks/${this.id}/logs?page=${this.page}&size=${this.size}`).then(response => {
                    let logs = response.data
                    if (logs.length < this.size) {
                        this.enough = false
                    }

                    this.logs = this.logs.concat(logs)
                    this.page = this.page + 1
                }).catch(error => {
                    this.$alert.error("加载回调日志失败")
                    console.error(error)
                })
            }
        }
    }
</script>