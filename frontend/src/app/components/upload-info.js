export const UploadInfo = function () {
  this.document = null
  this.file = null
  this.progress = 0

  this.selected = () => {
    return typeof this.file !== 'undefined'
  }

}

export default UploadInfo