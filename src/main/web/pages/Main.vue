<template>
    <div class="container">
        <div class="alert alert-success alert-pos" role="alert" v-if="$root.alert.success">
            {{$root.alert.message}}
        </div>
        <div class="alert alert-danger alert-pos" role="alert" v-if="$root.alert.error">
            {{$root.alert.message}}
        </div>
        <nav class="navbar navbar-expand-lg bg-dark logo">
            <a class="navbar-brand text-white" href="#">Notty</a>
            <div class="mr-auto"></div>
            <form class="form-inline mt-2 mt-md-0">
                <input class="form-control mr-sm-2" type="text" placeholder="Search" aria-label="Search">
                <button class="btn btn-secondary my-2 my-sm-0" type="submit">搜 索</button>
            </form>
        </nav>
        <task-list></task-list>
    </div>
</template>
<script>
    import TaskList from "../components/TaskList.vue"

    export default {
        components: {
            TaskList
        },
        name: 'Main'
    }
</script>
<style lang="stylus">
    @import "../task.styl"

    .logo
        background-image url("../public/img/logo.png")
        background-repeat no-repeat
        background-position 10px center
        background-size 32px
        padding-left 48px

    .alert-pos
        position absolute
        z-index 1000
        right 10px
        top 10px
</style>
