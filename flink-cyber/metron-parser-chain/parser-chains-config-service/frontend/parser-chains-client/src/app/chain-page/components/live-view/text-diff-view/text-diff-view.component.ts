import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MonacoDiffEditorConstructionOptions} from "@materia-ui/ngx-monaco-editor/lib/interfaces";

@Component({
    selector: 'app-text-diff-view',
    templateUrl: './text-diff-view.component.html',
    styleUrls: ['./text-diff-view.component.scss']
})
export class TextDiffViewComponent implements OnInit {

    @Input() originalModel: string;
    @Input() modifiedModel: string;
    @Output() expectedValueChange = new EventEmitter<void>();

    diffOptions: MonacoDiffEditorConstructionOptions = {theme: "vs", automaticLayout: true, readOnly: true, renderSideBySide: true};


    public ngOnInit() {

    }

    onChangeInline(checked) {
        this.diffOptions = Object.assign({}, this.diffOptions, {renderSideBySide: !checked});
    }

    updateExpectedValueButtonClick($event: MouseEvent) {
        this.expectedValueChange.emit()
    }
}
