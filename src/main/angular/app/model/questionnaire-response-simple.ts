
export class QuestionnaireResponseSimple {
  facilityId: string;
  summaryCensusId: string;
  date: string;

  hospitalized: number;
  hospitalizedAndVentilated: number;
  hospitalOnset: number;
  edOverflow: number;
  edOverflowAndVentilated: number;
  deaths: number;

  allHospitalBeds: number;
  hospitalInpatientBeds: number;
  hospitalInpatientBedOccupancy: number;
  icuBeds: number;
  icuBedOccupancy: number;
  mechanicalVentilators: number;
  mechanicalVentilatorsInUse: number;
}
