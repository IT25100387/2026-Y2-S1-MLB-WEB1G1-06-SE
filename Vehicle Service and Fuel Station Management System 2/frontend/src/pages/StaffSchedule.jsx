import {Input,Select,TextArea} from '../components/Validation';
import {useEffect,useState} from 'react';
import {Link,useLocation} from 'react-router-dom';
import {api,notice} from '../lib/api';
import {DataTable,Field,Badge} from '../components/DataUI';

export default function StaffSchedule(){
  const leavePage=useLocation().pathname.endsWith('/leave');
  const [data,setData]=useState(null),[error,setError]=useState(''),[busy,setBusy]=useState(false);
  const [form,setForm]=useState({leaveType:'Annual',startDate:'',endDate:'',reason:''});
  const load=async()=>{try{setData(await api('/api/v1/hr/self'));setError('');}catch(e){setError(e.message);}};
  useEffect(()=>{load();},[]);
  const leave=async e=>{
    e.preventDefault();setError('');
    if(!form.startDate||!form.endDate||form.endDate<form.startDate||!form.reason.trim())return setError('Enter a valid date range and reason');
    setBusy(true);
    try{await api('/api/v1/hr/self/leave',{method:'POST',body:JSON.stringify(form)});setForm({...form,startDate:'',endDate:'',reason:''});await load();notice('Leave request submitted');}
    catch(e){setError(e.message);}finally{setBusy(false);}
  };
  return <div className="space-y-8 max-w-6xl mx-auto">
    <div><p className="text-brand-accent text-xs uppercase tracking-widest mb-3">Your work schedule</p><h1 className="text-3xl font-black uppercase">{leavePage?'Request leave':'My shifts'}</h1><p className="text-gray-400 text-sm mt-3">{leavePage?'Submit a request and follow its approval status.':'Your assigned shifts, dates and work stations.'}</p></div>
    <nav aria-label="Work schedule" className="flex flex-wrap gap-3">{[['My shifts','/dashboard/profile/shifts',!leavePage],['Request leave','/dashboard/profile/leave',leavePage]].map(([label,path,active])=><Link key={path} to={path} aria-current={active?'page':undefined} className={`${active?'btn-accent':'border border-brand-border text-gray-300 hover:text-white'} rounded-full px-5 py-3 text-xs font-bold`}>{label}</Link>)}</nav>
    {error&&<p role="alert" className="text-brand-accent text-sm">{error}{!data&&<button onClick={load} className="underline ml-4">Retry</button>}</p>}
    {!data&&!error&&<p className="text-gray-400 text-sm">Loading your schedule...</p>}
    {data&&(leavePage?<>
      <section className="card-dark p-6"><h2 className="text-xl font-bold mb-5">New leave request</h2><form onSubmit={leave} noValidate className="grid md:grid-cols-2 gap-5">
        <Field label="Leave type"><Select validationKey="leaveType" className="input-dark w-full p-3" value={form.leaveType} onChange={e=>setForm({...form,leaveType:e.target.value})}>{['Annual','Sick','Casual','Emergency','Unpaid'].map(type=><option key={type}>{type}</option>)}</Select></Field>
        {['startDate','endDate'].map(key=><Field key={key} label={key==='startDate'?'Start date':'End date'}><Input validationKey={key} className="input-dark w-full p-3" type="date" value={form[key]} onChange={e=>setForm({...form,[key]:e.target.value})}/></Field>)}
        <Field label="Reason"><TextArea validationKey="reason" className="input-dark w-full p-3" value={form.reason} onChange={e=>setForm({...form,reason:e.target.value})}/></Field>
        <button disabled={busy} className="btn-accent px-6 py-3 rounded-full text-xs w-fit">{busy?'Submitting...':'Submit request'}</button>
      </form></section>
      <section><h2 className="font-bold text-xl mb-5">My leave requests</h2><DataTable rows={data.leave} columns={[{key:'leaveType',label:'Type'},{key:'startDate',label:'From'},{key:'endDate',label:'To'},{key:'reason',label:'Reason'},{key:'status',label:'State',render:row=><Badge value={row.status}/>}]} /></section>
    </>:<DataTable rows={data.shifts} columns={[{key:'shiftDate',label:'Date'},{key:'shiftTiming',label:'Shift'},{key:'station',label:'Station'},{key:'notes',label:'Notes'},{key:'status',label:'State',render:row=><Badge value={row.status}/>}]} />)}
  </div>;
}
