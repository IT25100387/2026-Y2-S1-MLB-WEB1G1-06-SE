import {getCountries,getCountryCallingCode,parsePhoneNumberFromString,validatePhoneNumberLength} from 'libphonenumber-js/max';

export function countryFlag(code){
  if(!code||typeof code!=='string'||code.length!==2)return '';
  return String.fromCodePoint(...[...code.toUpperCase()].map(c=>127397+c.charCodeAt(0)));
}

const names=new Intl.DisplayNames(['en'],{type:'region'});
export const phoneCountries=getCountries().map(code=>({code,callingCode:getCountryCallingCode(code),name:names.of(code)||code,flag:countryFlag(code)})).sort((a,b)=>a.code==='LK'?-1:b.code==='LK'?1:a.name.localeCompare(b.name));
const preferred={'1':'US','7':'RU','44':'GB','61':'AU'};
export const callingCode=country=>getCountryCallingCode(country);
export function countryForPhone(value,current='LK'){
  if(!value?.startsWith('+'))return current;
  const parsed=parsePhoneNumberFromString(value);
  if(parsed?.country)return parsed.country;
  if(value.startsWith('+'+callingCode(current)))return current;
  const match=phoneCountries.filter(country=>value.startsWith('+'+country.callingCode)).sort((a,b)=>b.callingCode.length-a.callingCode.length)[0];
  return match?preferred[match.callingCode]||match.code:current;
}
export function nationalNumber(value,country){
  const prefix='+'+callingCode(country);
  return String(value??'').startsWith(prefix)?String(value).slice(prefix.length):String(value??'').replace(/\D/g,'');
}
export function normalizeNational(value,country){
  let digits=String(value).replace(/\D/g,'');
  if(country==='LK')digits=digits.replace(/^0+/,'');
  const parsed=parsePhoneNumberFromString(digits,country);
  return parsed?String(parsed.nationalNumber):digits;
}
export function tooLongPhone(digits,country){
  return digits.length>15-callingCode(country).length||validatePhoneNumberLength(digits,country)==='TOO_LONG';
}
export function validPhone(value,country){
  if(!/^\+[1-9]\d{6,14}$/.test(value))return false;
  const parsed=parsePhoneNumberFromString(value);
  return !!parsed&&parsed.number===value&&parsed.isValid()&&(!country||parsed.country===country||!parsed.country&&parsed.getPossibleCountries().includes(country));
}
