import {DebugElement} from '@angular/core';
import {ComponentFixture} from '@angular/core/testing';
import {By} from '@angular/platform-browser';

/**
 * Spec helpers for working with the DOM
 */

/**
 * Returns a selector for the `data-qe-id` attribute with the given attribute value.
 *
 * @param testId Test id set by `data-qe-id`
 *
 */
export function testIdSelector(testId: string): string {
  return `[data-qe-id="${testId}"]`;
}

/**
 * Dispatches a fake event (synthetic event) at the given element.
 *
 * @param element Element that is the target of the event
 * @param type Event name, e.g. `input`
 * @param bubbles Whether the event bubbles up in the DOM tree
 */
export function dispatchFakeEvent(
  element: EventTarget,
  type: string,
  bubbles: boolean = false,
): void {
  const event = new CustomEvent(type, {bubbles, cancelable: false});
  Object.defineProperty(event, 'value', {writable: false, value: 'Router'});

  element.dispatchEvent(event);
}

/**
 * Enters text into a form field (`input`, `textarea` or `select` element).
 * Triggers appropriate events so Angular takes notice of the change.
 * If you listen for the `change` event on `input` or `textarea`,
 * you need to trigger it separately.
 *
 * @param element Form field
 * @param value Form field value
 */
export function setFieldElementValue(
  element: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement,
  value: string,
): void {
  element.value = value;
  // Dispatch an `input` or `change` fake event
  // so Angular form bindings take notice of the change.
  dispatchFakeEvent(element, isSelect(element), !isSelect);
}

const isSelect = (element: HTMLElement) => {
  if (element instanceof HTMLSelectElement) {
    return 'change';
  }
  if (element.classList.contains("ant-select"))
  {
    return 'change';
  }
  return 'input';
}

/**
 * Sets the value of a form field with the given `data-qe-id` attribute.
 *
 * @param fixture Component fixture
 * @param testId Test id set by `data-qe-id`
 * @param value Form field value
 */
export function setFieldValue<T>(
  fixture: ComponentFixture<T>,
  testId: string,
  value: string,
): void {
  setFieldElementValue(findEl(fixture, testId).nativeElement, value);
}

/**
 * Finds a single element inside the Component by the given CSS selector.
 * Throws an error if no element was found.
 *
 * @param fixture Component fixture
 * @param selector CSS selector
 *
 */
export function queryByCss<T>(
  fixture: ComponentFixture<T>,
  selector: string,
): DebugElement {
  // The return type of DebugElement#query() is declared as DebugElement,
  // but the actual return type is DebugElement | null.
  // See https://github.com/angular/angular/issues/22449.
  const debugElement = fixture.debugElement.query(By.css(selector));
  // Fail on null so the return type is always DebugElement.
  if (!debugElement) {
    throw new Error(`queryByCss: Element with ${selector} not found`);
  }
  return debugElement;
}

/**
 * Finds an element inside the Component by the given `data-qe-id` attribute.
 * Throws an error if no element was found.
 *
 * @param fixture Component fixture
 * @param testId Test id set by `data-qe-id`
 *
 */
export function findEl<T>(fixture: ComponentFixture<T>, testId: string): DebugElement {
  return queryByCss<T>(fixture, testIdSelector(testId));
}

/**
 * Finds all elements with the given `data-qe-id` attribute.
 *
 * @param fixture Component fixture
 * @param testId Test id set by `data-qe-id`
 */
export function findEls<T>(fixture: ComponentFixture<T>, testId: string): DebugElement[] {
  return fixture.debugElement.queryAll(By.css(testIdSelector(testId)));
}
