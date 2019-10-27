const JSON_EXTENSION = /^(.*)\.json$/;

const _fs = require('fs');
const _path = require('path');

function jsonFiles(dirName) {
  const fileList = _fs.readdirSync(_path.join('node_modules/cldr-data', dirName));

  return fileList.reduce((sum, file) => {
    if (JSON_EXTENSION.test(file)) {
      return sum.concat(file);
    }
  }, []);
}

function cldrData(loader, paths) {
  let jsonArray = '[';
  paths.forEach((path) => {
    const jsonFile = _path.resolve(`node_modules/cldr-data/${path}`);
    loader.addDependency(jsonFile);
    jsonArray += `${_fs.readFileSync(jsonFile, 'utf-8')},`;
  });
  jsonArray += ']';
  return jsonArray;
}

function buildPaths(source) {
  return source.split(/\r?\n/).reduce((allFiles, directory) => {
    const files = jsonFiles(directory);
    return allFiles.concat(files.map(jsonFile => _path.join(directory, jsonFile)));
  }, []);
}

module.exports = function (source) {
  const jsonArray = cldrData(this, buildPaths(source));
  return `export default ${jsonArray}`;
};

// todo #69: load only necessary files instead of the whole main  and supplement
