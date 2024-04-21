import {Component} from '@angular/core';
import {select, Store} from "@ngrx/store";
import {DiffPopupState} from "./diff-popup.reducers";
import {HideDiffModalAction} from "./diff-popup.actions";
import {Observable} from "rxjs";
import {getDiffModalVisible, getNewDiffValue, getPreviousDiffValue} from "./diff-popup.selectors";

@Component({
    selector: 'app-diff-popup',
    templateUrl: './diff-popup.component.html',
    styleUrls: ['./diff-popup.component.scss']
})
export class DiffPopupComponent {
    diffModalVisible$: Observable<boolean>;
    previousDiffValue$: Observable<string>;
    newDiffValue$: Observable<string>;

    constructor(private _store: Store<DiffPopupState>) {
        this.diffModalVisible$ = _store.pipe(select(getDiffModalVisible));
        this.previousDiffValue$ = _store.pipe(select(getPreviousDiffValue));
        this.newDiffValue$ = _store.pipe(select(getNewDiffValue));
    }

    handleOkModal() {
        this._store.dispatch(HideDiffModalAction());
    }

    handleCancelModal() {
        this._store.dispatch(HideDiffModalAction());
    }
}
