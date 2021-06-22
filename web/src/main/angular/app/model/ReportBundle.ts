export class ReportBundle {
    bundleId: string;
    list: {
        reportIdentifier: string;
        measureIdentifier: string;
        status: string;
        docStatus: string;
        author: string;
        periodStartDate: Date;
        periodEndDate : Date;
        creationDate : Date;
    }[];
}

