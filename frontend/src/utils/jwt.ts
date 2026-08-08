import type { DecodedToken } from "../types/auth";

// A JWT is three base64url-encoded segments joined by dots:
// header.payload.signature
// We only need the payload (the middle segment) - that's where the
// backend's JwtUtil put the username ("sub") and role ("role") claims.
// We don't (and can't, without the secret) verify the signature here -
// that's the backend's job on every request. This is purely for reading
// what's already in a token we trust because we just received it from
// our own login endpoint.
export function decodeToken(token: string): DecodedToken | null {
  try {
    const payload = token.split(".")[1];
    // base64url uses -/_ instead of +/ and has no padding - convert back
    // to standard base64 before decoding
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = JSON.parse(atob(base64));
    return decoded as DecodedToken;
  } catch {
    return null;
  }
}

export function isTokenExpired(decoded: DecodedToken): boolean {
  return decoded.exp * 1000 < Date.now();
}