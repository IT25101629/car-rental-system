"use strict";

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

function inventoryTable() {
  if (!state.vehicles.length) return empty("No vehicles", "Add the first vehicle to the fleet.");
  return `<div class="table-wrap"><table><thead><tr><th>Vehicle</th><th>Registration</th><th>Rate</th><th>Status</th><th>Actions</th></tr></thead><tbody>${state.vehicles.map(vehicle => `<tr><td><strong>${escapeHtml(vehicle.brand)} ${escapeHtml(vehicle.model)}</strong><br><small>${escapeHtml(vehicle.year)} · ${escapeHtml(vehicle.category)}</small></td><td>${escapeHtml(vehicle.registrationNumber)}</td><td>${money(vehicle.rentalPricePerDay)}</td><td>${badge(vehicle.status)}</td><td><div class="table-actions"><button class="btn btn-outline btn-sm" data-action="edit-vehicle" data-id="${vehicle.id}">Edit</button><button class="btn btn-blue btn-sm" data-action="vehicle-status" data-id="${vehicle.id}">Status</button><button class="btn btn-red btn-sm" data-action="delete-vehicle" data-id="${vehicle.id}">Delete</button></div></td></tr>`).join("")}</tbody></table></div>`;
}

function vehicleDetails(id) {
  const vehicle = state.vehicles.find(item => item.id === id);
  if (!vehicle) return;
  const canBook = vehicle.status === "AVAILABLE" && (!state.user || state.user.role === "CUSTOMER");
  const features = (vehicle.features || "").split(",").filter(Boolean).map(item => `<span>${escapeHtml(item.trim())}</span>`).join("");
  showModal(`${vehicle.brand} ${vehicle.model}`, `${vehicle.year} ${vehicle.category} · ${vehicle.registrationNumber}`, `<img class="modal-image" src="${escapeHtml(vehicle.imageUrl || "/favicon.svg")}" alt="Vehicle"><div class="booking-details"><div>Daily rate<strong>${money(vehicle.rentalPricePerDay)}</strong></div><div>Status<strong>${escapeHtml(vehicle.status)}</strong></div><div>Free mileage<strong>${escapeHtml(vehicle.mileageRateLimit)} km/day</strong></div><div>Extra kilometre<strong>${money(vehicle.extraMileageRate)}</strong></div></div><div class="feature-list">${features}</div><div class="form-actions"><button class="btn btn-outline" data-action="close-modal">Close</button>${canBook ? `<button class="btn btn-primary" data-action="book-vehicle" data-id="${vehicle.id}">Book now</button>` : ""}</div>`);
}

function vehicleForm(vehicle = {}) {
  const editing = Boolean(vehicle.id);
  showModal(editing ? "Edit vehicle" : "Add vehicle", "Fleet inventory record", `<form id="vehicle-form" data-id="${vehicle.id || ""}"><div class="form-grid"><div class="form-group"><label>Brand</label><input name="brand" value="${escapeHtml(vehicle.brand || "")}" required></div><div class="form-group"><label>Model</label><input name="model" value="${escapeHtml(vehicle.model || "")}" required></div><div class="form-group"><label>Year</label><input type="number" name="year" value="${vehicle.year || new Date().getFullYear()}" required></div><div class="form-group"><label>Category</label><select name="category">${["Economy","Sedan","SUV","Luxury","Van"].map(value => `<option ${vehicle.category === value ? "selected" : ""}>${value}</option>`).join("")}</select></div><div class="form-group"><label>Registration number</label><input name="registrationNumber" value="${escapeHtml(vehicle.registrationNumber || "")}" required></div><div class="form-group"><label>Daily rate (LKR)</label><input type="number" name="rentalPricePerDay" min="0" value="${vehicle.rentalPricePerDay || ""}" required></div><div class="form-group"><label>Seats</label><input type="number" name="seatingCapacity" min="1" value="${vehicle.seatingCapacity || 5}"></div><div class="form-group"><label>Luggage</label><input type="number" name="luggageCapacity" min="0" value="${vehicle.luggageCapacity || 2}"></div><div class="form-group"><label>Fuel</label><select name="fuelType">${["Petrol","Diesel","Hybrid","Electric"].map(value => `<option ${vehicle.fuelType === value ? "selected" : ""}>${value}</option>`).join("")}</select></div><div class="form-group"><label>Transmission</label><select name="transmission"><option ${vehicle.transmission === "Automatic" ? "selected" : ""}>Automatic</option><option ${vehicle.transmission === "Manual" ? "selected" : ""}>Manual</option></select></div><div class="form-group"><label>Free km/day</label><input type="number" name="mileageRateLimit" min="0" value="${vehicle.mileageRateLimit ?? 100}"></div><div class="form-group"><label>Extra km rate</label><input type="number" name="extraMileageRate" min="0" value="${vehicle.extraMileageRate ?? 80}"></div><div class="form-group"><label>Current mileage</label><input type="number" name="currentMileage" min="0" value="${vehicle.currentMileage || 0}"></div><div class="form-group"><label>Fuel level</label><select name="fuelLevel">${["Full","3/4","1/2","1/4","Empty"].map(value => `<option ${vehicle.fuelLevel === value ? "selected" : ""}>${value}</option>`).join("")}</select></div><div class="form-group full"><label for="vehicle-image-file">Vehicle image (.png, .jpg)</label><input type="file" id="vehicle-image-file" name="imageFile" accept="image/png, image/jpeg, image/webp"><div style="margin-top:6px;"><label for="vehicle-image-url" style="font-size:0.8rem;color:var(--text-muted);">Or image URL / path:</label><input id="vehicle-image-url" type="text" name="imageUrl" placeholder="/uploads/... or https://..." value="${escapeHtml(vehicle.imageUrl || "")}"></div></div><div class="form-group full"><label>Features (comma separated)</label><textarea name="features">${escapeHtml(vehicle.features || "")}</textarea></div></div><div class="form-actions"><button class="btn btn-primary" type="submit">${editing ? "Save vehicle" : "Add vehicle"}</button></div></form>`);
}

// Vehicle Catalog UI actions.
document.addEventListener("click", async event => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const id = Number(target.dataset.id);
  try {
    if (target.dataset.action === "vehicle-details") vehicleDetails(id);
    else if (target.dataset.action === "add-vehicle") vehicleForm();
    else if (target.dataset.action === "edit-vehicle") vehicleForm(state.vehicles.find(item => item.id === id));
    else if (target.dataset.action === "delete-vehicle") {
      if (confirm("Permanently delete this vehicle?")) {
        await request(`/api/vehicles/${id}`, { method: "DELETE" });
        await refreshCurrent();
        toast("Vehicle deleted.");
      }
    }
  } catch (error) { toast(error.message, true); }
});

document.addEventListener("change", event => {
  if (event.target.id !== "vehicle-image-file" || event.target.form?.id !== "vehicle-form") return;
  const urlInput = event.target.form.querySelector("#vehicle-image-url");
  if (urlInput && event.target.files?.[0]) {
    urlInput.value = "";
    urlInput.placeholder = `Will upload: ${event.target.files[0].name}`;
  }
});

document.addEventListener("submit", async event => {
  const form = event.target;
  if (form.id !== "vehicle-form") return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  form.dataset.submitting = "true";
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  const values = formData(form);
  try {
    const fileInput = form.querySelector("#vehicle-image-file");
    if (fileInput && fileInput.files && fileInput.files[0]) {
      const uploadData = new FormData();
      uploadData.append("file", fileInput.files[0]);
      const uploadRes = await fetch("/api/vehicles/upload-image", {
        method: "POST", body: uploadData, credentials: "include"
      });
      if (!uploadRes.ok) {
        const errData = await uploadRes.json().catch(() => ({}));
        throw new Error(errData.message || "Failed to upload image file");
      }
      values.imageUrl = (await uploadRes.json()).imageUrl;
    }
    delete values.imageFile;
    const numbers = ["year","rentalPricePerDay","seatingCapacity","luggageCapacity","mileageRateLimit","extraMileageRate","currentMileage"];
    numbers.forEach(key => values[key] = Number(values[key] || 0));
    values.status = form.dataset.id ? state.vehicles.find(item => item.id === Number(form.dataset.id)).status : "AVAILABLE";
    await request(form.dataset.id ? `/api/vehicles/${form.dataset.id}` : "/api/vehicles", { method: form.dataset.id ? "PUT" : "POST", body: values });
    closeModal(); await refreshCurrent(); toast(form.dataset.id ? "Vehicle updated." : "Vehicle added.");
  } catch (error) { toast(error.message, true); }
  finally { delete form.dataset.submitting; if (submitButton) submitButton.disabled = false; }
});
