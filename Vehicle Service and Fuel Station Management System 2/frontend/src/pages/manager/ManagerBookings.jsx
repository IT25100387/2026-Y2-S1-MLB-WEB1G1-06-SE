import {useCallback,useEffect,useState} from 'react';
import {Link,useLocation,useNavigate} from 'react-router-dom';
import {motion} from 'framer-motion';
import {api,confirmAction,notice} from '../../lib/api';
import {Badge,Modal,QuickDeck} from '../../components/DataUI';
import BookingForm from '../../components/ManagerBookingForm';
import {BookingDetails,ServiceFilters,ServiceHeader,ServiceTable} from './ManagerServiceUI';
import {useLiveRefresh} from '../../lib/useLiveRefresh';

export default function ManagerBookings(){
  const location=useLocation(),navigate=useNavigate();
  const [data,setData]=useState({bookings:[],vehicles:[]}),[services,setServices]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [draft,setDraft]=useState(''),[query,setQuery]=useState(''),[status,setStatus]=useState('');
  const [selection,setSelection]=useState(()=>location.state?.openNewBooking?{kind:'form',direct:true,booking:{licensePlate:location.state.vehicle?.licensePlate||'',customerName:location.state.vehicle?.ownerName||'',customerPhone:location.state.vehicle?.ownerContact||''}}:null);
  const load=useCallback(async()=>{try{const [feed,catalog]=await Promise.all([api('/api/workshop/bookings'),api('/api/services')]);setData(feed);setServices(catalog.services);setError('');}catch(error){setError(error.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();},[load]);
  useLiveRefresh(load);
  useEffect(()=>{if(location.state?.openNewBooking)navigate(location.pathname+location.search,{replace:true,state:null});},[location.state,location.pathname,location.search,navigate]);
  const cancel=async booking=>{if(!booking.cancellable||!booking.referenceNumber)return;if(!(await confirmAction('Cancel this unpaid Pending booking? Its invoice will be voided.')))return;try{await api('/api/workshop/bookings/status/'+encodeURIComponent(booking.referenceNumber),{method:'POST',body:JSON.stringify({status:'Cancelled'})});await load();notice('Booking cancelled');}catch(error){setError(error.message);}};
  const filters=<ServiceFilters label="Search bookings" draft={draft} setDraft={setDraft} query={query} onSearch={setQuery} status={status} onStatus={setStatus} statuses={[...new Set(['Pending','Approved','Assigned','In Progress','Waiting for Parts','Quality Check','Completed','Cancelled',...data.bookings.map(booking=>booking.status).filter(Boolean)])]}><button className="btn-accent manager-service-primary" onClick={()=>setSelection({kind:'form',booking:{}})}>New booking</button></ServiceFilters>;
  const rows=data.bookings.filter(booking=>(!status||booking.status===status)&&[booking.referenceNumber,booking.invoiceNumber,booking.licensePlate,booking.customerName,booking.customerPhone,booking.customerType,booking.serviceType].some(value=>String(value??'').toLowerCase().includes(query.toLowerCase())));
  return <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2}} className="manager-service manager-bookings"><ServiceHeader kicker="Service department" title="Service appointments.">{filters}</ServiceHeader>{error&&<p role="alert" className="text-brand-accent mb-5">{error} <button onClick={load}>Retry</button></p>}<QuickDeck>{filters}</QuickDeck><ServiceTable name="Bookings" rows={rows} loading={loading} columns={[
    {label:'Ref #',width:'15%',render:b=><><span className="manager-service-id">{b.referenceNumber||'Unavailable'}</span>{b.invoiceId&&<Link className="manager-service-meta" to={`/dashboard/invoices/${b.invoiceId}`}>{b.invoiceNumber||'View invoice'}</Link>}</>},
    {key:'licensePlate',label:'Vehicle plate',width:'11%',render:b=><span className="font-mono">{b.licensePlate}</span>},
    {label:'Customer',width:'16%',render:b=><><strong>{b.customerName||'Unavailable'}</strong><span className="manager-service-meta manager-service-classification">{b.customerType?.replaceAll('_',' ')||'Unspecified'}</span></>},
    {key:'serviceType',label:'Service package',width:'14%'},
    {label:'Date & time',width:'11%',render:b=><><span className="font-mono">{b.serviceDate}</span><span className="manager-service-meta">{b.timeSlot}</span></>},
    {label:'Status',width:'11%',render:b=><Badge value={b.status}/>},
    {label:'Actions',width:'17%',render:b=><div className="manager-service-actions"><button className="manager-service-action" onClick={()=>setSelection({kind:'view',booking:b})}>View</button><button className="manager-service-action" disabled={!b.editable||!b.referenceNumber} onClick={()=>setSelection({kind:'form',booking:b})}>Edit</button><button className="manager-service-action" disabled={!b.cancellable||!b.referenceNumber} onClick={()=>cancel(b)}>Cancel</button></div>}
  ]}/>{selection&&<Modal direct={selection.direct} title={selection.kind==='view'?'Booking details':selection.booking.id?'Edit booking':'New booking'} onClose={()=>setSelection(null)}>{selection.kind==='view'?<BookingDetails booking={selection.booking}/>:loading?<p>Loading booking options...</p>:<BookingForm initial={selection.booking} vehicles={data.vehicles} services={services} onSaved={()=>{setSelection(null);load();}}/>}</Modal>}</motion.div>;
}
