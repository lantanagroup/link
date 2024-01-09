/* user data model */

export interface ProfileModel {
  name: string
  id: string
  email: string
  enabled: boolean
  role?: string
  organization?: string
  department?: string
  phone?: string
}

// demo data
export const FjorgeUser: ProfileModel = {
  name: 'Fjorge Developers',
  id: 'ef0a782d-1e0f-4846-9c67-24f63d855a7e',
  email: 'developers@fjorgedigital.com',
  enabled: true,
  role: 'to come',
  organization: 'fjorge',
  department: 'tech',
  phone: '123-456-7890'
}
