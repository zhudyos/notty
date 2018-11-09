import Vue from 'vue'
import dayjs from "dayjs"
import './public/css/bootstrap.css'
import '@fortawesome/fontawesome-free/css/all.css'

import Main from './pages/Main.vue'

Vue.prototype.$dayjs = dayjs

new Vue({
    el: '#app',
    render: h => h(Main)
})