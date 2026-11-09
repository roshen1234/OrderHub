import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import { ErrorMessage, SuccessMessage } from "../../components/ui";

/*
  PAYMENT SIMULATOR (development only)
  With the mock payment gateway, nobody pays on a real bank page, so the "payment succeeded"
  message that a real gateway would send never arrives. This form plays the gateway: it asks the
  backend to send itself a signed webhook for a payment reference.
  It only works while the backend runs with orderhub.payment.gateway=mock.
*/
export function AdminToolsPage() {
  const [providerRef, setProviderRef] = useState("");
  const [success, setSuccess] = useState(true);
  const [eventId, setEventId] = useState("");
  const [sendWebhook, setSendWebhook] = useState(true);

  const simulate = useMutation({
    mutationFn: () => adminApi.simulateGateway({
      providerRef: providerRef.trim(),
      success,
      eventId: eventId.trim() || undefined,
      sendWebhook,
    }),
  });

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    simulate.mutate();
  }

  return (
    <form onSubmit={onSubmit} className="panel max-w-xl space-y-4 p-5">
      <div>
        <h2 className="text-lg">Simulate the payment gateway</h2>
        <p className="mt-1 text-sm text-muted">
          Copy the payment reference shown on the customer's order page (it looks like <code className="font-mono">mock_…</code>).
        </p>
      </div>

      <div>
        <label htmlFor="ref" className="label">Payment reference</label>
        <input id="ref" className="input font-mono" required value={providerRef} onChange={(e) => setProviderRef(e.target.value)} />
      </div>

      <fieldset>
        <legend className="label">Outcome</legend>
        <div className="flex gap-4 text-sm">
          <label className="flex items-center gap-2"><input type="radio" checked={success} onChange={() => setSuccess(true)} /> Payment succeeded</label>
          <label className="flex items-center gap-2"><input type="radio" checked={!success} onChange={() => setSuccess(false)} /> Payment failed</label>
        </div>
      </fieldset>

      <div>
        <label htmlFor="evt" className="label">Event id (optional)</label>
        <input id="evt" className="input font-mono" value={eventId} onChange={(e) => setEventId(e.target.value)} />
        <p className="mt-1 text-xs text-muted">Send the same event id twice to see that duplicates are ignored.</p>
      </div>

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={sendWebhook} onChange={(e) => setSendWebhook(e.target.checked)} />
        Send the webhook (untick to test the background reconciliation job instead)
      </label>

      <ErrorMessage error={simulate.error} />
      {simulate.isSuccess && <SuccessMessage>{typeof simulate.data === "string" && simulate.data ? simulate.data : "Done."}</SuccessMessage>}
      <button type="submit" className="btn btn-primary" disabled={simulate.isPending}>Send</button>
    </form>
  );
}
