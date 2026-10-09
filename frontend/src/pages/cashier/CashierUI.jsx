import {useEffect,useRef} from 'react';
import {motion,useReducedMotion} from 'framer-motion';
import {getStatusTone} from '../../lib/tableUtils';
export function CashierState({value}){const tone=getStatusTone(value);return <span className={`cashier-state status-${tone}`}>{value||'Unknown'}</span>;}
export function CashierModal({title,onClose,busy=false,children}){
 const box=useRef(null),close=useRef(onClose),working=useRef(busy),reduced=useReducedMotion();
 useEffect(()=>{close.current=onClose;working.current=busy;},[onClose,busy]);
 useEffect(()=>{const previous=document.activeElement,overflow=document.body.style.overflow;document.body.style.overflow='hidden';const focusable=()=>[...box.current.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]')].filter(el=>el.getClientRects().length);const timer=requestAnimationFrame(()=>focusable()[0]?.focus());const keyboard=event=>{if(event.key==='Escape'&&!working.current)close.current();if(event.key==='Tab'){const nodes=focusable();if(event.shiftKey&&document.activeElement===nodes[0]){event.preventDefault();nodes.at(-1)?.focus();}else if(!event.shiftKey&&document.activeElement===nodes.at(-1)){event.preventDefault();nodes[0]?.focus();}}};window.addEventListener('keydown',keyboard);return()=>{cancelAnimationFrame(timer);window.removeEventListener('keydown',keyboard);document.body.style.overflow=overflow;if(previous?.isConnected)previous.focus();};},[]);
 return <motion.div className="cashier-modal-backdrop" initial={reduced?false:{opacity:0}} animate={{opacity:1}}><motion.section className="cashier-modal" role="dialog" aria-modal="true" aria-label={title} ref={box} initial={reduced?false:{y:12,opacity:0}} animate={{y:0,opacity:1}} transition={{duration:.18}}><header><h2>{title}</h2><button disabled={busy} onClick={onClose} aria-label="Close dialog"><i aria-hidden="true" className="fa-solid fa-xmark"/></button></header>{children}</motion.section></motion.div>;
}
