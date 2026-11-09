import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { addressApi } from "../api/addresses";
import { EmptyState, ErrorMessage, Loading, PageHeader } from "../components/ui";
import type { Address, AddressInput } from "../types";

const EMPTY: AddressInput = {
  label: "", recipientName: "", phone: "", line1: "", line2: "",
  city: "", state: "", postalCode: "", country: "IN", makeDefault: false,
};

/** Turns a saved address into the shape the form edits. */
const toInput = (a: Address): AddressInput => ({
  label: a.label ?? "", recipientName: a.recipientName, phone: a.phone, line1: a.line1, line2: a.line2 ?? "",
  city: a.city, state: a.state, postalCode: a.postalCode, country: a.country, makeDefault: a.defaultAddress,
});

export function AddressesPage() {
  const queryClient = useQueryClient();
  const addresses = useQuery({ queryKey: ["addresses"], queryFn: addressApi.list });
  // null = form closed, "new" = adding, otherwise the address being edited
  const [editing, setEditing] = useState<Address | "new" | null>(null);

  const remove = useMutation({
    mutationFn: (id: number) => addressApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["addresses"] }),
  });

  return (
    <div>
      <PageHeader title="Delivery addresses">
        {editing === null && <button className="btn btn-primary" onClick={() => setEditing("new")}>Add address</button>}
      </PageHeader>

      {editing !== null && (
        <AddressForm
          // "key" makes React build a fresh form when we switch between add and edit
          key={editing === "new" ? "new" : editing.id}
          address={editing === "new" ? null : editing}
          onDone={() => setEditing(null)}
        />
      )}

      <div className="mt-6 space-y-3">
        <ErrorMessage error={addresses.error ?? remove.error} />
        {addresses.isPending ? <Loading /> : addresses.data?.length === 0 && editing === null ? (
          <EmptyState title="No addresses yet">Add one so we know where to deliver your orders.</EmptyState>
        ) : (
          <ul className="grid gap-4 md:grid-cols-2">
            {addresses.data?.map((a) => (
              <li key={a.id} className="panel p-4 text-sm">
                <p className="font-semibold">
                  {a.recipientName}{a.label ? ` (${a.label})` : ""}
                  {a.defaultAddress && <span className="ml-2 rounded bg-pallet-soft px-1.5 py-0.5 text-xs text-pallet-dark">Default</span>}
                </p>
                <p className="text-muted">{a.line1}{a.line2 ? `, ${a.line2}` : ""}</p>
                <p className="text-muted">{a.city}, {a.state} {a.postalCode}, {a.country}</p>
                <p className="text-muted">{a.phone}</p>
                <div className="mt-3 flex gap-2">
                  <button className="btn btn-secondary" onClick={() => setEditing(a)}>Edit</button>
                  <button className="btn btn-danger" disabled={remove.isPending}
                          onClick={() => { if (window.confirm("Delete this address? Past orders keep their own copy.")) remove.mutate(a.id); }}>
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function AddressForm({ address, onDone }: { address: Address | null; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<AddressInput>(address ? toInput(address) : EMPTY);

  // One small helper updates a single field without repeating the same code for every input.
  const set = <K extends keyof AddressInput>(key: K, value: AddressInput[K]) => setForm((f) => ({ ...f, [key]: value }));

  const save = useMutation({
    mutationFn: () => (address ? addressApi.update(address.id, form) : addressApi.create(form)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["addresses"] });
      onDone();
    },
  });

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    save.mutate();
  }

  // The browser checks "required" and "pattern" before onSubmit runs; the backend checks again.
  return (
    <form onSubmit={onSubmit} className="panel grid gap-4 p-5 sm:grid-cols-2">
      <h2 className="text-lg sm:col-span-2">{address ? "Edit address" : "New address"}</h2>

      <Field label="Full name" id="recipientName" value={form.recipientName} onChange={(v) => set("recipientName", v)} required maxLength={100} />
      <Field label="Phone" id="phone" value={form.phone} onChange={(v) => set("phone", v)} required type="tel"
             pattern="^\+?[0-9 ()\-]{7,20}$" hint="Digits, spaces and + ( ) - are allowed." />
      <Field label="Address line 1" id="line1" value={form.line1} onChange={(v) => set("line1", v)} required maxLength={200} />
      <Field label="Address line 2 (optional)" id="line2" value={form.line2} onChange={(v) => set("line2", v)} maxLength={200} />
      <Field label="City" id="city" value={form.city} onChange={(v) => set("city", v)} required maxLength={100} />
      <Field label="State" id="state" value={form.state} onChange={(v) => set("state", v)} required maxLength={100} />
      <Field label="Postal code" id="postalCode" value={form.postalCode} onChange={(v) => set("postalCode", v)} required maxLength={20} />
      <Field label="Country code" id="country" value={form.country} onChange={(v) => set("country", v.toUpperCase())} required
             pattern="^[A-Za-z]{2}$" maxLength={2} hint="Two letters, for example IN." />
      <Field label="Label (optional)" id="label" value={form.label} onChange={(v) => set("label", v)} maxLength={50} hint="For example Home or Office." />

      <label className="flex items-center gap-2 self-end text-sm">
        <input type="checkbox" checked={form.makeDefault} onChange={(e) => set("makeDefault", e.target.checked)} />
        Use as my default address
      </label>

      <div className="space-y-3 sm:col-span-2">
        <ErrorMessage error={save.error} />
        <div className="flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={save.isPending}>
            {save.isPending ? "Saving…" : "Save address"}
          </button>
          <button type="button" className="btn btn-secondary" onClick={onDone}>Cancel</button>
        </div>
      </div>
    </form>
  );
}

function Field(props: {
  label: string; id: string; value: string; onChange: (value: string) => void;
  required?: boolean; type?: string; pattern?: string; maxLength?: number; hint?: string;
}) {
  return (
    <div>
      <label htmlFor={props.id} className="label">{props.label}</label>
      <input id={props.id} className="input" type={props.type ?? "text"} value={props.value} required={props.required}
             pattern={props.pattern} maxLength={props.maxLength} onChange={(e) => props.onChange(e.target.value)} />
      {props.hint && <p className="mt-1 text-xs text-muted">{props.hint}</p>}
    </div>
  );
}
