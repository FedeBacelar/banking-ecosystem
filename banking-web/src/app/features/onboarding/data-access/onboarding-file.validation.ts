const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_FILE_TYPES = new Set(['image/jpeg', 'image/png', 'application/pdf']);

export function validateOnboardingFile(
  file: Pick<File, 'size' | 'type'>
): string | null {
  if (file.size === 0) {
    return 'El archivo está vacío.';
  }
  if (file.size > MAX_FILE_SIZE) {
    return 'El archivo supera los 10 MB.';
  }
  if (!ALLOWED_FILE_TYPES.has(file.type)) {
    return 'Usá una imagen JPG o PNG, o un PDF.';
  }
  return null;
}
