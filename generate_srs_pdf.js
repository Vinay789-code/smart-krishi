const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function inlineFormat(text) {
  // Inline code
  text = text.replace(/`([^`]+)`/g, (m, code) => `<code>${escapeHtml(code)}</code>`);
  // Bold
  text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  // Italic
  text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  // Links
  text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
  // Priority Badges
  text = text.replace(/\[High\]/g, '<span class="badge badge-high">High</span>');
  text = text.replace(/\[Medium\]/g, '<span class="badge badge-medium">Medium</span>');
  text = text.replace(/\[Low\]/g, '<span class="badge badge-low">Low</span>');
  // Math placeholders
  text = text.replace(/\$([^$]+)\$/g, '<span class="math">$1</span>');
  return text;
}

function parseMarkdown(md) {
  const lines = md.split(/\r?\n/);
  const out = [];
  let inCode = false;
  let codeLang = '';
  let codeBuffer = [];
  let inTable = false;
  let tableRows = [];
  let inList = false;
  let listType = 'ul';

  function flushList() {
    if (inList) {
      out.push(`</${listType}>`);
      inList = false;
    }
  }

  function flushTable() {
    if (inTable) {
      if (tableRows.length > 0) {
        let html = '<div class="table-container"><table>';
        // First row is header
        const headerCells = tableRows[0];
        html += '<thead><tr>';
        for (const c of headerCells) {
          html += `<th>${inlineFormat(c)}</th>`;
        }
        html += '</tr></thead><tbody>';
        // Remaining rows
        for (let i = 1; i < tableRows.length; i++) {
          const rowCells = tableRows[i];
          html += '<tr>';
          for (let j = 0; j < headerCells.length; j++) {
            const cell = rowCells[j] !== undefined ? rowCells[j] : '';
            html += `<td>${inlineFormat(cell)}</td>`;
          }
          html += '</tr>';
        }
        html += '</tbody></table></div>';
        out.push(html);
      }
      inTable = false;
      tableRows = [];
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i];
    const trimmed = rawLine.trim();

    // Code block toggle
    if (trimmed.startsWith('```')) {
      if (inCode) {
        flushList();
        out.push(`<pre class="code-block ${codeLang ? 'language-' + codeLang : ''}"><code>${escapeHtml(codeBuffer.join('\n'))}</code></pre>`);
        inCode = false;
        codeBuffer = [];
        codeLang = '';
      } else {
        flushList();
        flushTable();
        inCode = true;
        codeLang = trimmed.substring(3).trim();
      }
      continue;
    }

    if (inCode) {
      codeBuffer.push(rawLine);
      continue;
    }

    // Tables
    if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
      flushList();
      // Check if it's separator row
      if (/^\|(\s*:?-+:?\s*\|)+$/.test(trimmed)) {
        continue;
      }
      const cells = trimmed
        .slice(1, -1)
        .split('|')
        .map(c => c.trim());
      inTable = true;
      tableRows.push(cells);
      continue;
    } else {
      flushTable();
    }

    // Horizontal Rule
    if (/^---{1,}$/.test(trimmed) || /^___+$/.test(trimmed)) {
      flushList();
      out.push('<hr/>');
      continue;
    }

    // Headings
    if (trimmed.startsWith('# ') && !trimmed.startsWith('## ')) {
      flushList();
      const title = inlineFormat(trimmed.substring(2).trim());
      out.push(`<h1>${title}</h1>`);
      continue;
    }
    if (trimmed.startsWith('## ')) {
      flushList();
      const title = inlineFormat(trimmed.substring(3).trim());
      out.push(`<h2>${title}</h2>`);
      continue;
    }
    if (trimmed.startsWith('### ')) {
      flushList();
      const title = inlineFormat(trimmed.substring(4).trim());
      out.push(`<h3>${title}</h3>`);
      continue;
    }
    if (trimmed.startsWith('#### ')) {
      flushList();
      const title = inlineFormat(trimmed.substring(5).trim());
      out.push(`<h4>${title}</h4>`);
      continue;
    }

    // Bullet lists
    const bulletMatch = rawLine.match(/^(\s*)[-*]\s+(.*)$/);
    if (bulletMatch) {
      if (!inList || listType !== 'ul') {
        flushList();
        inList = true;
        listType = 'ul';
        out.push('<ul>');
      }
      out.push(`<li>${inlineFormat(bulletMatch[2])}</li>`);
      continue;
    }

    // Numbered lists
    const numMatch = rawLine.match(/^(\s*)\d+\.\s+(.*)$/);
    if (numMatch) {
      if (!inList || listType !== 'ol') {
        flushList();
        inList = true;
        listType = 'ol';
        out.push('<ol>');
      }
      out.push(`<li>${inlineFormat(numMatch[2])}</li>`);
      continue;
    }

    // Empty line
    if (trimmed === '') {
      flushList();
      continue;
    }

    // Blockquote
    if (trimmed.startsWith('>')) {
      flushList();
      out.push(`<blockquote>${inlineFormat(trimmed.replace(/^>\s*/, ''))}</blockquote>`);
      continue;
    }

    // Regular paragraph
    flushList();
    out.push(`<p>${inlineFormat(trimmed)}</p>`);
  }

  flushList();
  flushTable();
  return out.join('\n');
}

const mdContent = fs.readFileSync('SRS.md', 'utf8');

// Separate the initial title/heading if present
const parsedHtml = parseMarkdown(mdContent);

const htmlTemplate = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Smart Krishi – Software Requirements Specification (SRS)</title>
<style>
  @page {
    size: A4 portrait;
    margin: 18mm 16mm 18mm 16mm;
    @top-center {
      content: "Smart Krishi – Precision Agriculture Platform | SRS v2.0";
      font-size: 8pt;
      color: #6b7280;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }
    @bottom-right {
      content: "Page " counter(page);
      font-size: 8pt;
      color: #6b7280;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }
  }

  *, *::before, *::after {
    box-sizing: border-box;
  }

  body {
    font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif;
    font-size: 9.8pt;
    line-height: 1.55;
    color: #1f2937;
    margin: 0;
    padding: 0;
    background: #ffffff;
  }

  /* Cover Page */
  .cover-page {
    height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    page-break-after: always;
    break-after: page;
    padding: 40px 20px 20px 20px;
    border: 3px double #2e7d32;
    border-radius: 8px;
    background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 40%);
  }

  .cover-header {
    text-align: center;
  }

  .cover-icon {
    font-size: 48pt;
    margin-bottom: 10px;
  }

  .cover-platform-name {
    font-size: 26pt;
    font-weight: 800;
    color: #1b5e20;
    letter-spacing: -0.5px;
    margin-bottom: 4px;
    text-transform: uppercase;
  }

  .cover-subtitle {
    font-size: 13pt;
    color: #374151;
    font-weight: 500;
    margin-bottom: 25px;
  }

  .cover-divider {
    width: 100px;
    height: 4px;
    background: #2e7d32;
    margin: 0 auto 30px auto;
    border-radius: 2px;
  }

  .cover-doc-title {
    font-size: 20pt;
    font-weight: 800;
    color: #111827;
    margin-bottom: 8px;
    letter-spacing: 0.5px;
  }

  .cover-doc-standard {
    font-size: 10pt;
    color: #4b5563;
    font-style: italic;
  }

  .cover-meta-box {
    background: #ffffff;
    border: 1px solid #d1fae5;
    border-left: 5px solid #2e7d32;
    padding: 16px 20px;
    border-radius: 6px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.03);
    margin-top: 30px;
  }

  .cover-meta-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 9.5pt;
  }

  .cover-meta-table td {
    padding: 5px 8px;
    border: none;
  }

  .cover-meta-table td.label {
    font-weight: 700;
    color: #1b5e20;
    width: 32%;
  }

  .cover-footer {
    text-align: center;
    font-size: 8.5pt;
    color: #6b7280;
    border-top: 1px solid #e5e7eb;
    padding-top: 15px;
  }

  /* Typography */
  h1 {
    font-size: 17pt;
    font-weight: 800;
    color: #14532d;
    margin-top: 24pt;
    margin-bottom: 10pt;
    padding-bottom: 4pt;
    border-bottom: 2px solid #22c55e;
    page-break-before: always;
    break-before: page;
  }

  h1:first-of-type {
    page-break-before: avoid;
    break-before: avoid;
  }

  h2 {
    font-size: 13.5pt;
    font-weight: 700;
    color: #1b5e20;
    margin-top: 18pt;
    margin-bottom: 8pt;
    padding-bottom: 2pt;
    border-bottom: 1px solid #e5e7eb;
    page-break-after: avoid;
    break-after: avoid;
  }

  h3 {
    font-size: 11pt;
    font-weight: 700;
    color: #1f2937;
    margin-top: 14pt;
    margin-bottom: 6pt;
    page-break-after: avoid;
    break-after: avoid;
  }

  h4 {
    font-size: 10pt;
    font-weight: 700;
    color: #374151;
    margin-top: 10pt;
    margin-bottom: 4pt;
    page-break-after: avoid;
    break-after: avoid;
  }

  p {
    margin-top: 0;
    margin-bottom: 8pt;
    text-align: justify;
  }

  ul, ol {
    margin-top: 0;
    margin-bottom: 8pt;
    padding-left: 20px;
  }

  li {
    margin-bottom: 3pt;
  }

  hr {
    border: 0;
    height: 1px;
    background: #e5e7eb;
    margin: 16pt 0;
  }

  blockquote {
    margin: 8pt 0;
    padding: 6pt 12pt;
    background: #f0fdf4;
    border-left: 3px solid #22c55e;
    color: #166534;
    font-size: 9.5pt;
  }

  /* Tables */
  .table-container {
    width: 100%;
    margin: 10pt 0 14pt 0;
    page-break-inside: avoid;
    break-inside: avoid;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 8.8pt;
    page-break-inside: avoid;
    break-inside: avoid;
  }

  th {
    background-color: #f0fdf4;
    color: #14532d;
    font-weight: 700;
    text-align: left;
    padding: 6px 8px;
    border: 1px solid #cbd5e1;
    font-size: 8.8pt;
  }

  td {
    padding: 5px 8px;
    border: 1px solid #cbd5e1;
    vertical-align: top;
  }

  tr:nth-child(even) td {
    background-color: #f8fafc;
  }

  /* Code & Pre Blocks */
  pre.code-block {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-left: 3px solid #059669;
    padding: 8pt 10pt;
    border-radius: 4px;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 8.2pt;
    line-height: 1.4;
    overflow-x: hidden;
    white-space: pre-wrap;
    word-break: break-all;
    margin: 8pt 0 12pt 0;
    page-break-inside: avoid;
    break-inside: avoid;
  }

  code {
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 8.6pt;
    background: #f1f5f9;
    padding: 1px 4px;
    border-radius: 3px;
    color: #0f766e;
  }

  /* Badges */
  .badge {
    display: inline-block;
    padding: 1px 6px;
    border-radius: 10px;
    font-size: 7.5pt;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }

  .badge-high {
    background: #fee2e2;
    color: #991b1b;
    border: 1px solid #fca5a5;
  }

  .badge-medium {
    background: #fef3c7;
    color: #92400e;
    border: 1px solid #fcd34d;
  }

  .badge-low {
    background: #e0f2fe;
    color: #075985;
    border: 1px solid #7dd3fc;
  }

  .math {
    font-family: 'Times New Roman', Times, serif;
    font-style: italic;
    color: #1e3a8a;
    padding: 0 2px;
  }
</style>
</head>
<body>

<!-- Cover Page -->
<div class="cover-page">
  <div class="cover-header">
    <div class="cover-icon">🌾</div>
    <div class="cover-platform-name">Smart Krishi</div>
    <div class="cover-subtitle">Precision Agriculture & Decision Support Platform</div>
    <div class="cover-divider"></div>
    <div class="cover-doc-title">SOFTWARE REQUIREMENTS SPECIFICATION</div>
    <div class="cover-doc-standard">Prepared in accordance with IEEE Std 830-1998 / ISO/IEC/IEEE 29148:2018</div>
  </div>

  <div class="cover-meta-box">
    <table class="cover-meta-table">
      <tr>
        <td class="label">Document Version:</td>
        <td>2.0 (Post-Disease Architecture, 8 Operational Modules)</td>
      </tr>
      <tr>
        <td class="label">Project Title:</td>
        <td>Smart Krishi – Precision Agriculture Platform</td>
      </tr>
      <tr>
        <td class="label">Release Status:</td>
        <td>Approved, Verified & Production-Ready</td>
      </tr>
      <tr>
        <td class="label">Publication Date:</td>
        <td>October 2026</td>
      </tr>
      <tr>
        <td class="label">Backend Architecture:</td>
        <td>Spring Boot 3.3.4, Java 17, Spring Security 6 (JWT), JPA / Hibernate</td>
      </tr>
      <tr>
        <td class="label">Frontend Tech Stack:</td>
        <td>HTML5, CSS3, ES6+ Modules, Bootstrap 5.3, Fetch API</td>
      </tr>
      <tr>
        <td class="label">Database Engine:</td>
        <td>H2 In-Memory (Dev) / Cloud MySQL 8.0+ InnoDB (Prod)</td>
      </tr>
      <tr>
        <td class="label">Cloud Targets:</td>
        <td>Netlify (Static Frontend) & Render (Dockerized Java Web Service)</td>
      </tr>
    </table>
  </div>

  <div class="cover-footer">
    <div><strong>Confidential & Proprietary</strong> — For Engineering, Evaluation, and Deployment Use Only</div>
    <div>© 2026 Smart Krishi Platform Engineering. All Rights Reserved.</div>
  </div>
</div>

<!-- Main Body -->
<div class="content-body">
${parsedHtml}
</div>

</body>
</html>
`;

fs.writeFileSync('SRS.html', htmlTemplate, 'utf8');
console.log('Successfully generated SRS.html');

// Run Edge headless to render PDF
const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const htmlFullPath = path.resolve('SRS.html');
const pdfFullPath = path.resolve('Smart_Krishi_SRS.pdf');

const cmd = `"${edgePath}" --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf="${pdfFullPath}" "file:///${htmlFullPath.replace(/\\/g, '/')}"`;

console.log('Generating PDF via Microsoft Edge headless...');
try {
  execSync(cmd, { stdio: 'inherit' });
  const stats = fs.statSync(pdfFullPath);
  console.log(`PDF successfully generated: ${pdfFullPath} (${(stats.size / 1024).toFixed(1)} KB)`);
} catch (err) {
  console.error('Error generating PDF:', err);
  process.exit(1);
}
