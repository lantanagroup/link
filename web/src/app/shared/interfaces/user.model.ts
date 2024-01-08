/* user data model */

export interface UserModel {
  name: string
  userId: string
  email: string
  role?: string
  organization?: string
  department?: string
  phone?: string
}

// demo data
export const FjorgeUser: UserModel = {
  name: 'Fjorge Developers',
  userId: 'ef0a782d-1e0f-4846-9c67-24f63d855a7e',
  email: 'developers@fjorgedigital.com',
  role: 'to come',
  organization: 'fjorge',
  department: 'tech',
  phone: '123-456-7890'
}
