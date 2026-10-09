import {useCallback,useEffect,useRef,useState} from 'react';
import {Link,useNavigate,useSearchParams,useLocation} from 'react-router-dom';
import {api,notice} from '../../lib/api';
import BookingWizard from '../../components/BookingWizard';

export default function CustomerServiceBooking() {
  const navigate=useNavigate(),location=useLocation(),[params]=useSearchParams(),serviceId=params.get('service');
  const [vehicles,setVehicles]=useState([]),[services,setServices]=useState([]),[slots,setSlots]=useState([]);
  const [form,setForm]=useState({licensePlate:location.state?.vehicle?.licensePlate||'',serviceType:'',serviceDate:'',timeSlot:'',problemDescription:''});
  const [errors,setErrors]=useState({}),[busy,setBusy]=useState(false),[loading,setLoading]=useState(true),[loadError,setLoadError]=useState('');
  const [availabilityLoading,setAvailabilityLoading]=useState(false),[availabilityError,setAvailabilityError]=useState(''),[reload,setReload]=useState(0);
  const saving=useRef(false),loads=useRef(0);
  const load=useCallback(async()=>{
    const request=++loads.current;setLoading(true);setLoadError('');
    try{
      const [v,s]=await Promise.all([api('/api/v1/customer/vehicles'),api('/api/v1/customer/services')]);
      if(request!==loads.current)return;
      setVehicles(v);setServices(s);
      const match=s.find(item=>String(item.id)===serviceId);
      if(match)setForm(old=>old.serviceType?old:{...old,serviceType:match.name});
    }catch(error){if(request===loads.current)setLoadError(error.message);}
    finally{if(request===loads.current)setLoading(false);}
  },[serviceId]);
  useEffect(()=>{load();return()=>{loads.current++;};},[load]);
  useEffect(()=>{
    setSlots([]);setAvailabilityError('');setAvailabilityLoading(false);
    if(!form.serviceDate||!form.serviceType)return;
    let alive=true;setAvailabilityLoading(true);
    api(`/api/v1/customer/appointments/availability?date=${form.serviceDate}&serviceType=${encodeURIComponent(form.serviceType)}`).then(data=>{if(alive)setSlots(data.slots);}).catch(error=>{if(alive)setAvailabilityError(error.message);}).finally(()=>{if(alive)setAvailabilityLoading(false);});
    return()=>{alive=false;};
  },[form.serviceDate,form.serviceType,reload]);
  const change=(key,value)=>{setForm(old=>({...old,[key]:value,...(['serviceDate','serviceType'].includes(key)?{timeSlot:''}:{})}));setErrors({});};
  const submit=async()=>{
    if(saving.current)return;saving.current=true;setBusy(true);
    try{const data=await api('/api/v1/customer/appointments',{method:'POST',body:JSON.stringify(form)});notice(`Appointment saved · ${data.booking.referenceNumber}`);navigate('/customer/appointments');}
    catch(error){setErrors({form:error.message});}
    finally{saving.current=false;setBusy(false);}
  };
  return <div className="max-w-3xl mx-auto">
    <h1 className="text-3xl font-black uppercase tracking-tighter mb-3">Book an appointment</h1>
    <p className="text-gray-400 text-sm mb-6">Follow the three steps to choose your service, schedule your visit and review your booking.</p>
    {loadError&&<p role="alert" className="text-brand-accent text-xs mb-6">{loadError} <button type="button" onClick={load}>Retry booking options</button></p>}
    {!loading&&!loadError&&!vehicles.length&&<p className="text-gray-400 mb-6 text-sm">Add a vehicle in <Link className="text-brand-accent" to="/customer/vehicles">My garage</Link> to book its service.</p>}
    <div className="bg-[#141414] border border-[#333] rounded-xl p-6">
      <BookingWizard customer form={form} onChange={change} vehicles={vehicles} services={services} slots={slots} errors={errors} setErrors={setErrors} busy={busy} loading={loading} onConfirm={submit} notesKey="problemDescription" availabilityLoading={availabilityLoading} availabilityError={availabilityError} onRetryAvailability={()=>setReload(value=>value+1)}/>
    </div>
  </div>;
}
