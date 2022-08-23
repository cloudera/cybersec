import { CanDeactivateComponent } from './can-deactivate-component';
import { DeactivatePreventer } from './deactivate-preventer.interface';

describe('CanDeactivateComponent', () => {
  it('should prevent deactivation', () => {
    class Preventer implements DeactivatePreventer {
      canDeactivate() {
        return false;
      }
    }
    const canDeactivateComponent = new CanDeactivateComponent();
    const preventer = new Preventer();
    expect(canDeactivateComponent.canDeactivate(preventer))
      .toBe(false);
  });

  it('should not prevent deactivation', () => {
    class Preventer implements DeactivatePreventer {
      canDeactivate() {
        return true;
      }
    }
    const canDeactivateComponent = new CanDeactivateComponent();
    const preventer = new Preventer();
    expect(canDeactivateComponent.canDeactivate(preventer))
      .toBe(true);
  });
});
