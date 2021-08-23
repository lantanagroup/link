import {Component, Input, OnInit} from '@angular/core';
import {IExtension} from "../../fhir";
import {ReportModel} from "../../model/ReportModel";

@Component({
    selector: 'notes',
    templateUrl: './notes.component.html',
    styleUrls: ['./notes.component.css']
})
export class NotesComponent implements OnInit {
    private static readonly notesUrl = 'https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-report-note';
    @Input() report: ReportModel;

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
            if (extensions.length === 0) delete this.report.measureReport.extension;
        } else if (!notesExtension && value) {
            extensions.push(notesExt);
        } else {
            notesExtension.valueMarkdown = value;
        }
    }

    get isDisabled() {
        if (!this.report) return;
        console.log("Status is:" + this.report.status);
        return this.report.status === 'FINAL';
    }

    private getExtensions() {
        if (!this.report) return;
        this.report.measureReport.extension = this.report.measureReport.extension || [];
        return this.report.measureReport.extension;
    }


    async ngOnInit() {

    }

    ngOnDestroy() {

    }

}
