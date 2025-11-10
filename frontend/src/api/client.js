const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

export async function apiRequest(path, options = {}, token) {
  const { method = 'GET', data, headers, plainText } = options;
  const fetchOptions = {
    method,
    headers: {
      ...(data ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
  };

  if (token) {
    fetchOptions.headers.Authorization = `Bearer ${token}`;
  }

  if (data !== undefined) {
    fetchOptions.body = JSON.stringify(data);
  }

  const response = await fetch(`${API_BASE}${path}`, fetchOptions);
  if (!response.ok) {
    const errorPayload = await safeParseJson(response);
    const message =
      errorPayload?.message || `Request to ${path} failed with ${response.status}`;
    throw new ApiError(message, response.status, errorPayload);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type');
  if (!plainText && contentType?.includes('application/json')) {
    return response.json();
  }
  return response.text();
}

export async function downloadCsv(path, token) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      Accept: 'text/csv',
    },
  });

  if (!response.ok) {
    const errorPayload = await safeParseJson(response);
    const message =
      errorPayload?.message || `CSV download failed with ${response.status}`;
    throw new ApiError(message, response.status, errorPayload);
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'purchasers.csv';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

async function safeParseJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
