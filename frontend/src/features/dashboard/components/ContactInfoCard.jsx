import { Alert, Button, Card, CardContent, CardHeader, Stack, TextField, Typography } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { apiRequest } from '../../../api/client';

export default function ContactInfoCard({ token, user, onUpdated }) {
  const [form, setForm] = useState(() => ({
    address: user?.address ?? '',
    phoneNumber: user?.phoneNumber ?? '',
  }));
  const [status, setStatus] = useState({ saving: false, success: null, error: null });

  useEffect(() => {
    setForm({
      address: user?.address ?? '',
      phoneNumber: user?.phoneNumber ?? '',
    });
  }, [user?.address, user?.phoneNumber]);

  const hasChanges = useMemo(() => {
    const normalizedAddress = (form.address ?? '').trim();
    const normalizedPhone = (form.phoneNumber ?? '').trim();
    const currentAddress = (user?.address ?? '').trim();
    const currentPhone = (user?.phoneNumber ?? '').trim();
    return normalizedAddress !== currentAddress || normalizedPhone !== currentPhone;
  }, [form.address, form.phoneNumber, user?.address, user?.phoneNumber]);

  if (!token) {
    return null;
  }

  const handleChange = (field) => (event) => {
    const value = event.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
    setStatus((prev) => ({ ...prev, error: null }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!hasChanges || status.saving) {
      return;
    }
    setStatus({ saving: true, success: null, error: null });
    const address = form.address.trim();
    const phoneNumber = form.phoneNumber.trim();
    try {
      const updated = await apiRequest(
        '/me/contact-info',
        {
          method: 'PUT',
          data: {
            address: address || null,
            phoneNumber: phoneNumber || null,
          },
        },
        token
      );
      onUpdated?.(updated);
      setStatus({ saving: false, success: 'Contact info saved.', error: null });
    } catch (err) {
      console.error(err);
      setStatus({ saving: false, success: null, error: err.message ?? 'Failed to save contact info.' });
    }
  };

  return (
    <Card variant="outlined">
      <CardHeader
        title="Contact Information"
        subheader="Share optional details so event staff can reach you if needed."
        titleTypographyProps={{ component: 'h2', variant: 'h6' }}
      />
      <CardContent>
        <Stack component="form" spacing={2} onSubmit={handleSubmit}>
          <Typography variant="body2" color="text.secondary">
            Address and phone are kept private but appear in manager CSV downloads for events you've purchased.
          </Typography>
          {status.error && <Alert severity="error">{status.error}</Alert>}
          {status.success && <Alert severity="success">{status.success}</Alert>}
          <TextField
            label="Mailing Address"
            value={form.address}
            onChange={handleChange('address')}
            placeholder="123 Main St, Springfield, IL"
            multiline
            minRows={2}
          />
          <TextField
            label="Phone Number"
            value={form.phoneNumber}
            onChange={handleChange('phoneNumber')}
            placeholder="(555) 123-4567"
          />
          <Button
            type="submit"
            variant="contained"
            disabled={!hasChanges || status.saving}
          >
            {status.saving ? 'Saving…' : 'Save Contact Info'}
          </Button>
        </Stack>
      </CardContent>
    </Card>
  );
}
