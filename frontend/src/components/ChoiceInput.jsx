import {useState} from 'react';
import {Input,Select} from './Validation';
import {withCurrent} from '../lib/fieldOptions';

export default function ChoiceInput({options=[],allowCustom=false,placeholder='Choose an option',customPlaceholder,onChange,...props}){
  const [custom,setCustom]=useState(false);
  const choices=withCurrent(options,props.value);
  if(custom)return <div className="choice-input-custom"><Input {...props} onChange={onChange} placeholder={customPlaceholder}/><button type="button" className="choice-input-back" onClick={()=>setCustom(false)}>Choose from list</button></div>;
  return <Select {...props} onChange={event=>{if(event.target.value==='__enter_custom_value__'){setCustom(true);event.target.value='';}onChange?.(event);}}><option value="">{placeholder}</option>{choices.map(option=>{
    const item=typeof option==='object'?option:{value:option,label:option};
    return <option key={item.value} value={item.value} disabled={item.disabled}>{item.label}</option>;
  })}{allowCustom&&<option value="__enter_custom_value__">Enter another value…</option>}</Select>;
}
