import {padDateNumber} from '../../helper';

export class BaseReportModel {
  getCodeSystem(code: string) {
    if (!code) return code;
    const codeSplit = code.split('|');
    return codeSplit != null && codeSplit.length === 2 ?
        (codeSplit[0] != 'null' ? codeSplit[0] : '') :
        code;
  }

  getCodeDisplay(code: string) {
    if (!code) return code;
    const codeSplit = code.split('|');
    return codeSplit != null && codeSplit.length === 2 && codeSplit[1] !== 'null' ? codeSplit[1] : '';
  }

  getDateDisplay(date: string) {
    if (!date) return '';
    const ad = new Date(date);
    const datePart = `${ad.getMonth() + 1}/${ad.getDate()}/${ad.getFullYear()}`;

    if (date.length > 10) {
      return `${datePart} @ ${padDateNumber(ad.getHours())}:${padDateNumber(ad.getMinutes())}`
    } else {
      return datePart;
    }
  }
}