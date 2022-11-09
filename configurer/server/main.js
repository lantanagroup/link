// Use Express
var express = require("express");
const jsyaml = require('js-yaml');

// Use body-parser
var bodyParser = require("body-parser");
var fs = require('fs');
var path = require('path');



var directory = 'db/';

// Create new instance of the express server
var app = express();



// Define the JSON parser as a default way
// to consume and produce data through the
// exposed APIs
app.use(bodyParser.json());

// Create link to Angular build directory
// The `ng build` command will save the result
// under the `dist` folder.
var distDir = __dirname + "/client-dist/";
app.use(express.static(distDir));

// Init the server
var main = app.listen(process.env.PORT || 8080, function () {
  var port = main.address().port;
  console.log("App now running on port", port);
});

function getData(dataPath) {
  let data = undefined;
  if (fs.existsSync(dataPath)) {
    let yamlStr = jsyaml.safeLoad(fs.readFileSync(dataPath).toString());
    data = JSON.stringify(yamlStr);
  }
  return data;
}

function updateData(data, dataPath) {
  if (!fs.existsSync('db/')) {
    fs.mkdirSync('db/');
  }
  // let yamlStr = jsyaml.safeDump(data);
  console.log(data);
  fs.writeFileSync(dataPath,  data);
}


app.get('/api/config/:configType', function(req, res) {
  let dataPath = directory + req.params.configType + '.yml';
  console.log(dataPath);
  var data = getData(dataPath);
  console.log(data);
  res.send(data);
});

app.post('/api/config/:configType', function (req, res) {
  let dataPath = directory + req.params.configType + '.yml';
  updateData(req.body.api, dataPath);
  res.send();
});

app.delete('/api/config/:configType', function(req, res) {
  let dataPath = directory + req.params.configType + '.yml';
  fs.unlink(dataPath, function (err) {
    if (err) throw err;
    // if no error, file has been deleted successfully
    console.log('File deleted!');
});
  res.send();
});


app.get('*', (req,res) => {
  res.sendFile(path.join(__dirname, '/client-dist/index.html'));
});


