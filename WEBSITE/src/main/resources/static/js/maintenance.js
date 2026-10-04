"use strict";

function maintenanceCard(item) {
  const dateTime = value => value ? escapeHtml(new Date(value).toLocaleString()) : "—";
  const actions = item.status === "PENDING"
    ? `<button class="btn btn-blue btn-sm" data-action="start-repair" data-id="${item.id}">Start repair</button>`
    : item.status === "IN_PROGRESS"
      ? `<button class="btn btn-green btn-sm" data-action="complete-repair" data-id="${item.id}">Complete repair</button>` : "";
  return `<article class="card booking-card"><div class="booking-top"><div><h3>${escapeHtml(item.vehicle.brand)} ${escapeHtml(item.vehicle.model)}</h3><p>${escapeHtml(item.vehicle.registrationNumber)} · ${escapeHtml(item.returnRecord.reservation.bookingReference)}</p></div>${badge(item.status)}</div>
    <p><strong>Repair required:</strong> ${escapeHtml(item.description)}</p>
    <p><strong>Damages found:</strong> ${escapeHtml(item.returnRecord.damagesFound || "—")}</p>
    <div class="booking-details"><div>Damage fee<strong>${money(item.returnRecord.damageFee)}</strong></div><div>Reported by<strong>${escapeHtml(item.reportedBy.name)}</strong></div><div>Reported on<strong>${dateTime(item.reportedAt)}</strong></div></div>
    ${item.startedAt ? `<p>Started ${dateTime(item.startedAt)}</p>` : ""}
    ${item.status === "COMPLETED"
      ? `<p><strong>Repair notes:</strong> ${escapeHtml(item.repairNotes)}</p><p>Completed ${dateTime(item.completedAt)}</p><div class="card-actions"><button class="btn btn-red btn-sm" data-action="hide-maintenance-history" data-id="${item.id}">Delete from view</button></div>`
      : `<div class="card-actions">${actions}</div>`}
    </article>`;
}

function openCompleteRepair(id) {
  const item = state.maintenance.find(request => request.id === id);
  if (!item) return;
  showModal("Complete repair", `${item.vehicle.registrationNumber} · ${item.vehicle.brand} ${item.vehicle.model}`, `<form id="repair-form" data-id="${id}">
    <p>${escapeHtml(item.description)}</p><div class="form-group"><label for="repair-notes">Repair notes</label><textarea id="repair-notes" name="repairNotes" maxlength="1000" required placeholder="Describe the work completed"></textarea></div>
    <p class="notice notice-info">Confirm that the vehicle is ready to rent again.</p>
    <div class="form-actions"><button type="button" class="btn btn-outline" data-action="close-modal">Cancel</button><button type="submit" class="btn btn-green">Complete repair</button></div></form>`);
}

document.addEventListener("click", async event => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const id = Number(target.dataset.id);
  try {
    if (target.dataset.action === "start-repair") {
      await request(`/api/maintenance/${id}/start`, { method: "PATCH" });
      await refreshCurrent(); toast("Repair started.");
    } else if (target.dataset.action === "complete-repair") openCompleteRepair(id);
    else if (target.dataset.action === "hide-maintenance-history") {
      const hidden = JSON.parse(localStorage.getItem("hidden_maintenance_history") || "[]");
      if (!hidden.includes(id)) { hidden.push(id); localStorage.setItem("hidden_maintenance_history", JSON.stringify(hidden)); }
      render(); toast("Maintenance record removed from history view.");
    }
  } catch (error) { toast(error.message, true); }
});

document.addEventListener("submit", async event => {
  const form = event.target;
  if (form.id !== "repair-form") return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  form.dataset.submitting = "true";
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  const values = formData(form);
  try {
    await request(`/api/maintenance/${form.dataset.id}/complete`, { method: "PATCH", body: values });
    closeModal(); await refreshCurrent(); toast("Repair completed. Vehicle availability updated.");
  } catch (error) { toast(error.message, true); }
  finally { delete form.dataset.submitting; if (submitButton) submitButton.disabled = false; }
});
