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


function emu(inches) { return Math.round(inches * 914400); }

function pptShape(id, name, x, y, width, height, text, options = {}) {
  const { fill = 'FFFFFF', color = '20332B', fontSize = 1100, bold = false, align = 'l', margin = 0.08 } = options;
  const paragraphs = String(text ?? '').split('\n').map((line) =>
    `<a:p><a:pPr algn="${align}"/><a:r><a:rPr lang="es-PE" sz="${fontSize}"${bold ? ' b="1"' : ''}><a:solidFill><a:srgbClr val="${color}"/></a:solidFill><a:latin typeface="Aptos"/></a:rPr><a:t xml:space="preserve">${escapeXml(line)}</a:t></a:r><a:endParaRPr lang="es-PE" sz="${fontSize}"/></a:p>`
  ).join('');
  return `<p:sp><p:nvSpPr><p:cNvPr id="${id}" name="${escapeXml(name)}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x="${emu(x)}" y="${emu(y)}"/><a:ext cx="${emu(width)}" cy="${emu(height)}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val="${fill}"/></a:solidFill><a:ln><a:noFill/></a:ln></p:spPr><p:txBody><a:bodyPr wrap="square" lIns="${emu(margin)}" rIns="${emu(margin)}" tIns="${emu(margin / 2)}" bIns="${emu(margin / 2)}" anchor="ctr"><a:spAutoFit/></a:bodyPr><a:lstStyle/>${paragraphs}</p:txBody></p:sp>`;
}

function pptSlideXml(shapes) {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>${shapes}</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>`;
}

function pptThemeXml() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Cerro Verde"><a:themeElements><a:clrScheme name="Cerro Verde"><a:dk1><a:srgbClr val="20332B"/></a:dk1><a:lt1><a:srgbClr val="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="173D35"/></a:dk2><a:lt2><a:srgbClr val="F4F8F5"/></a:lt2><a:accent1><a:srgbClr val="176B57"/></a:accent1><a:accent2><a:srgbClr val="6B9B84"/></a:accent2><a:accent3><a:srgbClr val="DCE9E1"/></a:accent3><a:accent4><a:srgbClr val="C58B43"/></a:accent4><a:accent5><a:srgbClr val="52766A"/></a:accent5><a:accent6><a:srgbClr val="A9C9B5"/></a:accent6><a:hlink><a:srgbClr val="0563C1"/></a:hlink><a:folHlink><a:srgbClr val="954F72"/></a:folHlink></a:clrScheme><a:fontScheme name="Aptos"><a:majorFont><a:latin typeface="Aptos Display"/></a:majorFont><a:minorFont><a:latin typeface="Aptos"/></a:minorFont></a:fontScheme><a:fmtScheme name="Office"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"><a:alpha val="95000"/></a:schemeClr></a:solidFill><a:solidFill><a:schemeClr val="phClr"><a:alpha val="90000"/></a:schemeClr></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="6350"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln><a:ln w="12700"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln><a:ln w="19050"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"><a:alpha val="95000"/></a:schemeClr></a:solidFill><a:solidFill><a:schemeClr val="phClr"><a:alpha val="90000"/></a:schemeClr></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements><a:objectDefaults/><a:extraClrSchemeLst/></a:theme>`;
}

function pptxPackage(slides) {
  const slideOverrides = slides.map((_, index) => `<Override PartName="/ppt/slides/slide${index + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>`).join('');
  const slideIds = slides.map((_, index) => `<p:sldId id="${256 + index}" r:id="rId${index + 2}"/>`).join('');
  const slideRels = slides.map((_, index) => `<Relationship Id="rId${index + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${index + 1}.xml"/>`).join('');
  const files = [
    { name: '[Content_Types].xml', content: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/><Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/><Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/><Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>${slideOverrides}</Types>` },
    { name: '_rels/.rels', content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/></Relationships>' },
    { name: 'ppt/presentation.xml', content: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst><p:sldIdLst>${slideIds}</p:sldIdLst><p:sldSz cx="12192000" cy="6858000" type="wide"/><p:notesSz cx="6858000" cy="9144000"/><p:defaultTextStyle/></p:presentation>` },
    { name: 'ppt/_rels/presentation.xml.rels', content: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>${slideRels}</Relationships>` },
    { name: 'ppt/slideMasters/slideMaster1.xml', content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/><p:sldLayoutIdLst><p:sldLayoutId id="1" r:id="rId1"/></p:sldLayoutIdLst><p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles></p:sldMaster>' },
    { name: 'ppt/slideMasters/_rels/slideMaster1.xml.rels', content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/></Relationships>' },
    { name: 'ppt/slideLayouts/slideLayout1.xml', content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1"><p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>' },
    { name: 'ppt/slideLayouts/_rels/slideLayout1.xml.rels', content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/></Relationships>' },
    { name: 'ppt/theme/theme1.xml', content: pptThemeXml() },
  ];
  slides.forEach((shapes, index) => {
    files.push({ name: `ppt/slides/slide${index + 1}.xml`, content: pptSlideXml(shapes) });
    files.push({ name: `ppt/slides/_rels/slide${index + 1}.xml.rels`, content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/></Relationships>' });
  });
  return makeZip(files);
}

/** Crea un PowerPoint 16:9 editable en PowerPoint y compatible con la importación de Canva. */
export function exportPptx(rows, title = 'Reporte de monitoreo hídrico · Cerro Verde', filename = 'reporte-cerro-verde.pptx') {
  const columns = rows.length ? Object.keys(rows[0]) : ['Información'];
  const displayRows = rows.length ? rows : [{ Información: 'No se seleccionaron registros' }];
  const generated = new Date().toLocaleString('es-PE');
  const slides = [];
  let id = 2;
  let cover = '';
  cover += pptShape(id++, 'Fondo', 0, 0, 13.333, 7.5, '', { fill: 'F4F8F5' });
  cover += pptShape(id++, 'Franja principal', 0, 0, 13.333, 1.65, '', { fill: '173D35' });
  cover += pptShape(id++, 'Marca', 0.65, 0.42, 11.9, 0.35, 'SISTEMA WEB DE MONITOREO HÍDRICO · SOCIEDAD MINERA CERRO VERDE', { fill: '173D35', color: 'DCE9E1', fontSize: 1100, bold: true });
  cover += pptShape(id++, 'Título', 0.65, 2.15, 11.9, 1.05, title, { fill: 'F4F8F5', color: '173D35', fontSize: 2900, bold: true });
  cover += pptShape(id++, 'Resumen', 0.68, 3.45, 11.5, 0.72, `${displayRows.length} filas seleccionadas · ${columns.length} columnas`, { fill: 'F4F8F5', color: '52766A', fontSize: 1700, bold: true });
  cover += pptShape(id++, 'Fecha', 0.68, 4.42, 11.5, 0.55, `Generado: ${generated}`, { fill: 'F4F8F5', color: '20332B', fontSize: 1300 });
  cover += pptShape(id++, 'Nota', 0.68, 5.35, 11.5, 0.85, 'El contenido corresponde a las categorías y filas seleccionadas en el centro de exportación del sistema.', { fill: 'E6F0E9', color: '20332B', fontSize: 1300 });
  slides.push(cover);

  const rowsPerSlide = 11;
  const columnWeights = columns.map((column) => /detalle|descripci[oó]n/i.test(column) ? 4.2 : /fecha|estaci[oó]n|estado/i.test(column) ? 1.5 : 1.0);
  const totalWeight = columnWeights.reduce((sum, weight) => sum + weight, 0);
  const tableX = 0.45;
  const tableWidth = 12.43;
  const colWidths = columnWeights.map((weight) => tableWidth * weight / totalWeight);
  const maxSlides = Math.ceil(displayRows.length / rowsPerSlide);
  for (let page = 0; page < maxSlides; page += 1) {
    let shapes = '';
    id = 2;
    shapes += pptShape(id++, 'Fondo', 0, 0, 13.333, 7.5, '', { fill: 'F4F8F5' });
    shapes += pptShape(id++, 'Franja', 0, 0, 13.333, 0.95, '', { fill: '173D35' });
    shapes += pptShape(id++, 'Título de página', 0.48, 0.18, 11.9, 0.48, `${title} · Detalle`, { fill: '173D35', color: 'FFFFFF', fontSize: 1800, bold: true });
    shapes += pptShape(id++, 'Número de página', 10.9, 1.02, 1.95, 0.32, `Página ${page + 1} de ${maxSlides}`, { fill: 'F4F8F5', color: '52766A', fontSize: 900, align: 'r' });
    let x = tableX;
    const headerY = 1.42;
    const rowHeight = 0.43;
    columns.forEach((column, index) => {
      shapes += pptShape(id++, `Encabezado ${column}`, x, headerY, colWidths[index], rowHeight, column, { fill: 'DCE9E1', color: '173D35', fontSize: 850, bold: true });
      x += colWidths[index];
    });
    const pageRows = displayRows.slice(page * rowsPerSlide, (page + 1) * rowsPerSlide);
    pageRows.forEach((row, rowIndex) => {
      const y = headerY + rowHeight * (rowIndex + 1);
      x = tableX;
      columns.forEach((column, index) => {
        const width = colWidths[index];
        const maxChars = Math.max(10, Math.floor(width * 18));
        const raw = String(row[column] ?? '').replace(/\s+/g, ' ').trim();
        const display = raw.length > maxChars ? `${raw.slice(0, maxChars - 1)}…` : raw;
        shapes += pptShape(id++, `Fila ${rowIndex + 1} columna ${index + 1}`, x, y, width, rowHeight, display, { fill: rowIndex % 2 ? 'FFFFFF' : 'EEF4F0', color: '20332B', fontSize: 760 });
        x += width;
      });
    });
    shapes += pptShape(id++, 'Pie', 0.5, 6.72, 12.3, 0.32, 'PTAR La Enlozada · Reporte exportado desde el sistema', { fill: 'F4F8F5', color: '52766A', fontSize: 850 });
    slides.push(shapes);
  }
  const bytes = pptxPackage(slides);
  saveBlob(new Blob([bytes], { type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation' }), filename);
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
