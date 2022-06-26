const translationFilesDeferred = import.meta.glob('./*.ts');
for (const path in translationFilesDeferred) {
  if (Object.prototype.hasOwnProperty.call(translationFilesDeferred, path)) {
    translationFilesDeferred[path.replace('./', '')
      .replace('.ts', '')] = translationFilesDeferred[path];
    delete translationFilesDeferred[path];
  }
}

export default translationFilesDeferred;
