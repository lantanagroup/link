export interface IResource {
  resourceType: string;
  id?: string;
}

export interface IBundleEntry {
  fullUrl?: string;
  resource?: IResource;
}

export interface IBundle {
  entry?: IBundleEntry[];
}

export interface ICoding {
  code?: string;
  display?: string;
  system?: string;
}

export interface ICodeableConcept {
  coding?: ICoding[];
  text?: string;
}

export interface IIdentifier {
  value?: string;
}

export interface IResourceReference {
  reference?: string;
  display?: string;
}

export interface IHumanName {
  use?: string;
  family?: string;
  given?: string[];
}

export interface IPatient extends IResource {
  identifier?: IIdentifier[];
  name?: IHumanName[];
}

export interface ICondition extends IResource {
  code?: ICodeableConcept;
}

export interface IComposition extends IResource {
  identifier?: IIdentifier[];
  type?: ICodeableConcept;
  subject?: IResourceReference;
  date?: string;
  author?: IResourceReference[];
  title?: string;
  section?: {
    title?: string;
    code?: ICodeableConcept;
    entry?: IResourceReference[];
  }[];
}

export interface IExtension {
  id?: string;
  extension?: IExtension[];
  url: string;
  // value[x]
}

export interface IDomainResource extends IResource {
  resourceType: string;
  contained?: IDomainResource[];
  extension?: IExtension[];
  modifierExtension?: IExtension[];
}

export interface IQuestionnaireResponseAnswerComponent {
  item?: IQuestionnaireResponseItemComponent[];
  valueUri?: string;
  valueString?: string;
  valueDate?: string;
  valueInteger?: number;
}

export interface IQuestionnaireResponseItemComponent {
  linkId: string;
  definition?: string;
  text?: string;
  answer?: IQuestionnaireResponseAnswerComponent[];
  item?: IQuestionnaireResponseItemComponent[];
}

export interface IQuestionnaireResponse extends IDomainResource {
  identifier?: IIdentifier;
  basedOn?: IResourceReference[];
  partOf?: IResourceReference[];
  questionnaire?: string;
  status: string;
  subject?: IResourceReference;
  context?: IResourceReference;
  authored?: Date;
  author?: IResourceReference;
  source?: IResourceReference;
  item?: IQuestionnaireResponseItemComponent[];
}
