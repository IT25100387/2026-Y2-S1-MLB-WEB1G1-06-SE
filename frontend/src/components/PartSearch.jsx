import {useEffect,useId,useRef,useState} from 'react';
import {createPortal} from 'react-dom';
import {Input} from './Validation';
import './ThemedSelect.css';

/** Compact suggestions share the theme's single-line, yellow-highlighted select rows. */
export default function PartSearch({products,value,onChange,onSearch}){
  const id=useId(),anchor=useRef(null),menu=useRef(null),[open,setOpen]=useState(false),[active,setActive]=useState(0),[position,setPosition]=useState(null);
  const matches=products.filter(p=>[p.partName,p.category,p.id,'PRT-'+p.id].some(v=>String(v??'').toLowerCase().includes(value.trim().toLowerCase())));
  const visible=open&&value.trim()&&matches.length;
  useEffect(()=>{
    if(!visible)return;
    const place=()=>{const box=anchor.current.getBoundingClientRect(),height=Math.min(matches.length,3)*32+2,below=window.innerHeight-box.bottom-8;setPosition({left:box.left,width:box.width,top:below>=height?box.bottom:Math.max(4,box.top-height),height});};
    place();window.addEventListener('resize',place);window.addEventListener('scroll',place,true);
    const outside=e=>{if(!anchor.current?.contains(e.target)&&!menu.current?.contains(e.target))setOpen(false);};document.addEventListener('pointerdown',outside);
    return()=>{window.removeEventListener('resize',place);window.removeEventListener('scroll',place,true);document.removeEventListener('pointerdown',outside);};
  },[visible,matches.length]);
  useEffect(()=>{menu.current?.querySelector('[aria-selected="true"]')?.scrollIntoView({block:'nearest'});},[active]);
  const select=part=>{onChange(part.partName);onSearch(part.partName);setOpen(false);anchor.current?.querySelector('input')?.focus();};
  return <form ref={anchor} noValidate className="manager-service-search" onSubmit={event=>{event.preventDefault();onSearch(value.trim());setOpen(false);}}>
    <Input className="input-dark" maxLength={150} value={value} onChange={event=>{onChange(event.target.value);setOpen(true);setActive(0);}} onFocus={()=>setOpen(true)} aria-label="Search spare parts" role="combobox" aria-autocomplete="list" aria-expanded={Boolean(visible)} aria-controls={visible?id:undefined} aria-activedescendant={visible?id+'-'+Math.min(active,matches.length-1):undefined} autoComplete="off" placeholder="Search name, category or item #" onKeyDown={event=>{if(event.key==='Escape'){setOpen(false);return;}if(!visible)return;if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();setActive(current=>Math.max(0,Math.min(matches.length-1,current+(event.key==='ArrowDown'?1:-1))));}else if(event.key==='Enter'){event.preventDefault();select(matches[Math.min(active,matches.length-1)]);}else if(event.key==='Tab')setOpen(false);}}/>
    {value&&<button type="button" aria-label="Clear product search" onClick={()=>{onChange('');onSearch('');setOpen(false);}}><i aria-hidden="true" className="fa-solid fa-xmark"/></button>}
    <button aria-label="Search parts"><i aria-hidden="true" className="fa-solid fa-magnifying-glass"/></button>
    {visible&&position&&createPortal(<div ref={menu} id={id} role="listbox" aria-label="Matching spare parts" className="themed-select-menu" style={position}>{matches.map((part,index)=><div key={part.id} id={id+'-'+index} role="option" aria-selected={index===Math.min(active,matches.length-1)} className={'themed-select-option'+(index===active?' is-active':'')} onPointerMove={()=>setActive(index)} onPointerDown={event=>event.preventDefault()} onClick={()=>select(part)} title={part.partName+' · PRT-'+part.id}>{part.partName} · PRT-{part.id}</div>)}</div>,document.body)}
  </form>;
}
