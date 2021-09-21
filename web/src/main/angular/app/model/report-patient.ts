export class ReportPatient {
  id: string;
  name: string;
  sex: string;
  dateOfBirth: string;

  getDOBDisplay() {
    const date = new Date(Date.parse(this.dateOfBirth));
    return date.toDateString();
  }
}
