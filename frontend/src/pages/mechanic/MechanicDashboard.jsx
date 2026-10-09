import {Input,TextArea} from '../../components/Validation';
import {useCallback,useEffect,useRef,useState} from 'react';
import {api,requestKey,notice} from '../../lib/api';
import {ruleFor,validateValue} from '../../lib/validation';
import {DataTable,SearchBar,QuickDeck,Badge,Field,Modal,money} from '../../components/DataUI';

const unstarted=new Set(['Approved','Assigned','Pending']);
const finished=new Set(['Completed','Cancelled']);
const stationToday=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());

export default function MechanicDashboard({manager=false}){
  const [data,setData]=useState({jobs:[],parts:[],today:stationToday()}),[selected,setSelected]=useState(null);
  const [query,setQuery]=useState(''),[filter,setFilter]=useState(''),[day,setDay]=useState(stationToday);
  const [notes,setNotes]=useState(''),[partSearch,setPartSearch]=useState(''),[partQuery,setPartQuery]=useState('');
  const [partId,setPartId]=useState(''),[quantity,setQuantity]=useState('1'),[error,setError]=useState(''),[busy,setBusy]=useState(false);
  const key=useRef(requestKey()),customDay=useRef(false);
  const load=useCallback(async()=>{
    const feed=await api(manager?'/api/workshop/jobs/workspace':'/api/v1/mechanic/workspace');
    setData({...feed,today:feed.today||stationToday()});
    if(!customDay.current)setDay(feed.today||stationToday());
    setSelected(old=>old?feed.jobs.find(row=>row.job.id===old.job.id)||null:null);
    return feed;
  },[manager]);
  useEffect(()=>{
    const refresh=()=>load().catch(failure=>setError(failure.message));
    refresh();const tick=setInterval(()=>{if(!document.hidden)refresh();},20000);
    return()=>clearInterval(tick);
  },[load]);
  const open=row=>{
    setSelected(row);setNotes(row.job.mechanicNotes||'');setError('');setPartId('');setQuantity('1');
    setPartSearch('');setPartQuery('');key.current=requestKey();
  };
  const update=async(row,status,mechanicNotes)=>api(manager?'/api/workshop/job/update/'+row.job.id:'/api/v1/mechanic/jobs/'+row.job.id,{
    method:manager?'POST':'PATCH',body:JSON.stringify({status,mechanicNotes})
  });
  const start=async row=>{
    if(busy)return;setBusy(true);setError('');
    try{await update(row,'In Progress',row.job.mechanicNotes||'');await load();notice('Job started');}
    catch(failure){setError(failure.message);await load().catch(()=>{});}finally{setBusy(false);}
  };
  const save=async(event,end=false)=>{
    event.preventDefault();if(busy||!selected||finished.has(selected.job.status))return;
    const issue=validateValue(notes,ruleFor('mechanicNotes','Mechanic notes'));if(issue)return setError(issue);
    setBusy(true);setError('');
    try{
      await update(selected,end?'Completed':selected.job.status,notes);await load();
      if(end)setSelected(null);notice(end?'Job completed. Details are now read-only.':'Job progress saved');
    }catch(failure){setError(failure.message);await load().catch(()=>{});}finally{setBusy(false);}
  };
  const chosen=data.parts.find(part=>String(part.id)===String(partId));
  const eligibleParts=data.parts.filter(part=>part.stockQuantity>0&&!['inactive','deactivated'].includes(String(part.status||'').toLowerCase()));
  const matches=eligibleParts.filter(part=>(part.partName+' '+(part.category||'')).toLowerCase().includes(partQuery.toLowerCase()));
  const usePart=async event=>{
    event.preventDefault();if(busy||!selected||finished.has(selected.job.status))return;
    const qty=Number(quantity);setError('');
    if(!chosen||!eligibleParts.some(part=>part.id===chosen.id))return setError('Choose an available spare part.');
    if(!Number.isInteger(qty)||qty<1||qty>chosen.stockQuantity||qty>9999)return setError('Enter a whole quantity from 1 to '+Math.min(chosen.stockQuantity,9999)+'.');
    setBusy(true);
    try{
      await api((manager?'/api/workshop':'/api/v1/mechanic')+'/jobs/'+selected.job.id+'/parts',{
        method:'POST',headers:{'Idempotency-Key':key.current},body:JSON.stringify({partId:chosen.id,quantity:qty})
      });
      await load();setPartId('');setQuantity('1');key.current=requestKey();notice('Part recorded against this reference and invoice updated');
    }catch(failure){setError(failure.message);await load().catch(()=>{});}finally{setBusy(false);}
  };
  const rows=data.jobs.filter(row=>(!day||row.booking?.serviceDate===day)&&(!filter||row.job.status===filter)&&
    (row.job.referenceNumber+' '+row.job.licensePlate+' '+(row.booking?.serviceType||'')).toLowerCase().includes(query.toLowerCase())).map(row=>({...row,id:row.job.id}));
  const controls=<div className="mechanic-job-filters">
    <SearchBar onSearch={setQuery} status={filter} setStatus={setFilter} statuses={[...new Set(data.jobs.map(row=>row.job.status))]}/>
    <div className="mechanic-day-filter"><label>Schedule day<Input validationKey="day" aria-label="Schedule day" className="input-dark" type="date" value={day} onChange={event=>{customDay.current=true;setDay(event.target.value);}}/></label>
      <button className="admin-action" aria-pressed={day===data.today} onClick={()=>{customDay.current=false;setDay(data.today);}}>Today</button>
      <button className="admin-action" aria-pressed={!day} onClick={()=>{customDay.current=true;setDay('');}}>All days</button>
    </div>
  </div>;
  const readOnly=selected&&finished.has(selected.job.status);
  return <div className="mechanic-workspace">
    <header className="mechanic-job-heading"><div className="mechanic-job-heading-copy">
      <p className="mechanic-job-kicker">Your workshop workspace</p><h1>{manager?'Assigned workshop jobs':'My jobs'}</h1>
      <p className="mechanic-job-intro">Start your assigned jobs, record the parts you use and keep each service up to date.</p>
    </div><div className="mechanic-job-photo" aria-hidden="true"/></header>
    <section className="mechanic-job-list" aria-label="Assigned jobs">
      {!selected&&error&&<p role="alert" className="text-brand-accent text-xs mb-4">{error}</p>}
      {controls}<QuickDeck>{controls}</QuickDeck>
      <DataTable rows={rows} empty={day?'No jobs scheduled for this day.':'No assigned jobs found.'} columns={[
        {label:'Reference',width:'19%',render:row=><span className="font-mono">{row.job.referenceNumber}</span>},
        {label:'Vehicle',width:'12%',render:row=>row.job.licensePlate},
        {label:'Service',width:'16%',render:row=>row.booking?.serviceType},
        {label:'Schedule',width:'17%',render:row=><div className="text-xs">{row.booking?.serviceDate}<br/>{row.booking?.timeSlot}{row.scheduledEnd&&<span className="admin-meta">Ends {row.scheduledEnd.slice(11,16)}</span>}</div>},
        {label:'Bay',width:'10%',render:row=>row.job.bayName},
        {label:'State',width:'10%',render:row=><Badge value={row.job.status}/>},
        {label:'Actions',width:'11%',render:row=>unstarted.has(row.job.status)?
          <button className="admin-action" disabled={busy||row.booking?.serviceDate>data.today} title={row.booking?.serviceDate>data.today?'Available on the scheduled service day':undefined} onClick={()=>start(row)}>Start</button>:
          <button className="admin-action" disabled={busy} onClick={()=>open(row)}>{finished.has(row.job.status)?'Details':'Progress'}</button>}
      ]}/>
    </section>
    {selected&&<Modal title={selected.job.referenceNumber} onClose={()=>{if(!busy)setSelected(null);}}>
      <div className="mechanic-progress-dialog"><p className="mechanic-job-meta">{selected.job.licensePlate} &middot; {selected.booking?.serviceType} &middot; {selected.job.bayName}</p>
        <div className="mechanic-job-details">
          <section className="mechanic-progress-panel"><h3>{readOnly?'Job details':'Progress & notes'}</h3>
            {readOnly?<><Badge value={selected.job.status}/><h4 className="mechanic-notes-label">Mechanic notes</h4><p className="mechanic-saved-notes">{selected.job.mechanicNotes||'No notes recorded.'}</p><p className="admin-meta">This job is read-only.</p></>:
              <form onSubmit={save} noValidate className="space-y-4"><Field label="Progress"><Input validationKey="status" className="input-dark w-full" readOnly value={selected.job.status}/></Field>
                <Field label="Mechanic notes" optional><TextArea validationKey="mechanicNotes" className="input-dark w-full" maxLength={4000} value={notes} disabled={busy} onChange={event=>setNotes(event.target.value)}/></Field>
                <div className="mechanic-progress-actions"><button disabled={busy} className="btn-accent">Save progress</button><button disabled={busy} type="button" className="admin-action" onClick={()=>setSelected(null)}>Cancel</button><button disabled={busy} type="button" className="btn-accent" onClick={event=>save(event,true)}>End job</button></div>
              </form>}
          </section>
          <section className="mechanic-parts-panel"><h3>Used spare parts</h3>
            <ul className="mechanic-used-parts">{(selected.usedParts||[]).map(part=><li key={part.id}><div><strong>{part.partName}</strong><span className="admin-meta">{part.quantity} used &middot; Rs. {money(part.unitPrice)} each</span></div><span className="font-mono">Rs. {money(part.totalAmount)}</span></li>)}
              {!selected.usedParts?.length&&<li className="admin-meta">No parts recorded.</li>}</ul>
            {!readOnly&&!unstarted.has(selected.job.status)&&<div className="mechanic-part-picker">
              <form noValidate className="mechanic-part-search" onSubmit={event=>{event.preventDefault();setPartQuery(partSearch.trim());}}><label className="sr-only" htmlFor="mechanic-parts-search">Search spare parts</label><Input id="mechanic-parts-search" type="search" maxLength={100} className="input-dark" placeholder="Search by part name or category" value={partSearch} onChange={event=>setPartSearch(event.target.value)}/><button className="admin-action" disabled={busy}>Search</button></form>
              {partQuery&&<button className="admin-action" onClick={()=>{setPartSearch('');setPartQuery('');}}>Clear part search</button>}
              <ul className="mechanic-part-results" aria-label="Available spare parts">{matches.slice(0,8).map(part=><li key={part.id}><div><strong>{part.partName}</strong><span className="admin-meta">{part.category||'Spare part'} &middot; {part.stockQuantity} available</span></div><button className="admin-action" disabled={busy} aria-label={'Select '+part.partName} aria-pressed={String(part.id)===String(partId)} onClick={()=>{setPartId(String(part.id));setQuantity('1');setError('');key.current=requestKey();}}>Select</button></li>)}{!matches.length&&<li className="admin-meta">No available parts match this search.</li>}</ul>
              {matches.length>8&&<p className="admin-meta">Showing eight matches. Refine your search to find a specific part.</p>}
              {chosen&&<form noValidate onSubmit={usePart} className="mechanic-part-quantity"><p>Selected: <strong>{chosen.partName}</strong></p><Field label="Quantity"><Input validationKey="quantity" className="input-dark w-full" aria-label="Used quantity" type="number" min="1" max={Math.min(chosen.stockQuantity,9999)} step="1" disabled={busy} value={quantity} onChange={event=>{setQuantity(event.target.value);key.current=requestKey();}}/></Field><button disabled={busy} className="btn-accent">Record used part</button></form>}
            </div>}
          </section>
        </div>{error&&<p role="alert" className="text-brand-accent text-xs mt-4">{error}</p>}
      </div>
    </Modal>}
  </div>;
}
