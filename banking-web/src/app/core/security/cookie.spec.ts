import { readCookie } from './cookie';

describe('readCookie', () => {
  it('reads and decodes the requested cookie', () => {
    const cookie = 'theme=light; NB-XSRF-TOKEN=token%2Bseguro; language=es';

    expect(readCookie(cookie, 'NB-XSRF-TOKEN')).toBe('token+seguro');
  });

  it('does not match a cookie with a similar name', () => {
    expect(readCookie('OLD-NB-XSRF-TOKEN=value', 'NB-XSRF-TOKEN')).toBe('');
  });

  it('returns an empty value when the cookie is absent', () => {
    expect(readCookie('', 'NB-XSRF-TOKEN')).toBe('');
  });
});
