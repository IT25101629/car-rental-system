"use strict";

// Dialogs for editing records on the current page.

function vehicleDetails(id) {
  const vehicle = state.vehicles.find(item => item.id === id);
  if (!vehicle) return;
  const canBook = vehicle.status === "AVAILABLE" && (!state.user || state.user.role === "CUSTOMER");
  const features = (vehicle.features || "").split(",").filter(Boolean).map(item => `<span>${escapeHtml(item.trim())}</span>`).join("");
  showModal(`${vehicle.brand} ${vehicle.model}`, `${vehicle.year} ${vehicle.category} · ${vehicle.registrationNumber}`, `<img class="modal-image" src="${escapeHtml(vehicle.imageUrl || "/favicon.svg")}" alt="Vehicle"><div class="booking-details"><div>Daily rate<strong>${money(vehicle.rentalPricePerDay)}</strong></div><div>Status<strong>${escapeHtml(vehicle.status)}</strong></div><div>Free mileage<strong>${escapeHtml(vehicle.mileageRateLimit)} km/day</strong></div><div>Extra kilometre<strong>${money(vehicle.extraMileageRate)}</strong></div></div><div class="feature-list">${features}</div><div class="form-actions"><button class="btn btn-outline" data-action="close-modal">Close</button>${canBook ? `<button class="btn btn-primary" data-action="book-vehicle" data-id="${vehicle.id}">Book now</button>` : ""}</div>`);
}

function openBooking(id) {
  if (!state.user) { location.assign("/login.html"); return; }
  if (state.user.role !== "CUSTOMER") { toast("Only Customer accounts can create bookings.", true); return; }
  const vehicle = state.vehicles.find(item => item.id === id);
  showModal("Book your vehicle", `${vehicle.brand} ${vehicle.model} · ${money(vehicle.rentalPricePerDay)} per day`, `
    <form id="booking-form" data-id="${id}" autocomplete="off">
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
            <input type="text" name="chName" autocomplete="off" placeholder="${escapeHtml(state.user.name || "Name on card")}" value="${escapeHtml(state.user.name || "")}">
          </div>
          <div class="form-group" style="grid-column: span 3; margin-bottom:0.25rem;">
            <label style="font-size:0.8rem;">Card Number</label>
            <input type="text" name="chNum" autocomplete="off" maxlength="19" placeholder="XXXX XXXX XXXX XXXX">
          </div>
          <div class="form-group" style="margin-bottom:0.25rem;">
            <label style="font-size:0.8rem;">Expiry (MM/YY)</label>
            <input type="text" name="chExp" autocomplete="off" maxlength="5" placeholder="MM/YY">
          </div>
          <div class="form-group" style="margin-bottom:0.25rem;">
            <label style="font-size:0.8rem;">CVV</label>
            <input type="text" name="chCvv" autocomplete="off" maxlength="4" placeholder="CVV">
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

async function openPickup(id) {
  const item = state.reservations.find(reservation => reservation.id === id);
  const drivers = await request("/api/pickups/available-drivers");
  showModal("Process vehicle pickup", item.bookingReference, `<form id="pickup-form" data-id="${id}"><div class="form-grid"><div class="form-group"><label>Initial mileage</label><input type="number" name="initialMileage" min="0" value="${item.vehicle.currentMileage || 0}" required></div><div class="form-group"><label>Fuel level</label><select name="initialFuelLevel"><option>Full</option><option>3/4</option><option>1/2</option><option>1/4</option><option>Empty</option></select></div><div class="form-group"><label>Driver</label><select name="driverId" required><option value="">${drivers.length ? "Select driver" : "No drivers available"}</option>${drivers.map(driver => `<option value="${driver.id}">${escapeHtml(driver.name)} · ${escapeHtml(driver.drivingLicense)}</option>`).join("")}</select></div><div class="form-group full"><label>Condition notes</label><textarea name="conditionNotes"></textarea></div><div class="form-group full"><label><input type="checkbox" name="customerSignatureConfirmed" style="width:auto" checked> Customer signature confirmed</label></div></div><div class="form-actions"><button class="btn btn-blue" type="submit">Complete handover</button></div></form>`);
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

function vehicleForm(vehicle = {}) {
  const editing = Boolean(vehicle.id);
  showModal(editing ? "Edit vehicle" : "Add vehicle", "Fleet inventory record", `<form id="vehicle-form" data-id="${vehicle.id || ""}"><div class="form-grid"><div class="form-group"><label>Brand</label><input name="brand" value="${escapeHtml(vehicle.brand || "")}" required></div><div class="form-group"><label>Model</label><input name="model" value="${escapeHtml(vehicle.model || "")}" required></div><div class="form-group"><label>Year</label><input type="number" name="year" value="${vehicle.year || new Date().getFullYear()}" required></div><div class="form-group"><label>Category</label><select name="category">${["Economy","Sedan","SUV","Luxury","Van"].map(value => `<option ${vehicle.category === value ? "selected" : ""}>${value}</option>`).join("")}</select></div><div class="form-group"><label>Registration number</label><input name="registrationNumber" value="${escapeHtml(vehicle.registrationNumber || "")}" required></div><div class="form-group"><label>Daily rate (LKR)</label><input type="number" name="rentalPricePerDay" min="0" value="${vehicle.rentalPricePerDay || ""}" required></div><div class="form-group"><label>Seats</label><input type="number" name="seatingCapacity" min="1" value="${vehicle.seatingCapacity || 5}"></div><div class="form-group"><label>Luggage</label><input type="number" name="luggageCapacity" min="0" value="${vehicle.luggageCapacity || 2}"></div><div class="form-group"><label>Fuel</label><select name="fuelType">${["Petrol","Diesel","Hybrid","Electric"].map(value => `<option ${vehicle.fuelType === value ? "selected" : ""}>${value}</option>`).join("")}</select></div><div class="form-group"><label>Transmission</label><select name="transmission"><option ${vehicle.transmission === "Automatic" ? "selected" : ""}>Automatic</option><option ${vehicle.transmission === "Manual" ? "selected" : ""}>Manual</option></select></div><div class="form-group"><label>Free km/day</label><input type="number" name="mileageRateLimit" min="0" value="${vehicle.mileageRateLimit ?? 100}"></div><div class="form-group"><label>Extra km rate</label><input type="number" name="extraMileageRate" min="0" value="${vehicle.extraMileageRate ?? 80}"></div><div class="form-group"><label>Current mileage</label><input type="number" name="currentMileage" min="0" value="${vehicle.currentMileage || 0}"></div><div class="form-group"><label>Fuel level</label><select name="fuelLevel">${["Full","3/4","1/2","1/4","Empty"].map(value => `<option ${vehicle.fuelLevel === value ? "selected" : ""}>${value}</option>`).join("")}</select></div><div class="form-group full"><label for="vehicle-image-file">Vehicle image (.png, .jpg)</label><input type="file" id="vehicle-image-file" name="imageFile" accept="image/png, image/jpeg, image/webp"><div style="margin-top:6px;"><label for="vehicle-image-url" style="font-size:0.8rem;color:var(--text-muted);">Or image URL / path:</label><input id="vehicle-image-url" type="text" name="imageUrl" placeholder="/uploads/... or https://..." value="${escapeHtml(vehicle.imageUrl || "")}"></div></div><div class="form-group full"><label>Features (comma separated)</label><textarea name="features">${escapeHtml(vehicle.features || "")}</textarea></div></div><div class="form-actions"><button class="btn btn-primary" type="submit">${editing ? "Save vehicle" : "Add vehicle"}</button></div></form>`);
}

function openCompleteRepair(id) {
  const item = state.maintenance.find(request => request.id === id);
  if (!item) return;
  showModal("Complete repair", `${item.vehicle.registrationNumber} · ${item.vehicle.brand} ${item.vehicle.model}`, `<form id="repair-form" data-id="${id}">
    <p>${escapeHtml(item.description)}</p><div class="form-group"><label for="repair-notes">Repair notes</label><textarea id="repair-notes" name="repairNotes" maxlength="1000" required placeholder="Describe the work completed"></textarea></div>
    <p class="notice notice-info">Confirm that the vehicle is ready to rent again.</p>
    <div class="form-actions"><button type="button" class="btn btn-outline" data-action="close-modal">Cancel</button><button type="submit" class="btn btn-green">Complete repair</button></div></form>`);
}
