const path = require('path')
const CopyWebpackPlugin = require('copy-webpack-plugin')

function resolve(dir) {
    return path.join(__dirname, dir)
}

module.exports = {
    configureWebpack: {
        plugins: [
            new CopyWebpackPlugin([{
                from: 'src/main/web/public/img/logo.png',
                to: 'logo.png'
            }])
        ]
    },
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
