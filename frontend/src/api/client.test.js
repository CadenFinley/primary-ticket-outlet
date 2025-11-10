import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest, downloadCsv } from './client';

const ORIGINAL_FETCH = globalThis.fetch;
const ORIGINAL_URL = globalThis.URL;
const ORIGINAL_CREATE_ELEMENT = document.createElement;

afterEach(() => {
  vi.restoreAllMocks();
  globalThis.fetch = ORIGINAL_FETCH;
  globalThis.URL = ORIGINAL_URL;
  document.createElement = ORIGINAL_CREATE_ELEMENT;
});

describe('api/client', () => {
  it('performs json request and attaches authorization header', async () => {
    const responseBody = { success: true };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: vi.fn().mockResolvedValue(responseBody),
    });

    const result = await apiRequest('/status', {}, 'token-123');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/status', {
      headers: { Authorization: 'Bearer token-123' },
      method: 'GET',
    });
    expect(result).toEqual(responseBody);
  });

  it('throws ApiError when response is not ok', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: vi.fn().mockResolvedValue({ message: 'boom' }),
    });

    await expect(apiRequest('/status', {}, 'token-123')).rejects.toThrow(
      ApiError
    );
  });

  it('returns text when response is not json', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'text/plain' }),
      text: vi.fn().mockResolvedValue('plain text'),
    });

    const result = await apiRequest('/status', { plainText: true });
    expect(result).toEqual('plain text');
  });

  it('downloads csv by creating anchor tag', async () => {
    const revokeSpy = vi.fn();
    const appendSpy = vi
      .spyOn(document.body, 'appendChild')
      .mockImplementation(() => {});
    const clickMock = vi.fn();
    document.createElement = vi.fn().mockReturnValue({
      click: clickMock,
      remove: vi.fn(),
    });
    globalThis.URL = {
      ...globalThis.URL,
      createObjectURL: vi.fn().mockReturnValue('blob-url'),
      revokeObjectURL: revokeSpy,
    };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      blob: vi.fn().mockResolvedValue(new Blob(['content'], { type: 'text/csv' })),
    });

    await downloadCsv('/events/123/purchasers', 'token-123');

    expect(globalThis.fetch).toHaveBeenCalledWith('/api/events/123/purchasers', {
      headers: {
        Accept: 'text/csv',
        Authorization: 'Bearer token-123',
      },
    });
    expect(document.createElement).toHaveBeenCalledWith('a');
    expect(appendSpy).toHaveBeenCalled();
    expect(clickMock).toHaveBeenCalled();
    expect(revokeSpy).toHaveBeenCalledWith('blob-url');
  });
});
