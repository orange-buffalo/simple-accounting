type Formatter = (messageTemplate: string, values?: Record<string, unknown> | unknown[]) => string;

let formatter: Formatter;

export function setTranslationsFormatter(newFormatter: Formatter) {
  formatter = newFormatter;
}

export function format(messageTemplate: string, values?: Record<string, unknown> | unknown[]): string {
  if (!formatter) throw new Error('Formatter not initialized');
  return formatter(messageTemplate, values);
}
