import {useCallback,useEffect,useRef,useState} from 'react';
import {Link} from 'react-router-dom';
import {motion,useReducedMotion} from 'framer-motion';
import {useAuth} from '../../context/AuthContext';
import {api} from '../../lib/api';
import {useLiveRefresh} from '../../lib/useLiveRefresh';
import {adminNavigation} from '../../lib/adminNavigation';
import {money} from '../../components/DataUI';
import {AdminState,dateText} from './AdminUI';
import './AdminHome.css';

const shortcuts=adminNavigation.filter(item=>item.name!=='Home').map(item=>({name:item.name,path:item.path||item.groups[0].links[0][1]}));
const icons={'Administration':'fa-users-gear','Payment':'fa-file-invoice-dollar','Inventory':'fa-boxes-stacked','Catalog & offers':'fa-tags','Team & support':'fa-headset','Audit logs':'fa-shield-halved'};
const stationDate=value=>value?new Date(value+'T12:00:00+05:30').toLocaleDateString('en-GB',{timeZone:'Asia/Colombo',weekday:'long',day:'numeric',month:'long',year:'numeric'}):'Loading station date…';

export default function AdminHome(){
  const {account}=useAuth(),reduced=useReducedMotion();
  const [data,setData]=useState(null),[error,setError]=useState(''),[busy,setBusy]=useState(false),request=useRef(0);
  const load=useCallback(async()=>{
    const current=++request.current;setBusy(true);
    try{const feed=await api('/api/dashboard/admin-overview');if(current===request.current){setData(feed);setError('');}}
    catch(failure){if(current===request.current)setError(failure.message);}
    finally{if(current===request.current)setBusy(false);}
  },[]);
  useEffect(()=>{load();return()=>{request.current++;};},[load]);useLiveRefresh(load);
  const stats=[
    {name:'Active accounts',value:data?.activeAccounts,detail:'Accounts ready to use FuelCore',path:'/dashboard/users',icon:'fa-users'},
    {name:'Net collection',value:data?.netCollection,detail:'All time · after recorded refunds',path:'/dashboard/payments',icon:'fa-wallet',currency:true},
    {name:'Pending bookings',value:data?.pendingBookings,detail:'Bookings awaiting allocation',icon:'fa-calendar-check'},
    {name:'Pending leave',value:data?.pendingLeave,detail:'Requests awaiting a decision',path:'/dashboard/hr/leave',icon:'fa-calendar-minus'},
  ];
  const attention=[
    {name:'Fuel stock',value:data?.lowFuelCount,path:'/dashboard/inventory/fuel',detail:'tanks at or below their warning level',icon:'fa-gas-pump'},
    {name:'Spare-part stock',value:data?.lowPartsCount,path:'/dashboard/inventory/parts',detail:'available parts needing replenishment',icon:'fa-box'},
    {name:'Support tickets',value:data?.openTickets,path:'/dashboard/support/tickets',detail:'open tickets awaiting a response',icon:'fa-headset'},
  ];
  const ready=Boolean(data),attentionTotal=attention.reduce((total,item)=>total+(item.value||0),0);
  return <div className="admin-home">
    <section className="admin-home-hero" aria-labelledby="admin-home-title">
      <div className="admin-home-photo" aria-hidden="true"/>
      <div className="admin-home-hero-copy"><p className="admin-home-eyebrow">FuelCore / Administration</p>
        <h1 id="admin-home-title">Welcome back,<br/><span>{account?.fullName||account?.username}.</span></h1>
        <p className="admin-home-intro">Your station, at a glance. Keep the team, stock and customer experience moving forward.</p>
        <div className="admin-home-hero-bottom"><p><i aria-hidden="true" className="fa-regular fa-calendar"/>{stationDate(data?.stationDate)}</p><Link className="admin-action" to="/dashboard/audit-logs">Open audit logs <i aria-hidden="true" className="fa-solid fa-arrow-up-right-from-square"/></Link></div>
      </div>
      <div className="admin-home-hero-mark" aria-hidden="true"><span>FUEL</span><span>CORE</span><i className="fa-solid fa-arrow-up-right-from-square"/></div>
    </section>
    <div className="admin-home-section-heading"><div><p className="admin-home-eyebrow">The overview</p><h2>A clear view of operations.</h2></div><div className="admin-home-refresh"><span>{data?<>Updated {dateText(data.refreshedAt,true)}</>:'Loading live data…'}</span><button className="admin-action" disabled={busy} onClick={load} aria-label="Refresh homepage"><i aria-hidden="true" className={'fa-solid fa-rotate '+(busy?'is-refreshing':'')}/>Refresh</button></div></div>
    {error&&<p className="admin-home-error" role="alert">{data?'The last update failed. Values below are from the previous refresh. ':'Unable to load the overview. '}{error} <button className="admin-action" onClick={load} disabled={busy}>Retry</button></p>}
    <section className="admin-home-stats" aria-label="Station summary" aria-busy={busy}>{stats.map((stat,index)=>{
      const content=<><div className="admin-home-stat-top"><span>{stat.name}</span><i aria-hidden="true" className={'fa-solid '+stat.icon}/></div><p className="admin-home-stat-value">{ready?<>{stat.currency&&<small>LKR</small>}{stat.currency?money(stat.value):stat.value}</>:<span className="admin-home-skeleton" aria-label="Loading value"/>}</p><p className="admin-home-stat-detail">{stat.detail}{stat.path&&<i aria-hidden="true" className="fa-solid fa-arrow-right"/>}</p></>;
      return <motion.div className="admin-home-stat" key={stat.name} initial={reduced?false:{opacity:0,y:8}} animate={{opacity:1,y:0}} transition={{duration:.2,delay:reduced?0:index*.04}}>{stat.path?<Link to={stat.path}>{content}</Link>:<div>{content}</div>}</motion.div>;
    })}</section>
    <section className="admin-home-shortcuts" aria-label="Admin shortcuts">{shortcuts.map(item=><Link key={item.name} to={item.path}><i aria-hidden="true" className={'fa-solid '+icons[item.name]}/><span>{item.name}</span><i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>)}</section>
    <div className="admin-home-lower">
      <section className="admin-home-panel" aria-labelledby="admin-attention-title"><div className="admin-home-panel-heading"><div><p className="admin-home-eyebrow">Keep things moving</p><h2 id="admin-attention-title">Needs your attention.</h2></div>{ready&&<span className="admin-home-count">{attentionTotal}</span>}</div>
        <p className="admin-home-panel-description">{ready?(attentionTotal?'A few items are ready for your review.':'No items currently need attention here.'):'Checking stock and support…'}</p>
        <ul className="admin-home-attention">{attention.map(item=><li key={item.name}><Link to={item.path}><span className="admin-home-attention-icon"><i aria-hidden="true" className={'fa-solid '+item.icon}/></span><div><h3>{item.name}</h3><p>{ready?<><strong>{item.value}</strong> {item.detail}</>:'Loading…'}</p></div><i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link></li>)}</ul>
      </section>
      <section className="admin-home-panel" aria-labelledby="admin-activity-title"><div className="admin-home-panel-heading"><div><p className="admin-home-eyebrow">Recent activity</p><h2 id="admin-activity-title">The latest audit events.</h2></div><Link className="admin-action" to="/dashboard/audit-logs">View audit logs <i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link></div>
        <ol className="admin-home-activity">{(data?.recentAudit||[]).map(event=><li key={event.id}><span className="admin-home-activity-dot" aria-hidden="true"/><div><div className="admin-home-activity-head"><h3>{(event.action||'Activity').replaceAll('_',' ')}</h3><AdminState value={event.status}/></div><p>{event.details||'No details recorded'}</p><span className="admin-home-activity-meta">@{event.username||'system'} <span aria-hidden="true">·</span> {dateText(event.timestamp,true)}</span></div></li>)}</ol>
        {!data?.recentAudit?.length&&<p className="admin-home-empty">{ready?'No audit events recorded yet.':'Loading recent activity…'}</p>}
      </section>
    </div>
  </div>;
}
