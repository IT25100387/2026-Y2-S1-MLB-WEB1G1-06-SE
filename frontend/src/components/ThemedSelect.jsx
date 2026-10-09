import {Children,isValidElement,useEffect,useId,useLayoutEffect,useRef,useState} from 'react';
import {createPortal} from 'react-dom';
import * as Flags from 'country-flag-icons/react/3x2';
import './ThemedSelect.css';

function readOptions(children,groupDisabled=false){
  return Children.toArray(children).flatMap(child=>{
    if(!isValidElement(child))return [];
    if(child.type==='option'){
      const label=Children.toArray(child.props.children).join('');
      return [{
        value:String(child.props.value??label),
        label,
        country:child.props['data-country']||null,
        title:child.props.title||label,
        search:String(child.props['data-search']||child.props.title||label),
        disabled:groupDisabled||!!child.props.disabled
      }];
    }
    return readOptions(child.props.children,groupDisabled||!!child.props.disabled);
  });
}

function renderOptionContent(option){
  if(!option)return null;
  if(option.country){
    const FlagComponent=Flags[String(option.country).toUpperCase()];
    return (
      <span className="themed-select-flag-row">
        {FlagComponent?<FlagComponent className="themed-select-flag-icon" aria-hidden="true"/>:null}
        <span className="themed-select-flag-label">{option.label}</span>
      </span>
    );
  }
  return option.label;
}

// The native control keeps form values and validation; the visible menu has
// consistent colours, compact rows and keyboard support across browsers.
export default function ThemedSelect({ref:controlRef,attributes,children,maxRows=8}){
  const id=useId(),trigger=useRef(null),menu=useRef(null),search=useRef({text:'',time:0});
  const [open,setOpen]=useState(false),[active,setActive]=useState(-1),[position,setPosition]=useState(null);
  const options=readOptions(children),selected=options.findIndex(option=>option.value===String(attributes.value??attributes.defaultValue??''));
  const disabled=attributes.disabled;
  const close=()=>setOpen(false);
  const move=(start,direction)=>{
    for(let offset=1;offset<=options.length;offset++){
      const index=(start+direction*offset+options.length)%options.length;
      if(!options[index].disabled){setActive(index);return;}
    }
  };
  const show=()=>{
    if(disabled||trigger.current?.matches(':disabled'))return;
    setActive(selected>=0&&!options[selected]?.disabled?selected:options.findIndex(option=>!option.disabled));setOpen(true);
  };
  const choose=index=>{
    const option=options[index];if(!option||option.disabled||trigger.current?.matches(':disabled'))return;
    const node=controlRef.current;
    Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype,'value').set.call(node,option.value);
    node.dispatchEvent(new Event('change',{bubbles:true}));
    close();trigger.current?.focus();
  };
  useLayoutEffect(()=>{
    if(!open)return;
    const place=()=>{
      const rect=trigger.current.getBoundingClientRect(),rowHeight=32,padding=2,gap=2;
      const requested=Math.min(Math.max(1,maxRows),options.length)*rowHeight+padding;
      const below=window.innerHeight-rect.bottom-10,above=rect.top-10;
      const up=below<Math.min(requested,128)&&above>below;
      const height=Math.max(rowHeight,Math.min(requested,up?above:below));
      const width=Math.min(rect.width,window.innerWidth-16);
      setPosition({left:Math.min(Math.max(8,rect.left),window.innerWidth-width-8),top:up?rect.top-height-gap:rect.bottom+gap,width,maxHeight:height});
    };
    place();window.addEventListener('resize',place);window.addEventListener('scroll',place,true);
    return()=>{window.removeEventListener('resize',place);window.removeEventListener('scroll',place,true);};
  },[open,maxRows,options.length]);
  useEffect(()=>{
    if(!open)return;
    const dismiss=event=>{if(!trigger.current?.contains(event.target)&&!menu.current?.contains(event.target))close();};
    const escape=event=>{if(event.key==='Escape'){event.preventDefault();event.stopPropagation();close();trigger.current?.focus();}};
    document.addEventListener('pointerdown',dismiss);document.addEventListener('keydown',escape,true);
    return()=>{document.removeEventListener('pointerdown',dismiss);document.removeEventListener('keydown',escape,true);};
  },[open]);
  useLayoutEffect(()=>{if(open&&position)menu.current?.children[active]?.scrollIntoView({block:'nearest'});},[active,open,position]);
  const keyboard=event=>{
    attributes.onKeyDown?.(event);if(event.defaultPrevented)return;
    if(['ArrowDown','ArrowUp','Home','End','Enter',' '].includes(event.key)){
      event.preventDefault();
      if(!open){show();return;}
      if(event.key==='ArrowDown')move(active,1);
      else if(event.key==='ArrowUp')move(active,-1);
      else if(event.key==='Home')setActive(options.findIndex(option=>!option.disabled));
      else if(event.key==='End')setActive(options.findLastIndex(option=>!option.disabled));
      else choose(active);
    }else if(event.key==='Tab')close();
    else if(event.key.length===1&&!event.ctrlKey&&!event.metaKey){
      event.preventDefault();const now=Date.now();
      search.current={text:(now-search.current.time<700?search.current.text:'')+event.key.toLowerCase(),time:now};
      const start=open?active:selected;
      for(let offset=1;offset<=options.length;offset++){
        const index=(Math.max(-1,start)+offset)%options.length;
        const opt=options[index];if(!opt.disabled&&(opt.label.toLowerCase().startsWith(search.current.text)||opt.search?.toLowerCase().includes(search.current.text))){if(!open)show();setActive(index);break;}
      }
    }
  };
  const {className,style,id:fieldId,onBlur,onFocus,...native}=attributes;
  return <span className="themed-select">
    <select {...native} ref={controlRef} className="themed-select-native" tabIndex={-1} aria-hidden="true" onInvalid={event=>{event.preventDefault();trigger.current?.focus();}}>{children}</select>
    <button id={fieldId} ref={trigger} type="button" role="combobox" className={(className||'input-dark')+' themed-select-trigger'} style={style}
      disabled={disabled} aria-label={attributes['aria-label']} aria-labelledby={attributes['aria-labelledby']} aria-describedby={attributes['aria-describedby']}
      aria-required={attributes['aria-required']} aria-invalid={attributes['aria-invalid']} aria-expanded={open} aria-haspopup="listbox" aria-controls={'select-menu-'+id}
      aria-activedescendant={open&&active>=0?'select-option-'+id+'-'+active:undefined}
      onClick={()=>open?close():show()} onKeyDown={keyboard} onFocus={onFocus} onBlur={event=>{close();onBlur?.({...event,target:controlRef.current,currentTarget:controlRef.current});}}>
      <span>{options[selected]?renderOptionContent(options[selected]):options[0]?renderOptionContent(options[0]):'Choose an option'}</span><svg aria-hidden="true" viewBox="0 0 12 8"><path d="m1 1 5 5 5-5"/></svg>
    </button>
    {open&&position&&createPortal(<div ref={menu} id={'select-menu-'+id} role="listbox" aria-label={attributes['aria-label']||'Available choices'} className="themed-select-menu" style={position} onPointerDown={event=>event.preventDefault()}>
      {options.map((option,index)=><div id={'select-option-'+id+'-'+index} key={option.value+'-'+index} role="option" aria-selected={index===selected} aria-disabled={option.disabled||undefined}
        className={`themed-select-option${index===active?' is-active':''}${index===selected?' is-selected':''}`} title={option.title||option.label} onPointerMove={()=>!option.disabled&&setActive(index)} onClick={()=>choose(index)}>{renderOptionContent(option)}</div>)}
      {!options.length&&<div className="themed-select-option" aria-disabled="true">No choices available</div>}
    </div>,document.body)}
  </span>;
}
