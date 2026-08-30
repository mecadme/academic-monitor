import { describe, expect, it } from "vitest"
import { createIdukayFingerprint } from "./idukayFingerprint"

describe("createIdukayFingerprint", () => {
  it("creates the browser fingerprint contract", async () => {
    const fingerprint =
      await createIdukayFingerprint()

    expect(fingerprint.user_agent)
      .toBeTypeOf("string")

    expect(fingerprint.language)
      .toBeTypeOf("string")

    expect(
      Array.isArray(fingerprint.languages),
    ).toBe(true)

    expect(fingerprint.screen)
      .toEqual(
        expect.objectContaining({
          width: expect.any(Number),
          height: expect.any(Number),
          avail_width: expect.any(Number),
          avail_height: expect.any(Number),
          color_depth: expect.any(Number),
          pixel_ratio: expect.any(Number),
        }),
      )

    expect(fingerprint.touch_points)
      .toBeTypeOf("number")
  })
})
