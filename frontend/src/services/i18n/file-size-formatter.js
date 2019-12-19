export default function fileSizeFormatter() {
  return function fileSizeFormatterImpl(sizeInBytes, locale, arg) {
    if (arg !== 'pretty') {
      throw Error(`${arg} is not supported for file size formatter`);
    }

    // todo #76: pretty print
    return sizeInBytes.toString();
  };
}
