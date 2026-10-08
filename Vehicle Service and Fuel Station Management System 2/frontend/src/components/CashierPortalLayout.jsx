import {useEffect,useRef,useState} from 'react';
import {Link,Outlet,useLocation,useNavigate} from 'react-router-dom';
import {AnimatePresence,motion,useReducedMotion} from 'framer-motion';
import {useAuth} from '../context/AuthContext';
import AccountMenu from './AccountMenu';
import NotificationBell from './NotificationBell';
import {CashierPOSProvider} from '../pages/cashier/CashierPOSContext';
import '../pages/manager/ManagerPortal.css';
import '../pages/cashier/CashierPortal.css';

const links=[{name:'Point of sale',path:'/dashboard/pos'},
  {name:'Vehicles',path:'/dashboard/vehicles'},
  {name:'Catalog',items:[['Service catalog','/dashboard/cashier/catalog'],['Fuel prices','/dashboard/fuel/prices']]},
  {name:'Invoices',path:'/dashboard/invoices'}];
export default function CashierPortalLayout(){
  const location=useLocation(),navigate=useNavigate(),{logout}=useAuth(),reduced=useReducedMotion();
  const pos=location.pathname==='/dashboard/pos';
  const [menu,setMenu]=useState(null),[mobile,setMobile]=useState(false),[profile,setProfile]=useState(false),triggers=useRef({});
  useEffect(()=>{setMenu(null);setMobile(false);setProfile(false);window.scrollTo({top:0,behavior:'instant'});},[location.pathname]);
  useEffect(()=>{const close=event=>{if(event.key==='Escape'){triggers.current[mobile?'mobile':menu]?.focus();setMenu(null);setMobile(false);setProfile(false);}};window.addEventListener('keydown',close);return()=>window.removeEventListener('keydown',close);},[menu,mobile]);
  const close=()=>{setMenu(null);setMobile(false);};
  const navigation=(item,small=false)=>item.path?<Link key={item.name} className={'manager-nav-link '+(location.pathname===item.path?'is-active':'')} to={item.path} onClick={close}>{item.name}</Link>:<div className="manager-nav-group" key={item.name}><button ref={el=>{if(!small)triggers.current[item.name]=el;}} className={'manager-nav-link '+(item.items.some(([,path])=>path===location.pathname)?'is-active':'')} aria-expanded={menu===item.name} onClick={()=>{setProfile(false);setMenu(menu===item.name?null:item.name);}}>{item.name}<i aria-hidden="true" className={'fa-solid fa-chevron-down '+(menu===item.name?'is-open':'')}/></button><AnimatePresence>{menu===item.name&&<motion.div initial={reduced?false:{opacity:0,y:-8}} animate={{opacity:1,y:0}} exit={{opacity:0,y:-4}} transition={{duration:.18}} className={small?'manager-mobile-submenu':'manager-dropdown-position'}><div className="manager-menu-lists manager-list-count-1"><section className="manager-menu-list"><h2>{item.name}</h2>{item.items.map(([name,path])=><Link key={path} to={path} onClick={close}>{name}<i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>)}</section></div></motion.div>}</AnimatePresence></div>;
  return <CashierPOSProvider><div className={'manager-portal cashier-portal'+(pos?' cashier-pos-layout':'')+(menu||mobile?' cashier-menu-open':'')}>
    {(menu||mobile)&&<motion.button className="manager-nav-backdrop" aria-label="Close navigation menu" onClick={close} initial={{opacity:0}} animate={{opacity:1}}/>}
    <header className="manager-header"><div className="manager-header-inner"><Link className="manager-logo" to="/dashboard/pos">FUEL<span>CORE</span></Link><nav className="manager-desktop-nav" aria-label="Main navigation">{links.map(item=>navigation(item))}</nav><div className="manager-header-actions"><NotificationBell/><AccountMenu open={profile} setOpen={value=>{close();setProfile(value);}} onLogout={async()=>{await logout();navigate('/login');}}/><button ref={el=>{triggers.current.mobile=el;}} className="manager-mobile-toggle" aria-label="Toggle navigation" aria-expanded={mobile} onClick={()=>{setMobile(!mobile);setMenu(null);setProfile(false);}}><i aria-hidden="true" className={'fa-solid '+(mobile?'fa-xmark':'fa-bars')}/></button></div></div>{mobile&&<nav className="manager-mobile-nav" aria-label="Mobile navigation">{links.map(item=>navigation(item,true))}</nav>}</header>
    <main className="manager-main manager-content cashier-main"><motion.div key={location.pathname} initial={reduced?false:{opacity:0}} animate={{opacity:1}} transition={{duration:.18}}><Outlet/></motion.div></main>{!pos&&<footer className="manager-footer"><span>FUELCORE / CASHIER WORKSPACE</span><button onClick={()=>window.scrollTo({top:0,behavior:reduced?'instant':'smooth'})}>Back to top <i aria-hidden="true" className="fa-solid fa-arrow-up"/></button></footer>}
  </div></CashierPOSProvider>;
}
