import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';

const POINTS_PER_MM = 72 / 25.4;

// Renders the label preview image onto a PDF page sized to the exact sticker
// dimensions (in points, 1mm = 72/25.4pt) — so viewing it at 100% shows true size.
export async function generateLabelPdf(
  previewImageBase64: string,
  widthMm: number,
  heightMm: number
): Promise<string> {
  const html = `
    <html>
      <head>
        <meta charset="utf-8" />
        <style>
          @page { margin: 0; size: ${widthMm}mm ${heightMm}mm; }
          html, body { margin: 0; padding: 0; }
          img { width: 100%; height: 100%; object-fit: contain; display: block; }
        </style>
      </head>
      <body>
        <img src="data:image/png;base64,${previewImageBase64}" />
      </body>
    </html>
  `;
  const { uri } = await Print.printToFileAsync({
    html,
    width: Math.round(widthMm * POINTS_PER_MM),
    height: Math.round(heightMm * POINTS_PER_MM),
    base64: false,
  });
  return uri;
}

export async function shareLabelPdf(uri: string): Promise<void> {
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(uri, { mimeType: 'application/pdf', dialogTitle: 'Label preview' });
  }
}
