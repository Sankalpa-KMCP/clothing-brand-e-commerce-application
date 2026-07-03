const SAFE_RETURN_PREFIXES = [
  '/profile',
  '/cart',
  '/checkout',
  '/orders',
  '/addresses'
];

export function sanitizeReturnTo(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  let decoded = value.trim();
  for (let i = 0; i < 2; i += 1) {
    try {
      decoded = decodeURIComponent(decoded);
    } catch {
      return null;
    }
  }

  if (!decoded.startsWith('/') || decoded.startsWith('//')) {
    return null;
  }
  if (decoded.includes('\\') || decoded.includes('..') || decoded.includes('\u0000')) {
    return null;
  }
  if (/^\/admin(?:\/|$)/i.test(decoded)) {
    return null;
  }

  const pathOnly = decoded.split(/[?#]/)[0];
  return SAFE_RETURN_PREFIXES.some((prefix) => pathOnly === prefix || pathOnly.startsWith(`${prefix}/`))
    ? decoded
    : null;
}
