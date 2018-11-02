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
                    <div class="ml-1">{{item.createdAt}}</div>
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
                        <pre class="text-white mb-0"><read-more :max-chars="20"
                                                                :text="item.http_res_headers"></read-more></pre>
                    </div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Body:</div>
                    <div class="ml-1">
                        <pre class="text-white mb-0"><read-more :max-chars="20"
                                                                :text="item.http_res_body"></read-more></pre>
                    </div>
                </div>
            </div>
            <p class="text-center" v-if="logs.length == 0">无</p>
        </div>
    </div>
</template>
<script>
    import ReadMore from "./ReadMore.vue";

    export default {
        components: {ReadMore},
        name: 'TaskDetails',
        props: {
            id: String
        },
        data: () => ({
            logs: []
        }),
        computed: {
            rowBorderClass(log) {
                if (log.success) {
                    return "border border-success"
                }
                return "border border-danger"
            }
        }
    }
</script>