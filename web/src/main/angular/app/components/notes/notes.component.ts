import {Component, Input, OnInit} from '@angular/core';
import {IExtension, IMeasureReport} from "../../fhir";

@Component({
    selector: 'notes',
    templateUrl: './notes.component.html',
    styleUrls: ['./notes.component.css']
})
export class NotesComponent implements OnInit {
    private static readonly notesUrl = 'https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-report-note';
    @Input() measureReport: IMeasureReport;

    constructor() {
    }

    get notes(): string {
        const extensions = this.getExtensions();
        if (!extensions) return;
        let notesExtension = extensions.find(e => e.url === NotesComponent.notesUrl);
        if (notesExtension) {
            return notesExtension.valueMarkdown;
        }
        return "";
    }

    set notes(value: string) {
        const extensions = this.getExtensions();
        const notesExt = {
            url: NotesComponent.notesUrl,
            valueMarkdown: value
        } as IExtension;

        let notesExtension = extensions.find(e => e.url === NotesComponent.notesUrl);

        if (!value && notesExtension) {
            extensions.splice(extensions.indexOf(notesExt), 1);
            if (extensions.length === 0) delete this.measureReport.extension;
        } else if (!notesExtension && value) {
            extensions.push(notesExt);
        } else {
            notesExtension.valueMarkdown = value;
        }
    }

    async ngOnInit() {

    }

    ngOnDestroy() {

    }

    private getExtensions() {
        if (!this.measureReport) return;
        this.measureReport.extension = this.measureReport.extension || [];
        return this.measureReport.extension;
    }

}
