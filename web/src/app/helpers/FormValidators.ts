import { AbstractControl, ValidatorFn } from "@angular/forms";


// * Generic regex validator
export function regexValidator(testString: RegExp, isRequired: boolean = false): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {
    if(!isRequired && !control.value) {
      return null
    }

    const regex = testString,
          isValid = regex.test(control.value)

    return isValid ? null : { 'invalidFormat': { value: control.value } }
  }

}

// * Check to see if url inputed matches the following criteria
// - starts with http:// or https://
// - includes localhost or 127.0.0.1

export function urlValidator(): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {

    if(!control.value) {
      // if there's no value, return null (no error)
      return null
    }

    const urlRegex = /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/,
          localhostOrIpRegex = /(localhost|127\.0\.0\.1)/

    const isValidUrl = urlRegex.test(control.value),
          containsLocalhostOrIp = localhostOrIpRegex.test(control.value)

    return isValidUrl || containsLocalhostOrIp ? null : { 'invalidUrl': { value: control.value } }
  }
}

// * Check to see if input is an ISO 8601 formatted duration

export function iso8601Validator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: any } | null => {

    if(!control.value) {
      // if there's no value, return null (no error)
      return null
    }

    // This regex covers basic ISO 8601 durations)
    const iso8601Regex = /^P(?=\d|T\d)(\d+Y)?(\d+M)?(\d+D)?(T(\d+H)?(\d+M)?(\d+(.\d+)?S)?)?$/,
          isValid = iso8601Regex.test(control.value)

    return isValid ? null : { 'invalidIso': { value: control.value } }
  }
}

// * Check to see if database string is formatted correctly
// - starts with 'jdbc:sqlserver://'

export function databaseValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: any } | null => {
    if(!control.value) {
      // if there's no value, return null (no error)
      return null
    }

    const jdbcRegex = /^jdbc:sqlserver:\/\//,
          isValid = jdbcRegex.test(control.value)

    return isValid ? null : { 'invalidDb': { value: control.value } }
  }
}

