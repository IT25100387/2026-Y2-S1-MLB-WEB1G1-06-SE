import {useEffect,useRef,useState} from 'react';
import {Link,Outlet,useLocation,useNavigate} from 'react-router-dom';
import {AnimatePresence,motion,useReducedMotion} from 'framer-motion';
import {useAuth} from '../context/AuthContext';
import NotificationBell from './NotificationBell';
import CustomerAccountMenu from './CustomerAccountMenu';
import '../pages/customer/CustomerPortal.css';

const navigation=[
  {name:'My garage',path:'/customer'},
  {name:'Service',icon:'fa-wrench',links:[['Service catalog','/customer/catalog'],['Book service','/customer/book'],['Appointments & tracking','/customer/appointments'],['Service history','/customer/service-history']]},
  {name:'Billing',icon:'fa-file-invoice-dollar',links:[['Invoices','/customer/invoices'],['Payment history','/customer/payments']]},
  {name:'Store',icon:'fa-bag-shopping',links:[['Spare parts store','/customer/store'],['Fuel prices','/customer/fuel']]},
  {name:'Support',icon:'fa-headset',links:[['Helpdesk','/customer/support'],['Feedback','/customer/feedback']]},
];

export default function CustomerPortalLayout() {
  const {logout}=useAuth(),location=useLocation(),navigate=useNavigate(),reduced=useReducedMotion();
  const [mobile,setMobile]=useState(false),[dropdown,setDropdown]=useState(null),[profile,setProfile]=useState(false);
  const triggers=useRef({}),mobileTrigger=useRef(null);
  const garage=['/customer','/customer/','/customer/vehicles'].includes(location.pathname);
  const close=()=>{setDropdown(null);setMobile(false);};
  useEffect(()=>{setDropdown(null);setMobile(false);setProfile(false);window.scrollTo({top:0,behavior:'instant'});},[location.pathname]);
  useEffect(()=>{const escape=e=>{if(e.key==='Escape'){if(mobile)mobileTrigger.current?.focus();else if(dropdown)triggers.current[dropdown]?.focus();setDropdown(null);setMobile(false);setProfile(false);}};window.addEventListener('keydown',escape);return()=>window.removeEventListener('keydown',escape);},[dropdown,mobile]);
  const signOut=async()=>{await logout();navigate('/login');};
  const toggle=name=>{setProfile(false);setDropdown(current=>current===name?null:name);};
  const menu=(item,small=false)=>{
    const active=item.path?garage:item.links.some(([,path])=>location.pathname===path||location.pathname.startsWith(path+'/'));
    if(item.path)return <Link key={item.name} to={item.path} className={`customer-nav-link ${active?'is-active':''}`} aria-current={active?'page':undefined}>{item.name}</Link>;
    return <div key={item.name} className="customer-nav-group">
      <button ref={el=>{if(!small)triggers.current[item.name]=el;}} className={`customer-nav-link ${active||dropdown===item.name?'is-active':''}`} aria-expanded={dropdown===item.name} aria-controls={`customer-${small?'mobile-':''}${item.name.toLowerCase()}-menu`} onClick={()=>toggle(item.name)}>{item.name}<i aria-hidden="true" className={`fa-solid fa-chevron-down ${dropdown===item.name?'is-open':''}`}/></button>
      <AnimatePresence>{dropdown===item.name&&<motion.div id={`customer-${small?'mobile-':''}${item.name.toLowerCase()}-menu`} className={small?'customer-mobile-submenu':'customer-nav-dropdown'} initial={reduced?false:{opacity:0,y:-8}} animate={{opacity:1,y:0}} exit={{opacity:0,y:-4}} transition={{duration:.18}}><p><i aria-hidden="true" className={`fa-solid ${item.icon}`}/>{item.name}</p>{item.links.map(([name,path])=><Link key={path} to={path} onClick={close} className={location.pathname===path?'is-active':''} aria-current={location.pathname===path?'page':undefined}>{name}<i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>)}</motion.div>}</AnimatePresence>
    </div>;
  };
  return <div className={`customer-portal ${garage?'customer-garage-portal':''} ${dropdown||mobile?'customer-nav-open':''}`}>
    <AnimatePresence>{(dropdown||mobile)&&<motion.button type="button" className="customer-nav-backdrop" aria-label="Close navigation menu" onClick={close} initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} transition={{duration:.18}}/>}</AnimatePresence>
    <header className="customer-header"><div className="customer-header-inner">
      <Link to="/customer" className="customer-logo">FUEL<span>CORE</span></Link>
      <nav aria-label="Main navigation" className="customer-desktop-nav">{navigation.map(item=>menu(item))}</nav>
      <div className="customer-header-actions"><NotificationBell/><CustomerAccountMenu open={profile} setOpen={value=>{if(value){setDropdown(null);setMobile(false);}setProfile(value);}} onLogout={signOut}/><button ref={mobileTrigger} className="customer-mobile-toggle" aria-label="Toggle navigation" aria-expanded={mobile} onClick={()=>{setMobile(current=>!current);setDropdown(null);setProfile(false);}}><i aria-hidden="true" className={`fa-solid ${mobile?'fa-xmark':'fa-bars'}`}/></button></div>
    </div><AnimatePresence>{mobile&&<motion.nav aria-label="Mobile navigation" className="customer-mobile-nav" initial={reduced?false:{opacity:0,height:0}} animate={{opacity:1,height:'auto'}} exit={{opacity:0,height:0}} transition={{duration:.18}}>{navigation.map(item=>menu(item,true))}</motion.nav>}</AnimatePresence></header>
    <main className="customer-main"><motion.div key={location.pathname} initial={reduced?false:{opacity:0,y:6}} animate={{opacity:1,y:0}} transition={{duration:.2}}><Outlet/></motion.div></main>
    <footer className="customer-footer"><div className="customer-footer-grid">
      <div><Link to="/customer" className="customer-logo">FUEL<span>CORE</span></Link><p>Vehicle service. Fuel. Spare parts.<br/>Everything for the road ahead.</p><a href="https://wa.me/94771234567" target="_blank" rel="noopener noreferrer">WhatsApp us <i aria-hidden="true" className="fa-brands fa-whatsapp"/></a></div>
      <div><h3>Explore</h3><Link to="/customer/catalog">Our services</Link><Link to="/#about">About FuelCore</Link><Link to="/#how-it-works">How it works</Link><Link to="/customer/book">Book an appointment</Link></div>
      <div><h3>Visit us</h3><p>123 Main Street<br/>Malabe, Sri Lanka</p><p>Mon - Sat<br/>9:00 AM - 6:00 PM</p></div>
      <div><h3>Let's talk</h3><a href="tel:+94771234567">(+94) 77 123 4567</a><a href="mailto:info@fuelcore.com">info@fuelcore.com</a><Link to="/customer/support">Customer support <i aria-hidden="true" className="fa-solid fa-arrow-up-right-from-square"/></Link></div>
    </div><div className="customer-footer-bottom"><span>FUELCORE / SERVICE CENTRE & FUEL STATION</span><button onClick={()=>window.scrollTo({top:0,behavior:reduced?'instant':'smooth'})}>Back to top <i aria-hidden="true" className="fa-solid fa-arrow-up"/></button></div></footer>
  </div>;
}
