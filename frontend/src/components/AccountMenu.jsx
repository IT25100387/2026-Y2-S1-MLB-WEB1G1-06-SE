import {useEffect,useId,useRef,useState} from 'react';
import {Link} from 'react-router-dom';
import {AnimatePresence,motion,useReducedMotion} from 'framer-motion';
import {useAuth} from '../context/AuthContext';
import './AccountMenu.css';

const staffRoles=['ROLE_MANAGER','ROLE_CASHIER','ROLE_MECHANIC'];

export default function AccountMenu({open,setOpen,onLogout}) {
  const {account}=useAuth();
  const box=useRef(null),trigger=useRef(null),id=useId(),reduced=useReducedMotion();
  const [error,setError]=useState(''),[busy,setBusy]=useState(false);
  const name=account.fullName||account.username;
  const initials=name.trim().split(/\s+/).map(word=>word[0]).join('').slice(0,2).toUpperCase();
  const base=account.role==='ROLE_CUSTOMER'?'/customer':'/dashboard';
  const role=account.role.replace('ROLE_','').toLowerCase();
  const options=[['View profile',`${base}/profile`,'fa-user'],['Edit personal details',`${base}/profile/edit`,'fa-user-pen'],['Change password',`${base}/profile/security`,'fa-key'],['Notification settings',`${base}/settings`,'fa-sliders']];
  useEffect(()=>{
    const outside=event=>{if(open&&box.current&&!box.current.contains(event.target))setOpen(false);};
    const escape=event=>{if(open&&event.key==='Escape'){setOpen(false);trigger.current?.focus();}};
    document.addEventListener('mousedown',outside);
    window.addEventListener('keydown',escape);
    return()=>{document.removeEventListener('mousedown',outside);window.removeEventListener('keydown',escape);};
  },[open,setOpen]);
  const row=([label,path,icon])=><Link key={path} to={path} onClick={()=>setOpen(false)}><span className="fuelcore-account-icon"><i aria-hidden="true" className={`fa-solid ${icon}`}/></span><span>{label}</span><i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>;
  return <div className="fuelcore-account-menu" ref={box}>
    <button ref={trigger} type="button" className="fuelcore-account-trigger" aria-label="Account center" aria-expanded={open} aria-controls={id} title={account.username} onClick={()=>setOpen(!open)}>
      <span className="fuelcore-account-avatar" aria-hidden="true">{initials}</span><strong>{account.username}</strong><i aria-hidden="true" className={`fa-solid fa-chevron-down ${open?'is-open':''}`}/>
    </button>
    <AnimatePresence>{open&&<motion.div id={id} className="fuelcore-account-dropdown" initial={reduced?false:{opacity:0,y:8}} animate={{opacity:1,y:0}} exit={{opacity:0,y:4}} transition={{duration:.18}}>
      <div className="fuelcore-account-summary"><span className="fuelcore-account-avatar" aria-hidden="true">{initials}</span><div><strong>{name}</strong><span>{account.email}</span><small>{role} account</small></div></div>
      <nav aria-label="Account options">{options.map(row)}</nav>
      {staffRoles.includes(account.role)&&<nav className="fuelcore-account-work" aria-label="My work schedule"><p>Work schedule</p>{[['My shifts','/dashboard/profile/shifts','fa-calendar-days'],['Request leave','/dashboard/profile/leave','fa-calendar-plus']].map(row)}</nav>}
      {error&&<p className="fuelcore-account-error" role="alert">{error}</p>}
      <button type="button" className="fuelcore-account-signout" disabled={busy} onClick={async()=>{setError('');setBusy(true);try{await onLogout();}catch(e){setError(e.message||'Unable to sign out. Please try again.');setBusy(false);}}}><i aria-hidden="true" className="fa-solid fa-arrow-right-from-bracket"/>{busy?'Signing out...':'Sign out'}</button>
    </motion.div>}</AnimatePresence>
  </div>;
}
