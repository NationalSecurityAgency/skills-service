import DOMPurify from 'dompurify';

export default class InputSanitizer {
  static sanitize(input) {
    if (input) {
      return DOMPurify.sanitize(input);
    }
    return input;
  }

  static removeSpecialChars(input) {
    return input.replace(/[\W_]/gi, '');
  }
}
