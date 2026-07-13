export function readCookie(cookieHeader: string, name: string): string {
  const encodedValue = cookieHeader
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${name}=`))
    ?.slice(name.length + 1);

  if (!encodedValue) {
    return '';
  }

  try {
    return decodeURIComponent(encodedValue);
  } catch {
    return encodedValue;
  }
}
