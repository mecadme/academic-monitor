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

  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL ??
    "http://localhost:8080"

  const response = await fetch(
    `${apiBaseUrl}/api/v1/integrations/idukay/test-login`,
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

  if (!response.ok) {
    const body =
      await response.text()

    throw new Error(
      `Idukay login test failed: ${response.status} ${body}`,
    )
  }

  return response.json()
}
