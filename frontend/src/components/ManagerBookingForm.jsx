import {useEffect,useRef,useState} from 'react';
import {api,notice} from '../lib/api';
import BookingForm from './BookingForm';
import BookingWizard from './BookingWizard';

function NewManagerBooking({initial={},vehicles,services,onSaved}) {
  const [form,setForm]=useState(initial),[slots,setSlots]=useState([]),[errors,setErrors]=useState({}),[busy,setBusy]=useState(false);
  const [availabilityLoading,setAvailabilityLoading]=useState(false),[availabilityError,setAvailabilityError]=useState(''),[reload,setReload]=useState(0),[offers,setOffers]=useState([]),[offerError,setOfferError]=useState('');
  const saving=useRef(false);
  useEffect(()=>{let alive=true;api('/api/offers/services').then(feed=>{if(alive)setOffers(feed.offers||[]);}).catch(()=>{if(alive)setOfferError('Offers could not be loaded. Estimates show catalog prices; the invoice applies current offers.');});return()=>{alive=false;};},[]);
  useEffect(()=>{
    setSlots([]);setAvailabilityError('');setAvailabilityLoading(false);
    if(!form.serviceDate||!form.serviceType)return;
    let alive=true;setAvailabilityLoading(true);
    api(`/api/workshop/bookings/availability?date=${form.serviceDate}&serviceType=${encodeURIComponent(form.serviceType)}`).then(data=>{if(alive)setSlots(data.slots);}).catch(error=>{if(alive)setAvailabilityError(error.message);}).finally(()=>{if(alive)setAvailabilityLoading(false);});
    return()=>{alive=false;};
  },[form.serviceDate,form.serviceType,reload]);
  const change=(key,value)=>{setForm(old=>({...old,[key]:value,...(['serviceDate','serviceType'].includes(key)?{timeSlot:''}:{})}));setErrors({});};
  const save=async()=>{
    if(saving.current)return;saving.current=true;setBusy(true);
    try{const result=await api('/api/workshop/bookings/add',{method:'POST',body:JSON.stringify(form)});notice('Booking saved'+(result.referenceNumber?' · '+result.referenceNumber:''));onSaved();}
    catch(error){setErrors({form:error.message});}
    finally{saving.current=false;setBusy(false);}
  };
  const packages=services.filter(item=>item.active).map(item=>{const offer=offers.find(row=>row.active&&String(row.targetId)===String(item.id));return offer?{...item,offerPrice:Math.round(item.estimatedCost*(1-offer.discountPercentage/100)*100)/100,discountPercentage:offer.discountPercentage}:item;});
  return <BookingWizard form={form} onChange={change} vehicles={vehicles} services={packages} slots={slots} errors={errors} setErrors={setErrors} busy={busy} onConfirm={save} availabilityLoading={availabilityLoading} availabilityError={availabilityError} onRetryAvailability={()=>setReload(value=>value+1)} estimateNotice={offerError}/>;
}

// Existing edit forms and every other role retain the original BookingForm.
export default function ManagerBookingForm(props){return props.initial?.id?<BookingForm {...props}/>:<NewManagerBooking {...props}/>;}
