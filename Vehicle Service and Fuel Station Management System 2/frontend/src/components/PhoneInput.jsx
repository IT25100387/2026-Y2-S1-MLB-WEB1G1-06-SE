import {useEffect,useRef,useState} from 'react';
import ThemedSelect from './ThemedSelect';
import {callingCode,countryForPhone,nationalNumber,normalizeNational,phoneCountries,tooLongPhone} from '../lib/phoneNumbers';

export default function PhoneInput({ref:controlRef,attributes,label}){
  const countryRef=useRef(null),emitted=useRef(null),[selected,setSelected]=useState(()=>countryForPhone(attributes.value||attributes.defaultValue)),[draft,setDraft]=useState(attributes.defaultValue||'');
  const value=attributes.value??draft,country=selected,digits=nationalNumber(value,country);
  useEffect(()=>{if(attributes.value!=null&&attributes.value!==emitted.current)setSelected(current=>countryForPhone(attributes.value,current));},[attributes.value]);
  const emit=(next,event)=>{
    const node=controlRef.current;node.value=next;emitted.current=next;setDraft(next);
    attributes.onChange?.({target:node,currentTarget:node,nativeEvent:event?.nativeEvent});
  };
  const update=event=>{
    const raw=event.target.value;
    let nextCountry=country,nextDigits;
    if(/^\s*(\+|00)/.test(raw)){
      const international=raw.trim().replace(/^00/,'+').replace(/[^+\d]/g,'');
      nextCountry=countryForPhone(international,country);nextDigits=nationalNumber(international,nextCountry);
    }else nextDigits=normalizeNational(raw,country);
    if(tooLongPhone(nextDigits,nextCountry))return;
    setSelected(nextCountry);emit(nextDigits?'+'+callingCode(nextCountry)+nextDigits:'',event);
  };
  const disabled=attributes.disabled||attributes.readOnly;
  const example=country==='LK'?'e.g. 771234567':`Enter the number without +${callingCode(country)}`;
  return <span className="phone-input">
    <input {...attributes} ref={controlRef} type="hidden" value={value}/>
    <ThemedSelect ref={countryRef} maxRows={6} attributes={{className:attributes.className,value:country,disabled,'aria-label':`Country code for ${label}`,onChange:event=>{const next=event.target.value;setSelected(next);emit(digits?'+'+callingCode(next)+digits:'',event);},onBlur:()=>attributes.onBlur?.({target:controlRef.current})}}>
      {phoneCountries.map(item=><option key={item.code} value={item.code}>{item.name} (+{item.callingCode})</option>)}
    </ThemedSelect>
    <input data-phone-national name={attributes.name+'National'} type="tel" inputMode="tel" autoComplete="tel-national" className={attributes.className} value={digits} disabled={attributes.disabled} readOnly={attributes.readOnly} placeholder={example} maxLength={25} aria-label={label} aria-required={attributes['aria-required']} aria-invalid={attributes['aria-invalid']} aria-describedby={attributes['aria-describedby']} onChange={update} onFocus={event=>attributes.onFocus?.(event)} onBlur={()=>attributes.onBlur?.({target:controlRef.current})}/>
  </span>;
}
