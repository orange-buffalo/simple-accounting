import {isNil} from 'lodash'

let idCounter = 0;

export const UploadInfo = function () {
  this.id = idCounter++
  this.document = null
  this.file = null
  this.progress = 0
  this.notes = null
  this.uploadError = null
  this.name = null
  this.size = null

  this.isFileSelected = () => {
    return !isNil(this.file)
  }

  this.isEmpty = () => {
    return !this.notes && !this.isFileSelected() && !this.isDocumentUploaded()
  }

  this.isDocumentUploaded = () => {
    return !isNil(this.document)
  }

  this.selectFile = (file) => {
    this.file = file
    this.name = file.name
    this.size = file.size
  }

  this.setDocument = (document) => {
    this.document = document
    this.name = isNil(document) ? null : document.name
    this.size = isNil(document) ? null : document.sizeInBytes
    this.notes = isNil(document) ? null : document.notes
  }

  this.isDocumentUploadFailed = () => {
    return this.uploadError !== null
  }

  this.validate = (callback) => {
    if (this.notes && this.notes.length > 1024) {
      callback(new Error("Too long"))
    } else {
      callback()
    }
  }

  this.clear = () => {
    this.file = null
    this.document = null
    this.name = null
    this.size = null
    this.notes = null
  }

  this.hasNotes = () => {
    return !isNil(this.notes)
  }
}

export const UploadsInfo = function () {
  this.uploads = []

  this.add = function (document) {
    let uploadInfo = new UploadInfo()
    uploadInfo.setDocument(document)
    this.uploads.push(uploadInfo)
  }

  this.executeIfUploaded = function (onSuccess, onFailure) {
    let successfulCount = 0
    let failedCount = 0
    this.uploads.forEach((upload) => {
      if (upload.isDocumentUploaded() || upload.isEmpty()) {
        successfulCount++
      } else if (upload.isDocumentUploadFailed()) {
        failedCount++
      }
    })

    if (successfulCount + failedCount === this.uploads.length) {
      if (failedCount === 0) {
        onSuccess()
      } else {
        onFailure()
      }
    }
  }

  this.ensureCompleteness = function () {
    if (this.uploads[this.uploads.length - 1].isFileSelected()
        || this.uploads[this.uploads.length - 1].isDocumentUploaded()) {
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