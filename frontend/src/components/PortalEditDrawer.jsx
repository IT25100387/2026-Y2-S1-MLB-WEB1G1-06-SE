import {useEffect,useRef} from 'react';
import {motion,useReducedMotion} from 'framer-motion';

export default function PortalEditDrawer({title,children,onClose,kicker='Edit record',busy=false,direct=false}){
  const ref=useRef(null),close=useRef(onClose),working=useRef(busy),reduced=useReducedMotion();
  useEffect(()=>{close.current=onClose;working.current=busy;},[onClose,busy]);
  useEffect(()=>{
    const previous=document.activeElement,overflow=document.body.style.overflow;
    document.body.style.overflow='hidden';
    const elements=()=>[...ref.current.querySelectorAll('input:not(:disabled),button:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]')].filter(item=>item.getClientRects().length);
    const frame=requestAnimationFrame(()=>elements()[0]?.focus());
    const key=event=>{
      if(event.key==='Escape'&&!working.current)close.current();
      if(event.key==='Tab'){const list=elements();if(event.shiftKey&&document.activeElement===list[0]){event.preventDefault();list.at(-1)?.focus();}else if(!event.shiftKey&&document.activeElement===list.at(-1)){event.preventDefault();list[0]?.focus();}}
    };
    window.addEventListener('keydown',key);
    return()=>{cancelAnimationFrame(frame);document.body.style.overflow=overflow;window.removeEventListener('keydown',key);if(previous?.isConnected)previous.focus({preventScroll:true});};
  },[]);
  return <motion.div className="admin-drawer-backdrop" initial={reduced||direct?false:{opacity:0}} animate={{opacity:1}} transition={{duration:.2}}><motion.section ref={ref} className="admin-drawer portal-edit-drawer" role="dialog" aria-modal="true" aria-label={title} initial={reduced||direct?false:{x:60,opacity:0}} animate={{x:0,opacity:1}} transition={{duration:.2}}><header><div><p>{kicker}</p><h2>{title}</h2></div><button type="button" disabled={busy} aria-label="Close dialog" onClick={onClose}><i aria-hidden="true" className="fa-solid fa-xmark"/></button></header>{children}</motion.section></motion.div>;
}
