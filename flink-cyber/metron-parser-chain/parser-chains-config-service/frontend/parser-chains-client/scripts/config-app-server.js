const path = require("path");
const html = path.join(__dirname, "../dist/parser-chaining/");
const bodyParser = require("body-parser");
const compression = require("compression");
const express = require("express");
const { createProxyMiddleware } = require('http-proxy-middleware');

const PORT = process.env.PORT || 4200;
const REST_URL = process.env.REST_URL || 'localhost:3000';

const app = express();

app.use('/api', createProxyMiddleware({ target: 'http://' + REST_URL, changeOrigin: false }));

app
  .use(compression())
  .use(bodyParser.json())
  .use(express.static(html))
  .use(function(req, res) {
    res.sendFile(html + "index.html");
  })
  .listen(PORT, function() {
    console.log("Parser Configuration UI is listening on port " + PORT);
    console.log(`
      You can reach our UI by using CMD + Click on the link below
      or copying it to your browser.
      http://localhost:${PORT}
    `);
  });
