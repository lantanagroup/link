export interface IPractitioner {
  id: string;
  identifier?: {
    use?: string;
    system: string;
    value: string;
  }[];
  address?: {
    line: string[];
    city: string;
    postalCode: string;
    state: string;
    country: string;
  }[];
  gender?: string;
  telecom?: {
    use?: string;
    system: string;
    value: string;
  }[];
  name?: {
    family: string;
    given: string[];
    prefix?: string[];
  }[];
}