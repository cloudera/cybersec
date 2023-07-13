import {Component, Input, OnInit} from '@angular/core';
import {MonacoDiffEditorConstructionOptions} from "@materia-ui/ngx-monaco-editor/lib/interfaces";

@Component({
    selector: 'app-text-diff-view',
    templateUrl: './text-diff-view.component.html',
    styleUrls: ['./text-diff-view.component.scss']
})
export class TextDiffViewComponent implements OnInit {

    @Input() originalModel: string;
    @Input() modifiedModel: string;

    diffOptions: MonacoDiffEditorConstructionOptions = {theme: "vs", readOnly: true, renderSideBySide: true};


    public ngOnInit() {

    }

    onChangeInline(checked) {
        this.diffOptions = Object.assign({}, this.diffOptions, {renderSideBySide: !checked});
    }
}
