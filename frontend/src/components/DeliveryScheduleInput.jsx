import {useRef} from 'react';
import ThemedSelect from './ThemedSelect';
import {deliveryFrequencies,deliveryTimes,parseDeliverySchedule,serializeDeliverySchedule} from '../lib/deliverySchedule';

export default function DeliveryScheduleInput({ref:controlRef,attributes}){
  const frequencyRef=useRef(null),startRef=useRef(null),endRef=useRef(null);
  const value=attributes.value??'',parsed=parseDeliverySchedule(value),schedule=parsed||{frequency:'',start:'',end:''};
  const disabled=attributes.disabled||attributes.readOnly;
  const update=(key,next)=>{
    const node=controlRef.current;node.value=serializeDeliverySchedule({...schedule,[key]:next});attributes.onChange?.({target:node,currentTarget:node});
  };
  const select=(key,label,options,ref)=> <ThemedSelect ref={ref} maxRows={6} attributes={{className:attributes.className,value:schedule[key],disabled,'aria-label':label,'aria-invalid':attributes['aria-invalid'],'aria-describedby':attributes['aria-describedby'],onChange:event=>update(key,event.target.value),onBlur:()=>attributes.onBlur?.({target:controlRef.current})}}>
    <option value="">{key==='frequency'?'No scheduled deliveries':key==='start'?'Choose start time':'Choose end time'}</option>
    {[...new Set([...options,...(schedule[key]?[schedule[key]]:[])])].map(item=><option key={item} value={item}>{item}</option>)}
  </ThemedSelect>;
  return <span className="delivery-schedule-input">
    <input {...attributes} type="hidden" ref={controlRef}/>
    {select('frequency','Delivery frequency',deliveryFrequencies,frequencyRef)}
    {schedule.frequency&&<span className="delivery-schedule-times">{select('start','Delivery start time',deliveryTimes,startRef)}{select('end','Delivery end time',deliveryTimes,endRef)}</span>}
    <span className="field-validation-hint">{value&&!parsed?`Saved schedule: ${value}. Choose a frequency and time window.`:'Times use Sri Lanka time. The end must be later on the same day.'}</span>
  </span>;
}
