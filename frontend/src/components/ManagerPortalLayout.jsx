import {useEffect,useRef,useState} from 'react';
import {Link,Outlet,useLocation,useNavigate} from 'react-router-dom';
import {AnimatePresence,motion,useReducedMotion} from 'framer-motion';
import {useAuth} from '../context/AuthContext';
import NotificationBell from './NotificationBell';
import ProfileDropdown from './ProfileDropdown';
import '../pages/manager/ManagerPortal.css';

export const managerNavigation=[
  {name:'Home',path:'/dashboard'},
  {name:'Service',groups:[{name:'Vehicles & history',links:[['Vehicles','/dashboard/vehicles'],['Service history','/dashboard/service-history']]},{name:'Bookings',links:[['Bookings','/dashboard/bookings']]}]},
  {name:'Payment',groups:[{name:'Billing',links:[['Invoices','/dashboard/invoices'],['Payments','/dashboard/payments']]}]},
  {name:'Workshop',groups:[{name:'Schedules',links:[['Bay schedule','/dashboard/workshop/bays'],['Mechanic schedule','/dashboard/workshop/mechanics']]},{name:'Job allocation',links:[['Assign job cards','/dashboard/workshop/job-cards']]}]},
  {name:'Fuel',groups:[{name:'Prices & tanks',links:[['Fuel prices','/dashboard/fuel/prices'],['Tank states','/dashboard/fuel/inventory']]},{name:'Daily operations',links:[['Daily pump figures','/dashboard/fuel/sales'],['Pump settings','/dashboard/fuel/pumps'],['Combined daily collection','/dashboard/fuel/collection'],['Individual pump history','/dashboard/fuel/history']]}]},
  {name:'Inventory',groups:[{name:'Stock',links:[['Fuel tanks','/dashboard/inventory/fuel'],['Spare parts','/dashboard/inventory/parts']]},{name:'Suppliers',links:[['Suppliers','/dashboard/inventory/suppliers']]}]},
  {name:'Catalog & offers',groups:[{name:'Offers',links:[['Spare part offers','/dashboard/offers/spare-parts'],['Service offers','/dashboard/offers/services']]},{name:'Service catalog',links:[['Service catalog','/dashboard/offers/service-catalog']]}]},
  {name:'Team & support',groups:[{name:'Team',links:[['Staff directory','/dashboard/hr/staff'],['Shift schedules','/dashboard/hr/shifts'],['Leave requests','/dashboard/hr/leave']]},{name:'Customers & support',links:[['Support tickets','/dashboard/support/tickets'],['Customer feedback','/dashboard/support/feedback'],['Customer directory','/dashboard/customers']]}]},
];

export default function ManagerPortalLayout(){
  const {logout}=useAuth(),location=useLocation(),navigate=useNavigate(),reduced=useReducedMotion();
  const [mobile,setMobile]=useState(false),[dropdown,setDropdown]=useState(null),[profile,setProfile]=useState(false);
  const triggers=useRef({}),mobileTrigger=useRef(null),accountArea=useRef(null);
  const home=['/dashboard','/dashboard/'].includes(location.pathname),open=Boolean(dropdown||mobile);
  const close=()=>{setDropdown(null);setMobile(false);};
  useEffect(()=>{setDropdown(null);setMobile(false);setProfile(false);},[location.pathname,location.hash]);
  useEffect(()=>{
    if(!location.hash){window.scrollTo({top:0,behavior:'instant'});return;}
    const id=location.hash.slice(1);let timer;
    const scroll=()=>{const target=document.getElementById(id);if(!target)return;observer.disconnect();clearTimeout(timer);timer=setTimeout(()=>target.scrollIntoView({block:'start',behavior:reduced?'instant':'smooth'}),220);};
    const observer=new MutationObserver(scroll);observer.observe(document.body,{childList:true,subtree:true});scroll();
    return()=>{observer.disconnect();clearTimeout(timer);};
  },[location.pathname,location.hash,location.key,reduced]);
  useEffect(()=>{const escape=e=>{if(e.key!=='Escape')return;if(profile)accountArea.current?.querySelector('[aria-label="Account center"]')?.focus();else if(mobile)mobileTrigger.current?.focus();else if(dropdown)triggers.current[dropdown]?.focus();setDropdown(null);setMobile(false);setProfile(false);};window.addEventListener('keydown',escape);return()=>window.removeEventListener('keydown',escape);},[dropdown,mobile,profile]);
  const signOut=async()=>{await logout();navigate('/login');};
  const active=path=>location.pathname===path.split('#')[0]&&(!path.includes('#')||location.hash===('#'+path.split('#')[1])||!location.hash&&path.endsWith('#pump-figures'));
  const toggle=name=>{setProfile(false);setDropdown(value=>value===name?null:name);};
  const render=(item,small=false)=>{
    if(item.path)return <Link key={item.name} to={item.path} onClick={close} className={`manager-nav-link ${home?'is-active':''}`} aria-current={home?'page':undefined}>{item.name}</Link>;
    const selected=item.groups.some(group=>group.links.some(([,path])=>active(path)));
    return <div className="manager-nav-group" key={item.name}><button ref={el=>{if(!small)triggers.current[item.name]=el;}} onClick={()=>toggle(item.name)} className={`manager-nav-link ${selected||dropdown===item.name?'is-active':''}`} aria-expanded={dropdown===item.name} aria-controls={`manager-${small?'mobile-':''}${item.name.replaceAll(' ','-')}-menu`}>{item.name}<i aria-hidden="true" className={`fa-solid fa-chevron-down ${dropdown===item.name?'is-open':''}`}/></button>
      <AnimatePresence>{dropdown===item.name&&<motion.div id={`manager-${small?'mobile-':''}${item.name.replaceAll(' ','-')}-menu`} className={small?'manager-mobile-submenu':'manager-dropdown-position'} initial={reduced?false:{opacity:0,y:-8}} animate={{opacity:1,y:0}} exit={{opacity:0,y:-4}} transition={{duration:.18}}>
        <div className={`manager-menu-lists manager-list-count-${item.groups.length}`}>{item.groups.map(group=><section className="manager-menu-list" key={group.name} aria-label={group.name}><h2>{group.name}</h2>{group.links.map(([name,path])=><Link key={path} to={path} onClick={close} className={active(path)?'is-active':''} aria-current={active(path)?'page':undefined}>{name}<i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>)}</section>)}</div>
      </motion.div>}</AnimatePresence></div>;
  };
  return <div className={`manager-portal ${home?'manager-home-route':''} ${open?'manager-nav-open':''}`}>
    <AnimatePresence>{open&&<motion.button className="manager-nav-backdrop" aria-label="Close navigation menu" onClick={close} initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} transition={{duration:.18}}/>}</AnimatePresence>
    <header className="manager-header"><div className="manager-header-inner"><Link to="/dashboard" className="manager-logo">FUEL<span>CORE</span></Link><nav aria-label="Main navigation" className="manager-desktop-nav">{managerNavigation.map(item=>render(item))}</nav>
      <div className="manager-header-actions"><NotificationBell/><div ref={accountArea} className="manager-account-area"><ProfileDropdown handleLogout={signOut} isOpen={profile} setIsOpen={value=>{if(value){setDropdown(null);setMobile(false);}setProfile(value);}}/></div><button ref={mobileTrigger} className="manager-mobile-toggle" aria-label="Toggle navigation" aria-expanded={mobile} onClick={()=>{setMobile(value=>!value);setDropdown(null);setProfile(false);}}><i aria-hidden="true" className={`fa-solid ${mobile?'fa-xmark':'fa-bars'}`}/></button></div>
    </div><AnimatePresence>{mobile&&<motion.nav aria-label="Mobile navigation" className="manager-mobile-nav" initial={reduced?false:{height:0,opacity:0}} animate={{height:'auto',opacity:1}} exit={{height:0,opacity:0}} transition={{duration:.18}}>{managerNavigation.map(item=>render(item,true))}</motion.nav>}</AnimatePresence></header>
    <main className={`manager-main ${home?'manager-home-content':'manager-content'}`}><motion.div key={['/dashboard/fuel/sales','/dashboard/fuel/pumps','/dashboard/fuel/collection','/dashboard/fuel/history'].includes(location.pathname)?'fuel-operations':location.pathname} initial={reduced?false:{opacity:0}} animate={{opacity:1}} transition={{duration:.2}}><Outlet/></motion.div></main>
    <footer className="manager-footer"><span>FUELCORE / MANAGER WORKSPACE</span><button onClick={()=>window.scrollTo({top:0,behavior:reduced?'instant':'smooth'})}>Back to top <i aria-hidden="true" className="fa-solid fa-arrow-up"/></button></footer>
  </div>;
}
