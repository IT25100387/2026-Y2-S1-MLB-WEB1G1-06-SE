import {useCallback,useEffect,useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {motion} from 'framer-motion';
import {api,confirmAction,notice} from '../../lib/api';
import {Modal,QuickDeck} from '../../components/DataUI';
import EntityForm from '../../components/EntityForm';
import {vehicleFields} from '../../components/VehiclePages';
import {ServiceHeader,ServiceFilters,ServiceTable} from './ManagerServiceUI';

export default function ManagerVehicles(){
  const [rows,setRows]=useState([]),[accounts,setAccounts]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [draft,setDraft]=useState(''),[query,setQuery]=useState(''),[selected,setSelected]=useState(null),[deleting,setDeleting]=useState(null);
  const navigate=useNavigate(),root='/api/workshop/vehicles/registry';
  const load=useCallback(async()=>{try{const [feed,people]=await Promise.all([api('/api/workshop/vehicles/registry'),api('/api/billing/customer-accounts')]);setRows(feed.vehicles);setAccounts(people);setError('');}catch(error){setError(error.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();window.addEventListener('fuelcore:refresh',load);return()=>window.removeEventListener('fuelcore:refresh',load);},[load]);
  const save=async form=>{await api(root+(selected.id?'/update/'+selected.id:'/add'),{method:'POST',body:JSON.stringify(form)});setSelected(null);await load();notice('Vehicle saved');};
  const erase=async vehicle=>{if(!(await confirmAction(`Delete ${vehicle.licensePlate}? Vehicles with service or invoice history must be retained.`)))return;setDeleting(vehicle.id);try{await api(root+'/delete/'+vehicle.id,{method:'POST'});await load();notice('Vehicle deleted');}catch(error){setError(error.message);}finally{setDeleting(null);}};
  const book=vehicle=>navigate('/dashboard/bookings',{state:{openNewBooking:true,vehicle}});
  const filters=<ServiceFilters label="Search vehicles" draft={draft} setDraft={setDraft} query={query} onSearch={setQuery}><button className="btn-accent manager-service-primary" onClick={()=>setSelected({})}>Register vehicle</button></ServiceFilters>;
  return <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2}} className="manager-service manager-vehicles"><ServiceHeader kicker="Fleet directory" title="Vehicle registry.">{filters}</ServiceHeader>{error&&<p role="alert" className="text-brand-accent mb-5">{error} <button onClick={load}>Retry</button></p>}<QuickDeck>{filters}</QuickDeck><ServiceTable name="Vehicles" rows={rows.filter(vehicle=>[vehicle.licensePlate,vehicle.make,vehicle.model,vehicle.ownerName,vehicle.ownerContact,vehicle.ownerUsername].some(value=>String(value??'').toLowerCase().includes(query.toLowerCase())))} loading={loading} columns={[
    {key:'licensePlate',label:'Registration',width:'15%',render:v=><span className="manager-service-id">{v.licensePlate}</span>},
    {label:'Make / Model',width:'15%',render:v=><><strong>{v.make}</strong><span className="manager-service-meta">{v.model}</span></>},
    {key:'ownerName',label:'Owner',width:'16%'},{key:'ownerContact',label:'Contact',width:'14%'},{key:'registeredDate',label:'Enrolled',width:'11%'},
    {label:'Actions',width:'24%',render:v=><div className="manager-service-actions"><button className="manager-service-action" onClick={()=>book(v)}>Book slot</button><button className="manager-service-action" onClick={()=>setSelected({...v})}>Edit</button><button className="manager-service-action" onClick={()=>navigate('/dashboard/service-history?licensePlate='+encodeURIComponent(v.licensePlate))}>History</button><button className="manager-service-action" disabled={deleting!==null} onClick={()=>erase(v)}>{deleting===v.id?'Deleting...':'Delete'}</button></div>}
  ]}/>{selected&&<Modal title={selected.id?'Edit vehicle':'Register vehicle'} onClose={()=>setSelected(null)}><EntityForm initial={selected} fields={[...vehicleFields,{key:'ownerUsername',label:'Customer account',optional:true,options:accounts.map(account=>({value:account.username,label:account.fullName+' (@'+account.username+')'}))}]} submitLabel="Save vehicle" onSave={save}/></Modal>}</motion.div>;
}
