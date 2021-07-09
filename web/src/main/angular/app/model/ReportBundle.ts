export class ReportBundle {
    bundleId: string;
    list: {
        reportIdentifier: string;
        measureIdentifier: string;
        status: string;
        docStatus: string;
        author: string;
        periodStartDate: string;
        periodEndDate: string;
        creationDate: Date;
        submittedDate: string;
    }[];
    totalSize: bigint;
}

