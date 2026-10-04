"use strict";

async function openPickup(id) {
  const item = state.reservations.find(reservation => reservation.id === id);
  const drivers = await request("/api/pickups/available-drivers");
  showModal("Process vehicle pickup", item.bookingReference, `<form id="pickup-form" data-id="${id}"><div class="form-grid"><div class="form-group"><label>Initial mileage</label><input type="number" name="initialMileage" min="0" value="${item.vehicle.currentMileage || 0}" required></div><div class="form-group"><label>Fuel level</label><select name="initialFuelLevel"><option>Full</option><option>3/4</option><option>1/2</option><option>1/4</option><option>Empty</option></select></div><div class="form-group"><label>Driver</label><select name="driverId" required><option value="">${drivers.length ? "Select driver" : "No drivers available"}</option>${drivers.map(driver => `<option value="${driver.id}">${escapeHtml(driver.name)} · ${escapeHtml(driver.drivingLicense)}</option>`).join("")}</select></div><div class="form-group full"><label>Condition notes</label><textarea name="conditionNotes"></textarea></div><div class="form-group full"><label><input type="checkbox" name="customerSignatureConfirmed" style="width:auto" checked> Customer signature confirmed</label></div></div><div class="form-actions"><button class="btn btn-blue" type="submit">Complete handover</button></div></form>`);
}

document.addEventListener("click", async event => {
  const target = event.target.closest("[data-action]");
  if (target?.dataset.action !== "pickup-reservation") return;
  try { await openPickup(Number(target.dataset.id)); } catch (error) { toast(error.message, true); }
});

document.addEventListener("submit", async event => {
  const form = event.target;
  if (form.id !== "pickup-form") return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  form.dataset.submitting = "true";
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  const values = formData(form);
  try {
    await request("/api/pickups", { method: "POST", body: {
      ...values, reservationId: Number(form.dataset.id), staffId: state.user.id, driverId: Number(values.driverId), initialMileage: Number(values.initialMileage), customerSignatureConfirmed: values.customerSignatureConfirmed === "on"
    }});
    closeModal(); await refreshCurrent(); toast("Vehicle handover completed.");
  } catch (error) { toast(error.message, true); }
  finally { delete form.dataset.submitting; if (submitButton) submitButton.disabled = false; }
});
