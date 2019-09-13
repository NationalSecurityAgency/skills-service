import DOMPurify from 'dompurify';

export default class InputSanitizer {
  static sanitize(input) {
    if (input) {
      return DOMPurify.sanitize(input);
    }
    return input;
  }
}
