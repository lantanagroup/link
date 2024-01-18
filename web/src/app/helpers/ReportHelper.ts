import { Report } from "../shared/interfaces/report.model";


type StatusType = 'pending' | 'submitted' | 'failed';

// Below funcitons are all helper functions. All of these function could be moved to a separate helper file or something.
export function generateRandomData(numEntries: number): Report[] {
  const facilities = ["University of Minnesota", "University of South Dakota", "University of Wisconsin"];
  const measuresArray = ["ABC", "DE", "ABE", "REW"];
  const statusOptions: StatusType[] = ["pending", "submitted", "failed"];
  const activitiesMap: { [K in StatusType]: string } = {
    "pending": "Submission Initiated",
    "submitted": "Successful Submission",
    "failed": "Failed Submission"
  };

  const randomData: Report[] = [];

  for (let i = 0; i < numEntries; i++) {
    const status: StatusType = statusOptions[Math.floor(Math.random() * statusOptions.length)];
    const measures = status === 'submitted' ? getRandomSubarray(measuresArray, Math.ceil(Math.random() * measuresArray.length)) : (status === 'pending' ? ['Pending'] : ['n/a']);
    const aggregates = status === 'submitted' ? generateRandomNumbersAsStrings(5) : ['--'];
    const { periodStart, periodEnd } = generateRandomPeriod()

    randomData.push({
      id: (Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000).toString(),
      reportId: createGUID(),
      submittedTime: generateRandomDateTime(),
      generatedTime: generateRandomDateTime(),
      details: (Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000).toString(),
      tenantName: facilities[Math.floor(Math.random() * facilities.length)],
      tenantId: '#',
      cdcOrgId: (Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000).toString(),
      periodStart: periodStart,
      periodEnd: periodEnd,
      measureIds: measures,
      aggregates: aggregates,
      status: status,
      version: "1.0",
      patientLists: [],
    });
  }
  return randomData;
}

export function getRandomSubarray(arr: any[], size: number) {
  const shuffled = arr.slice(0);
  let i = arr.length;
  const min = i - size;

  while (i-- > min) {
    const index = Math.floor((i + 1) * Math.random());
    const temp = shuffled[index];
    shuffled[index] = shuffled[i];
    shuffled[i] = temp;
  }

  return shuffled.slice(min);
}

export function generateRandomNumbersAsStrings(count: number): string[] {
  const numbers = [];
  for (let i = 0; i < count; i++) {
    // Generate a random number between 10,000 (inclusive) and 99,999 (inclusive)
    const randomNum = Math.floor(Math.random() * (9999 - 1000 + 1)) + 1000;
    numbers.push(randomNum.toLocaleString()); // Convert to string with commas
  }
  return numbers;
}

export function generateRandomPeriod() {
  const start = new Date(2023, Math.floor(Math.random() * 12), Math.floor(Math.random() * 28) + 1);
  const end = new Date(start);
  end.setDate(end.getDate() + 7);

  const formatDate = (date: any) => `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}.${String(date.getFullYear()).substring(2)}`;
  return {
    periodStart: formatDate(start),
    periodEnd: formatDate(end)
  };
}

export function createGUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

export function generateRandomDateTime() {
  return `03.14.2023 ${Math.floor(Math.random() * 24)}:${Math.floor(Math.random() * 60)} ${Math.random() > 0.5 ? "AM" : "PM"} EST`;
}



/**
 * Calculates the length of a period in days, given the start and end dates.
 * This function takes two date strings, parses them into Date objects using the 'parseDate' function
 * (which should be defined to handle your specific date format), and then calculates the difference
 * in days between these two dates.
 *
 * Note: The 'parseDate' function is expected to correctly parse the dates from the format "MM.DD.YY"
 * into JavaScript Date objects. The difference in days is rounded to the nearest whole number.
 *
 * @param {string} periodStart - The start date of the period in "MM.DD.YY" format.
 * @param {string} periodEnd - The end date of the period in "MM.DD.YY" format.
 * @returns {number} The length of the period in days.
 */
export function calculatePeriodLength(periodStart: string, periodEnd: string): number {
  // Parse the dates from the format "MM.DD.YY" to JavaScript Date objects
  const startDate: Date = parseDate(periodStart);
  const endDate: Date = parseDate(periodEnd);

  // Calculate the difference in milliseconds and convert to days
  const diffInMilliseconds: number = endDate.getTime() - startDate.getTime();
  const diffInDays: number = diffInMilliseconds / (1000 * 60 * 60 * 24);

  return Math.round(diffInDays); // rounding to the nearest whole number
}

/**
 * Determines and formats the data for a given report period based on the submission status.
 * It calculates the length of the period, formats the start and end dates, and then returns
 * this data along with an appropriate message based on the status:
 * 1. If the status is 'submitted', it returns the formatted report period and its length in days.
 * 2. If the status is 'failed', it returns 'n/a'.
 * 3. For any other status, it returns 'Pending'.
 * The function assumes the existence of 'calculatePeriodLength' and 'formatDate' functions
 * for calculating the period length and formatting the dates, respectively.
 *
 * @param {string} status - The submission status of the period.
 * @param {string} periodStart - The start date of the period in ISO 8601 format.
 * @param {string} periodEnd - The end date of the period in ISO 8601 format.
 * @returns {string | string[]} An array containing the formatted report period and its length, or a status string.
 */
export function getPeriodData(status: string, periodStart: string, periodEnd: string) {
  const periodLength = calculatePeriodLength(periodStart, periodEnd);
  const reportPeriod = `${formatDate(periodStart)} - ${formatDate(periodEnd)}`;
  let periodData;
  if (status === "submitted") {
    periodData = [reportPeriod, periodLength.toString() + ' Days'];
  } else {
    periodData = status === "failed" ? "n/a" : "Pending";
  }

  return periodData;
}

/**
 * Formats a date string from ISO 8601 format to 'MM.DD.YYYY'.
 * The function takes an ISO 8601 date string as input, converts it to a Date object,
 * and then formats it to a string in the 'MM.DD.YYYY' format.
 *
 * @param {string} dateStr - The ISO 8601 date string to be formatted.
 * @returns {string} A string representing the formatted date in 'YYYY-MM-DD' format.
 */
export const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return `${String(date.getFullYear())}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

/**
 * Parses a date string and returns a Date object.
 * The function can handle two formats:
 * 1. ISO 8601 format (e.g., '2024-01-01T00:00:00.000Z'). In this case, it directly converts the string to a Date object.
 * 2. A custom format 'MM.DD.YY'. For this format, it splits the string, converts the parts to numbers, and adjusts the year to the full format (assuming 2000s), then creates a Date object.
 *
 * @param {string} dateStr - The date string to be parsed.
 * @returns {Date} The parsed Date object.
 */
export function parseDate(dateStr: string): Date {
  // Check if the date string is in ISO 8601 format (e.g., '2024-01-01T00:00:00.000Z')
  if (dateStr.includes('T')) {
    return new Date(dateStr);
  }

  // Otherwise, assume the format is 'MM.DD.YY' and parse accordingly
  const [month, day, year] = dateStr.split('.').map(Number);
  // Adjust year format from YY to YYYY (assuming 2000s)
  const fullYear: number = 2000 + year;
  return new Date(fullYear, month - 1, day);
}

/**
 * Processes measure data based on the provided status.
 * Depending on the status, it either returns a modified array of measure IDs or a status string:
 * 1. If the status is 'submitted', it truncates each measure ID in the array to the first 4 characters.
 * 2. If the status is 'pending', it returns the string 'Pending'.
 * 3. For all other statuses, it returns 'n/a'.
 *
 * @param {string} status - The status based on which the processing is done.
 * @param {string[]} measureIds - An array of measure IDs to be processed.
 * @returns {string | string[]} Either a modified array of measure IDs or a status string.
 */
export function processMeasuresData(status: string, measureIds: string[]): string | string[] {
  let measuresData;

  switch (status.toLowerCase()) {
    case 'submitted':
      measuresData = measureIds.map(id => id.slice(0, 4)); //TODO: We shouldn't have to trim the measureIds.
      break;
    case 'pending':
      measuresData = 'Pending';
      break;
    default:
      measuresData = 'n/a';
      break;
  }

  return measuresData;
}

/**
 * Translates a submission status into a more descriptive message.
 * Based on the provided status, the function returns one of three strings:
 * 1. If the status is 'submitted', it returns 'Successful Submission'.
 * 2. If the status is 'failed', it returns 'Failed Submission'.
 * 3. For any other status, it returns 'Submission Initiated'.
 * The function is case-insensitive to the status input.
 *
 * @param {string} status - The submission status to be translated.
 * @returns {string} A descriptive message corresponding to the submission status.
 */
export function getSubmissionStatus(status: string): string {
  switch (status.toLowerCase()) {
    case 'submitted':
      return 'Successful Submission';
    case 'failed':
      return 'Failed Submission';
    default:
      return 'Submission Initiated';
  }
}


/******************/
/* placeholder normalization detail data */
/******************/

export const normalizationData: any = [
  {
    "Timestamp": "03.14.2023 10:12 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 10:12 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 2:35 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 22:13 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 12:48 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 10:12 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 10:12 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 2:35 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 22:13 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 12:48 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 10:12 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 10:12 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 2:35 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 22:13 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 12:48 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 10:12 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 10:12 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 2:35 PM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 22:13 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  },
  {
    "Timestamp": "03.14.2023 12:48 AM EST",
    "Column2": "############",
    "Column3": "############",
    "Column4": "############",
    "Column5": "############"
  }
]