const encoder = new TextEncoder();

function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1500);
}

function escapeXml(value) {
  return String(value ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&apos;');
}

export function exportCsv(rows, filename = 'reporte-cerro-verde.csv') {
  const cols = rows.length ? Object.keys(rows[0]) : ['Información'];
  const csv = [cols, ...rows.map((row) => cols.map((key) => row[key] ?? ''))]
    .map((line) => line.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(';'))
    .join('\r\n');
  // CSV UTF-8 con BOM: Excel suele reconocer mejor los acentos de este formato.
  saveBlob(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8;' }), filename);
}

function crc32(bytes) {
  let crc = -1;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
  }
  return (crc ^ -1) >>> 0;
}

function u16(value) { return [value & 0xff, (value >>> 8) & 0xff]; }
function u32(value) { return [value & 0xff, (value >>> 8) & 0xff, (value >>> 16) & 0xff, (value >>> 24) & 0xff]; }
function joinBytes(chunks) {
  const length = chunks.reduce((total, chunk) => total + chunk.length, 0);
  const result = new Uint8Array(length);
  let offset = 0;
  for (const chunk of chunks) { result.set(chunk, offset); offset += chunk.length; }
  return result;
}

// ZIP mínimo sin compresión, escrito en JavaScript para no depender de bibliotecas.
function makeZip(files) {
  const localParts = [];
  const centralParts = [];
  let offset = 0;
  for (const file of files) {
    const name = encoder.encode(file.name);
    const data = typeof file.content === 'string' ? encoder.encode(file.content) : file.content;
    const checksum = crc32(data);
    const localHeader = new Uint8Array([
      ...u32(0x04034b50), ...u16(20), ...u16(0x0800), ...u16(0),
      ...u16(0), ...u16(0), ...u32(checksum), ...u32(data.length), ...u32(data.length),
      ...u16(name.length), ...u16(0),
    ]);
    const local = joinBytes([localHeader, name, data]);
    localParts.push(local);
    const centralHeader = new Uint8Array([
      ...u32(0x02014b50), ...u16(20), ...u16(20), ...u16(0x0800), ...u16(0),
      ...u16(0), ...u16(0), ...u32(checksum), ...u32(data.length), ...u32(data.length),
      ...u16(name.length), ...u16(0), ...u16(0), ...u16(0), ...u16(0), ...u32(0), ...u32(offset),
    ]);
    centralParts.push(joinBytes([centralHeader, name]));
    offset += local.length;
  }
  const central = joinBytes(centralParts);
  const ending = new Uint8Array([
    ...u32(0x06054b50), ...u16(0), ...u16(0), ...u16(files.length), ...u16(files.length),
    ...u32(central.length), ...u32(offset), ...u16(0),
  ]);
  return joinBytes([...localParts, central, ending]);
}

function columnName(number) {
  let name = '';
  let index = number;
  while (index > 0) { const remainder = (index - 1) % 26; name = String.fromCharCode(65 + remainder) + name; index = Math.floor((index - 1) / 26); }
  return name;
}

export function exportXlsx(rows, filename = 'reporte-cerro-verde.xlsx') {
  const headers = rows.length ? Object.keys(rows[0]) : ['Información'];
  const matrix = [headers, ...rows.map((row) => headers.map((key) => row[key] ?? ''))];
  const sheetRows = matrix.map((row, rowIndex) => {
    const cells = row.map((value, colIndex) => {
      const cellRef = `${columnName(colIndex + 1)}${rowIndex + 1}`;
      if (typeof value === 'number' && Number.isFinite(value)) return `<c r="${cellRef}"><v>${value}</v></c>`;
      return `<c r="${cellRef}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>`;
    }).join('');
    return `<row r="${rowIndex + 1}">${cells}</row>`;
  }).join('');
  const sheet = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${sheetRows}</sheetData><autoFilter ref="A1:${columnName(Math.max(1, headers.length))}${Math.max(1, matrix.length)}"/></worksheet>`;
  const files = [
    { name: '[Content_Types].xml', content: '<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>' },
    { name: '_rels/.rels', content: '<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>' },
    { name: 'xl/workbook.xml', content: '<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Reporte" sheetId="1" r:id="rId1"/></sheets></workbook>' },
    { name: 'xl/_rels/workbook.xml.rels', content: '<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>' },
    { name: 'xl/worksheets/sheet1.xml', content: sheet },
  ];
  saveBlob(new Blob([makeZip(files)], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), filename);
}

export function exportPdf(rows, title = 'Reporte de monitoreo Cerro Verde') {
  const tab = window.open('', '_blank');
  if (!tab) return false;
  const cols = rows.length ? Object.keys(rows[0]) : ['Información'];
  const header = cols.map((column) => `<th>${escapeXml(column)}</th>`).join('');
  const body = rows.map((row) => `<tr>${cols.map((column) => `<td>${escapeXml(row[column] ?? '')}</td>`).join('')}</tr>`).join('');
  tab.document.open();
  tab.document.write(`<!doctype html><html lang="es"><head><meta charset="utf-8"><title>${escapeXml(title)}</title><style>body{font-family:Arial,sans-serif;color:#20332b;padding:28px}h1{font-size:22px;margin:0 0 8px}p{font-size:11px;color:#52665c;line-height:1.5}.note{padding:10px;border:1px solid #dce7df;background:#f4f8f5;margin:18px 0}table{width:100%;border-collapse:collapse;font-size:10px}th,td{border:1px solid #dce4de;padding:7px;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#eaf2ed}tr{break-inside:avoid}@page{size:landscape;margin:12mm}@media print{body{padding:0}.note{print-color-adjust:exact;-webkit-print-color-adjust:exact}th{print-color-adjust:exact;-webkit-print-color-adjust:exact}}</style></head><body><h1>${escapeXml(title)}</h1><p>Interfaz de demostración. Las filas exportadas proceden de la vista actual y no sustituyen una fuente oficial validada.</p><div class="note">En el diálogo de impresión, selecciona <strong>Guardar como PDF</strong> para generar el archivo.</div><table><thead><tr>${header}</tr></thead><tbody>${body}</tbody></table><script>window.addEventListener('load',()=>window.print());</script></body></html>`);
  tab.document.close();
  return true;
}

export function exportPngTable(rows, filename = 'reporte-cerro-verde.png') {
  const columns = rows.length ? Object.keys(rows[0]) : ['Información'];
  const maxRows = Math.max(rows.length, 1);
  const width = 1500;
  const rowHeight = 42;
  const headerHeight = 140;
  const height = Math.min(12000, headerHeight + (maxRows + 1) * rowHeight + 36);
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#f4f8f5'; ctx.fillRect(0, 0, width, height);
  ctx.fillStyle = '#173d35'; ctx.fillRect(0, 0, width, 95);
  ctx.fillStyle = '#ffffff'; ctx.font = 'bold 28px Segoe UI, Arial'; ctx.fillText('CERRO VERDE · REPORTE', 42, 43);
  ctx.font = '15px Segoe UI, Arial'; ctx.fillStyle = '#d9eee5'; ctx.fillText('Exportación de datos visibles · Prototipo visual, no oficial', 42, 70);
  const colWidth = (width - 80) / columns.length;
  let y = 118;
  columns.forEach((column, index) => { const x = 40 + index * colWidth; ctx.fillStyle = '#dbeae1'; ctx.fillRect(x, y, colWidth, rowHeight); ctx.strokeStyle = '#c7d8ce'; ctx.strokeRect(x, y, colWidth, rowHeight); ctx.fillStyle = '#173d35'; ctx.font = 'bold 13px Segoe UI, Arial'; ctx.fillText(String(column).slice(0, 24), x + 9, y + 25); });
  const displayRows = rows.length ? rows : [{ Información: 'No se seleccionaron registros' }];
  displayRows.forEach((row, rowIndex) => {
    y = 118 + (rowIndex + 1) * rowHeight;
    columns.forEach((column, colIndex) => {
      const x = 40 + colIndex * colWidth;
      ctx.fillStyle = rowIndex % 2 ? '#ffffff' : '#f8fbf9'; ctx.fillRect(x, y, colWidth, rowHeight);
      ctx.strokeStyle = '#d8e3dc'; ctx.strokeRect(x, y, colWidth, rowHeight);
      ctx.fillStyle = '#33483e'; ctx.font = '12px Segoe UI, Arial';
      const raw = String(row[column] ?? '');
      const maxChars = Math.max(8, Math.floor((colWidth - 18) / 7));
      ctx.fillText(raw.length > maxChars ? `${raw.slice(0, maxChars - 1)}…` : raw, x + 9, y + 25);
    });
  });
  canvas.toBlob((blob) => { if (blob) saveBlob(blob, filename); }, 'image/png');
}

export async function downloadSvgAsPng(svgId, filename = 'mapa-2d.png', notify = () => {}) {
  const svg = document.getElementById(svgId);
  if (!svg) { notify('No se encontró el mapa para exportar.'); return; }
  const clone = svg.cloneNode(true);
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
  clone.setAttribute('width', '1520');
  clone.setAttribute('height', '940');
  const svgText = new XMLSerializer().serializeToString(clone);
  const url = URL.createObjectURL(new Blob([svgText], { type: 'image/svg+xml;charset=utf-8' }));
  try {
    const img = new Image();
    const loaded = new Promise((resolve, reject) => { img.onload = resolve; img.onerror = reject; });
    img.src = url;
    await loaded;
    const canvas = document.createElement('canvas'); canvas.width = 1520; canvas.height = 940;
    const context = canvas.getContext('2d'); context.fillStyle = '#f3f8f4'; context.fillRect(0, 0, canvas.width, canvas.height); context.drawImage(img, 0, 0, canvas.width, canvas.height);
    canvas.toBlob((blob) => { if (blob) saveBlob(blob, filename); else notify('No se pudo crear la imagen PNG.'); }, 'image/png');
  } catch {
    notify('El navegador no pudo convertir el mapa a PNG. Intenta de nuevo.');
  } finally {
    URL.revokeObjectURL(url);
  }
}
