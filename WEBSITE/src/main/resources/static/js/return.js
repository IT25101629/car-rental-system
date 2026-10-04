"use strict";

function driverHistoryCard(report) {
  const reservation = report.reservation;
  const vehicle = reservation?.vehicle;
  return `
    <article class="card booking-card">
      <div class="booking-top">
        <div>
          <h3>${escapeHtml(reservation?.bookingReference || "Trip")} · ${escapeHtml(vehicle?.brand || "")} ${escapeHtml(vehicle?.model || "")}</h3>
          <p>${escapeHtml(reservation?.customer?.name || "Customer")} · Registration: ${escapeHtml(vehicle?.registrationNumber || "—")}</p>
        </div>
        <div style="display:flex;flex-direction:column;align-items:flex-end;gap:0.35rem;">
          <span class="badge badge-green" style="font-size:0.75rem;">COMPLETED</span>
        </div>
      </div>
      <div class="booking-details">
        <div>Final Mileage<strong>${escapeHtml(report.finalMileage)} km</strong></div>
        <div>Final Fuel<strong>${escapeHtml(report.finalFuelLevel)}</strong></div>
        <div>Submitted<strong>${new Date(report.submittedAt).toLocaleDateString()}</strong></div>
        <div>Route<strong>${escapeHtml(reservation?.pickupLocation || "—")} → ${escapeHtml(reservation?.returnLocation || "—")}</strong></div>
      </div>
      ${report.damagesFound ? `<p><strong>Damages reported:</strong> ${escapeHtml(report.damagesFound)}</p>` : ""}
      ${report.remarks ? `<p><strong>Remarks:</strong> ${escapeHtml(report.remarks)}</p>` : ""}
      <div class="card-actions">
        <button class="btn btn-red btn-sm" data-action="hide-driver-trip" data-id="${report.id}">Delete from view</button>
      </div>
    </article>`;
}

function returnDetailFields(details, minimumMileage) {
  return `<div class="form-group"><label for="return-mileage">Final mileage (km)</label><input id="return-mileage" type="number" name="finalMileage" min="${minimumMileage}" step="1" value="${details.finalMileage ?? minimumMileage}" required></div>
    <div class="form-group"><label for="return-fuel">Fuel level</label><select id="return-fuel" name="finalFuelLevel" required>${["Full", "3/4", "1/2", "1/4", "Empty"].map(fuel => `<option ${fuel === details.finalFuelLevel ? "selected" : ""}>${fuel}</option>`).join("")}</select></div>
    <div class="form-group full"><label for="return-damages">Damages found</label><textarea id="return-damages" name="damagesFound" maxlength="1000" placeholder="Describe any damage, or enter None">${escapeHtml(details.damagesFound || "")}</textarea></div>
    <div class="form-group full"><label for="return-remarks">Remarks</label><textarea id="return-remarks" name="remarks" maxlength="1000">${escapeHtml(details.remarks || "")}</textarea></div>`;
}

function openDriverReturn(id) {
  const trip = state.reservations.find(item => item.id === id);
  if (!trip) return;
  showModal("Submit return details", trip.bookingReference, `<form id="driver-return-form" data-id="${id}">
    <div class="form-grid">${returnDetailFields({ finalFuelLevel: trip.vehicle.fuelLevel }, trip.vehicle.currentMileage || 0)}</div>
    <p class="notice notice-info">Staff will review these details and confirm the return. Your trip stays active until then.</p>
    <div class="form-actions"><button type="button" class="btn btn-outline" data-action="close-modal">Cancel</button><button class="btn btn-primary" type="submit">Send to staff</button></div></form>`);
}

function reportSummary(report) {
  return `<p><strong>Driver:</strong> ${escapeHtml(report.driver.name)} · <strong>Submitted:</strong> ${escapeHtml(new Date(report.submittedAt).toLocaleString())}</p>
    <div class="booking-details"><div>Reported mileage<strong>${escapeHtml(report.finalMileage)} km</strong></div><div>Reported fuel<strong>${escapeHtml(report.finalFuelLevel)}</strong></div></div>
    <p><strong>Damages:</strong> ${escapeHtml(report.damagesFound || "None reported")}</p><p><strong>Remarks:</strong> ${escapeHtml(report.remarks || "—")}</p>`;
}

function viewReturnReport(id) {
  const report = state.returnReports.find(item => item.reservation.id === id);
  if (!report) return;
  showModal("Driver return report", report.reservation.bookingReference, `${reportSummary(report)}
    <p class="notice notice-info">${report.status === "APPROVED" ? `Confirmed by ${escapeHtml(report.reviewedBy?.name || "staff")}.` : "Awaiting staff confirmation."}</p>`);
}

async function openReturn(id) {
  const item = state.reservations.find(reservation => reservation.id === id);
  const report = state.returnReports.find(report => report.reservation.id === id);
  if (!report || report.status !== "SUBMITTED") { toast("Wait for the driver's return details.", true); return; }
  const pickup = await request(`/api/pickups/reservation/${id}`);
  showModal("Review & confirm return", item.bookingReference, `${reportSummary(report)}<form id="return-form" data-id="${id}">
    <p>Verify the driver's details below and add any applicable charges.</p>
    <div class="form-grid">${returnDetailFields(report, pickup.initialMileage)}
    <div class="form-group"><label for="damage-fee">Damage fee (LKR)</label><input id="damage-fee" type="number" name="damageFee" min="0" step="0.01" value="0" required></div>
    <div class="form-group"><label for="fuel-fee">Fuel shortage fee (LKR)</label><input id="fuel-fee" type="number" name="fuelShortageFee" min="0" step="0.01" value="0" required></div>
    <div class="form-group"><label for="late-fee">Late return fee (LKR)</label><input id="late-fee" type="number" name="lateReturnFee" min="0" step="0.01" value="0" required></div></div>
    <div class="form-group"><label><input type="checkbox" name="maintenanceRequired" style="width:auto"> Maintenance required — send to fleet manager</label></div>
    <div class="form-group" data-maintenance-details hidden><label for="maintenance-description">Damage / repair required</label><textarea id="maintenance-description" name="maintenanceDescription" maxlength="1000" disabled placeholder="Describe what the fleet manager needs to repair">${escapeHtml(report.damagesFound || "")}</textarea></div>
    <p class="notice notice-info">Extra mileage charges and the final total are calculated when you confirm. Vehicles sent for maintenance stay unavailable until the fleet manager completes the repair.</p>
    <div class="form-actions"><button type="button" class="btn btn-outline" data-action="close-modal">Cancel</button><button class="btn btn-dark" type="submit">Confirm return</button></div></form>`);
}

document.addEventListener("click", async event => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const id = Number(target.dataset.id);
  try {
    if (target.dataset.action === "return-reservation") await openReturn(id);
    else if (target.dataset.action === "driver-return") openDriverReturn(id);
    else if (target.dataset.action === "view-return-report") viewReturnReport(id);
    else if (target.dataset.action === "hide-driver-trip") {
      const hidden = JSON.parse(localStorage.getItem("hidden_driver_trips") || "[]");
      if (!hidden.includes(id)) { hidden.push(id); localStorage.setItem("hidden_driver_trips", JSON.stringify(hidden)); }
      render(); toast("Trip removed from history view.");
    }
  } catch (error) { toast(error.message, true); }
});

document.addEventListener("change", event => {
  if (event.target.name !== "maintenanceRequired" || event.target.form?.id !== "return-form") return;
  const details = event.target.form.querySelector("[data-maintenance-details]");
  details.hidden = !event.target.checked;
  const input = details.querySelector("textarea");
  input.disabled = !event.target.checked;
  input.required = event.target.checked;
});

document.addEventListener("submit", async event => {
  const form = event.target;
  if (!["driver-return-form","return-form"].includes(form.id)) return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  form.dataset.submitting = "true";
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  const values = formData(form);
  try {
    if (form.id === "driver-return-form") {
      await request("/api/driver-return-reports", { method: "POST", body: { ...values, reservationId: Number(form.dataset.id), finalMileage: Number(values.finalMileage) }});
      closeModal(); await refreshCurrent(); toast("Return details sent. Awaiting staff confirmation.");
    } else {
      values.maintenanceRequired = values.maintenanceRequired === "on";
      const record = await request("/api/returns", { method: "POST", body: { ...values, reservationId: Number(form.dataset.id), staffId: state.user.id, finalMileage: Number(values.finalMileage), damageFee: Number(values.damageFee || 0), fuelShortageFee: Number(values.fuelShortageFee || 0), lateReturnFee: Number(values.lateReturnFee || 0) }});
      closeModal(); await refreshCurrent(); toast(`Return completed. Grand total: ${money(record.grandTotal)}.${values.maintenanceRequired ? " Maintenance request sent to fleet manager." : ""}`);
    }
  } catch (error) { toast(error.message, true); }
  finally { delete form.dataset.submitting; if (submitButton) submitButton.disabled = false; }
});
