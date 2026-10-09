import {useCallback,useEffect,useRef,useState} from 'react';
import {Link,useNavigate} from 'react-router-dom';
import {useReducedMotion} from 'framer-motion';
import {useAuth} from '../../context/AuthContext';
import {api,confirmAction,notice} from '../../lib/api';
import {DataTable,SearchBar,QuickDeck,Modal} from '../../components/DataUI';
import EntityForm from '../../components/EntityForm';
import GarageOffers from '../../components/GarageOffers';
import {vehicleFields} from '../../components/VehiclePages';
import './CustomerGarage.css';

export default function CustomerMyGarage() {
  const {account}=useAuth(),navigate=useNavigate(),section=useRef(null),addButton=useRef(null),reduced=useReducedMotion();
  const [vehicles,setVehicles]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[query,setQuery]=useState(''),[selected,setSelected]=useState(null);
  const load=useCallback(async()=>{try{setVehicles(await api('/api/v1/customer/vehicles'));setError('');}catch(e){setError(e.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();},[load]);
  const closeForm=()=>{setSelected(null);addButton.current?.focus();};
  const save=async form=>{await api('/api/v1/customer/vehicles'+(selected.id?'/'+selected.id:''),{method:selected.id?'PUT':'POST',body:JSON.stringify(form)});closeForm();await load();notice('Changes saved');};
  const erase=async row=>{if(!await confirmAction(`Delete ${row.licensePlate}?`))return;try{await api('/api/v1/customer/vehicles/'+row.id,{method:'DELETE'});await load();notice('Record deleted');}catch(e){setError(e.message);}};
  const openAdd=()=>setSelected({});
  const goToVehicles=()=>{section.current?.scrollIntoView({behavior:reduced?'instant':'smooth',block:'start'});addButton.current?.focus({preventScroll:true});};
  const controls=<SearchBar onSearch={setQuery} actions={<button ref={addButton} className="btn-accent garage-add-button" onClick={openAdd}><i aria-hidden="true" className="fa-solid fa-plus"/>Add new vehicle</button>}/>;
  const shortcuts=[{label:'Book a service',path:'/customer/catalog',icon:'fa-calendar-plus',detail:'Find the right service'}, {label:'Spare part store',path:'/customer/store',icon:'fa-gears',detail:'Parts for your next mile'}, {label:'Live tracking',path:'/customer/appointments',icon:'fa-location-dot',detail:'Follow your appointments'}, {label:'Pending payments',path:'/customer/invoices',icon:'fa-file-invoice-dollar',detail:'View and pay invoices'}];
  const filtered=vehicles.filter(vehicle=>JSON.stringify(vehicle).toLowerCase().includes(query.toLowerCase()));
  return <div className="fuelcore-garage">
    <section className="garage-welcome" aria-labelledby="garage-welcome-title">
      <div className="garage-welcome-photo" aria-hidden="true"><img src="/customer-garage.jpg" alt=""/></div>
      <div className="garage-welcome-copy"><p className="garage-kicker">Your personal garage</p><h1 id="garage-welcome-title">Welcome Back,<br/><span>{account.fullName||account.username}!</span></h1><p className="garage-welcome-description">Here's what's happening with your vehicles today.</p></div>
      <div className="garage-shortcuts">{shortcuts.map(item=><Link className="garage-shortcut" key={item.path} to={item.path}><span className="garage-shortcut-top"><i aria-hidden="true" className={'fa-solid '+item.icon}/></span><strong>{item.label}</strong><span>{item.detail}<i aria-hidden="true" className="fa-solid fa-arrow-up-right-from-square"/></span></Link>)}<button className="garage-shortcut" onClick={goToVehicles}><span className="garage-shortcut-top"><i aria-hidden="true" className="fa-solid fa-car-side"/></span><strong>Add new vehicle</strong><span>Grow your garage<i aria-hidden="true" className="fa-solid fa-arrow-down"/></span></button></div>
    </section>
    <GarageOffers/>
    <section ref={section} id="garage-vehicles" className="garage-vehicles" aria-labelledby="garage-vehicles-title">
      <div className="garage-section-heading"><div><p className="garage-kicker">Vehicles you call yours</p><h2 id="garage-vehicles-title">My garage.</h2></div><p>{loading?'Loading your vehicles...':`${vehicles.length} registered ${vehicles.length===1?'vehicle':'vehicles'}`}<span>Keep your details ready for your next visit.</span></p></div>
      <div className="garage-vehicle-panel">
        {error&&<p role="alert" className="garage-error">{error} <button onClick={load}>Retry vehicles</button></p>}
        {controls}<QuickDeck><SearchBar onSearch={setQuery} actions={<button className="btn-accent garage-add-button" onClick={openAdd}><i aria-hidden="true" className="fa-solid fa-plus"/>Add new vehicle</button>}/></QuickDeck>
        {loading?<p className="garage-table-loading">Loading your garage...</p>:<DataTable rows={filtered} columns={[
          {key:'licensePlate',label:'Vehicle',width:'17%',render:row=><Link className="garage-plate" to={'/customer/vehicles/'+row.id}>{row.licensePlate}<small>View details & history <i aria-hidden="true" className="fa-solid fa-arrow-up-right-from-square"/></small></Link>},
          {key:'make',label:'Make',width:'12%'},{key:'model',label:'Model',width:'13%'},{key:'ownerName',label:'Owner',width:'15%'},{key:'ownerContact',label:'Contact',width:'13%'},
          {label:'Actions',width:'25%',render:row=><div className="garage-row-actions"><button onClick={()=>setSelected({...row})} aria-label={'Edit '+row.licensePlate}>Edit</button><button onClick={()=>erase(row)} aria-label={'Delete '+row.licensePlate}>Delete</button><button onClick={()=>navigate('/customer/book',{state:{vehicle:row}})} aria-label={'Book service for '+row.licensePlate}>Book service</button></div>}
        ]}/>}
      </div>
    </section>
    {selected&&<Modal title={selected.id?'Edit vehicle':'Add new vehicle'} onClose={closeForm}><EntityForm fields={vehicleFields} initial={selected} onSave={save}/></Modal>}
  </div>;
}
