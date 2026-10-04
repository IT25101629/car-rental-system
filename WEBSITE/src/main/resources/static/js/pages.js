"use strict";

// Fill data containers in the current HTML page. Layout stays in the HTML files.
function renderPage() {
  switch (page) {
    case "home": {
      document.querySelector("#home-start").min = today;
      document.querySelector("#home-end").min = today;
      const featured = state.vehicles.filter(vehicle => vehicle.status === "AVAILABLE").slice(0, 3);
      setHtml("featured-vehicles", featured.map(vehicleCard).join("") || empty("No vehicles available", "Try again later."));
      break;
    }
    case "vehicles": {
      const parameters = new URLSearchParams(location.search);
      for (const [name, value] of parameters) {
        const input = document.querySelector("#vehicle-filter").elements.namedItem(name);
        if (input) input.value = value;
      }
      setText("vehicle-count", state.vehicles.length + " vehicles found");
      setHtml("vehicle-grid", state.vehicles.map(vehicleCard).join("") || empty("No matching vehicles", "Adjust your filters and try again."));
      break;
    }
    case "customer": {
      setText("booking-count", state.reservations.length);
      setText("active-count", state.reservations.filter(item => !["COMPLETED", "CANCELLED"].includes(item.status)).length);
      setHtml("reservations", state.reservations.map(item => reservationCard(item)).join("") || empty("No bookings yet", "Browse vehicles to create your first reservation."));
      break;
    }
    case "staff": {
      setText("pending-count", state.reservations.filter(item => item.status === "PENDING").length);
      setText("pickup-count", state.reservations.filter(item => item.status === "CONFIRMED").length);
      setText("return-count", state.reservations.filter(item => item.status === "PICKED_UP").length);
      setHtml("reservations", state.reservations.map(item => reservationCard(item, "staff")).join("") || empty("Queue is empty", "No rental operations need attention."));
      break;
    }
    case "fleet": {
      setText("vehicle-count", state.vehicles.length);
      setText("available-count", state.vehicles.filter(item => item.status === "AVAILABLE").length);
      setHtml("inventory-table", inventoryTable());
      const hiddenMaintenance = JSON.parse(localStorage.getItem("hidden_maintenance_history") || "[]");
      const openRepairs = state.maintenance.filter(item => item.status !== "COMPLETED");
      const completedRepairs = state.maintenance.filter(item => item.status === "COMPLETED" && !hiddenMaintenance.includes(item.id));
      setText("maintenance-count", `${openRepairs.length} open repair${openRepairs.length === 1 ? "" : "s"}`);
      setHtml("maintenance-list", openRepairs.map(maintenanceCard).join("") || empty("No open repairs", "Maintenance requests from rental staff will appear here."));
      setHtml("maintenance-history", completedRepairs.map(maintenanceCard).join("") || empty("No completed repairs", "Completed repairs will be kept here."));
      break;
    }
    case "admin": {
      setHtml("user-table", userTable());
      break;
    }
    case "driver": {
      const hiddenTrips = JSON.parse(localStorage.getItem("hidden_driver_trips") || "[]");
      const assigned = state.vehicles.filter(vehicle => vehicle.assignedDriver?.id === state.user.id);
      const trips = state.reservations.filter(item => item.status === "PICKED_UP");
      const history = (state.returnReports || []).filter(item => !hiddenTrips.includes(item.id));
      setHtml("assigned-vehicles", assigned.map(driverVehicleCard).join("") || empty("No vehicle assigned", "Fleet management will assign your next vehicle."));
      setHtml("trips", trips.map(item => reservationCard(item, "driver")).join("") || empty("No active trips", "Trips assigned to you will appear here."));
      setHtml("trip-history", history.map(driverHistoryCard).join("") || empty("No trip history", "Completed return reports will be shown here."));
      break;
    }
  }
}
