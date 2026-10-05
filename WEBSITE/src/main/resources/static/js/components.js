"use strict";

// Repeated cards and table rows populated from API responses.

function vehicleCard(vehicle) {
  const image = vehicle.imageUrl || "/favicon.svg";
  return `
    <article class="card vehicle-card">
      <img class="vehicle-image" src="${escapeHtml(image)}" alt="${escapeHtml(vehicle.brand)} ${escapeHtml(vehicle.model)}" loading="lazy">
      <div class="vehicle-body">
        <div class="vehicle-title"><div><h3>${escapeHtml(vehicle.brand)} ${escapeHtml(vehicle.model)}</h3><small>${escapeHtml(vehicle.year)} · ${escapeHtml(vehicle.category)}</small></div>${badge(vehicle.status)}</div>
        <div class="vehicle-meta"><span>👥 ${escapeHtml(vehicle.seatingCapacity)} seats</span><span>⚙ ${escapeHtml(vehicle.transmission)}</span><span>⛽ ${escapeHtml(vehicle.fuelType)}</span><span>🧳 ${escapeHtml(vehicle.luggageCapacity)} bags</span></div>
        <div class="vehicle-footer"><div class="price"><strong>${money(vehicle.rentalPricePerDay)}</strong><small> / day</small></div><button class="btn btn-dark btn-sm" data-action="vehicle-details" data-id="${vehicle.id}">View details</button></div>
      </div>
    </article>`;
}

function reservationCard(reservation, mode = "customer") {
  const actions = [];
  const report = state.returnReports.find(item => item.reservation.id === reservation.id);
  const returnRecord = state.returns.find(r => r.reservation?.id === reservation.id);
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
      <div class="booking-details"><div>Pickup<strong>${formatDate(reservation.startDate)}</strong></div><div>Return<strong>${formatDate(reservation.endDate)}</strong></div><div>Route<strong>${escapeHtml(reservation.pickupLocation)} → ${escapeHtml(reservation.returnLocation)}</strong></div><div>${returnRecord ? `Grand Total (incl. fees)` : `Total (${escapeHtml(reservation.totalDays)} days)`}<strong>${money(returnRecord ? returnRecord.grandTotal : reservation.totalAmount)}</strong></div>${returnRecord && returnRecord.totalAdditionalCharges > 0 ? `<div>Additional charges<strong>${money(returnRecord.totalAdditionalCharges)}</strong></div>` : ""}</div>
      <p>Chauffeur included${reservation.notes ? ` · ${escapeHtml(reservation.notes)}` : ""}</p>
      ${returnNotice}
      ${actions.length ? `<div class="card-actions">${actions.join("")}</div>` : ""}
    </article>`;
}

function inventoryTable() {
  if (!state.vehicles.length) return empty("No vehicles", "Add the first vehicle to the fleet.");
  return `<div class="table-wrap"><table><thead><tr><th>Vehicle</th><th>Registration</th><th>Rate</th><th>Status</th><th>Actions</th></tr></thead><tbody>${state.vehicles.map(vehicle => `<tr><td><strong>${escapeHtml(vehicle.brand)} ${escapeHtml(vehicle.model)}</strong><br><small>${escapeHtml(vehicle.year)} · ${escapeHtml(vehicle.category)}</small></td><td>${escapeHtml(vehicle.registrationNumber)}</td><td>${money(vehicle.rentalPricePerDay)}</td><td>${badge(vehicle.status)}</td><td><div class="table-actions"><button class="btn btn-outline btn-sm" data-action="edit-vehicle" data-id="${vehicle.id}">Edit</button><button class="btn btn-blue btn-sm" data-action="vehicle-status" data-id="${vehicle.id}">Status</button><button class="btn btn-red btn-sm" data-action="delete-vehicle" data-id="${vehicle.id}">Delete</button></div></td></tr>`).join("")}</tbody></table></div>`;
}

function userTable() {
  return `<div class="table-wrap"><table><thead><tr><th>User</th><th>Role</th><th>Actions</th></tr></thead><tbody>${state.users.map(user => `<tr><td><strong>${escapeHtml(user.name)}</strong><br><small>${escapeHtml(user.email)}</small></td><td>${escapeHtml(roleLabels[user.role])}</td><td><div class="table-actions"><button class="btn btn-outline btn-sm" data-action="change-role" data-id="${user.id}">Change role</button>${user.id !== state.user.id ? `<button class="btn btn-red btn-sm" data-action="delete-user" data-id="${user.id}">Delete</button>` : ""}</div></td></tr>`).join("")}</tbody></table></div>`;
}

function driverVehicleCard(vehicle) {
  return `<article class="booking-card"><div class="booking-top"><h3>${escapeHtml(vehicle.brand)} ${escapeHtml(vehicle.model)}</h3>${badge(vehicle.status)}</div><p>${escapeHtml(vehicle.registrationNumber)} · ${escapeHtml(vehicle.currentMileage)} km · Fuel ${escapeHtml(vehicle.fuelLevel)}</p></article>`;
}

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
