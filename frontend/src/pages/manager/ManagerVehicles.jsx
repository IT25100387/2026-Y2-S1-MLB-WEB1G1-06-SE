import {useCallback,useEffect,useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {motion} from 'framer-motion';
import {api,confirmAction,notice} from '../../lib/api';
import {Modal,QuickDeck} from '../../components/DataUI';
import {Input,Select} from '../../components/Validation';
import {exportCSV} from '../../components/BillingPages';
import EntityForm from '../../components/EntityForm';
import {vehicleFields} from '../../components/VehiclePages';
import {ServiceHeader,ServiceTable} from './ManagerServiceUI';

const emptyVehicleFilters={
  make:'',
  model:'',
  fuelType:'',
  registeredFrom:'',
  registeredTo:'',
  year:''
};

function VehicleFilters({draft,setDraft,query,onSearch,filters,setFilters,makes,models,fuelTypes,years,onExport,compact=false}){
  const active=Boolean(query||draft||Object.values(filters).some(Boolean));
  const update=(key,value)=>{
    setFilters(cur=>{
      const next={...cur,[key]:value};
      if(key==='make')next.model='';
      return next;
    });
  };

  return <div className={`manager-service-filters-panel${compact?' is-compact':''}`}>
    <div className="manager-service-primary-filters">
      <form noValidate className="manager-service-search" onSubmit={event=>{event.preventDefault();onSearch(draft.trim());}}>
        <Input aria-label="Search vehicles" placeholder="Search vehicles..." className="input-dark" value={draft} onChange={event=>setDraft(event.target.value)}/>
        {draft&&<button type="button" aria-label="Clear search text" onClick={()=>{setDraft('');onSearch('');}}><i aria-hidden="true" className="fa-solid fa-xmark"/></button>}
        <button type="submit" aria-label="Search"><i aria-hidden="true" className="fa-solid fa-magnifying-glass"/></button>
      </form>
      <div className="manager-service-select">
        <Select aria-label="Filter make" className="input-dark" value={filters.make} onChange={e=>update('make',e.target.value)}>
          <option value="">All Makes</option>
          {makes.map(m=><option key={m} value={m}>{m}</option>)}
        </Select>
        <i aria-hidden="true" className="fa-solid fa-chevron-down"/>
      </div>
      <div className="manager-service-select">
        <Select aria-label="Filter model" className="input-dark" value={filters.model} onChange={e=>update('model',e.target.value)}>
          <option value="">All Models</option>
          {models.map(m=><option key={m} value={m}>{m}</option>)}
        </Select>
        <i aria-hidden="true" className="fa-solid fa-chevron-down"/>
      </div>
      <div className="manager-service-select">
        <Select aria-label="Filter fuel type" className="input-dark" value={filters.fuelType} onChange={e=>update('fuelType',e.target.value)}>
          <option value="">All Fuel Types</option>
          {fuelTypes.map(f=><option key={f} value={f}>{f}</option>)}
        </Select>
        <i aria-hidden="true" className="fa-solid fa-chevron-down"/>
      </div>
    </div>
    <div className="manager-service-date-filters">
      <div className="manager-service-date-range" role="group" aria-label="Enrolled date range">
        <span>Enrolled:</span>
        <Input validationKey="registeredFrom" type="date" aria-label="Enrolled date from" className="input-dark" value={filters.registeredFrom} onChange={e=>update('registeredFrom',e.target.value)}/>
        <span aria-hidden="true">&ndash;</span>
        <Input validationKey="registeredTo" min={filters.registeredFrom||'1900-01-01'} type="date" aria-label="Enrolled date to" className="input-dark" value={filters.registeredTo} onChange={e=>update('registeredTo',e.target.value)}/>
      </div>
      {years.length>0&&<div className="manager-service-select">
        <Select aria-label="Filter manufacture year" className="input-dark" value={filters.year} onChange={e=>update('year',e.target.value)}>
          <option value="">All Years</option>
          {years.map(y=><option key={y} value={y}>{y}</option>)}
        </Select>
        <i aria-hidden="true" className="fa-solid fa-chevron-down"/>
      </div>}
      {active&&<button type="button" className="manager-service-reset" onClick={()=>{setDraft('');onSearch('');setFilters(emptyVehicleFilters);}}>
        <i aria-hidden="true" className="fa-solid fa-rotate-left mr-1.5"/>Reset filters
      </button>}
      {compact&&<button className="btn-accent manager-invoice-export" onClick={onExport}>Export CSV</button>}
    </div>
  </div>;
}

export default function ManagerVehicles(){
  const [rows,setRows]=useState([]),[accounts,setAccounts]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [draft,setDraft]=useState(''),[query,setQuery]=useState(''),[filters,setFilters]=useState(emptyVehicleFilters);
  const [selected,setSelected]=useState(null),[deleting,setDeleting]=useState(null);
  const navigate=useNavigate(),root='/api/workshop/vehicles/registry';

  const load=useCallback(async()=>{
    try{
      const [feed,people]=await Promise.all([api('/api/workshop/vehicles/registry'),api('/api/billing/customer-accounts')]);
      setRows(feed.vehicles||[]);
      setAccounts(people||[]);
      setError('');
    }catch(error){
      setError(error.message);
    }finally{
      setLoading(false);
    }
  },[]);

  useEffect(()=>{
    load();
    window.addEventListener('fuelcore:refresh',load);
    return()=>window.removeEventListener('fuelcore:refresh',load);
  },[load]);

  const changeQuery=value=>{setQuery(value);};
  const changeFilters=value=>{setFilters(value);};

  const save=async form=>{await api(root+(selected.id?'/update/'+selected.id:'/add'),{method:'POST',body:JSON.stringify(form)});setSelected(null);await load();notice('Vehicle saved');};
  const erase=async vehicle=>{if(!(await confirmAction(`Delete ${vehicle.licensePlate}? Vehicles with service or invoice history must be retained.`)))return;setDeleting(vehicle.id);try{await api(root+'/delete/'+vehicle.id,{method:'POST'});await load();notice('Vehicle deleted');}catch(error){setError(error.message);}finally{setDeleting(null);}};
  const book=vehicle=>navigate('/dashboard/bookings',{state:{openNewBooking:true,vehicle}});

  const dateError=Boolean(filters.registeredFrom&&filters.registeredTo&&filters.registeredFrom>filters.registeredTo);
  const within=(value,from,to)=>(!from&&!to)||Boolean(value&&(!from||value>=from)&&(!to||value<=to));

  const filtered=rows.filter(v=>{
    if(dateError)return false;
    if(filters.make&&v.make!==filters.make)return false;
    if(filters.model&&v.model!==filters.model)return false;
    if(filters.fuelType&&v.fuelType!==filters.fuelType)return false;
    if(filters.year&&String(v.manufactureYear)!==String(filters.year))return false;
    if(!within(v.registeredDate,filters.registeredFrom,filters.registeredTo))return false;
    if(query&&![v.licensePlate,v.make,v.model,v.ownerName,v.ownerContact,v.ownerUsername].some(val=>String(val??'').toLowerCase().includes(query.toLowerCase())))return false;
    return true;
  });

  const exportVehicles=()=>exportCSV(filtered.map(v=>({
    'License plate':v.licensePlate,
    Make:v.make||'',
    Model:v.model||'',
    Owner:v.ownerName||'',
    Contact:v.ownerContact||'',
    'Customer account':v.ownerUsername||'',
    'Fuel type':v.fuelType||'',
    'Manufacture year':v.manufactureYear||'',
    'Mileage (km)':v.mileage!=null?v.mileage:'',
    'Engine number':v.engineNumber||'',
    'Chassis number':v.chassisNumber||'',
    'Enrolled date':v.registeredDate||''
  })),'vehicles.csv');

  const makes=[...new Set(rows.map(r=>r.make).filter(Boolean))].sort();
  const models=[...new Set(rows.filter(r=>!filters.make||r.make===filters.make).map(r=>r.model).filter(Boolean))].sort();
  const fuelTypes=['Petrol','Diesel','Hybrid','Electric'];
  const years=[...new Set(rows.map(r=>r.manufactureYear).filter(Boolean))].sort((a,b)=>b-a);

  const filterProps={
    draft,
    setDraft,
    query,
    onSearch:changeQuery,
    filters,
    setFilters:changeFilters,
    makes,
    models,
    fuelTypes,
    years,
    onExport:exportVehicles
  };

  return <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2}} className="manager-service manager-vehicles">
    <ServiceHeader
      kicker="Fleet directory"
      title="Vehicle registry."
      subtext={`${rows.length} total registered vehicles`}
      actions={<>
        <button className="btn-accent manager-invoice-export" disabled={!filtered.length} onClick={exportVehicles}>EXPORT CSV</button>
        <button className="btn-accent manager-service-primary" onClick={()=>setSelected({})}>REGISTER VEHICLE</button>
      </>}
    />
    {error&&<p role="alert" className="text-brand-accent mb-5">{error} <button onClick={load}>Retry</button></p>}
    <VehicleFilters {...filterProps}/>
    <QuickDeck><VehicleFilters {...filterProps} compact/></QuickDeck>
    {dateError&&<p role="alert" className="text-brand-accent text-sm mt-4">A date range must end on or after its starting date.</p>}
    <ServiceTable name="Vehicles" rows={filtered} loading={loading} columns={[
      {key:'licensePlate',label:'License plate',width:'14%',render:v=><span className="manager-service-id">{v.licensePlate}</span>},
      {key:'make',label:'Make',width:'9%',render:v=><strong>{v.make}</strong>},
      {key:'model',label:'Model',width:'9%',render:v=><span className="manager-service-meta">{v.model}</span>},
      {key:'ownerName',label:'Owner',width:'14%'},
      {key:'ownerContact',label:'Contact',width:'13%'},
      {key:'registeredDate',label:'Enrolled',width:'11%'},
      {label:'Service',width:'15%',render:v=><div className="manager-service-actions"><button className="manager-service-action" onClick={()=>book(v)}>Book slot</button><button className="manager-service-action" onClick={()=>navigate('/dashboard/service-history?licensePlate='+encodeURIComponent(v.licensePlate))}>History</button></div>},
      {label:'Actions',width:'15%',render:v=><div className="manager-service-actions"><button className="manager-service-action" onClick={()=>setSelected({...v})}>Edit</button><button className="manager-service-action" disabled={deleting!==null} onClick={()=>erase(v)}>{deleting===v.id?'Deleting...':'Delete'}</button></div>}
    ]}/>
    {selected&&<Modal title={selected.id?'Edit vehicle':'Register vehicle'} onClose={()=>setSelected(null)}>
      <EntityForm initial={selected} fields={[...vehicleFields,{key:'ownerUsername',label:'Customer account',optional:true,options:accounts.map(account=>({value:account.username,label:account.fullName+' (@'+account.username+')'}))}]} submitLabel="Save vehicle" onSave={save}/>
    </Modal>}
  </motion.div>;
}
