/**
 * Utility functions for grids, pagination, CSV exports and status color coding.
 */

export function extractTextFromNode(node) {
  if (node == null || typeof node === 'boolean') return '';
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node)) return node.map(extractTextFromNode).filter(Boolean).join(' ').trim();
  if (node.props?.value != null) return String(node.props.value);
  if (node.props?.children) return extractTextFromNode(node.props.children);
  return '';
}

export function getStatusTone(value) {
  const text = String(value || '').toLowerCase().trim().replace(/[-_]/g, ' ');
  if (!text || text === '?') return 'gray';
  
  if (
    /^(paid|success|completed|active|available|approved|resolved|confirmed|optimal|in stock|read)$/i.test(text) ||
    ((text.includes('paid') || text.includes('completed') || text.includes('active') || text.includes('available') || text.includes('approved') || text.includes('resolved') || text.includes('confirmed')) &&
      !/inactive|deactivated|partial|unpaid/i.test(text))
  ) {
    return 'green';
  }
  if (/cancel|reject|overdue|inactive|deactivated|void|failed|critical|out of stock|danger|error/i.test(text)) {
    return 'red';
  }
  if (/pending|waiting|hold|unpaid|draft|warning|low stock|low/i.test(text)) {
    return 'amber';
  }
  if (/quality|check|review|testing|inspect/i.test(text)) {
    return 'purple';
  }
  if (/progress|service|assigned|scheduled|open|partial|refund|advance/i.test(text)) {
    return 'blue';
  }
  return 'gray';
}

export function getPaginationRange(current, totalPages, maxVisible = 5) {
  if (totalPages <= 1) return [1];
  if (totalPages <= maxVisible + 2) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  const pages = [];
  const start = Math.max(2, current - 1);
  const end = Math.min(totalPages - 1, current + 1);

  pages.push(1);
  if (start > 2) pages.push('...');
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  if (end < totalPages - 1) pages.push('...');
  pages.push(totalPages);
  return pages;
}

export function exportTableToCsv(name = 'records', rows = [], columns = []) {
  if (!rows || !rows.length) {
    alert('No records available to export.');
    return;
  }
  // Exclude action columns
  const exportableCols = columns && columns.length
    ? columns.filter(c => !/^(action|actions|service|services|manage|select|check)$/i.test(String(c.label || '')))
    : null;

  const quote = value => '"' + String(value ?? '').replaceAll('"', '""') + '"';

  let headers = [];
  let dataRows = [];

  if (exportableCols && exportableCols.length) {
    headers = exportableCols.map(c => c.label || c.key || 'Column');
    dataRows = rows.map(row => {
      return exportableCols.map(c => {
        if (typeof c.csvValue === 'function') return c.csvValue(row);
        if (c.key && row[c.key] != null && typeof row[c.key] !== 'object') return row[c.key];
        if (typeof c.render === 'function') {
          const rendered = c.render(row);
          return extractTextFromNode(rendered);
        }
        return row[c.key] ?? '';
      });
    });
  } else {
    const sample = rows[0];
    headers = Object.keys(sample).filter(k => typeof sample[k] !== 'object');
    dataRows = rows.map(row => headers.map(k => row[k] ?? ''));
  }

  const csvContent = '\uFEFF' + [
    headers.map(quote).join(','),
    ...dataRows.map(row => row.map(quote).join(','))
  ].join('\r\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
  const filename = `${String(name).toLowerCase().replace(/[^a-z0-9_-]+/g, '_') || 'export'}_${new Date().toISOString().slice(0, 10)}.csv`;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function parseDurationMinutes(dur) {
  if (!dur) return 60;
  const str = String(dur).trim();
  const matches = [...str.matchAll(/(\d+(?:\.\d+)?)\s*(hours?|hrs?|h|minutes?|mins?|m)?/gi)];
  let total = 0;
  let found = false;
  for (const m of matches) {
    if (!m[1]) continue;
    const val = parseFloat(m[1]);
    const unit = (m[2] || '').toLowerCase();
    if (unit.startsWith('h')) {
      total += Math.round(val * 60);
      found = true;
    } else if (unit.startsWith('m')) {
      total += Math.round(val);
      found = true;
    } else if (!unit && !found) {
      total += val <= 12 ? Math.round(val * 60) : Math.round(val);
      found = true;
    }
  }
  return total > 0 ? total : 60;
}

export function formatTimeRange(slot, duration) {
  if (!slot) return 'Unavailable';
  if (slot.includes('–') || slot.includes(' - ')) return slot;

  const match = String(slot).trim().match(/^(\d{1,2}):(\d{2})\s*(AM|PM)$/i);
  if (!match) return slot;

  let h = parseInt(match[1], 10);
  const m = parseInt(match[2], 10);
  const ampm = match[3].toUpperCase();

  if (ampm === 'PM' && h !== 12) h += 12;
  if (ampm === 'AM' && h === 12) h = 0;

  const mins = parseDurationMinutes(duration);
  const endMinutesTotal = h * 60 + m + mins;

  const endH24 = Math.floor(endMinutesTotal / 60) % 24;
  const endM = endMinutesTotal % 60;
  const endAmPm = endH24 >= 12 ? 'PM' : 'AM';
  const endH12 = endH24 % 12 || 12;

  const endFormatted = `${String(endH12).padStart(2, '0')}:${String(endM).padStart(2, '0')} ${endAmPm}`;
  return `${slot} – ${endFormatted}`;
}
