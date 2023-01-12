// Use Express
var express = require("express");
const jsyaml = require('js-yaml');


// Use body-parser
var bodyParser = require("body-parser");
var fs = require('fs');
var path = require('path');
var multer = require('multer');


var DIRECTORY = 'db/';


// Create new instance of the express server
var app = express();


let storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, DIRECTORY);
  },
  filename: (req, file, cb) => {
    cb(null, file.fieldname + path.extname(file.originalname));
  }
});

let upload = multer({
  storage: storage
});


// Define the JSON parser as a default way
// to consume and produce data through the
// exposed APIs
app.use(bodyParser.json());

// Create link to Angular build directory
// The `ng build` command will save the result
// under the `dist` folder.
// var distDir = __dirname + "/client-dist/";
app.use('/', express.static(path.join(__dirname + "/client-dist/")));
app.use('/configurer', express.static(path.join(__dirname + "/client-dist/")));

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
  fs.writeFileSync(dataPath, data);
}


app.get('/configurer/api', function (req, res) {
  let data = "Up and Running";
  res.send(data);
});

app.get('/configurer/api/config/:configType', function (req, res) {
  let dataPath = DIRECTORY + req.params.configType + '.yml';
  console.log(dataPath);
  let data = getData(dataPath);
  console.log(data);
  res.send(data);
});

app.post('/configurer/api/config/:configType', function (req, res) {
  let dataPath = DIRECTORY + req.params.configType + '.yml';
  updateData(req.body.api, dataPath);
  res.send();
});

app.delete('/configurer/api/config/:configType', function (req, res) {
  let dataPath = DIRECTORY + req.params.configType + '.yml';
  fs.unlink(dataPath, function (err) {
    if (err) throw err;
    // if no error, file has been deleted successfully
    console.log('File deleted!');
  });
  res.send();
});

// Upload Api File
app.post('/configurer/api/upload/api', upload.single('api'), function (req, res) {

  if (!req.file) {
    console.log("No file is available!");
    return res.send({
      success: false
    });

  } else {
    console.log('File is available!');
    return res.send({
      success: true
    })
  }
});

// Upload web File
app.post('/configurer/api/upload/web', upload.single('web'), function (req, res) {

  if (!req.file) {
    console.log("No file is available!");
    return res.send({
      success: false
    });

  } else {
    console.log('File is available!');
    return res.send({
      success: true
    })
  }
});

// Upload Consumer File
app.post('/configurer/api/upload/consumer', upload.single('consumer'), function (req, res) {

  if (!req.file) {
    console.log("No file is available!");
    return res.send({
      success: false
    });

  } else {
    console.log('File is available!');
    return res.send({
      success: true
    })
  }
});

app.get('*', (req, res) => {
  console.log("Request URL: " + req.url)
  res.sendFile(path.join(__dirname, '/client-dist/index.html'));
});


