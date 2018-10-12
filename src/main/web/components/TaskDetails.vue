<template>
    <div class="card">
        <div class="card-header">
            Logs
        </div>
        <div class="card-body">
            <div class="row border border-success">
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">N:</div>
                    <div class="text-secondary ml-1">2</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Created At:</div>
                    <div class="text-secondary ml-1">2018-10-12T10:11:12</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Status:</div>
                    <div class="text-secondary ml-1">200</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Reason:</div>
                    <div class="text-secondary ml-1">200</div>
                </div>
                <template v-if="false">
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Response Headers:</div>
                        <div class="text-secondary ml-1">
                        <pre>Content-Encoding: gzip
Content-Length: 645
Content-Type: application/javascript; charset=utf-8
Date: Fri, 12 Oct 2018 10:39:56 GMT
Server: //srv.buysellads.com
Vary: Accept-Encoding</pre>
                        </div>
                    </div>
                    <div class="col-md-6 d-flex">
                        <div class="t-data-item-title">Response Body:</div>
                        <div class="text-secondary ml-1">200</div>
                    </div>
                </template>
                <div class="fixed-bottom">...</div>
            </div>
            <div class="row mt-2 border border-danger">
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">N:</div>
                    <div class="text-secondary ml-1">1</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Created At:</div>
                    <div class="text-secondary ml-1">2018-10-12T10:11:12</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Status:</div>
                    <div class="text-secondary ml-1">200</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Headers:</div>
                    <div class="text-secondary ml-1">200</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Response Body:</div>
                    <div class="text-secondary ml-1">200</div>
                </div>
                <div class="col-md-6 d-flex">
                    <div class="t-data-item-title">Reason:</div>
                    <div class="text-secondary ml-1">200</div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    export default {
        name: 'TaskDetails'
    }
</script>