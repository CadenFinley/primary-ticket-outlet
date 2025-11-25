# Event Management (Creating Events & Managing Venues)

This guide explains how managers create new events and why certain patterns were chosen.

## Frontend flow
1. **Route**: When a manager logs in or uses the role switcher, the URL is `/manager`, rendering `Frontend/src/features/dashboard/pages/ManagerDashboard.jsx` inside `DashboardLayout`.
2. **Loading venues**: `useManagerVenues.js` fetches `/venues/{venueId}/events` for each venue assigned to the manager (list comes from auth context).
3. **Creating an event**:
   - The form collects title, description, start/end times, and face value.
   - On submit it calls `apiRequest('/venues/{venueId}/events', { method: 'POST', data: {...}})`.
   - After success we call `refresh()` from the hook to reload the event list and display a success message.
4. **Generating tickets**:
   - Each event card includes an inline quantity input; clicking **Generate Tickets** calls `generateTickets(eventId, quantity)` which posts to `/events/{eventId}/tickets:generate` and refreshes data.
5. **Downloading purchasers**: `downloadPurchasers(eventId)` invokes `downloadCsv('/events/{eventId}/purchasers')`, generating a CSV download.

## Backend flow
- `VenueController.createEvent(...)` handles the POST to create an event, delegating to `EventService`.
- `EventService.generateTickets(...)` works with `TicketRepository` to add more tickets.
- `EventService.getPurchasersCsv(...)` streams the CSV for download.

## Files involved
- Frontend: `ManagerDashboard.jsx`, `useManagerVenues.js`, `api/client.js` (for `apiRequest` and `downloadCsv`).
- Backend: `controller/VenueController.java`, `controller/EventController.java`, `service/EventService.java`, `repository/EventRepository.java`.

## Why hooks?
`useManagerVenues` keeps the component readable by extracting repeated API logic (loading, errors, success messages). Hooks also make it easier to test data-fetching in isolation.

## Testing
- `ManagerDashboard.test.jsx` ensures the manager sees the info alert when no venues are assigned.
- Playwright manager scenario (`tests/e2e/login-and-role.spec.ts`) confirms form elements and buttons exist after login.
- Backend service tests validate event creation and ticket generation rules.

