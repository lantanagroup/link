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
