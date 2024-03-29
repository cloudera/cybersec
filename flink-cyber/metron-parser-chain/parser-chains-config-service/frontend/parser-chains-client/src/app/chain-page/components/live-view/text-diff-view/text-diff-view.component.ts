import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {DiffEditorModel} from "ngx-monaco-editor-v2";

@Component({
    selector: 'app-text-diff-view',
    templateUrl: './text-diff-view.component.html',
    styleUrls: ['./text-diff-view.component.scss']
})
export class TextDiffViewComponent implements OnChanges {

    @Input() originalModelJson: string;
    @Input() modifiedModelJson: string;

    originalModel: DiffEditorModel;
    modifiedModel: DiffEditorModel;

    diffOptions = {theme: "vs", automaticLayout: true, readOnly: true, renderSideBySide: true};

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.originalModelJson !== undefined) {
            this.originalModel = {code: changes.originalModelJson.currentValue} as DiffEditorModel;
        }
        if (changes.modifiedModelJson !== undefined) {
            this.modifiedModel = {code: changes.modifiedModelJson.currentValue} as DiffEditorModel;
        }
    }

    onChangeInline(checked) {
        this.diffOptions = Object.assign({}, this.diffOptions, {renderSideBySide: !checked});
    }
}
