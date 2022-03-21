import * as moment from "moment";

export function padDateNumber(value: number) {
  if (value < 10) {
    return '0' + value.toString();
  }

  return value.toString();
}

/**
 * this method is used to format the date according to the fhir standard YYYY-MM-DD. This is called when the page
 * first loads as well as when a date is selected from the datepicker.
 * @param date
 */
export function getFhirDate(date) {
  // created string constants because of lint complaining
  const year = 'year';
  const month = 'month';
  const day = 'day';

  // check if the date is the default fhir date when screen loads. if not then format it
  if (typeof date === 'string') {
    return date;
  } else if (date) {
    const yyyy = date[year];
    const mm = padDateNumber(date[month]);
    const dd = padDateNumber(date[day]);
    return `${yyyy}-${mm}-${dd}`;
  }
  return null;
}

export function getFhirNow() {
  const now = new Date();
  const year = now.getFullYear().toString();
  const month = padDateNumber(now.getMonth() + 1);
  const date = padDateNumber(now.getDate());
  return `${year}-${month}-${date}`;
}

export function getFhirYesterday() {
  const now = new Date();
  now.setDate(now.getDate() - 1);
  const year = now.getFullYear().toString();
  const month = padDateNumber(now.getMonth() + 1);
  const date = padDateNumber(now.getDate());
  return `${year}-${month}-${date}`;
}

export function formatDateToISO(date) {
  return moment.utc(date).toISOString(false);
}

