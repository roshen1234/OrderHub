import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage } from "../components/ui";

/*
  Login and register share one form. The only differences are the title, the button text and
  which function runs on submit.
*/
function AuthForm({ mode }: { mode: "login" | "register" }) {
  const { user, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  // ProtectedRoute remembers the page the user wanted in location.state.from.
  const from = (location.state as { from?: string } | null)?.from ?? "/products";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // useMutation is the write-side counterpart of useQuery: it runs when WE call mutate().
  const submit = useMutation({
    mutationFn: () => (mode === "login" ? login(email, password) : register(email, password)),
    onSuccess: () => navigate(from, { replace: true }),
  });

  // Already signed in: nothing to do here.
  if (user && !submit.isPending) return <Navigate to={from} replace />;

  function onSubmit(event: FormEvent) {
    event.preventDefault(); // stop the browser from reloading the page
    submit.mutate();
  }

  const isLogin = mode === "login";
  return (
    <div className="mx-auto max-w-md">
      <h1 className="text-3xl">{isLogin ? "Sign in" : "Create your account"}</h1>
      <form onSubmit={onSubmit} className="panel mt-6 space-y-4 p-6">
        <div>
          <label htmlFor="email" className="label">Email</label>
          <input id="email" type="email" required autoComplete="email" className="input"
                 value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label htmlFor="password" className="label">Password</label>
          <input id="password" type="password" required minLength={isLogin ? undefined : 8}
                 autoComplete={isLogin ? "current-password" : "new-password"} className="input"
                 value={password} onChange={(e) => setPassword(e.target.value)} />
          {!isLogin && <p className="mt-1 text-xs text-muted">Use at least 8 characters.</p>}
        </div>
        <ErrorMessage error={submit.error} />
        <button type="submit" className="btn btn-primary w-full" disabled={submit.isPending}>
          {submit.isPending ? "Please wait…" : isLogin ? "Sign in" : "Create account"}
        </button>
      </form>
      <p className="mt-4 text-sm text-muted">
        {isLogin ? (
          <>New here? <Link to="/register" className="link">Create an account</Link></>
        ) : (
          <>Already have an account? <Link to="/login" className="link">Sign in</Link></>
        )}
      </p>
    </div>
  );
}

export const LoginPage = () => <AuthForm mode="login" />;
export const RegisterPage = () => <AuthForm mode="register" />;
