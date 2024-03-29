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
    const decoder = new TextDecoder('utf-8');
    return decoder.decode(new Uint8Array(value));
  } else {
    // It's already a string, return as is
    return value;
  }
}

/**
 * Get a property from an object.
 * If the object has the property, return the property value.
 * Otherwise, return null.
 *
 * @param obj
 * @param key
 */
export const getObjProp= (obj: any, key: string): any => {
  if (Object.prototype.hasOwnProperty.call(obj, key)) {
    return obj[key];
  }
  return null;
}
