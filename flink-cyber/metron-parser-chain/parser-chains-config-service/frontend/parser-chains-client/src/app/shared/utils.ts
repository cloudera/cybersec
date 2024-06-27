import {Observable, of} from 'rxjs';
import {catchError, switchMap, take} from 'rxjs/operators';
import {AsyncValidatorFn, FormArray, FormControl, FormGroup, ValidationErrors, ValidatorFn} from '@angular/forms';

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

export function findValues<T>(obj: object, searchKey: string): T[] {
  if (obj === null) {
    return [] as T[];
  }
  if (Array.isArray(obj)) {
    return obj.reduce<T[]>((values, value) => {
      return [...values, ...findValues<T>(value, searchKey)];
    }, []);
  }
  return Object.entries(obj).reduce<T[]>((values, [key, value]) => {
    if (key === searchKey) {
      return [...values, value as T];
    } else if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      return [...values, ...findValues<T>(value, key)];
    }
    return values;
  }, []);
}

export function sortBy<T>(arr: T[], field: keyof T, order: SortOrder = 'asc'): T[] {
  return arr.slice().sort((a: T, b: T) => {
    const valueA = a[field];
    const valueB = b[field];

    if (typeof valueA === 'string' || typeof valueA === 'number') {
      const comparisonValue = valueA > valueB ? 1 : -1;
      const sortMultiplier = order === 'asc' ? 1 : -1;
      return comparisonValue * sortMultiplier;
    }
    throw new Error(`Unsupported data type for field '${String(field)}'.`);
  });
}

export const uniqueAsyncValidator: UniqueAsyncValidatorType = (existValues, column?) => (control) => existValues.pipe(
  switchMap(values => unique(control.value, column)(values) ? of({uniqueValue: true}) : of(null)),
  catchError(() => of({httpError: true})),
  take(1)
);

export const uniqueValidator: UniqueValidatorType = (existValues, column?) => (control): ValidationErrors | null => {
  for (const existValue of existValues) {
    if (!!column && existValue[column] === control.value) {
      return {uniqueValue: true};
    } else if (existValue === control.value) {
      return {uniqueValue: true};
    }
  }
  return null
}

export function isExist<T>(items: T[], value: string, column: keyof T) {
  return items.some(item => {
    const itemValue = column ? String(item[column]).toLowerCase() : String(item).toLowerCase();
    return itemValue === value.toLowerCase();
  });
}


export const changeStateFn = <T>(value: any, key?: keyof T, newValue?: any) =>
  (state: T[]): T[] => state.map(mapState(key, value, newValue)).filter(item => item !== null);

function mapState<T>(key: keyof T, value: any, newValue?: any) {
  return (item: T) => {
    const condition = key ? item[key] === value : item === value;
    if (condition) {
      return newValue ? newValue : null;
    }
    return item;
  };
}

export const unique: UniqueFunction = (value, column) => (items) => isExist(items, value, column);

export type UniqueAsyncValidatorType = <T>(existingValues: Observable<T[]>, column?: keyof T) => AsyncValidatorFn;
export type UniqueValidatorType = <T>(existingValues: T[], column?: keyof T) => ValidatorFn;
export type UniqueFunction = <T>(value: string, column?: keyof T) => (items: T[]) => boolean;
export type SortOrder = 'asc' | 'desc';

export type TypedFormControls<T extends Record<string, any>> = {
  [K in keyof T]-?: T[K] extends (infer R)[]
    ? FormArray<R extends Record<any, any> ? FormGroup<TypedFormControls<R>> : FormControl<R>>
    : T[K] extends Record<any, any>
      ? FormGroup<TypedFormControls<T[K]>>
      : FormControl<T[K]>;
};
