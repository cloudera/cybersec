/**
 * Convert a string or ArrayBuffer to a string.
 * If the input is null, return an empty string.
 * If the input is an ArrayBuffer, decode it as UTF-8.
 * Otherwise, return the input as is.
 *
 * @param value Input value to convert to a string
 */
export const convertToString = (value: string | ArrayBuffer | null): string => {
  if (value === null) {
    // Convert null to an empty string or any other default string representation
    return '';
  } else if (value instanceof ArrayBuffer) {
    // Convert ArrayBuffer to string
    // Assuming the ArrayBuffer contains text data encoded in UTF-8
    let decoder = new TextDecoder('utf-8');
    return decoder.decode(new Uint8Array(value));
  } else {
    // It's already a string, return as is
    return value;
  }
}
