export function padDateNumber(value: number) {
  if (value < 10) {
    return '0' + value.toString();
  }

  return value.toString();
}

export function getFhirNow() {
  const now = new Date();
  const year = now.getFullYear().toString();
  const month = padDateNumber(now.getMonth() + 1);
  const date = padDateNumber(now.getDate());
  return `${year}-${month}-${date}`;
}
