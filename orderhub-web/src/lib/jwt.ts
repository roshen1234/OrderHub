/*
  READING A JWT IN THE BROWSER
  A JWT is three base64url parts separated by dots: header.payload.signature.
  The payload is NOT encrypted, so the browser can read it. We use it only to show the user's
  email and to decide whether to show admin links.

  IMPORTANT: this is for display only. The browser cannot verify the signature (it does not
  have the secret), and anyone can edit what the UI shows. The Spring Boot backend is what
  actually enforces permissions on every request.
*/
export interface JwtPayload {
  sub: string; // the user's email (the backend sets it as the subject)
  roles: string[]; // for example ["ROLE_CUSTOMER"]
  exp: number; // expiry, in seconds since 1970
}

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const payload = token.split(".")[1];
    // base64url -> base64, then decode and parse the JSON text
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join(""),
    );
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}
