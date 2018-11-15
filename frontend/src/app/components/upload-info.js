let idCounter = 0;

export const UploadInfo = function () {
  this.id = idCounter++
  this.document = null
  this.file = null
  this.progress = 0
  this.notes = null

  this.isFileSelected = () => {
    return this.file != null
  }

  this.isEmpty = () => {
    return !this.notes && !this.isFileSelected()
  }

  this.clearFile = () => {
    this.file = null
  }
}

export default UploadInfo