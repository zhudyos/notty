const path = require('path')

function resolve(dir) {
    return path.join(__dirname, dir)
}

module.exports = {
    pages: {
        index: {
            entry: 'src/main/web/main.js',
            template: 'src/main/web/index.html',
            filename: 'index.html'
        }
    },
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:7779'
            }
        }
    }
}
