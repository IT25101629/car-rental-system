"use strict";

function reservationCard(reservation, mode = "customer") {
  const actions = [];
  const report = state.returnReports.find(item => item.reservation.id === reservation.id);
  let returnNotice = "";
  if (mode === "staff" && reservation.status === "PENDING") {
    actions.push(`<button class="btn btn-green btn-sm" data-action="confirm-reservation" data-id="${reservation.id}">Confirm</button>`);
  }
  if (mode === "staff" && reservation.status === "CONFIRMED") {
    actions.push(`<button class="btn btn-blue btn-sm" data-action="pickup-reservation" data-id="${reservation.id}">Process pickup</button>`);
  }
  if (mode === "staff" && reservation.status === "PICKED_UP") {
    if (report?.status === "SUBMITTED") {
      returnNotice = '<p class="notice notice-info">Return details received from driver.</p>';
      actions.push(`<button class="btn btn-dark btn-sm" data-action="return-reservation" data-id="${reservation.id}">Review &amp; confirm return</button>`);
    } else {
      returnNotice = '<p class="notice notice-info">Awaiting driver return details.</p>';
    }
  }
  if (mode === "driver" && reservation.status === "PICKED_UP") {
    if (report) {
      returnNotice = '<p class="notice notice-info">Awaiting staff confirmation.</p>';
    } else {
      actions.push(`<button class="btn btn-primary btn-sm" data-action="driver-return" data-id="${reservation.id}">Submit return details</button>`);
    }
  }
  if (report && ["driver", "staff"].includes(mode)) {
    actions.push(`<button class="btn btn-outline btn-sm" data-action="view-return-report" data-id="${reservation.id}">View driver report</button>`);
  }
  if (mode === "staff" && reservation.status === "COMPLETED") {
    const maintenance = state.maintenance.find(item => item.returnRecord.reservation.id === reservation.id);
    if (maintenance) {
      returnNotice = `<p class="notice notice-info">Maintenance: ${escapeHtml(maintenance.status.replaceAll("_", " "))} · ${escapeHtml(maintenance.description)}</p>`;
    } else {
      actions.push(`<button class="btn btn-red btn-sm" data-action="delete-reservation" data-id="${reservation.id}">Delete</button>`);
    }
  }
  const isCard = reservation.paymentMethod === "CARD";
  const payBadge = isCard
    ? `<span class="badge badge-green" style="font-size:0.75rem;">Paid (${escapeHtml(reservation.paymentReference || "Card")})</span>`
    : `<span class="badge badge-amber" style="font-size:0.75rem;">Cash (${escapeHtml(reservation.paymentStatus || "Pending")})</span>`;

  return `
    <article class="card booking-card">
      <div class="booking-top">
        <div>
          <h3>${escapeHtml(reservation.bookingReference)} · ${escapeHtml(reservation.vehicle?.brand)} ${escapeHtml(reservation.vehicle?.model)}</h3>
          <p>${escapeHtml(reservation.customer?.name)} · ${escapeHtml(reservation.customer?.email)}</p>
        </div>
        <div style="display:flex;flex-direction:column;align-items:flex-end;gap:0.35rem;">
          ${badge(reservation.status)}
          ${payBadge}
        </div>
      </div>
      <div class="booking-details"><div>Pickup<strong>${formatDate(reservation.startDate)}</strong></div><div>Return<strong>${formatDate(reservation.endDate)}</strong></div><div>Route<strong>${escapeHtml(reservation.pickupLocation)} → ${escapeHtml(reservation.returnLocation)}</strong></div><div>Total (${escapeHtml(reservation.totalDays)} days)<strong>${money(reservation.totalAmount)}</strong></div></div>
      <p>Chauffeur included${reservation.notes ? ` · ${escapeHtml(reservation.notes)}` : ""}</p>
      ${returnNotice}
      ${actions.length ? `<div class="card-actions">${actions.join("")}</div>` : ""}
    </article>`;
}

function openBooking(id) {
  if (!state.user) { location.assign("/login.html"); return; }
  if (state.user.role !== "CUSTOMER") { toast("Only Customer accounts can create bookings.", true); return; }
  const vehicle = state.vehicles.find(item => item.id === id);
  showModal("Book your vehicle", `${vehicle.brand} ${vehicle.model} · ${money(vehicle.rentalPricePerDay)} per day`, `
    <form id="booking-form" data-id="${id}">
      <div class="form-grid">
        <div class="form-group"><label>Pickup date</label><input type="date" name="startDate" min="${today}" required></div>
        <div class="form-group"><label>Return date</label><input type="date" name="endDate" min="${today}" required></div>
        <div class="form-group"><label>Pickup location</label><input name="pickupLocation" required placeholder="e.g. Colombo Fort"></div>
        <div class="form-group"><label>Return location</label><input name="returnLocation" required placeholder="e.g. Kandy Hub"></div>
        <div class="form-group full"><label>Special notes</label><textarea name="notes" placeholder="Optional notes..."></textarea></div>
      </div>
      
      <div class="payment-section" style="margin: 1rem 0; padding: 0.9rem; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0;">
        <label style="font-weight: 600; display: block; margin-bottom: 0.5rem;">Choose Payment Method</label>
        <div style="display: flex; gap: 1.25rem; margin-bottom: 0.75rem;">
          <label style="display: flex; align-items: center; gap: 0.35rem; cursor: pointer; font-weight:500;">
            <input type="radio" name="paymentMethod" value="CARD" checked style="width:auto"> Credit / Debit Card
          </label>
          <label style="display: flex; align-items: center; gap: 0.35rem; cursor: pointer; font-weight:500;">
            <input type="radio" name="paymentMethod" value="CASH" style="width:auto"> Cash on Pickup
          </label>
        </div>

        <div id="card-payment-fields" class="form-grid" style="grid-template-columns: 2fr 1fr 1fr; gap: 0.5rem;">
          <div class="form-group" style="grid-column: span 3; margin-bottom:0.25rem;">
            <label style="font-size:0.8rem;">Cardholder Name</label>
            <input type="text" name="cardHolder" placeholder="${escapeHtml(state.user.name || "Name on card")}" value="${escapeHtml(state.user.name || "")}">
          </div>
          <div class="form-group" style="grid-column: span 3; margin-bottom:0.25rem;">
            <label style="font-size:0.8rem;">Card Number</label>
            <input type="text" name="cardNumber" maxlength="19" placeholder="4111 2222 3333 4444" value="4111 2222 3333 4444">
          </div>
          <div class="form-group" style="margin-bottom:0.25rem;">
            <label style="font-size:0.8rem;">Expiry (MM/YY)</label>
            <input type="text" name="cardExpiry" maxlength="5" placeholder="12/28" value="12/28">
          </div>
          <div class="form-group" style="margin-bottom:0.25rem;">
            <label style="font-size:0.8rem;">CVV</label>
            <input type="password" name="cardCvv" maxlength="4" placeholder="123" value="123">
          </div>
        </div>

        <div id="cash-payment-info" class="notice notice-info" hidden style="margin-top:0.5rem;">
          Pay in cash to our rental staff / chauffeur at vehicle handover.
        </div>
      </div>

      <div class="notice notice-info">Every vehicle rental includes an assigned chauffeur (+ LKR 2,500/day).</div>
      <div class="form-actions">
        <button type="button" class="btn btn-outline" data-action="close-modal">Cancel</button>
        <button class="btn btn-primary" type="submit">Confirm &amp; Book</button>
      </div>
    </form>`);
}

document.addEventListener("click", async event => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const id = Number(target.dataset.id);
  try {
    if (target.dataset.action === "book-vehicle") openBooking(id);
    else if (target.dataset.action === "confirm-reservation") {
      await request(`/api/reservations/${id}/confirm`, { method: "PATCH" });
      await refreshCurrent(); toast("Reservation confirmed.");
    } else if (target.dataset.action === "delete-reservation") {
      if (confirm("Permanently delete this completed reservation?")) {
        await request(`/api/reservations/${id}`, { method: "DELETE" });
        await refreshCurrent(); toast("Completed reservation deleted.");
      }
    }
  } catch (error) { toast(error.message, true); }
});

document.addEventListener("change", event => {
  if (event.target.name !== "paymentMethod" || event.target.form?.id !== "booking-form") return;
  const isCard = event.target.value === "CARD";
  const cardFields = event.target.form.querySelector("#card-payment-fields");
  const cashInfo = event.target.form.querySelector("#cash-payment-info");
  if (cardFields) cardFields.hidden = !isCard;
  if (cashInfo) cashInfo.hidden = isCard;
});

document.addEventListener("submit", async event => {
  const form = event.target;
  if (form.id !== "booking-form") return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  form.dataset.submitting = "true";
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  const values = formData(form);
  try {
    const reservation = await request("/api/reservations", { method: "POST", body: {
      ...values, customerId: state.user.id, vehicleId: Number(form.dataset.id), paymentMethod: values.paymentMethod || "CARD"
    }});
    closeModal();
    toast(`Booking ${reservation.bookingReference} created! Payment: ${reservation.paymentMethod} (${reservation.paymentStatus || "OK"}) · Total: ${money(reservation.totalAmount)}`);
    location.assign("/customer.html");
  } catch (error) { toast(error.message, true); }
  finally { delete form.dataset.submitting; if (submitButton) submitButton.disabled = false; }
});
