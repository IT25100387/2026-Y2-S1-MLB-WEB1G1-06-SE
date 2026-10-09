import {useEffect,useState,useRef} from 'react';
import {Link,Outlet,useLocation,useNavigate} from 'react-router-dom';
import {AnimatePresence,motion,useReducedMotion} from 'framer-motion';
import {useAuth} from '../context/AuthContext';
import AccountMenu from './AccountMenu';
import NotificationBell from './NotificationBell';
import {adminNavigation as navigation} from '../lib/adminNavigation';
import '../pages/admin/AdminPortal.css';

export default function AdminPortalLayout(){
  const location=useLocation(),navigate=useNavigate(),{logout}=useAuth(),reduced=useReducedMotion();
  const navigationTrigger=useRef(null),[menu,setMenu]=useState(null),[mobile,setMobile]=useState(false),[profile,setProfile]=useState(false);
  const close=()=>{setMenu(null);setMobile(false);setProfile(false);};
  useEffect(()=>{const previous=document.body.dataset.adminTheme;document.body.dataset.adminTheme='operations';return()=>{if(previous===undefined)delete document.body.dataset.adminTheme;else document.body.dataset.adminTheme=previous;};},[]);
  useEffect(()=>{setMenu(null);setMobile(false);setProfile(false);window.scrollTo({top:0,behavior:'instant'});},[location.pathname]);
  useEffect(()=>{const escape=event=>{if(event.key==='Escape'){if(navigationTrigger.current?.getAttribute('aria-expanded')==='true')navigationTrigger.current.focus();setMenu(null);setMobile(false);setProfile(false);}};window.addEventListener('keydown',escape);return()=>window.removeEventListener('keydown',escape);},[]);
  const active=path=>location.pathname===path||(path==='/dashboard'?location.pathname==='/dashboard/':location.pathname.startsWith(path+'/'));
  const render=item=>{
    if(item.path)return <Link key={item.name} to={item.path} onClick={close} className={'admin-nav-link '+(active(item.path)?'is-active':'')} aria-current={active(item.path)?'page':undefined}>{item.name}</Link>;
    const selected=item.groups.some(group=>group.links.some(([,path])=>active(path))),id='admin-'+item.name.replaceAll(' ','-')+'-menu';
    return <div className="admin-nav-group" key={item.name}>
      <button className={'admin-nav-link '+(selected||menu===item.name?'is-active':'')} aria-expanded={menu===item.name} aria-controls={id} onClick={event=>{navigationTrigger.current=event.currentTarget;setProfile(false);setMenu(value=>value===item.name?null:item.name);}}>{item.name}<i aria-hidden="true" className={'fa-solid fa-chevron-down '+(menu===item.name?'is-open':'')}/></button>
      <AnimatePresence>{menu===item.name&&<motion.div id={id} initial={reduced?false:{opacity:0,y:6}} animate={{opacity:1,y:0}} exit={{opacity:0,y:4}} transition={{duration:.18}} className="admin-nav-dropdown">
        <div className="admin-menu-lists" style={{'--admin-menu-columns':item.groups.length}}>{item.groups.map(group=><section className="admin-menu-list" key={group.name} aria-label={group.name}><h2>{group.name}</h2>{group.links.map(([name,path])=><Link to={path} key={path} onClick={close} className={active(path)?'is-active':''} aria-current={active(path)?'page':undefined}>{name}<i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>)}</section>)}</div>
      </motion.div>}</AnimatePresence>
    </div>;
  };
  return <div className="admin-portal"><header className="admin-header"><div className="admin-header-inner">
    <Link className="admin-logo" to="/dashboard" onClick={close}>FUEL<span>CORE</span></Link>
    <nav className="admin-desktop-nav" aria-label="Main navigation">{navigation.map(render)}</nav>
    <div className="admin-account-controls"><NotificationBell/><AccountMenu open={profile} setOpen={value=>{setMenu(null);setMobile(false);setProfile(value);}} onLogout={async()=>{await logout();navigate('/login');}}/>
      <button className="admin-mobile-toggle" aria-label="Toggle navigation" aria-expanded={mobile} onClick={event=>{navigationTrigger.current=event.currentTarget;setProfile(false);setMenu(null);setMobile(value=>!value);}}><i aria-hidden="true" className={'fa-solid '+(mobile?'fa-xmark':'fa-bars')}/></button>
    </div>
  </div>{mobile&&<nav className="admin-mobile-nav" aria-label="Mobile navigation">{navigation.map(item=><div key={item.name}>{item.path?<Link to={item.path} onClick={close} aria-current={active(item.path)?'page':undefined}>{item.name}</Link>:<><p>{item.name}</p>{item.groups.map(group=><section className="admin-menu-list" key={group.name} aria-label={group.name}><h2>{group.name}</h2>{group.links.map(([name,path])=><Link key={path} to={path} onClick={close} aria-current={active(path)?'page':undefined}>{name}</Link>)}</section>)}</>}</div>)}</nav>}</header>
    {(menu||mobile||profile)&&<motion.button initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.18}} className="admin-nav-backdrop" aria-label="Close navigation overlay" onClick={close}/>}<main className="admin-content"><motion.div key={location.pathname} initial={reduced?false:{opacity:0}} animate={{opacity:1}} transition={{duration:.2}}><Outlet/></motion.div></main>
    <footer className="admin-footer"><Link className="admin-logo" to="/dashboard">FUEL<span>CORE</span></Link><p>Administration workspace</p><button onClick={()=>window.scrollTo({top:0,behavior:reduced?'instant':'smooth'})}>Back to top <i aria-hidden="true" className="fa-solid fa-arrow-up"/></button></footer>
  </div>;
}
