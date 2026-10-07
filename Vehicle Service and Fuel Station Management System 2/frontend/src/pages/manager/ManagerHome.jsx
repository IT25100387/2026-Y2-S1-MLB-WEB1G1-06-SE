import {useCallback,useEffect,useRef,useState} from 'react';
import {Link} from 'react-router-dom';
import {motion,useReducedMotion} from 'framer-motion';
import {useAuth} from '../../context/AuthContext';
import {api} from '../../lib/api';
import {useLiveRefresh} from '../../lib/useLiveRefresh';
import {money} from '../../components/DataUI';
import {AdminState,AdminTable} from '../admin/AdminUI';
import './ManagerHome.css';

const feeds=[
  {key:'summary',name:'Station totals',path:'/api/dashboard/overview'},
  {key:'bookings',name:'Bookings',path:'/api/workshop/bookings'},
  {key:'workshop',name:'Workshop jobs',path:'/api/workshop'},
  {key:'parts',name:'Spare-part stock',path:'/api/inventory/parts'},
  {key:'support',name:'Support tickets',path:'/api/v1/support/admin'},
];
const shortcuts=[
  ['Bookings','/dashboard/bookings','fa-calendar-check'],
  ['Job assignment','/dashboard/workshop/job-cards','fa-screwdriver-wrench'],
  ['Daily pump figures','/dashboard/fuel/sales','fa-gas-pump'],
  ['Invoices','/dashboard/invoices','fa-file-invoice-dollar'],
  ['Inventory','/dashboard/inventory/parts','fa-boxes-stacked'],
  ['Staff','/dashboard/hr/staff','fa-users'],
  ['Customer support','/dashboard/support/tickets','fa-headset'],
];
const number=value=>value!==null&&value!==undefined&&Number.isFinite(Number(value))?Number(value):null;
const count=(rows,predicate)=>Array.isArray(rows)?rows.filter(predicate).length:null;
const phase=value=>String(value||'').toLowerCase().replace(/[\s_-]/g,'');
const calendarDay=date=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).format(date);
const dateLabel=date=>date.toLocaleDateString('en-GB',{timeZone:'Asia/Colombo',weekday:'long',day:'numeric',month:'long',year:'numeric'});
function slotMinutes(slot){
  const match=/^(\d{1,2}):(\d{2})\s*(AM|PM)?$/i.exec(String(slot||'').trim());
  if(!match)return 1440;
  const hour=match[3]?Number(match[1])%12+(match[3].toUpperCase()==='PM'?12:0):Number(match[1]);
  return hour*60+Number(match[2]);
}
function Value({value,loading,currency=false}){
  if(loading)return <span className="manager-home-skeleton" aria-label="Loading value"/>;
  if(value===null)return <span className="manager-home-unavailable">Unavailable</span>;
  return <>{currency&&<small>LKR</small>}{currency?money(value):value}</>;
}

export default function ManagerHome(){
  const {account}=useAuth(),reduced=useReducedMotion(),request=useRef(0);
  const [data,setData]=useState({}),[errors,setErrors]=useState({}),[busy,setBusy]=useState(false),[now,setNow]=useState(()=>new Date()),[updated,setUpdated]=useState(null);
  const load=useCallback(async()=>{
    const current=++request.current;setBusy(true);
    const results=await Promise.allSettled(feeds.map(feed=>api(feed.path)));
    if(current!==request.current)return;
    const next={},failures={};
    results.forEach((result,index)=>{
      const feed=feeds[index];
      if(result.status==='fulfilled')next[feed.key]=result.value;
      else failures[feed.key]=result.reason?.message||'Unable to load this section.';
    });
    setData(previous=>({...previous,...next}));setErrors(failures);setBusy(false);setNow(new Date());
    if(Object.keys(next).length)setUpdated(new Date());
  },[]);
  useEffect(()=>{load();return()=>{request.current++;};},[load]);useLiveRefresh(load);

  const today=calendarDay(now),bookings=data.bookings?.bookings;
  const schedule=(bookings||[]).filter(booking=>booking.serviceDate===today).sort((a,b)=>slotMinutes(a.timeSlot)-slotMinutes(b.timeSlot)||String(a.referenceNumber||'').localeCompare(String(b.referenceNumber||'')));
  const stats=[
    {name:'Today’s bookings',value:Array.isArray(bookings)?schedule.length:null,detail:'Scheduled for today',key:'bookings',path:'/dashboard/bookings',icon:'fa-calendar-day'},
    {name:'Jobs in progress',value:count(data.workshop?.jobCards,job=>phase(job.status)==='inprogress'),detail:'Active work in the workshop',key:'workshop',path:'/dashboard/workshop/job-cards',icon:'fa-screwdriver-wrench'},
    {name:'Pending bookings',value:number(data.summary?.pendingBookings),detail:'Bookings ready for review',key:'summary',path:'/dashboard/bookings',icon:'fa-calendar-check'},
    {name:'Net collection',value:number(data.summary?.totalRevenue),detail:'All time · after recorded refunds',key:'summary',path:'/dashboard/payments',icon:'fa-wallet',currency:true},
  ];
  const attention=[
    {name:'Fuel stock',value:count(data.summary?.inventories,tank=>number(tank.currentStockLitres)!==null&&number(tank.minStockWarning)!==null&&Number(tank.currentStockLitres)<=Number(tank.minStockWarning)),detail:'Tanks at or below their warning level',key:'summary',path:'/dashboard/inventory/fuel',icon:'fa-gas-pump'},
    {name:'Spare-part stock',value:count(data.parts?.parts,part=>!['inactive','deactivated'].includes(phase(part.status))&&number(part.stockQuantity)!==null&&Number(part.stockQuantity)<=(number(part.minStockWarning)??0)),detail:'Parts needing replenishment',key:'parts',path:'/dashboard/inventory/parts',icon:'fa-box'},
    {name:'Pending leave',value:number(data.summary?.pendingLeaves),detail:'Requests awaiting a decision',key:'summary',path:'/dashboard/hr/leave',icon:'fa-calendar-minus'},
    {name:'Open support tickets',value:number(data.support?.openCount),detail:'Tickets awaiting a response',key:'support',path:'/dashboard/support/tickets',icon:'fa-headset'},
  ];
  const loading=key=>!data[key]&&!errors[key];
  const allClear=attention.every(item=>item.value===0)&&!Object.keys(errors).length;

  return <div className="manager-home">
    <section className="manager-home-hero" aria-labelledby="manager-home-title">
      <div className="manager-home-photo" aria-hidden="true"/>
      <div className="manager-home-welcome"><p className="manager-home-eyebrow">FuelCore / Manager workspace</p>
        <h1 id="manager-home-title">Welcome back,<br/><span>{account?.fullName||account?.username}.</span></h1>
        <p className="manager-home-intro">A clear view of the day ahead. Keep service work, stock and your team moving together.</p>
        <p className="manager-home-date"><i className="fa-regular fa-calendar" aria-hidden="true"/><time dateTime={today}>{dateLabel(now)}</time></p>
      </div>
      <div className="manager-home-hero-mark" aria-hidden="true"><span>THE ROAD</span><strong>AHEAD.</strong><i className="fa-solid fa-arrow-up-right-from-square"/></div>
    </section>

    <div className="manager-home-section-heading"><div><p className="manager-home-eyebrow">Station overview</p><h2>Keep today moving.</h2></div>
      <div className="manager-home-refresh"><span>{updated?'Updated '+updated.toLocaleTimeString('en-GB',{timeZone:'Asia/Colombo',hour:'2-digit',minute:'2-digit'}):busy?'Loading live data…':'Live data unavailable'}</span><button className="admin-action" aria-label="Refresh homepage" disabled={busy} onClick={load}><i className={'fa-solid fa-rotate '+(busy?'is-refreshing':'')} aria-hidden="true"/>Refresh</button></div>
    </div>
    {Object.keys(errors).length>0&&<div className="manager-home-error" role="alert"><div><strong>Some sections could not update.</strong><p>Affected sections show their previous values when available. {feeds.filter(feed=>errors[feed.key]).map(feed=>feed.name+': '+errors[feed.key]).join(' ')}</p></div><button className="admin-action" disabled={busy} onClick={load}>Retry</button></div>}

    <section className="manager-home-stats" aria-label="Station summary" aria-busy={busy}>{stats.map((stat,index)=><motion.div className="manager-home-stat" key={stat.name} initial={reduced?false:{opacity:0,y:8}} animate={{opacity:1,y:0}} transition={{duration:.2,delay:reduced?0:index*.04}}><Link to={stat.path}>
      <div className="manager-home-stat-top"><span>{stat.name}</span><i className={'fa-solid '+stat.icon} aria-hidden="true"/></div>
      <p className="manager-home-stat-value"><Value value={stat.value} currency={stat.currency} loading={loading(stat.key)}/></p>
      <p className="manager-home-stat-detail">{stat.detail}<i className="fa-solid fa-arrow-right" aria-hidden="true"/></p>
      {errors[stat.key]&&<span className="manager-home-stale">Last update failed</span>}
    </Link></motion.div>)}</section>

    <section className="manager-home-shortcuts" aria-label="Manager shortcuts">{shortcuts.map(([name,path,icon])=><Link key={path} to={path}><i className={'fa-solid '+icon} aria-hidden="true"/><span>{name}</span><i className="fa-solid fa-arrow-right" aria-hidden="true"/></Link>)}</section>

    <div className="manager-home-lower">
      <section className="manager-home-attention-panel" aria-labelledby="manager-home-attention-title"><p className="manager-home-eyebrow">Needs your attention</p><h2 id="manager-home-attention-title">Ready for your review.</h2>
        <p className="manager-home-panel-description">{allClear?'No stock, leave or support items need attention here.':'Stock, team requests and customer support.'}</p>
        <ul className="manager-home-attention">{attention.map(item=><li key={item.name}><Link to={item.path}><span className="manager-home-attention-icon"><i className={'fa-solid '+item.icon} aria-hidden="true"/></span><div><h3>{item.name}</h3><p>{item.detail}</p>{errors[item.key]&&<span className="manager-home-stale">Last update failed</span>}</div><strong className="manager-home-attention-value"><Value value={item.value} loading={loading(item.key)}/></strong><i className="fa-solid fa-arrow-right" aria-hidden="true"/></Link></li>)}</ul>
      </section>

      <section className="manager-home-bookings" aria-labelledby="manager-home-bookings-title"><div className="manager-home-panel-heading"><div><p className="manager-home-eyebrow">Today’s schedule</p><h2 id="manager-home-bookings-title">Today’s bookings.</h2></div><Link className="admin-action" to="/dashboard/bookings">View bookings <i className="fa-solid fa-arrow-right" aria-hidden="true"/></Link></div>
        <p className="manager-home-panel-description">{dateLabel(now)} · In scheduled order.{errors.bookings&&<span className="manager-home-stale"> Last update failed</span>}</p>
        {!data.bookings&&errors.bookings?<p className="manager-home-empty">Bookings unavailable. Retry to load today’s schedule.</p>:<AdminTable name="Today’s bookings" rows={schedule} loading={loading('bookings')} columns={[
          {label:'Reference',width:'27%',render:row=><strong className="manager-home-reference">{row.referenceNumber||'Unavailable'}</strong>},
          {label:'Vehicle',width:'18%',key:'licensePlate'},
          {label:'Service',width:'23%',key:'serviceType'},
          {label:'Time',width:'12%',key:'timeSlot'},
          {label:'Status',width:'16%',render:row=><AdminState value={row.status}/>},
        ]}/>}
      </section>
    </div>
  </div>;
}
