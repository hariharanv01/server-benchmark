const http = require("http");
const url = require("url");

function start() {
  let server = http.createServer((req, res) => {
    res.writeHead(200);
    res.end(url.parse(req.url).pathname);
  });
  server.listen(8004);
}

start();
