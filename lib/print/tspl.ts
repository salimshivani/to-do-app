// Builds TSPL (TSC Printer Language) commands for a Code128 item label.
// TSPL is the command set most generic Bluetooth thermal label printers speak
// (as opposed to Zebra's ZPL) — sizes are parameterized in millimeters, which
// is what makes "dynamic sticker size" possible: same template, different numbers.

export interface LabelInput {
  widthMm: number;
  heightMm: number;
  barcodeValue: string;
  itemName: string;
  hsnCode?: string | null;
  unit?: string | null;
  dpi?: number; // most Bluetooth label printers are 203 DPI
  gapMm?: number; // gap between die-cut labels; use 0 for continuous/black-mark rolls
}

function escapeTspl(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

function truncate(value: string, max: number): string {
  return value.length > max ? `${value.slice(0, max - 1)}…` : value;
}

export function buildTsplLabel(input: LabelInput): string {
  const dpi = input.dpi ?? 203;
  const dotsPerMm = dpi / 25.4;
  const widthDots = Math.round(input.widthMm * dotsPerMm);
  const heightDots = Math.round(input.heightMm * dotsPerMm);
  const margin = Math.max(4, Math.round(widthDots * 0.05));
  const usableWidth = widthDots - margin * 2;

  const nameFontHeight = 24;
  const barcodeHeight = Math.max(30, Math.round(heightDots * 0.4));
  const barcodeY = nameFontHeight + 12;
  const detailY = barcodeY + barcodeHeight + 20;

  // ~11px-wide glyphs at TSPL font "3"; keeps the name from overflowing narrow labels.
  const maxNameChars = Math.max(6, Math.floor(usableWidth / 11));
  const name = escapeTspl(truncate(input.itemName, maxNameChars));

  const detailParts = [input.hsnCode ? `HSN ${input.hsnCode}` : null, input.unit ?? null].filter(
    Boolean
  );
  const detailLine = detailParts.length ? escapeTspl(truncate(detailParts.join(' · '), maxNameChars)) : null;

  const lines = [
    `SIZE ${input.widthMm} mm,${input.heightMm} mm`,
    `GAP ${input.gapMm ?? 2} mm,0 mm`,
    'DIRECTION 1',
    'CLS',
    `TEXT ${margin},8,"3",0,1,1,"${name}"`,
    `BARCODE ${margin},${barcodeY},"128",${barcodeHeight},1,0,2,2,"${escapeTspl(input.barcodeValue)}"`,
  ];
  if (detailLine) {
    lines.push(`TEXT ${margin},${detailY},"2",0,1,1,"${detailLine}"`);
  }
  lines.push('PRINT 1,1');

  return lines.join('\r\n') + '\r\n';
}
