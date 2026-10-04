"use strict";

// Form submissions send input to the existing Java controllers.

document.addEventListener("click", async event => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const id = Number(target.dataset.id);
  try {
    switch (target.dataset.action) {
      case "toggle-menu": navLinks.classList.toggle("open"); break;
      case "toggle-notifications": {
        const drop = document.querySelector("#notifications-dropdown");
        if (drop) drop.hidden = !drop.hidden;
        break;
      }
      case "close-modal": closeModal(); break;
      case "logout":
        await request("/api/auth/logout", { method: "POST" });
        location.assign("/index.html"); break;
      case "vehicle-details": vehicleDetails(id); break;
      case "book-vehicle": openBooking(id); break;
      case "confirm-reservation": await request(`/api/reservations/${id}/confirm`, { method: "PATCH" }); await refreshCurrent(); toast("Reservation confirmed."); break;
      case "pickup-reservation": await openPickup(id); break;
      case "return-reservation": await openReturn(id); break;
      case "driver-return": openDriverReturn(id); break;
      case "view-return-report": viewReturnReport(id); break;
      case "start-repair":
        await request(`/api/maintenance/${id}/start`, { method: "PATCH" });
        await refreshCurrent(); toast("Repair started."); break;
      case "complete-repair": openCompleteRepair(id); break;
      case "hide-maintenance-history": {
        const hidden = JSON.parse(localStorage.getItem("hidden_maintenance_history") || "[]");
        if (!hidden.includes(id)) {
          hidden.push(id);
          localStorage.setItem("hidden_maintenance_history", JSON.stringify(hidden));
        }
        render();
        toast("Maintenance record removed from history view.");
        break;
      }
      case "hide-driver-trip": {
        const hidden = JSON.parse(localStorage.getItem("hidden_driver_trips") || "[]");
        if (!hidden.includes(id)) {
          hidden.push(id);
          localStorage.setItem("hidden_driver_trips", JSON.stringify(hidden));
        }
        render();
        toast("Trip removed from history view.");
        break;
      }
      case "delete-reservation":
        if (confirm("Permanently delete this completed reservation?")) {
          await request(`/api/reservations/${id}`, { method: "DELETE" });
          await refreshCurrent();
          toast("Completed reservation deleted.");
        }
        break;
      case "add-vehicle": vehicleForm(); break;
      case "edit-vehicle": vehicleForm(state.vehicles.find(item => item.id === id)); break;
      case "vehicle-status":
        showModal("Update vehicle status", "Choose the current operational state.", `<form id="status-form" data-id="${id}"><div class="form-group"><label>Status</label><select name="status"><option>AVAILABLE</option><option>RESERVED</option><option>RENTED</option><option>UNDER_MAINTENANCE</option><option>BREAKDOWN</option></select></div><div class="form-actions"><button class="btn btn-primary">Update status</button></div></form>`); break;
      case "delete-vehicle": if (confirm("Permanently delete this vehicle?")) { await request(`/api/vehicles/${id}`, { method: "DELETE" }); await refreshCurrent(); toast("Vehicle deleted."); } break;
      case "change-role": {
        const user = state.users.find(item => item.id === id);
        const isDriver = user.role === "DRIVER";
        showModal("Change user role", `${user.name} · ${user.email}`, `<form id="role-form" data-id="${id}"><div class="form-group"><label>Role</label><select name="role">${Object.entries(roleLabels).map(([value, label]) => `<option value="${value}" ${user.role === value ? "selected" : ""}>${label}</option>`).join("")}</select></div><div class="form-group" data-driver-license ${isDriver ? "" : "hidden"}><label>Driving licence number</label><input name="drivingLicense" value="${escapeHtml(user.drivingLicense || "")}" ${isDriver ? "required" : ""}></div><div class="form-actions"><button class="btn btn-primary">Save role</button></div></form>`); break;
      }
      case "delete-user": if (confirm("Permanently delete this user?")) { await request(`/api/users/${id}`, { method: "DELETE" }); await refreshCurrent(); toast("User deleted."); } break;
      case "refresh": await refreshCurrent(); toast("Data refreshed."); break;
    }
  } catch (error) { toast(error.message, true); }
});

document.addEventListener("change", event => {
  if (event.target.name === "maintenanceRequired" && event.target.form?.id === "return-form") {
    const details = event.target.form.querySelector("[data-maintenance-details]");
    details.hidden = !event.target.checked;
    const input = details.querySelector("textarea");
    input.disabled = !event.target.checked;
    input.required = event.target.checked;
    return;
  }
  if (event.target.id === "vehicle-image-file" && event.target.form?.id === "vehicle-form") {
    const urlInput = event.target.form.querySelector("#vehicle-image-url");
    if (urlInput && event.target.files?.[0]) {
      urlInput.value = "";
      urlInput.placeholder = `Will upload: ${event.target.files[0].name}`;
    }
    return;
  }
  if (event.target.name === "paymentMethod" && event.target.form?.id === "booking-form") {
    const isCard = event.target.value === "CARD";
    const cardFields = event.target.form.querySelector("#card-payment-fields");
    const cashInfo = event.target.form.querySelector("#cash-payment-info");
    if (cardFields) cardFields.hidden = !isCard;
    if (cashInfo) cashInfo.hidden = isCard;
    return;
  }
  if (event.target.name !== "role" || event.target.form?.id !== "role-form") return;
  const licenseGroup = event.target.form.querySelector("[data-driver-license]");
  const licenseInput = licenseGroup.querySelector("input");
  const isDriver = event.target.value === "DRIVER";
  licenseGroup.hidden = !isDriver;
  licenseInput.required = isDriver;
});

document.addEventListener("submit", async event => {
  const form = event.target;
  // Search forms use normal GET navigation so filters survive reload and Back.
  if (form.id === "quick-search" || form.id === "vehicle-filter") return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  form.dataset.submitting = "true";
  const submitButton = form.querySelector('[type="submit"]');
  if (submitButton) submitButton.disabled = true;
  const values = formData(form);
  try {
    if (form.id === "login-form") {
      state.user = await request("/api/auth/login", { method: "POST", body: values });
      location.assign("/portal");
    } else if (form.id === "register-form") {
      state.user = await request("/api/auth/register", { method: "POST", body: values });
      location.assign("/portal");
    } else if (form.id === "booking-form") {
      const reservation = await request("/api/reservations", { 
        method: "POST", 
        body: { 
          ...values, 
          customerId: state.user.id, 
          vehicleId: Number(form.dataset.id),
          paymentMethod: values.paymentMethod || "CARD"
        } 
      });
      closeModal(); 
      toast(`Booking ${reservation.bookingReference} created! Payment: ${reservation.paymentMethod} (${reservation.paymentStatus || "OK"}) · Total: ${money(reservation.totalAmount)}`); 
      location.assign("/customer.html");
    } else if (form.id === "pickup-form") {
      await request("/api/pickups", { method: "POST", body: { ...values, reservationId: Number(form.dataset.id), staffId: state.user.id, driverId: Number(values.driverId), initialMileage: Number(values.initialMileage), customerSignatureConfirmed: values.customerSignatureConfirmed === "on" } });
      closeModal(); await refreshCurrent(); toast("Vehicle handover completed.");
    } else if (form.id === "driver-return-form") {
      await request("/api/driver-return-reports", { method: "POST", body: { ...values, reservationId: Number(form.dataset.id), finalMileage: Number(values.finalMileage) } });
      closeModal(); await refreshCurrent(); toast("Return details sent. Awaiting staff confirmation.");
    } else if (form.id === "return-form") {
      values.maintenanceRequired = values.maintenanceRequired === "on";
      const record = await request("/api/returns", { method: "POST", body: { ...values, reservationId: Number(form.dataset.id), staffId: state.user.id, finalMileage: Number(values.finalMileage), damageFee: Number(values.damageFee || 0), fuelShortageFee: Number(values.fuelShortageFee || 0), lateReturnFee: Number(values.lateReturnFee || 0) } });
      closeModal(); await refreshCurrent(); toast(`Return completed. Grand total: ${money(record.grandTotal)}.${values.maintenanceRequired ? " Maintenance request sent to fleet manager." : ""}`);
    } else if (form.id === "repair-form") {
      await request(`/api/maintenance/${form.dataset.id}/complete`, { method: "PATCH", body: values });
      closeModal(); await refreshCurrent(); toast("Repair completed. Vehicle availability updated.");
    } else if (form.id === "vehicle-form") {
      const fileInput = form.querySelector("#vehicle-image-file");
      if (fileInput && fileInput.files && fileInput.files[0]) {
        const uploadData = new FormData();
        uploadData.append("file", fileInput.files[0]);
        const uploadRes = await fetch("/api/vehicles/upload-image", {
          method: "POST",
          body: uploadData,
          credentials: "include"
        });
        if (!uploadRes.ok) {
          const errData = await uploadRes.json().catch(() => ({}));
          throw new Error(errData.message || "Failed to upload image file");
        }
        const uploadJson = await uploadRes.json();
        values.imageUrl = uploadJson.imageUrl;
      }
      delete values.imageFile;
      const numbers = ["year","rentalPricePerDay","seatingCapacity","luggageCapacity","mileageRateLimit","extraMileageRate","currentMileage"];
      numbers.forEach(key => values[key] = Number(values[key] || 0));
      values.status = form.dataset.id ? state.vehicles.find(item => item.id === Number(form.dataset.id)).status : "AVAILABLE";
      await request(form.dataset.id ? `/api/vehicles/${form.dataset.id}` : "/api/vehicles", { method: form.dataset.id ? "PUT" : "POST", body: values });
      closeModal(); await refreshCurrent(); toast(form.dataset.id ? "Vehicle updated." : "Vehicle added.");
    } else if (form.id === "status-form") {
      await request(`/api/vehicles/${form.dataset.id}/status?status=${values.status}`, { method: "PATCH" }); closeModal(); await refreshCurrent(); toast("Vehicle status updated.");
    } else if (form.id === "role-form") {
      const body = { role: values.role };
      if (values.role === "DRIVER") body.drivingLicense = values.drivingLicense;
      await request(`/api/users/${form.dataset.id}`, { method: "PUT", body }); closeModal(); await refreshCurrent(); toast("User role updated.");
    }
  } catch (error) { toast(error.message, true); }
  finally {
    delete form.dataset.submitting;
    if (submitButton) submitButton.disabled = false;
  }
});

modal.addEventListener("click", event => {
  if (event.target === modal) closeModal();
});
