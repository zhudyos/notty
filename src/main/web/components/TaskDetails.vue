<template>
    <div>
        <div class="d-block p-2 bg-dark text-white">Logs</div>
        <div class="container-fluid my-2">
            <div v-for="item in logs" class="row" :class="rowBorderClass(item)">
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">N:</div>
                    <div class="ml-1">{{item.n}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Created At:</div>
                    <div class="ml-1">{{item.created_at}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Status:</div>
                    <div class="ml-1">{{item.http_res_status}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Reason:</div>
                    <div class="ml-1">{{item.reason}}</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Headers:</div>
                    <div class="ml-1">
                        <read-more :max-chars="20" :text="item.http_res_headers"></read-more>
                    </div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Body:</div>
                    <div class="ml-1">
                        <read-more :max-chars="20" :text="item.http_res_body"></read-more>
                    </div>
                </div>
            </div>
            <p class="text-center" v-if="logs.length == 0">无</p>
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
                   this.logs = response.data
               }).catch(error => {

               })
            }
        }
    }
</script>