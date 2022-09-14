import { Observable } from 'rxjs';

export interface DeactivatePreventer {
  canDeactivate(): boolean | Observable<boolean>;
}
