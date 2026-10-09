import {useState} from 'react';
import {Input,Select} from './Validation';

export default function VehiclePicker({vehicles,value='',onChange,error,customer=false}){
  const [guest,setGuest]=useState(()=>!!value&&!vehicles.some(vehicle=>vehicle.licensePlate===value));
  if(guest&&!customer)return <div><Input validationKey="licensePlate" aria-invalid={!!error} className="input-dark w-full p-3" value={value} onChange={event=>onChange(event.target.value)}/><button type="button" className="choice-input-back" onClick={()=>{setGuest(false);onChange('');}}>Choose a registered vehicle</button></div>;
  return <Select validationKey="licensePlate" aria-invalid={!!error} className="input-dark w-full p-3" value={value} maxRows={6} onChange={event=>{
    if(event.target.value==='__guest_vehicle__'){setGuest(true);onChange('');}else onChange(event.target.value);
  }}><option value="">Choose a registered vehicle</option>{vehicles.map(vehicle=><option key={vehicle.id} value={vehicle.licensePlate}>{vehicle.licensePlate} — {customer?[vehicle.make,vehicle.model].filter(Boolean).join(' '):vehicle.ownerName}</option>)}{!customer&&<option value="__guest_vehicle__">Enter a guest vehicle plate…</option>}</Select>;
}
