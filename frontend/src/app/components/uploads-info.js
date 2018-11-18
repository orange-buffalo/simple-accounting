let idCounter = 0;

export const UploadInfo = function () {
  this.id = idCounter++
  this.document = null
  this.file = null
  this.progress = 0
  this.notes = null
  this.uploadError = null

  this.isFileSelected = () => {
    return this.file != null
  }

  this.isEmpty = () => {
    return !this.notes && !this.isFileSelected()
  }

  this.isDocumentUploaded = () => {
    return this.document !== null
  }

  this.isDocumentUploadFailed = () => {
    return this.uploadError !== null
  }

  this.validate = function (callback) {
    if (this.notes && this.notes.length > 1024) {
      callback(new Error("Too long"))
    }
    else if (this.notes && !this.isFileSelected()) {
      callback(new Error("Please select a file"))
    }
    else {
      callback()
    }
  }
}

export const UploadsInfo = function () {
  this.uploads = []

  this.add = function () {
    this.uploads.push(new UploadInfo())
  }

  this.executeIfUploaded = function (onSuccess, onFailure) {
    let successfulCount = 0
    let failedCount = 0
    this.uploads.forEach((upload) => {
      if (upload.isDocumentUploaded() || upload.isEmpty()) {
        successfulCount++
      }
      else if (upload.isDocumentUploadFailed()) {
        failedCount++
      }
    })

    if (successfulCount + failedCount === this.uploads.length) {
      if (failedCount === 0) {
        onSuccess()
      }
      else {
        onFailure()
      }
    }
  }

  this.ensureCompleteness = function () {
    if (this.uploads[this.uploads.length - 1].isFileSelected()) {
      this.add()
    }

    if (this.uploads.length > 1) {
      for (let i = 0; i < this.uploads.length - 1; i++) {
        if (this.uploads[i].isEmpty()) {
          this.uploads.splice(i, 1)
        }
      }
    }
  }

  this.getDocumentsIds = function () {
    return this.uploads
        .filter(upload => !upload.isEmpty())
        .filter(upload => upload.isDocumentUploaded())
        .map(upload => upload.document.id)
  }
}