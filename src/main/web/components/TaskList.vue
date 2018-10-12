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
            items: [
                {
                    id: "20181012d41d8cd98f00b204e9800998ecf8427e",
                    service_name: "hello-world",
                    sid: "d41d8cd98f00b204e9800998ecf8427e",
                    created_at: "2018-10-11T15:52:00",
                    cb_url: "https://www.2339.com/9699?clickfrom=1",
                    last_call_at: "2018-10-11T15:52:11",
                    retry_count: 10,
                    retry_max_count: 999,
                    cb_method: "GET",
                    cb_content_type: "application/json; charset=utf-8",
                    status: 1,
                    cb_data: `{ "menu":{ "id":"file", "value":"File", "popup":{ "menuitem":[ { "value":"New", "onclick":"CreateNewDoc()" }, { "value":"Open", "onclick":"OpenDoc()" }, { "value":"Close", "onclick":"CloseDoc()" } ] } } }`
                },
                {
                    id: "20181012d41d8cd98f00b204e9800998ecf8427e",
                    service_name: "hello-world",
                    sid: "d41d8cd98f00b204e9800998ecf8427e",
                    created_at: "2018-10-11T15:52:00",
                    cb_url: "https://www.2339.com/9699?clickfrom=1",
                    last_call_at: "2018-10-11T15:52:11",
                    retry_count: 10,
                    retry_max_count: 999,
                    cb_method: "GET",
                    cb_content_type: "application/json; charset=utf-8",
                    status: 7,
                    cb_data: `{ "menu":{ "id":"file", "value":"File", "popup":{ "menuitem":[ { "value":"New", "onclick":"CreateNewDoc()" }, { "value":"Open", "onclick":"OpenDoc()" }, { "value":"Close", "onclick":"CloseDoc()" } ] } } }`
                },
                {
                    id: "20181012d41d8cd98f00b204e9800998ecf8427e",
                    service_name: "hello-world",
                    sid: "d41d8cd98f00b204e9800998ecf8427e",
                    created_at: "2018-10-11T15:52:00",
                    cb_url: "https://www.2339.com/9699?clickfrom=1",
                    last_call_at: "2018-10-11T15:52:11",
                    retry_count: 10,
                    retry_max_count: 999,
                    cb_method: "GET",
                    cb_content_type: "application/json; charset=utf-8",
                    status: 8,
                    cb_data: `{ "menu":{ "id":"file", "value":"File", "popup":{ "menuitem":[ { "value":"New", "onclick":"CreateNewDoc()" }, { "value":"Open", "onclick":"OpenDoc()" }, { "value":"Close", "onclick":"CloseDoc()" } ] } } }`
                },
                {
                    id: "20181012d41d8cd98f00b204e9800998ecf8427e",
                    service_name: "hello-world",
                    sid: "d41d8cd98f00b204e9800998ecf8427e",
                    created_at: "2018-10-11T15:52:00",
                    cb_url: "https://www.2339.com/9699?clickfrom=1",
                    last_call_at: "2018-10-11T15:52:11",
                    retry_count: 10,
                    retry_max_count: 999,
                    cb_method: "GET",
                    cb_content_type: "application/json; charset=utf-8",
                    status: 9,
                    cb_data: `{ "menu":{ "id":"file", "value":"File", "popup":{ "menuitem":[ { "value":"New", "onclick":"CreateNewDoc()" }, { "value":"Open", "onclick":"OpenDoc()" }, { "value":"Close", "onclick":"CloseDoc()" } ] } } }`
                }
            ],
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

//                axios.get("/api/v1/tasks").then(response => {
//
//                }).catch(error => {
//
//                })
            }
        }
    }
</script>
<style lang="stylus">
    .task-container
        font-size 12px
        font-family Arial, sans-serif
</style>