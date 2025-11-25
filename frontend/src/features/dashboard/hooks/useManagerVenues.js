import { useCallback, useEffect, useState } from 'react';
import { apiRequest, downloadCsv } from '../../../api/client';

export function useManagerVenues(token, managedVenues) {
  const [eventsByVenue, setEventsByVenue] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const load = useCallback(async () => {
    if (!token || !managedVenues?.length) {
      setEventsByVenue({});
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const results = {};
      for (const venue of managedVenues) {
        const venueEvents = await apiRequest(
          `/venues/${venue.id}/events`,
          {},
          token
        );
        results[venue.id] = venueEvents ?? [];
      }
      setEventsByVenue(results);
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Failed to load events');
    } finally {
      setLoading(false);
    }
  }, [managedVenues, token]);

  useEffect(() => {
    load();
  }, [load]);

  const generateTickets = useCallback(
    async (eventId, quantity) => {
      try {
        await apiRequest(
          `/events/${eventId}/tickets:generate`,
          {
            method: 'POST',
            data: { quantity },
          },
          token
        );
        await load();
        setSuccessMessage(`Generated ${quantity} tickets.`);
      } catch (err) {
        console.error(err);
        setError(err.message);
      }
    },
    [load, token]
  );

  const downloadPurchasers = useCallback(
    async (eventId) => {
      try {
        await downloadCsv(`/events/${eventId}/purchasers`, token);
      } catch (err) {
        console.error(err);
        setError(err.message);
      }
    },
    [token]
  );

  return {
    eventsByVenue,
    loading,
    error,
    successMessage,
    setSuccessMessage,
    setError,
    generateTickets,
    downloadPurchasers,
    refresh: load,
  };
}
