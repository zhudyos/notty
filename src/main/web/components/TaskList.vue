<template>
    <div class="container task-container">
        <task-item v-for="item in items"
                   :id="item.id"
                   :service-name="item.service_name"
                   :sid="item.sid"
                   :created-at="item.created_at"
                   :url="item.cb_url"
                   :method="item.cb_method"
                   :content-type="item.cb_content_type"
                   :body="item.cb_data"
                   :last-call-at="item.last_call_at"
                   :retry-count="item.retry_count"
                   :retry-max-count="item.retry_max_count"
                   :status="item.status"
        ></task-item>
    </div>
</template>
<script>
    import axios from 'axios'
    import TaskItem from "./TaskItem.vue"

    export default {
        components: {TaskItem},
        name: 'TaskList',
        data: () => ({
            items: [],
            page: 1,
            size: 20,
            enough: true
        }),
        mounted() {
            this.loadMore()
        },
        methods: {
            loadMore() {
                if (!this.enough) {
                    return
                }

               axios.get(`/api/v1/tasks?page=${this.page}&size=${this.size}`).then(response => {
                   this.items = response.data
               }).catch(error => {

               })
            }
        }
    }
</script>
<style lang="stylus">
    .task-container
        font-size 12px
        font-family Arial, sans-serif
</style>