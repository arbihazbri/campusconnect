import {
  banOutline,
  checkmarkCircle,
  checkmarkDoneOutline,
  closeCircle,
  handLeftOutline,
  hourglassOutline,
  peopleOutline,
  radioButtonOnOutline,
  ribbonOutline,
  rocketOutline,
  sparklesOutline,
  star,
  starOutline,
} from "ionicons/icons";

import { OFFER_STATUS_LABEL, REPORT_STATUS_LABEL, REQUEST_STATUS_LABEL } from "@/lib/format";
import type { OfferStatus, ReportStatus, Reputation, ReputationBadge, RequestStatus } from "@/lib/types";

import Icon from "./Icon";

const REQUEST_STYLE: Record<RequestStatus, [string, string]> = {
  OUVERTE: ["badge-info", radioButtonOnOutline],
  AIDE_ACCEPTEE: ["badge-warning", peopleOutline],
  TERMINEE: ["badge-success", checkmarkDoneOutline],
};

const OFFER_STYLE: Record<OfferStatus, [string, string]> = {
  EN_ATTENTE: ["badge-warning", hourglassOutline],
  ACCEPTEE: ["badge-success", checkmarkCircle],
  REFUSEE: ["badge-danger", closeCircle],
};

const REPORT_STYLE: Record<ReportStatus, [string, string]> = {
  EN_ATTENTE: ["badge-warning", handLeftOutline],
  TRAITE: ["badge-success", checkmarkCircle],
  REJETE: ["", banOutline],
};

function Badge({ tone, icon, label }: { tone: string; icon: string; label: string }) {
  return (
    <span className={`badge ${tone}`}>
      <Icon icon={icon} size={14} />
      {label}
    </span>
  );
}

export function RequestStatusBadge({ status }: { status: RequestStatus }) {
  const [tone, icon] = REQUEST_STYLE[status];
  return <Badge tone={tone} icon={icon} label={REQUEST_STATUS_LABEL[status]} />;
}

export function OfferStatusBadge({ status }: { status: OfferStatus }) {
  const [tone, icon] = OFFER_STYLE[status];
  return <Badge tone={tone} icon={icon} label={OFFER_STATUS_LABEL[status]} />;
}

export function ReportStatusBadge({ status }: { status: ReportStatus }) {
  const [tone, icon] = REPORT_STYLE[status];
  return <Badge tone={tone} icon={icon} label={REPORT_STATUS_LABEL[status]} />;
}

export function Stars({ score, max = 5, size = 16 }: { score: number; max?: number; size?: number }) {
  return (
    <span className="stars" role="img" aria-label={`${score} sur ${max}`}>
      {Array.from({ length: max }, (_, index) => (
        <span key={index} className={index < score ? undefined : "off"}>
          <Icon icon={index < score ? star : starOutline} size={size} />
        </span>
      ))}
    </span>
  );
}

const REPUTATION_STYLE: Record<ReputationBadge, [string, string, string]> = {
  NOUVEAU: ["", sparklesOutline, "Nouveau"],
  CONTRIBUTEUR: ["badge-info", rocketOutline, "Contributeur"],
  MENTOR: ["badge-success", ribbonOutline, "Mentor"],
};

export const REPUTATION_HINT: Record<ReputationBadge, string> = {
  NOUVEAU: "N'a pas encore terminé de séance en tant qu'aidant",
  CONTRIBUTEUR: "A déjà aidé au moins un étudiant jusqu'au bout d'une séance",
  MENTOR: "Au moins 5 séances d'aide terminées avec une note moyenne de 4/5 ou plus",
};

/** Fonctionnalité originale : badge de reconnaissance des contributions. */
export function ReputationBadgeView({ badge }: { badge: ReputationBadge }) {
  const [tone, icon, label] = REPUTATION_STYLE[badge];
  return (
    <span className={`badge ${tone}`} title={REPUTATION_HINT[badge]}>
      <Icon icon={icon} size={14} />
      {label}
    </span>
  );
}

/** Résumé lisible : « ★ 4,7 (3 avis) · 2 aides réalisées ». */
export function ReputationSummary({ reputation }: { reputation: Reputation }) {
  const rating =
    reputation.averageRating !== null
      ? `${reputation.averageRating.toLocaleString("fr-FR", { minimumFractionDigits: 1 })}/5 (${reputation.ratingsCount} avis)`
      : "Pas encore évalué";
  const helped =
    reputation.sessionsHelped === 0
      ? "aucune aide terminée"
      : `${reputation.sessionsHelped} aide${reputation.sessionsHelped > 1 ? "s" : ""} réalisée${reputation.sessionsHelped > 1 ? "s" : ""}`;
  return (
    <span className="meta-item small muted">
      <Icon icon={reputation.averageRating !== null ? star : starOutline} size={14} />
      {rating} · {helped}
    </span>
  );
}
