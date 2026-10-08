import {Input,TextArea,RequiredMark} from './Validation';
import VehiclePicker from './VehiclePicker';
import {useRef,useState} from 'react';
import {ruleFor,validateValue} from '../lib/validation';
import {Field,money} from './DataUI';
import './BookingWizard.css';

export default function BookingWizard({form,onChange,vehicles,services,slots,errors,setErrors,busy,onConfirm,customer=false,loading=false,availabilityLoading=false,availabilityError='',onRetryAvailability,notesKey='notes',estimateNotice=''}) {
  const [step,setStep]=useState(1),heading=useRef(null);
  const vehicle=vehicles.find(item=>item.licensePlate===form.licensePlate);
  const service=services.find(item=>item.name===form.serviceType);
  const labels=['Vehicle & service','Date & time','Review & confirm'];
  const [today]=useState(()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date()));
  const issuesFor=stage=>{
    const issues={};
    if(stage===1){
      if(!form.licensePlate||(customer&&!vehicle))issues.licensePlate='Select your vehicle';
      if(!customer&&!vehicle){
        if(!form.customerName?.trim())issues.customerName='Enter guest customer name';
        if(!form.customerPhone?.trim())issues.customerPhone='Enter guest contact';
        for(const [key,label] of [['licensePlate','Vehicle plate'],['customerName','Guest customer name'],['customerPhone','Guest contact']]){const issue=validateValue(form[key],ruleFor(key,label,{required:true}));if(issue)issues[key]=issue;}
      }
      if(!service)issues.serviceType='Select a service package';
    }else{
      if(!form.serviceDate)issues.serviceDate='Select a service date';
      else if(form.serviceDate<today)issues.serviceDate='Service date cannot be in the past';
      if(!form.timeSlot||!slots.some(slot=>slot.timeSlot===form.timeSlot&&slot.available))issues.timeSlot='Select an available start time';
      const noteIssue=validateValue(form[notesKey],ruleFor(notesKey,'Notes',{maxLength:1000,multiline:true}));if(noteIssue)issues[notesKey]=noteIssue;
      if(availabilityLoading||availabilityError)issues.timeSlot='Wait for available times or retry loading them';
    }
    return issues;
  };
  const move=next=>{setStep(next);requestAnimationFrame(()=>{heading.current?.focus({preventScroll:true});heading.current?.scrollIntoView({block:'nearest',behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});});};
  const submit=event=>{
    event.preventDefault();if(busy||loading)return;
    const issues=step===3?{...issuesFor(1),...issuesFor(2)}:issuesFor(step);
    setErrors(issues);
    if(Object.keys(issues).length){if(step===3)move(issues.licensePlate||issues.serviceType||issues.customerName||issues.customerPhone?1:2);return;}
    if(step<3)move(step+1);else onConfirm();
  };
  const price=service?.offerPrice??service?.estimatedCost;
  return <form noValidate onSubmit={submit} className="booking-wizard">
    <ol className="booking-wizard-steps" aria-label="Booking steps">{labels.map((label,index)=><li key={label} className={step===index+1?'is-current':step>index+1?'is-complete':''} aria-current={step===index+1?'step':undefined}><span>{step>index+1?<i aria-hidden="true" className="fa-solid fa-check"/>:index+1}</span><strong>{label}</strong></li>)}</ol>
    <h2 ref={heading} tabIndex={-1} className="booking-wizard-heading">{labels[step-1]}</h2>
    {loading?<p role="status" className="booking-wizard-muted">Loading booking options...</p>:<fieldset disabled={busy} className="booking-wizard-stage">
      {step===1&&<>
        <Field label={customer?'Select your vehicle':'Vehicle plate'} error={errors.licensePlate}><VehiclePicker vehicles={vehicles} value={form.licensePlate||''} customer={customer} error={errors.licensePlate} onChange={value=>onChange('licensePlate',value)}/></Field>
        {!customer&&(vehicle?<p className="booking-wizard-muted">{vehicle.ownerName} · {vehicle.ownerContact} · {vehicle.ownerUsername?'Account customer':'Registered vehicle'}</p>:<div className="booking-wizard-guest">{[['customerName','Guest customer name'],['customerPhone','Guest contact']].map(([key,label])=><Field key={key} label={label} error={errors[key]}><Input validationKey={key} aria-invalid={!!errors[key]} className="input-dark w-full p-3" value={form[key]||''} onChange={event=>onChange(key,event.target.value)}/></Field>)}</div>)}
        <fieldset className="booking-wizard-packages"><legend>Select service package<RequiredMark/></legend><div className="booking-wizard-service-list">{services.map(item=><button type="button" className={`booking-wizard-service${form.serviceType===item.name?' is-selected':''}`} aria-pressed={form.serviceType===item.name} key={item.id} onClick={()=>onChange('serviceType',item.name)}><div><h3>{item.name}</h3>{form.serviceType===item.name&&<i aria-hidden="true" className="fa-solid fa-circle-check"/>}</div><p>{item.shortDescription||item.category||'Vehicle service'}</p><footer><strong>Rs. {money(item.offerPrice??item.estimatedCost)}</strong>{item.estimatedDuration&&<span>{item.estimatedDuration}</span>}</footer>{item.discountPercentage>0&&<small>{item.discountPercentage}% offer · Regular Rs. {money(item.estimatedCost)}</small>}</button>)}</div>{!services.length&&<p className="booking-wizard-muted">No service packages are currently available.</p>}{errors.serviceType&&<p role="alert" className="text-brand-accent text-xs">{errors.serviceType}</p>}</fieldset>
      </>}
      {step===2&&<>
        <Field label="Select date" error={errors.serviceDate}><Input validationKey="serviceDate" aria-invalid={!!errors.serviceDate} type="date" min={today} className="input-dark w-full p-3" value={form.serviceDate||''} onChange={event=>onChange('serviceDate',event.target.value)}/></Field>
        <fieldset className="booking-wizard-times"><legend>Select time slot<RequiredMark/></legend>{!form.serviceDate?<p className="booking-wizard-muted">Choose a date to see available start times.</p>:availabilityLoading?<p role="status" className="booking-wizard-muted">Checking available start times...</p>:availabilityError?<p role="alert" className="text-brand-accent text-xs">{availabilityError} <button type="button" onClick={onRetryAvailability}>Retry available times</button></p>:<div className="booking-wizard-slot-list">{slots.map(slot=><button type="button" key={slot.timeSlot} disabled={!slot.available} aria-pressed={form.timeSlot===slot.timeSlot} className={form.timeSlot===slot.timeSlot?'is-selected':''} onClick={()=>onChange('timeSlot',slot.timeSlot)}>{slot.timeSlot}{!slot.available&&<small>Unavailable</small>}</button>)}</div>}{form.serviceDate&&!availabilityLoading&&!availabilityError&&!slots.some(slot=>slot.available)&&<p className="booking-wizard-muted">No available start times. Choose another date.</p>}{errors.timeSlot&&<p role="alert" className="text-brand-accent text-xs">{errors.timeSlot}</p>}</fieldset>
        <Field label="Problem description / notes" error={errors[notesKey]} optional><TextArea validationKey={notesKey} maxLength={1000} rows={3} className="input-dark w-full p-3" placeholder="E.g., Brakes are squeaking, needs an oil filter change..." value={form[notesKey]||''} onChange={event=>onChange(notesKey,event.target.value)}/></Field>
      </>}
      {step===3&&<section className="booking-wizard-review" aria-label="Booking summary"><h3>Booking summary</h3><dl>{[['Vehicle',form.licensePlate],['Service package',form.serviceType],['Date',form.serviceDate],['Time',form.timeSlot],...(!customer?[['Customer',vehicle?.ownerName||form.customerName],['Contact',vehicle?.ownerContact||form.customerPhone]]:[]),['Estimated service total',`Rs. ${money(price)}`],...(form[notesKey]?[['Problem description / notes',form[notesKey]]]:[])].map(([label,value])=><div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl><p>The final invoice includes any parts recorded during the service. Your service reference and invoice are created when you confirm this booking.</p>{estimateNotice&&<p>{estimateNotice}</p>}</section>}
    </fieldset>}
    {errors.form&&<p role="alert" className="text-brand-accent text-xs">{errors.form}</p>}
    <div className="booking-wizard-actions"><button type="button" className="booking-wizard-back" disabled={step===1||busy} onClick={()=>{setErrors({});move(step-1);}}>Back</button><button type="submit" className="btn-accent px-6 py-3" disabled={busy||loading||(customer&&!vehicles.length)||!services.length}>{busy?'Saving booking...':step===3?'Confirm booking':'Next step'}<i aria-hidden="true" className={`fa-solid ${step===3?'fa-check':'fa-arrow-right'}`}/></button></div>
  </form>;
}
