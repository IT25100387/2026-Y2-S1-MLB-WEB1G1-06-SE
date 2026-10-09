import {useEffect,useRef} from 'react';
import {motion,useReducedMotion} from 'framer-motion';
import {inventoryState} from './inventoryValues';
import './ManagerInventory.css';
import {usePortalTheme} from '../../../components/PortalTheme';
import {getStatusTone} from '../../../lib/tableUtils';

export function InventoryBadge({kind,value}){
  const state=inventoryState(kind,value);
  const tone=getStatusTone(state);
  return <span className={`manager-inventory-badge status-${tone}`}>{state}</span>;
}
export function InventoryMessages({workspace}){
  return <>{workspace.error&&<p role="alert" className="manager-inventory-error">{workspace.error} <button onClick={workspace.load}>Retry</button></p>}{workspace.relatedError&&<p role="alert" className="manager-inventory-error">Related inventory options could not be loaded: {workspace.relatedError} <button onClick={workspace.load}>Retry options</button></p>}{workspace.actionError&&<p role="alert" className="manager-inventory-error">{workspace.actionError}</p>}</>;
}
export function InventoryModal({title,kicker,variant='parts',children,onClose,busy=false}){
  const themed=usePortalTheme(),drawer=themed&&/^edit\b/i.test(title);
  const ref=useRef(null),close=useRef(onClose),working=useRef(busy),reduced=useReducedMotion();
  useEffect(()=>{close.current=onClose;working.current=busy;},[onClose,busy]);
  useEffect(()=>{
    const previous=document.activeElement,overflow=document.body.style.overflow;
    document.body.style.overflow='hidden';
    const focusable=()=>[...ref.current.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]')].filter(el=>el.getClientRects().length);
    const frame=requestAnimationFrame(()=>{const elements=focusable();(elements.find(el=>el.tagName==='INPUT')||elements[0])?.focus();});
    const key=event=>{
      if(event.key==='Escape'&&!working.current){event.preventDefault();close.current();}
      if(event.key==='Tab'){
        const elements=focusable(),first=elements[0],last=elements.at(-1);
        if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}
        else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}
      }
    };
    window.addEventListener('keydown',key);
    return()=>{cancelAnimationFrame(frame);document.body.style.overflow=overflow;window.removeEventListener('keydown',key);if(previous?.isConnected)previous.focus({preventScroll:true});};
  },[]);
  return <motion.div className={drawer?'admin-drawer-backdrop':'manager-inventory-backdrop'} initial={reduced?false:{opacity:0}} animate={{opacity:1}} transition={{duration:.2}}><motion.section ref={ref} role="dialog" aria-modal="true" aria-label={title} className={drawer?'admin-drawer portal-edit-drawer manager-inventory-modal manager-inventory-modal-'+variant:'manager-inventory-modal manager-inventory-modal-'+variant} initial={reduced?false:drawer?{x:60,opacity:0}:{scale:.98}} animate={drawer?{x:0,opacity:1}:{scale:1}} transition={{duration:.2}}><header><div><p>{kicker}</p><h2>{title}.</h2></div><button type="button" aria-label="Close dialog" disabled={busy} onClick={onClose}><i aria-hidden="true" className="fa-solid fa-xmark"/></button></header>{children}</motion.section></motion.div>;
}
