import { useEffect, useRef } from "react";

/*
  "SIGN IN WITH GOOGLE" BUTTON
  Google provides a small script (Google Identity Services). It draws the official button, and when
  the user picks an account it gives us an ID TOKEN: a signed JWT that says who the person is.
  We never see a Google password. We send that token to OUR backend (POST /auth/google), the backend
  verifies it with Google's public keys, and then answers with OUR OWN access and refresh tokens.
  From that point on, the app works exactly as it does after an email and password login.

  The button only appears when VITE_GOOGLE_CLIENT_ID is set in your .env file, so the app still
  works without Google configured.
*/
const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;
const SCRIPT_SRC = "https://accounts.google.com/gsi/client";

export const googleSignInEnabled = Boolean(CLIENT_ID);

// TypeScript does not know Google's script, so we describe the small part of it that we use.
interface GoogleAccountsId {
  initialize(config: { client_id: string; callback: (response: { credential: string }) => void }): void;
  renderButton(parent: HTMLElement, options: Record<string, unknown>): void;
}
declare global {
  interface Window {
    google?: { accounts: { id: GoogleAccountsId } };
  }
}

// Load Google's script once, however many times the button is shown.
let scriptPromise: Promise<void> | null = null;
function loadGoogleScript(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  scriptPromise ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SCRIPT_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      scriptPromise = null; // allow a retry later
      reject(new Error("Could not load Google sign-in."));
    };
    document.head.appendChild(script);
  });
  return scriptPromise;
}

interface Props {
  /** Called with Google's ID token after the user chooses an account. */
  onCredential: (idToken: string) => void;
  onError?: (message: string) => void;
}

export function GoogleSignInButton({ onCredential, onError }: Props) {
  const container = useRef<HTMLDivElement>(null);

  // Google calls our callback later, from its own script. Refs always hold the LATEST callbacks,
  // so we do not have to re-initialize Google every time the parent component re-renders.
  const onCredentialRef = useRef(onCredential);
  const onErrorRef = useRef(onError);
  useEffect(() => {
    onCredentialRef.current = onCredential;
    onErrorRef.current = onError;
  });

  useEffect(() => {
    if (!CLIENT_ID) return;
    let cancelled = false;
    loadGoogleScript()
      .then(() => {
        if (cancelled || !container.current || !window.google) return;
        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (response) => onCredentialRef.current(response.credential),
        });
        window.google.accounts.id.renderButton(container.current, {
          theme: "outline",
          size: "large",
          text: "continue_with",
          shape: "rectangular",
          width: 320,
        });
      })
      .catch((error: Error) => onErrorRef.current?.(error.message));
    return () => {
      cancelled = true;
    };
  }, []);

  if (!CLIENT_ID) return null;
  return <div ref={container} aria-label="Continue with Google" />;
}