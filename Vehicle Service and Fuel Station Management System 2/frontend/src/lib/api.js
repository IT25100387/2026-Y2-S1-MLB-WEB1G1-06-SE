import {normalizePayload} from './validation';
const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
export function notice(message, type = 'info') { window.dispatchEvent(new CustomEvent('fuelcore:notice', {detail:{message:String(message),type}})); }
export function confirmAction(message) { return new Promise(resolve => window.dispatchEvent(new CustomEvent('fuelcore:confirm', {detail:{message,resolve}}))); }
export async function apiFetch(url, options = {}) {
  const {silentStatuses=[],...fetchOptions}=options;
  options=fetchOptions;
  const path = String(url).replace(/^http:\/\/(localhost|127\.0\.0\.1):8080/, '');
  if(typeof options.body==='string'){
    try{options={...options,body:JSON.stringify(normalizePayload(JSON.parse(options.body)))};}catch{/* Non-JSON requests retain their own encoding. */}
  }
  let response;
  try { response = await window.fetch(base + path, {...options, credentials:'include'}); }
  catch { const message = 'Cannot connect to the station server. Check the connection and try again.'; notice(message,'error'); throw new Error(message); }
  const json = response.headers.get('content-type')?.includes('application/json') ? await response.clone().json().catch(() => null) : null;
  if (!response.ok || json?.success === false || json?.status === 'error') {
    const message = json?.message || json?.error || (response.status === 403 ? 'Your role cannot perform this action.' : 'The request could not be completed.');
    if (response.status === 401 && !path.startsWith('/api/auth/')) window.dispatchEvent(new Event('fuelcore:session-expired'));
    if (json?.fields) window.dispatchEvent(new CustomEvent('fuelcore:field-errors',{detail:{fields:json.fields}}));
    else if (!path.startsWith('/api/auth/')&&!silentStatuses.includes(response.status)) notice(message,'error');
    const error = new Error(message); error.status = response.status; error.fields=json?.fields||{}; throw error;
  }
  if (options.method && options.method.toUpperCase() !== 'GET' && !path.startsWith('/api/auth/')) window.dispatchEvent(new Event('fuelcore:refresh'));
  return response;
}
export async function api(url, options={}) { const response = await apiFetch(url, {...options, headers:{...(options.body && !(options.body instanceof URLSearchParams) ? {'Content-Type':'application/json'} : {}),...options.headers}}); return response.status===204 ? null : response.json(); }
export function requestKey() { return crypto.randomUUID(); }
