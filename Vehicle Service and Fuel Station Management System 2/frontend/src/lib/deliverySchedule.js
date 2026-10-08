export const deliveryFrequencies=['Daily','Weekdays','Weekends',...['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'].map(day=>'Every '+day)];
export const deliveryTimes=Array.from({length:96},(_,i)=>String(Math.floor(i/4)).padStart(2,'0')+':'+String(i%4*15).padStart(2,'0'));
const timePattern=/^(?:[01]\d|2[0-3]):[0-5]\d$/;
function time(value){
  const clean=String(value).trim().toLowerCase();
  if(clean==='noon')return '12:00';
  if(clean==='midnight')return '00:00';
  if(timePattern.test(clean))return clean;
  const match=clean.match(/^(\d{1,2})(?::([0-5]\d))?\s*(am|pm)$/);
  if(!match||Number(match[1])<1||Number(match[1])>12)return '';
  return String(Number(match[1])%12+(match[3]==='pm'?12:0)).padStart(2,'0')+':'+(match[2]||'00');
}
export function parseDeliverySchedule(value){
  const match=String(value??'').trim().match(/^(Daily|Weekdays|Weekends|Every (?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)),\s*(.*?)\s*(?:–|—|-|\bto\b)\s*(.*?)$/i);
  if(!match)return null;
  const frequency=deliveryFrequencies.find(item=>item.toLowerCase()===match[1].toLowerCase());
  return {frequency,start:time(match[2]),end:time(match[3])};
}
export function serializeDeliverySchedule({frequency,start='',end=''}){return frequency?`${frequency}, ${start}–${end}`:'';}
export function deliveryScheduleError(value){
  if(!String(value??'').trim())return '';
  const schedule=parseDeliverySchedule(value);
  if(!schedule||!schedule.start||!schedule.end)return 'Choose a delivery frequency, start time and end time';
  if(schedule.end<=schedule.start)return 'Delivery end time must be after the start time on the same day';
  return '';
}
export function normalizeDeliverySchedule(value){const schedule=parseDeliverySchedule(value);return schedule&&!deliveryScheduleError(value)?serializeDeliverySchedule(schedule):value;}
