import {useEffect,useState} from 'react';
import {api} from '../lib/api';
import CrudPage from './CrudPage';
export default function PumpSettings(){
  const [types,setTypes]=useState([]),[error,setError]=useState('');
  const load=()=>api('/api/fuel-operations/pumps').then(feed=>{setTypes(feed.inventories.map(t=>t.fuelType));setError('');}).catch(e=>setError(e.message));
  useEffect(()=>{load();},[]);
  return <div className="manager-pump-settings">{error&&<p role="alert" className="text-brand-accent text-sm mb-4">{error}<button className="underline ml-3" onClick={load}>Retry</button></p>}<CrudPage title="Pump settings" endpoint="/api/fuel-operations/pumps" rowsKey="pumps" defaults={{status:'Active'}} create={{url:'/api/fuel-operations/pumps'}} update={id=>({url:'/api/fuel-operations/pumps/'+id,method:'PUT'})} remove={id=>({url:'/api/fuel-operations/pumps/'+id})} fields={[{key:'pumpName',label:'Pump name'},{key:'pumpNumber',label:'Pump number'},{key:'fuelType',label:'Fuel tank',options:types},{key:'location',label:'Location',optional:true},{key:'assignedOperator',label:'Operator',optional:true},{key:'status',label:'State',options:['Active','Maintenance','Disabled']}]} columns={[{key:'pumpName',label:'Pump',width:'20%'},{key:'pumpNumber',label:'Number',width:'15%'},{key:'fuelType',label:'Fuel',width:'25%'},{key:'status',label:'State',width:'20%'}]}/></div>;
}
