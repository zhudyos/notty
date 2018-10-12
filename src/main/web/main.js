import Vue from 'vue'
import 'bootstrap/dist/css/bootstrap.css'
import '@fortawesome/fontawesome-free/css/all.css'

import Main from './pages/Main.vue'

new Vue({
    el: '#app',
    render: h => h(Main)
})