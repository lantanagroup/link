// Table Models

export interface TableFilter {
  name: string,
  options: TableFilterOption[]
}

export interface TableFilterOption {
  label: string,
  value: string | boolean | number
}

export interface SearchBar {
  title: string,
  placeholder: string
}