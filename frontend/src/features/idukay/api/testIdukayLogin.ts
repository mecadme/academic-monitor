import { apiFetch } from '../../../api/apiFetch';

import {
  createIdukayFingerprint,
} from "../lib/idukayFingerprint"

export type TestIdukayLoginInput = {
  email: string
  password: string
  subdomainSchool?: string
  schoolId?: string
  profileId?: string
}

export async function testIdukayLogin(
  input: TestIdukayLoginInput,
) {
  const fingerprint =
    await createIdukayFingerprint()


  const response = await apiFetch(
    `/api/v1/integrations/idukay/test-login`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: input.email,
        password: input.password,
        subdomainSchool:
          input.subdomainSchool ?? null,
        schoolId:
          input.schoolId ?? null,
        profileId:
          input.profileId ?? null,
fingerprint,
      }),
    },
  )


  return response.json()
}
