import Vue from 'vue'
import dayjs from "dayjs"
import './public/css/bootstrap.css'
import '@fortawesome/fontawesome-free/css/all.css'

import Main from './pages/Main.vue'

/**
 *
 * @type {((config?: dayjs.ConfigType, option?: dayjs.OptionType) => dayjs.Dayjs) | dayjs}
 */
Vue.prototype.$dayjs = dayjs

/**
 *
 */
const root = new Vue({
    el: '#app',
    data: {
        alert: {
            message: "这是一个测试",
            success: false,
            error: false
        }
    },
    render: h => h(Main)
})

const $alert = {
    success: (message) => {
        let a = root.alert
        a.message = message
        a.success = true

        setTimeout(() => {
            a.success = false
        }, 3000)
    },
    error: (message) => {
        let a = root.alert
        a.message = message
        a.error = true

        setTimeout(() => {
            a.error = false
        }, 3000)
    }
}

/**
 *
 * @type {{success: $alert.success, error: $alert.error}}
 */
Vue.prototype.$alert = $alert
