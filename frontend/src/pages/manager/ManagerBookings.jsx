import {useCallback,useEffect,useState} from 'react';
import {Link,useLocation,useNavigate} from 'react-router-dom';
import {motion} from 'framer-motion';
import {api,confirmAction,notice} from '../../lib/api';
import {Badge,Modal,QuickDeck} from '../../components/DataUI';
import {Input,Select} from '../../components/Validation';
import {exportCSV} from '../../components/BillingPages';
import BookingForm from '../../components/ManagerBookingForm';
import {BookingDetails,ServiceHeader,ServiceTable} from './ManagerServiceUI';
import {useLiveRefresh} from '../../lib/useLiveRefresh';
import {formatTimeRange} from '../../lib/tableUtils';

const emptyBookingFilters={
  status:'',
  serviceType:'',
  customerType:'',
  dateFrom:'',
  dateTo:''
};

function BookingFilters({draft,setDraft,query,onSearch,filters,setFilters,statuses,packages,customerTypes,onExport,compact=false}){
  const active=Boolean(query||draft||Object.values(filters).some(Boolean));
  const update=(key,value)=>setFilters(cur=>({...cur,[key]:value}));

  return <div className={`manager-service-filters-panel${compact?' is-compact':''}`}>
    <div className="manager-service-primary-filters">
      <form noValidate className="manager-service-search" onSubmit={event=>{event.preventDefault();onSearch(draft.trim());}}>
        <Input aria-label="Search bookings" placeholder="Search bookings..." className="input-dark" value={draft} onChange={event=>setDraft(event.target.value)}/>
        {draft&&<button type="button" aria-label="Clear search text" onClick={()=>{setDraft('');onSearch('');}}><i aria-hidden="true" className="fa-solid fa-xmark"/></button>}
        <button type="submit" aria-label="Search"><i aria-hidden="true" className="fa-solid fa-magnifying-glass"/></button>
      </form>
      <div className="manager-service-select">
        <Select aria-label="Filter status" className="input-dark" value={filters.status} onChange={e=>update('status',e.target.value)}>
          <option value="">All Status</option>
          {statuses.map(s=><option key={s} value={s}>{s}</option>)}
        </Select>
        <i aria-hidden="true" className="fa-solid fa-chevron-down"/>
      </div>
      <div className="manager-service-select">
        <Select aria-label="Filter service package" className="input-dark" value={filters.serviceType} onChange={e=>update('serviceType',e.target.value)}>
          <option value="">All Packages</option>
          {packages.map(p=><option key={p} value={p}>{p}</option>)}
        </Select>
        <i aria-hidden="true" className="fa-solid fa-chevron-down"/>
      </div>
      <div className="manager-service-select">
        <Select aria-label="Filter customer type" className="input-dark" value={filters.customerType} onChange={e=>update('customerType',e.target.value)}>
          <option value="">All Customers</option>
          {customerTypes.map(ct=><option key={ct.value} value={ct.value}>{ct.label}</option>)}
        </Select>
        <i aria-hidden="true" className="fa-solid fa-chevron-down"/>
      </div>
    </div>
    <div className="manager-service-date-filters">
      <div className="manager-service-date-range" role="group" aria-label="Service date range">
        <span>Service date:</span>
        <Input validationKey="dateFrom" type="date" aria-label="Service date from" className="input-dark" value={filters.dateFrom} onChange={e=>update('dateFrom',e.target.value)}/>
        <span aria-hidden="true">&ndash;</span>
        <Input validationKey="dateTo" min={filters.dateFrom||'1900-01-01'} type="date" aria-label="Service date to" className="input-dark" value={filters.dateTo} onChange={e=>update('dateTo',e.target.value)}/>
      </div>
      {active&&<button type="button" className="manager-service-reset" onClick={()=>{setDraft('');onSearch('');setFilters(emptyBookingFilters);}}>
        <i aria-hidden="true" className="fa-solid fa-rotate-left mr-1.5"/>Reset filters
      </button>}
      {compact&&<button className="btn-accent manager-invoice-export" onClick={onExport}>Export CSV</button>}
    </div>
  </div>;
}

export default function ManagerBookings(){
  const location=useLocation(),navigate=useNavigate();
  const [data,setData]=useState({bookings:[],vehicles:[]}),[services,setServices]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [draft,setDraft]=useState(''),[query,setQuery]=useState(''),[filters,setFilters]=useState(emptyBookingFilters);
  const [selection,setSelection]=useState(()=>location.state?.openNewBooking?{kind:'form',direct:true,booking:{licensePlate:location.state.vehicle?.licensePlate||'',customerName:location.state.vehicle?.ownerName||'',customerPhone:location.state.vehicle?.ownerContact||''}}:null);

  const load=useCallback(async()=>{
    try{
      const [feed,catalog]=await Promise.all([api('/api/workshop/bookings'),api('/api/services')]);
      setData({bookings:feed?.bookings||[],vehicles:feed?.vehicles||[]});
      setServices(catalog?.services||[]);
      setError('');
    }catch(error){
      setError(error.message);
    }finally{
      setLoading(false);
    }
  },[]);

  useEffect(()=>{load();},[load]);
  useLiveRefresh(load);

  useEffect(()=>{
    if(location.state?.openNewBooking)navigate(location.pathname+location.search,{replace:true,state:null});
  },[location.state,location.pathname,location.search,navigate]);

  const changeQuery=value=>{setQuery(value);};
  const changeFilters=value=>{setFilters(value);};

  const cancel=async booking=>{
    if(!booking.cancellable||!booking.referenceNumber)return;
    if(!(await confirmAction('Cancel this unpaid Pending booking? Its invoice will be voided.')))return;
    try{
      await api('/api/workshop/bookings/status/'+encodeURIComponent(booking.referenceNumber),{method:'POST',body:JSON.stringify({status:'Cancelled'})});
      await load();
      notice('Booking cancelled');
    }catch(error){
      setError(error.message);
    }
  };

  const dateError=Boolean(filters.dateFrom&&filters.dateTo&&filters.dateFrom>filters.dateTo);
  const within=(value,from,to)=>(!from&&!to)||Boolean(value&&(!from||value>=from)&&(!to||value<=to));

  const filtered=(data?.bookings||[]).filter(booking=>{
    if(dateError)return false;
    if(filters.status&&booking.status!==filters.status)return false;
    if(filters.serviceType&&booking.serviceType!==filters.serviceType)return false;
    if(filters.customerType&&booking.customerType!==filters.customerType)return false;
    if(!within(booking.serviceDate,filters.dateFrom,filters.dateTo))return false;
    if(query&&![booking.referenceNumber,booking.invoiceNumber,booking.licensePlate,booking.customerName,booking.customerPhone,booking.customerType,booking.serviceType].some(val=>String(val??'').toLowerCase().includes(query.toLowerCase())))return false;
    return true;
  });

  const exportBookings=()=>exportCSV(filtered.map(b=>({
    'Reference Number':b.referenceNumber||'',
    'Invoice #':b.invoiceNumber||'',
    'Vehicle plate':b.licensePlate,
    Customer:b.customerName||'',
    Contact:b.customerPhone||'',
    'Customer type':b.customerType?b.customerType.replaceAll('_',' '):'Walk-in',
    'Service package':b.serviceType,
    'Service date':b.serviceDate,
    'Time':formatTimeRange(b.timeSlot, b.estimatedDuration || services?.find(s=>s.name===b.serviceType)?.estimatedDuration),
    'Start time':b.timeSlot,
    'Estimated cost LKR':b.estimatedCost!=null?b.estimatedCost:'',
    Status:b.status
  })),'service_bookings.csv');

  const statuses=[...new Set(['Pending','Approved','Assigned','In Progress','Waiting for Parts','Quality Check','Completed','Cancelled',...(data?.bookings||[]).map(b=>b.status).filter(Boolean)])];
  const packages=[...new Set([...services.map(s=>s.name||s.title||s),...(data?.bookings||[]).map(b=>b.serviceType)].filter(Boolean))].sort();
  const knownCustomerTypes=[
    {value:'WALK_IN',label:'Walk-in'},
    {value:'REGISTERED',label:'Registered'},
    {value:'CORPORATE',label:'Corporate'}
  ];
  const discoveredTypes=[...new Set((data?.bookings||[]).map(b=>b.customerType).filter(Boolean))];
  const customerTypes=[
    ...knownCustomerTypes,
    ...discoveredTypes.filter(d=>!knownCustomerTypes.some(k=>k.value===d)).map(d=>({value:d,label:d.replaceAll('_',' ')}))
  ];

  const filterProps={
    draft,
    setDraft,
    query,
    onSearch:changeQuery,
    filters,
    setFilters:changeFilters,
    statuses,
    packages,
    customerTypes,
    onExport:exportBookings
  };

  return <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2}} className="manager-service manager-bookings">
    <ServiceHeader
      kicker="Service department"
      title="Service appointments."
      subtext={`${(data?.bookings||[]).length} total appointments`}
      actions={<>
        <button className="btn-accent manager-invoice-export" disabled={!filtered.length} onClick={exportBookings}>EXPORT CSV</button>
        <button className="btn-accent manager-service-primary" onClick={()=>setSelection({kind:'form',booking:{}})}>NEW BOOKING</button>
      </>}
    />
    {error&&<p role="alert" className="text-brand-accent mb-5">{error} <button className="underline ml-2" onClick={()=>{setLoading(true);load();}}>Retry</button></p>}
    <BookingFilters {...filterProps}/>
    <QuickDeck><BookingFilters {...filterProps} compact/></QuickDeck>
    {dateError&&<p role="alert" className="text-brand-accent text-sm mt-4">A date range must end on or after its starting date.</p>}
    <ServiceTable name="Bookings" rows={filtered} loading={loading} columns={[
      {label:'Reference Number',width:'13%',render:b=><><span className="manager-service-id">{b.referenceNumber||'Unavailable'}</span>{b.invoiceId&&<Link className="manager-service-meta" to={`/dashboard/invoices/${b.invoiceId}`}>{b.invoiceNumber||'View invoice'}</Link>}</>},
      {key:'licensePlate',label:'Vehicle plate',width:'10%',render:b=><span className="font-mono">{b.licensePlate}</span>},
      {label:'Customer',width:'14%',render:b=><><strong>{b.customerName||'Unavailable'}</strong><span className="manager-service-meta manager-service-classification">{b.customerType?.replaceAll('_',' ')||'Unspecified'}</span></>},
      {key:'serviceType',label:'Service package',width:'13%'},
      {key:'serviceDate',label:'Date',width:'9%',render:b=><span className="font-mono">{b.serviceDate}</span>},
      {label:'Time',width:'14%',render:b=><span className="manager-service-meta font-mono">{formatTimeRange(b.timeSlot, b.estimatedDuration || services?.find(s=>s.name===b.serviceType)?.estimatedDuration)}</span>},
      {label:'Status',width:'9%',render:b=><Badge value={b.status}/>},
      {label:'Actions',width:'18%',render:b=><div className="manager-service-actions"><button className="manager-service-action" onClick={()=>setSelection({kind:'view',booking:b})}>View</button><button className="manager-service-action" disabled={!b.editable||!b.referenceNumber} onClick={()=>setSelection({kind:'form',booking:b})}>Edit</button><button className="manager-service-action" disabled={!b.cancellable||!b.referenceNumber} onClick={()=>cancel(b)}>Cancel</button></div>}
    ]}/>
    {selection&&<Modal direct={selection.direct} title={selection.kind==='view'?'Booking details':selection.booking.id?'Edit booking':'New booking'} onClose={()=>setSelection(null)}>
      {selection.kind==='view'?<BookingDetails booking={selection.booking}/>:loading?<p>Loading booking options...</p>:<BookingForm initial={selection.booking} vehicles={data.vehicles} services={services} onSaved={()=>{setSelection(null);load();}}/>}
    </Modal>}
  </motion.div>;
}
