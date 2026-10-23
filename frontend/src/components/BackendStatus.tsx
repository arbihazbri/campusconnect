"use client";

import { useEffect, useState } from "react";

type HealthResponse = {
  status: string;
  application: string;
};

type State =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ok"; data: HealthResponse };

export default function BackendStatus() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;

    async function checkBackend() {
      try {
        const response = await fetch("/api/health");
        if (!response.ok) {
          throw new Error(`Réponse inattendue : ${response.status}`);
        }
        const data: HealthResponse = await response.json();
        if (!cancelled) {
          setState({ kind: "ok", data });
        }
      } catch (error) {
        if (!cancelled) {
          const message =
            error instanceof Error ? error.message : "Erreur inconnue";
          setState({ kind: "error", message });
        }
      }
    }

    checkBackend();

    return () => {
      cancelled = true;
    };
  }, []);

  if (state.kind === "loading") {
    return <p role="status">Vérification du serveur…</p>;
  }

  if (state.kind === "error") {
    return <p role="alert">Serveur injoignable : {state.message}</p>;
  }

  return (
    <p role="status">
      Serveur {state.data.application} : {state.data.status}
    </p>
  );
}