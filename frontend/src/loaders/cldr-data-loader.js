var JSON_EXTENSION = /^(.*)\.json$/;

var _fs = require("fs");
var _path = require("path");

function jsonFiles(dirName) {
  let fileList = _fs.readdirSync(_path.join('node_modules/cldr-data', dirName))

  return fileList.reduce(function (sum, file) {
    if (JSON_EXTENSION.test(file)) {
      return sum.concat(file);
    }
  }, []);
}

function cldrData(loader, paths) {
  let jsonArray = "["
  paths.forEach(path => {
    let jsonFile = _path.resolve("node_modules/cldr-data/" + path);
    loader.addDependency(jsonFile);
    jsonArray += _fs.readFileSync(jsonFile, 'utf-8') + ","
  })
  jsonArray += "]"
  return jsonArray
}

function buildPaths(source) {
  return source.split(/\r?\n/).reduce(function (allFiles, directory) {
    let files = jsonFiles(directory)
    return allFiles.concat(files.map(function (jsonFile) {
      return _path.join(directory, jsonFile);
    }));
  }, []);
}

module.exports = function (source) {
  let jsonArray = cldrData(this, buildPaths(source));
  return 'export default ' + jsonArray;
}

// todo #69: load only necessary files instead of the whole main  and supplement