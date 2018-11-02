<template>
    <div class="container">
        <nav class="navbar navbar-expand-lg bg-dark">
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
</style>
