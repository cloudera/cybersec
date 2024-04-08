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
export const getObjProp = (obj: any, key: string): any => {
  if (Object.prototype.hasOwnProperty.call(obj, key)) {
    return obj[key];
  }
  return null;
}

/**
 * Checks if two values are objects.
 *
 * @param object - The value to check.
 * @return True if the value is an object and not null; otherwise, false.
 */
export function isObject(object: any): boolean {
  return object != null && typeof object === 'object';
}

/**
 * Recursively checks if two provided objects are deeply equal.
 * This function compares the objects' properties and their values, descending into nested
 * objects to perform a thorough equality check. It does not handle cyclical references.
 *
 * @param object1 - The first object to compare.
 * @param object2 - The second object to compare.
 * @return  True if both objects are deeply equal; otherwise, false.
 */
export function deepEqual(object1: any, object2: any): boolean {
  const keys1 = Object.keys(object1);
  const keys2 = Object.keys(object2);
  // Check if both objects have the same number of properties
  if (keys1.length !== keys2.length) {
    return false;
  }
  // Iterate through the properties of the first object
  for (const key of keys1) {
    const val1 = object1[key];
    const val2 = object2[key];
    const areObjects = isObject(val1) && isObject(val2);
    if (areObjects && !deepEqual(val1, val2) || !areObjects && val1 !== val2) {
      return false;
    }
  }
  return true;
}

export function findValues<T>(obj: Record<string, any>, searchKey: string): T[] {
  return Object.entries(obj).reduce<T[]>((values, [key, value]) => {
    if (key === searchKey) {
      return [...values, value as T];
    } else if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      return [...values, ...findValues<T>(value, key)];
    }
    return values;
  }, []);
}
