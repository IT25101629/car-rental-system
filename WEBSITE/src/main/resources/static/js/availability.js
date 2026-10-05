"use strict";

function vehicleStatusAction(id) {
  showModal("Update vehicle status", "Choose the current operational state.", `<form id="status-form" data-id="${id}"><div class="form-group"><label>Status</label><select name="status"><option>AVAILABLE</option><option>RESERVED</option><option>RENTED</option><option>UNDER_MAINTENANCE</option><option>BREAKDOWN</option></select></div><div class="form-actions"><button class="btn btn-primary">Update status</button></div></form>`);
}

function driverVehicleCard(vehicle) {
  return `<article class="booking-card"><div class="booking-top"><h3>${escapeHtml(vehicle.brand)} ${escapeHtml(vehicle.model)}</h3>${badge(vehicle.status)}</div><p>${escapeHtml(vehicle.registrationNumber)} · ${escapeHtml(vehicle.currentMileage)} km · Fuel ${escapeHtml(vehicle.fuelLevel)}</p></article>`;
}

document.addEventListener("click", event => {
  const target = event.target.closest("[data-action]");
  if (target?.dataset.action === "vehicle-status") vehicleStatusAction(Number(target.dataset.id));
});

document.addEventListener("submit", async event => {
  const form = event.target;
  if (form.id !== "status-form") return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  form.dataset.submitting = "true";
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  const values = formData(form);
  try {
    await request(`/api/vehicles/${form.dataset.id}/status?status=${values.status}`, { method: "PATCH" });
    closeModal(); await refreshCurrent(); toast("Vehicle status updated.");
  } catch (error) { toast(error.message, true); }
  finally { delete form.dataset.submitting; if (submitButton) submitButton.disabled = false; }
});
