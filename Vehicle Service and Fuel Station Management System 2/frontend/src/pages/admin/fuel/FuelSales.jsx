import {Input} from '../../../components/Validation';
import {Navigate,useLocation} from 'react-router-dom';
import {Badge,Field,money} from '../../../components/DataUI';
import {useFuelOperations} from './FuelOperationsContext';
import FuelPageShell from './FuelPageShell';

const legacyPages={'#pump-settings':'/dashboard/fuel/pumps','#daily-collection':'/dashboard/fuel/collection','#pump-history':'/dashboard/fuel/history','#pump-figures':'/dashboard/fuel/sales'};
export default function FuelSales(){
  const {data,forms,errors,busy,change,save}=useFuelOperations(),location=useLocation();
  if(legacyPages[location.hash])return <Navigate to={legacyPages[location.hash]} replace/>;
  return <FuelPageShell section="pump-figures" title="POS sales reconciliation." description={data&&<>Station day <strong>{data.stationDay}</strong> <span>·</span> 05:00 to 05:00 <span>·</span> Asia/Colombo</>}>
    <section id="pump-figures" aria-label="Daily pump figures">
      {data&&(!data.pumps.length?<div className="manager-fuel-empty"><h2>No pumps configured.</h2><p>Add a pump through Pump settings to enter daily figures.</p></div>:<div className="manager-pump-grid">{data.pumps.map(pump=>{
        const form=forms[pump.id]||{},confirmed=data.confirmedPumpIds.includes(pump.id),issue=errors[pump.id]||{},inactive=form.pumpStatus==='Inactive',working=busy.has(pump.id),disabled=confirmed||working;
        return <section className="manager-pump-card" key={pump.id} aria-label={pump.pumpName}>
          <div className="manager-pump-heading"><span className="manager-pump-icon"><i aria-hidden="true" className="fa-solid fa-gas-pump"/></span><div><h2>{pump.pumpName}</h2><p>{pump.fuelType}</p></div><span className="manager-pump-badge"><Badge value={confirmed?'Confirmed':'Draft'}/></span></div>
          <form noValidate onSubmit={event=>{event.preventDefault();save(pump.id,true);}}>
            <div className="manager-pump-state"><span id={`pump-status-${pump.id}`}>Pump status <small>{inactive?'Inactive':'Active'}</small></span><button type="button" role="switch" aria-label={`${pump.pumpName} active`} aria-checked={!inactive} disabled={disabled} onClick={()=>change(pump.id,'pumpStatus',inactive?'Active':'Inactive')} className="manager-pump-toggle"><span/></button></div>
            <div className="manager-pump-inputs">{inactive?<Field label="Reason for deactivation" error={issue.deactivationReason}><Input validationKey="deactivationReason" disabled={disabled} className="input-dark" maxLength={255} value={form.deactivationReason||''} placeholder="e.g. Under maintenance" aria-required="true" onChange={event=>change(pump.id,'deactivationReason',event.target.value)}/></Field>:<>{[['litersSold','Fuel liters sold today','e.g. 1500.50'],['collectedAmount','Actual cash collected (LKR)','e.g. 550000.00']].map(([key,label,placeholder])=><Field key={key} label={label} error={issue[key]}><Input validationKey={key} disabled={disabled} className="input-dark" type="number" min="0" step="0.01" inputMode="decimal" value={form[key]??''} placeholder={placeholder} onChange={event=>change(pump.id,key,event.target.value)}/></Field>)}</>}</div>
            {issue.form&&<p role="alert" className="manager-fuel-error">{issue.form}</p>}
            {confirmed?<div className="manager-pump-confirmed"><i aria-hidden="true" className="fa-solid fa-check"/> Confirmed for this station day <span>{inactive?'Inactive · '+(form.deactivationReason||''):money(form.litersSold)+' L · Rs. '+money(form.collectedAmount)}</span></div>:<><button type="submit" disabled={working} className="btn-accent manager-pump-confirm">{working?'Please wait…':inactive?'Confirm':'Confirm sale'}</button><button type="button" disabled={working} className="manager-pump-draft" onClick={()=>save(pump.id,false)}>Save draft</button></>}
          </form>
        </section>;
      })}</div>)}
      <p className="manager-fuel-footnote">Each pump is saved independently. Confirmed figures stay locked until the next station day starts at 5 a.m.</p>
    </section>
  </FuelPageShell>;
}
