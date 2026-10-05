"use strict";

const app = document.querySelector("#app");
const modal = document.querySelector("#app-modal");
const toastElement = document.querySelector("#toast");
const navLinks = document.querySelector("#nav-links");
const navActions = document.querySelector("#nav-actions");

const state = {
  user: null,
  vehicles: [],
  reservations: [],
  users: [],
  returnReports: [],
  maintenance: [],
  returns: []
};

const roleLabels = {
  ADMIN: "Admin",
  FLEET_MANAGER: "Fleet Manager",
  STAFF: "Rental Staff",
  DRIVER: "Driver",
  CUSTOMER: "Customer"
};

const today = new Date().toISOString().slice(0, 10);

const page = document.body.dataset.page;

function setText(id, value) {
  document.getElementById(id).textContent = value;
}

function setHtml(id, value) {
  document.getElementById(id).innerHTML = value;
}

async function request(path, options = {}) {
  const config = { credentials: "same-origin", ...options };
  if (config.body && typeof config.body !== "string") {
    config.headers = { "Content-Type": "application/json", ...(config.headers || {}) };
    config.body = JSON.stringify(config.body);
  }
  const response = await fetch(path, config);
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json()
    : await response.text();
  if (!response.ok) {
    const message = typeof data === "object" ? data.error || data.message : data;
    const error = new Error(message || `Request failed (${response.status})`);
    error.status = response.status;
    throw error;
  }
  return data;
}

function escapeHtml(value = "") {
  return String(value).replace(/[&<>'"]/g, character => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
  })[character]);
}

function money(value) {
  return new Intl.NumberFormat("en-LK", {
    style: "currency", currency: "LKR", maximumFractionDigits: 0
  }).format(Number(value || 0));
}

function formatDate(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-LK", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function badge(status) {
  const safe = escapeHtml(status || "UNKNOWN");
  return `<span class="badge badge-${safe}">${safe.replaceAll("_", " ")}</span>`;
}

function toast(message, isError = false) {
  toastElement.textContent = message;
  toastElement.className = `toast show${isError ? " error" : ""}`;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => toastElement.className = "toast", 3400);
}

function showModal(title, subtitle, body) {
  modal.innerHTML = `
    <div class="modal-head">
      <div><h2 id="modal-title">${escapeHtml(title)}</h2><p>${escapeHtml(subtitle || "")}</p></div>
      <button class="modal-close" type="button" data-action="close-modal" aria-label="Close">×</button>
    </div>
    <div class="modal-body">${body}</div>`;
  if (!modal.open) modal.showModal();
}

function closeModal() {
  if (modal.open) modal.close();
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function empty(title, text) {
  return `<div class="empty"><strong>${escapeHtml(title)}</strong>${escapeHtml(text)}</div>`;
}

function getNotifications() {
  const list = [];
  if (state.user && state.reservations && state.reservations.length) {
    state.reservations.slice(0, 5).forEach(res => {
      let title = `Booking · ${res.bookingReference}`;
      let msg = `${res.vehicle?.brand || "Vehicle"} ${res.vehicle?.model || ""} (${res.status})`;
      if (res.status === "PENDING") {
        title = `Booking Placed · ${res.bookingReference}`;
        msg = `Payment: ${res.paymentMethod || "CASH"} (${res.paymentStatus || "PENDING"}) · Awaiting staff review`;
      } else if (res.status === "CONFIRMED") {
        title = `Confirmed · ${res.bookingReference}`;
        msg = `Ready for pickup on ${formatDate(res.startDate)} with assigned chauffeur.`;
      } else if (res.status === "PICKED_UP") {
        title = `Active Trip · ${res.bookingReference}`;
        msg = `Chauffeur on trip. Return scheduled on ${formatDate(res.endDate)}.`;
      } else if (res.status === "COMPLETED") {
        title = `Completed · ${res.bookingReference}`;
        const ret = state.returns.find(r => r.reservation?.id === res.id);
        msg = `Rental finalized. Total: ${money(ret ? ret.grandTotal : res.totalAmount)}.`;
      }
      list.push({
        title: title,
        message: msg,
        time: formatDate(res.startDate)
      });
    });
  }
  if (state.user && state.maintenance && state.maintenance.length && ["FLEET_MANAGER", "ADMIN"].includes(state.user.role)) {
    state.maintenance.slice(0, 3).forEach(m => {
      list.push({
        title: `Maintenance · ${m.vehicle?.registrationNumber || "Vehicle"}`,
        message: `${m.status.replaceAll("_", " ")}: ${m.description || "Repair required"}`,
        time: "Fleet Alert"
      });
    });
  }
  return list;
}

function renderNavigation() {
  const portalOnly = state.user && state.user.role !== "CUSTOMER";
  const links = portalOnly ? [] : [
    { id: "home", label: "Home", url: "/index.html" },
    { id: "vehicles", label: "Vehicles", url: "/vehicles.html" }
  ];
  if (state.user) links.push({ id: "portal", label: roleLabels[state.user.role] + " Portal", url: "/portal" });
  document.querySelector(".brand").href = portalOnly ? "/portal" : "/index.html";
  const activeLink = ["home", "vehicles", "login", "register"].includes(page) ? page : "portal";
  navLinks.innerHTML = links.map(link => `<a class="nav-link ${activeLink === link.id ? "active" : ""}" href="${link.url}">${link.label}</a>`).join("");
  if (!state.user) {
    navActions.innerHTML = '<a class="btn btn-outline" href="/login.html">Log in</a><a class="btn btn-primary" href="/register.html">Create account</a>';
    return;
  }
  const notifications = getNotifications();
  const unreadCount = notifications.length;
  navActions.innerHTML = `
    <div class="notification-wrapper" style="position:relative;display:inline-block;">
      <button class="btn btn-outline btn-sm" data-action="toggle-notifications" title="Notifications" style="padding:0.4rem 0.65rem;font-size:0.95rem;display:flex;align-items:center;gap:0.35rem;">
        🔔 ${unreadCount > 0 ? `<span class="badge badge-amber" style="padding:0.1rem 0.4rem;font-size:0.75rem;border-radius:10px;">${unreadCount}</span>` : ""}
      </button>
      <div id="notifications-dropdown" class="card" hidden style="position:absolute;right:0;top:calc(100% + 8px);width:310px;max-height:360px;overflow-y:auto;z-index:200;box-shadow:0 10px 25px rgba(0,0,0,0.15);padding:0.75rem;text-align:left;background:#ffffff;border:1px solid #e2e8f0;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:0.5rem;border-bottom:1px solid #e2e8f0;padding-bottom:0.4rem;">
          <strong style="font-size:0.85rem;color:#0f172a;">🔔 Activity &amp; Alerts</strong>
          <small style="color:var(--text-muted);">${unreadCount} updates</small>
        </div>
        ${notifications.length ? notifications.map(n => `
          <div style="padding:0.5rem 0;border-bottom:1px solid #f1f5f9;font-size:0.82rem;">
            <div style="font-weight:600;color:#1e293b;">${escapeHtml(n.title)}</div>
            <div style="color:#64748b;font-size:0.78rem;margin-top:2px;">${escapeHtml(n.message)}</div>
            <small style="color:#0284c7;font-size:0.72rem;">${escapeHtml(n.time)}</small>
          </div>
        `).join("") : '<p style="color:#64748b;font-size:0.8rem;text-align:center;padding:0.8rem 0;">No new alerts</p>'}
      </div>
    </div>
    <div class="user-chip"><strong>${escapeHtml(state.user.name)}</strong><span>${escapeHtml(roleLabels[state.user.role])}</span></div>
    <button class="btn btn-dark" data-action="logout">Log out ↗</button>`;
}

function render() {
  renderNavigation();
  if (typeof renderPage === "function") renderPage();
}

async function loadRoleData() {
  state.reservations = [];
  state.users = [];
  state.returnReports = [];
  state.maintenance = [];
  state.returns = [];
  if (!state.user) return;
  const jobs = [];
  if (["STAFF", "FLEET_MANAGER", "ADMIN"].includes(state.user.role)) jobs.push(request("/api/maintenance").then(data => state.maintenance = data));
  if (state.user.role === "CUSTOMER") jobs.push(request(`/api/reservations?customerId=${state.user.id}`).then(data => state.reservations = data));
  if (["STAFF", "ADMIN"].includes(state.user.role)) jobs.push(request("/api/reservations").then(data => state.reservations = data));
  if (state.user.role === "DRIVER") jobs.push(request("/api/driver/trips").then(data => state.reservations = data));
  if (["STAFF", "ADMIN", "DRIVER"].includes(state.user.role)) jobs.push(request("/api/driver-return-reports").then(data => state.returnReports = data));
  if (["STAFF", "ADMIN"].includes(state.user.role)) jobs.push(request("/api/users").then(data => state.users = data));
  if (state.user.role === "CUSTOMER") jobs.push(request("/api/returns").then(data => state.returns = data.filter(r => r.reservation?.customer?.id === state.user.id)));
  if (["STAFF", "ADMIN"].includes(state.user.role)) jobs.push(request("/api/returns").then(data => state.returns = data));
  await Promise.all(jobs);
}

async function refreshCurrent() {
  state.vehicles = await request("/api/vehicles");
  await loadRoleData();
  render();
}

async function initialize() {
  try {
    try {
      state.user = await request("/api/auth/me");
    } catch (error) {
      state.user = null;
    }
    const privatePages = ["customer", "staff", "fleet", "admin", "driver"];
    if (privatePages.includes(page) && !state.user) {
      location.replace("/login.html");
      return;
    }
    if (state.user && ["login", "register"].includes(page)) {
      location.replace("/portal");
      return;
    }
    if (state.user && state.user.role !== "CUSTOMER" && ["home", "vehicles"].includes(page)) {
      location.replace("/portal");
      return;
    }
    if (!["login", "register"].includes(page)) {
      const parameters = new URLSearchParams();
      if (page === "vehicles") {
        const search = new URLSearchParams(location.search);
        for (const key of ["query", "category", "fuelType", "transmission", "maxPrice", "availableOnly", "startDate", "endDate"]) {
          if (search.get(key)) parameters.set(key, search.get(key));
        }
      }
      state.vehicles = await request("/api/vehicles" + (parameters.size ? "?" + parameters : ""));
      await loadRoleData();
    }
    render();
  } catch (error) {
    renderNavigation();
    toast(error.message, true);
    const notice = document.createElement("p");
    notice.className = "container notice notice-error";
    notice.textContent = error.message;
    app.prepend(notice);
  }
}

// Start after all deferred scripts have loaded.
document.addEventListener("DOMContentLoaded", initialize);

// Keep both desks updated while leaving any open form untouched.
let refreshingDashboard = false;
setInterval(async () => {
  if (!["driver", "staff", "fleet"].includes(page) || !state.user || modal.open || document.hidden || refreshingDashboard) return;
  refreshingDashboard = true;
  try { await refreshCurrent(); } catch (_) { /* The manual refresh button can retry. */ }
  finally { refreshingDashboard = false; }
}, 15000);
