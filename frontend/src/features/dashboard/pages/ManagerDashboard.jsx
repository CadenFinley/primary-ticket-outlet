import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  Divider,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { apiRequest } from '../../../api/client';
import { useAuth } from '../../auth';
import { useManagerVenues } from '../hooks/useManagerVenues';

function defaultDate(hoursAhead) {
  return dayjs().add(hoursAhead, 'hour').startOf('hour');
}

export default function ManagerDashboard() {
  const { token, user } = useAuth();
  const managedVenues = useMemo(
    () => user?.managedVenues ?? [],
    [user?.managedVenues]
  );
  const [form, setForm] = useState(() => ({
    venueId: managedVenues[0]?.id ?? '',
    title: '',
    description: '',
    startsAt: defaultDate(48),
    endsAt: defaultDate(52),
    faceValueCents: 7500,
  }));
  const [ticketForms, setTicketForms] = useState({});
  const {
    eventsByVenue,
    loading,
    error,
    successMessage,
    setError,
    setSuccessMessage,
    generateTickets,
    downloadPurchasers,
    refresh,
  } = useManagerVenues(token, managedVenues);

  useEffect(() => {
    setForm((prev) => ({
      ...prev,
      venueId: managedVenues[0]?.id ?? '',
    }));
  }, [managedVenues]);

  const handleChange = (key, value) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleStartChange = (value) => {
    setForm((prev) => ({
      ...prev,
      startsAt: value,
      endsAt: value ?? prev.endsAt,
    }));
  };

  const handleEndChange = (value) => {
    setForm((prev) => ({
      ...prev,
      endsAt:
        value && prev.startsAt && value.isBefore(prev.startsAt) ? prev.startsAt : value,
    }));
  };

  const updateTicketForm = (eventId, patch) => {
    setTicketForms((prev) => ({
      ...prev,
      [eventId]: {
        quantity: '50',
        ...prev[eventId],
        ...patch,
      },
    }));
  };

  const handleCreateEvent = async (event) => {
    event.preventDefault();
    setSuccessMessage(null);
    const startsAtIso = form.startsAt?.toISOString();
    const endsAtIso = form.endsAt?.toISOString();
    if (!startsAtIso || !endsAtIso) {
      setError('Please select both start and end times.');
      return;
    }
    try {
      await apiRequest(
        `/venues/${form.venueId}/events`,
        {
          method: 'POST',
          data: {
            title: form.title,
            description: form.description,
            startsAt: startsAtIso,
            endsAt: endsAtIso,
            faceValueCents: Number(form.faceValueCents),
          },
        },
        token
      );
      await refresh();
      setForm((prev) => ({
        ...prev,
        title: '',
        description: '',
        startsAt: defaultDate(48),
        endsAt: defaultDate(52),
      }));
      setSuccessMessage('Event created successfully.');
    } catch (err) {
      console.error(err);
      setError(err.message);
    }
  };

  const handleGenerateTickets = async (eventId) => {
    const current = ticketForms[eventId] ?? { quantity: '50' };
    const quantity = Number(current.quantity);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setError('Please enter a valid ticket quantity.');
      return;
    }
    try {
      await generateTickets(eventId, quantity);
      updateTicketForm(eventId, { quantity: 50 });
    } catch (err) {
      console.error(err);
      setError(err.message);
    }
  };

  const handleDownloadPurchasers = async (eventId) => {
    try {
      await downloadPurchasers(eventId);
    } catch (err) {
      console.error(err);
      setError(err.message);
    }
  };

  const allEvents = useMemo(() => {
    const list = [];
    Object.entries(eventsByVenue).forEach(([venueId, events]) => {
      events.forEach((eventItem) => {
        list.push({ ...eventItem, venueId });
      });
    });
    return list;
  }, [eventsByVenue]);

  if (!managedVenues.length) {
    return (
      <Alert severity="info">
        No venues assigned. Contact an administrator to grant venue manager
        access.
      </Alert>
    );
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <Stack spacing={3}>
        {error && <Alert severity="error">{error}</Alert>}
        {successMessage && <Alert severity="success">{successMessage}</Alert>}

        <Card variant="outlined">
          <CardHeader
            title="Create Event"
            titleTypographyProps={{ component: 'h2', variant: 'h5' }}
          />
          <CardContent>
            <Box component="form" onSubmit={handleCreateEvent}>
              <Stack spacing={2}>
                <TextField
                  label="Venue"
                  select
                  value={form.venueId}
                  onChange={(eventChange) =>
                    handleChange('venueId', eventChange.target.value)
                  }
                  required
                >
                  {managedVenues.map((venue) => (
                    <MenuItem key={venue.id} value={venue.id}>
                      {venue.name}
                    </MenuItem>
                  ))}
                </TextField>
                <TextField
                  label="Title"
                  value={form.title}
                  onChange={(eventChange) =>
                    handleChange('title', eventChange.target.value)
                  }
                  required
                />
                <TextField
                  label="Description"
                  multiline
                  minRows={3}
                  value={form.description}
                  onChange={(eventChange) =>
                    handleChange('description', eventChange.target.value)
                  }
                />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <DateTimePicker
                    label="Starts At"
                    value={form.startsAt}
                    onChange={handleStartChange}
                    slotProps={{ textField: { required: true, fullWidth: true } }}
                  />
                  <DateTimePicker
                    label="Ends At"
                    value={form.endsAt}
                    onChange={handleEndChange}
                    minDateTime={form.startsAt}
                    slotProps={{ textField: { required: true, fullWidth: true } }}
                  />
                </Stack>
                <TextField
                  label="Face Value (cents)"
                  type="number"
                  value={form.faceValueCents}
                  onChange={(eventChange) =>
                    handleChange('faceValueCents', eventChange.target.value)
                  }
                  required
                />
                <Button variant="contained" type="submit">
                  Create Event
                </Button>
              </Stack>
            </Box>
          </CardContent>
        </Card>

        <Divider />

        <Typography variant="h6">
          Upcoming Events ({allEvents.length})
        </Typography>
        {loading ? (
          <Typography>Loading events…</Typography>
        ) : (
          <Grid container spacing={2}>
            {managedVenues.map((venue) => (
              <Grid key={venue.id} item xs={12} md={6}>
                <Card variant="outlined">
                  <CardHeader
                    title={venue.name}
                    subheader={venue.location || 'Location TBA'}
                    titleTypographyProps={{ component: 'h3', variant: 'h6' }}
                    subheaderTypographyProps={{ component: 'p' }}
                  />
                  <CardContent>
                    <Stack spacing={2}>
                      {(eventsByVenue[venue.id] ?? []).map((eventItem) => {
                        const ticketState = ticketForms[eventItem.id] ?? { quantity: '50' };
                        return (
                          <Box
                            key={eventItem.id}
                            sx={{
                              border: '1px solid',
                              borderColor: 'divider',
                              borderRadius: 1,
                              p: 2,
                            }}
                          >
                            <Typography variant="subtitle1">
                              {eventItem.title}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {new Date(
                                eventItem.startsAt
                              ).toLocaleString()} • Tickets:{' '}
                              {eventItem.ticketsSold} / {eventItem.ticketsTotal}
                            </Typography>
                            <Typography variant="body2">
                              {eventItem.description || 'No description provided.'}
                            </Typography>
                            <CardActions sx={{ px: 0 }}>
                              <Stack
                                direction={{ xs: 'column', sm: 'row' }}
                                spacing={1}
                                width="100%"
                                alignItems={{ sm: 'center' }}
                              >
                                <TextField
                                  label="Ticket Quantity"
                                  type="number"
                                  size="small"
                                  value={ticketState.quantity}
                                  onChange={(eventChange) =>
                                    updateTicketForm(eventItem.id, {
                                      quantity: eventChange.target.value,
                                    })
                                  }
                                  inputProps={{ min: 1 }}
                                  sx={{ maxWidth: { sm: 180 } }}
                                />
                                <Button
                                  size="small"
                                  variant="contained"
                                  onClick={() => handleGenerateTickets(eventItem.id)}
                                  sx={{ whiteSpace: 'nowrap' }}
                                >
                                  Generate Tickets
                                </Button>
                                <Button
                                  size="small"
                                  onClick={() => handleDownloadPurchasers(eventItem.id)}
                                >
                                  Download Purchasers CSV
                                </Button>
                              </Stack>
                            </CardActions>
                          </Box>
                        );
                      })}
                      {!eventsByVenue[venue.id]?.length && (
                        <Typography color="text.secondary">
                          No events yet for this venue.
                        </Typography>
                      )}
                    </Stack>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Stack>
    </LocalizationProvider>
  );
}
