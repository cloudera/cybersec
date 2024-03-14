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

    constructor(private store: Store<DiffPopupState>) {
        this.diffModalVisible$ = store.pipe(select(getDiffModalVisible));
        this.previousDiffValue$ = store.pipe(select(getPreviousDiffValue));
        this.newDiffValue$ = store.pipe(select(getNewDiffValue));
    }

    handleOkModal() {
        this.store.dispatch(HideDiffModalAction());
    }

    handleCancelModal() {
        this.store.dispatch(HideDiffModalAction());
    }
}
