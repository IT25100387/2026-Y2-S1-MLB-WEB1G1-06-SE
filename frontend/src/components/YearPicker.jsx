import {useEffect,useId,useLayoutEffect,useRef,useState} from 'react';
import {createPortal} from 'react-dom';
import './YearPicker.css';

export default function YearPicker({ref:controlRef,attributes,rule={}}){
  const id=useId();
  const triggerRef=useRef(null);
  const popupRef=useRef(null);
  const [open,setOpen]=useState(false);
  const [position,setPosition]=useState(null);

  const rawValue=attributes.value!=null&&attributes.value!==''?String(attributes.value):'';
  const currentYear=new Date().getFullYear();
  const minYear=Number(rule.min)||1900;
  const maxYear=Number(rule.max)||(currentYear+1);

  const initialAnchor=Number(rawValue)&&Number(rawValue)>=minYear&&Number(rawValue)<=maxYear?Number(rawValue):currentYear;
  const [rangeStart,setRangeStart]=useState(()=>Math.floor(initialAnchor/12)*12);

  useEffect(()=>{
    if(rawValue&&Number(rawValue)>=minYear&&Number(rawValue)<=maxYear){
      setRangeStart(Math.floor(Number(rawValue)/12)*12);
    }
  },[rawValue,minYear,maxYear]);

  const years=Array.from({length:12},(_,i)=>rangeStart+i);
  const rangeEnd=rangeStart+11;
  const disabled=attributes.disabled||attributes.readOnly;

  const select=y=>{
    if(disabled||y<minYear||y>maxYear)return;
    const str=String(y);
    const node=controlRef.current;
    if(node){
      node.value=str;
      attributes.onChange?.({target:node,currentTarget:node});
    }
    setOpen(false);
    triggerRef.current?.focus();
  };

  const clear=e=>{
    e?.stopPropagation();
    if(disabled)return;
    const node=controlRef.current;
    if(node){
      node.value='';
      attributes.onChange?.({target:node,currentTarget:node});
    }
    setOpen(false);
    triggerRef.current?.focus();
  };

  useLayoutEffect(()=>{
    if(!open)return;
    const place=()=>{
      if(!triggerRef.current)return;
      const rect=triggerRef.current.getBoundingClientRect();
      const popupWidth=Math.min(300,window.innerWidth-16);
      const popupHeight=255;
      const below=window.innerHeight-rect.bottom-10;
      const above=rect.top-10;
      const up=below<popupHeight&&above>below;

      setPosition({
        left:Math.min(Math.max(8,rect.left),window.innerWidth-popupWidth-8),
        top:up?Math.max(8,rect.top-popupHeight-6):rect.bottom+6,
        width:popupWidth,
      });
    };
    place();
    window.addEventListener('resize',place);
    window.addEventListener('scroll',place,true);
    return()=>{
      window.removeEventListener('resize',place);
      window.removeEventListener('scroll',place,true);
    };
  },[open]);

  useEffect(()=>{
    if(!open)return;
    const dismiss=event=>{
      if(!triggerRef.current?.contains(event.target)&&!popupRef.current?.contains(event.target)){
        setOpen(false);
      }
    };
    const onKey=event=>{
      if(event.key==='Escape'){
        event.preventDefault();
        event.stopPropagation();
        setOpen(false);
        triggerRef.current?.focus();
      }
    };
    document.addEventListener('pointerdown',dismiss);
    document.addEventListener('keydown',onKey,true);
    return()=>{
      document.removeEventListener('pointerdown',dismiss);
      document.removeEventListener('keydown',onKey,true);
    };
  },[open]);

  const handleKeyDown=event=>{
    if(disabled)return;
    if(['Enter',' ','ArrowDown'].includes(event.key)){
      event.preventDefault();
      setOpen(o=>!o);
    }
  };

  const placeholder=attributes.placeholder||'Select year';

  return (
    <div className="year-picker">
      <input
        {...attributes}
        ref={controlRef}
        type="hidden"
        value={rawValue}
      />
      <button
        ref={triggerRef}
        type="button"
        id={attributes.id}
        name={attributes.name?attributes.name+'Trigger':undefined}
        disabled={disabled}
        aria-label={attributes['aria-label']||rule.label||'Manufacture year'}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={'year-dialog-'+id}
        aria-invalid={attributes['aria-invalid']}
        aria-required={attributes['aria-required']}
        className={(attributes.className||'input-dark')+' year-picker-trigger'}
        onClick={()=>!disabled&&setOpen(o=>!o)}
        onKeyDown={handleKeyDown}
        onBlur={()=>attributes.onBlur?.({target:controlRef.current})}
      >
        <span className={rawValue?'year-picker-text':'year-picker-placeholder'}>
          {rawValue||placeholder}
        </span>
        <div className="year-picker-icons">
          {rawValue&&!rule.required&&!disabled&&(
            <span
              role="button"
              tabIndex={-1}
              aria-label="Clear year"
              className="year-picker-clear"
              onClick={clear}
            >
              ×
            </span>
          )}
          <i aria-hidden="true" className="fa-solid fa-calendar year-picker-cal-icon" />
        </div>
      </button>

      {open&&position&&createPortal(
        <div
          ref={popupRef}
          id={'year-dialog-'+id}
          role="dialog"
          aria-label="Year calendar"
          className="year-picker-popup"
          style={{
            position:'fixed',
            left:position.left,
            top:position.top,
            width:position.width,
            zIndex:10000,
          }}
          onPointerDown={e=>e.stopPropagation()}
        >
          <div className="year-picker-header">
            <button
              type="button"
              className="year-picker-nav-btn"
              aria-label="Previous 12 years"
              disabled={rangeStart<=minYear}
              onClick={()=>setRangeStart(s=>Math.max(minYear-(minYear%12),s-12))}
            >
              ‹
            </button>
            <span className="year-picker-range-title">
              {rangeStart} – {rangeEnd}
            </span>
            <button
              type="button"
              className="year-picker-nav-btn"
              aria-label="Next 12 years"
              disabled={rangeEnd>=maxYear}
              onClick={()=>setRangeStart(s=>s+12)}
            >
              ›
            </button>
          </div>

          <div className="year-picker-grid" role="grid">
            {years.map(y=>{
              const isSelected=String(y)===rawValue;
              const isCurrent=y===currentYear;
              const isDisabled=y<minYear||y>maxYear;

              return (
                <button
                  key={y}
                  type="button"
                  disabled={isDisabled}
                  aria-pressed={isSelected}
                  className={`year-picker-cell ${isSelected?'is-selected':''} ${isCurrent?'is-current':''}`}
                  onClick={()=>select(y)}
                >
                  {y}
                  {isCurrent&&!isSelected&&<span className="year-picker-curr-indicator" title="Current year" />}
                </button>
              );
            })}
          </div>

          <div className="year-picker-footer">
            <button
              type="button"
              className="year-picker-quick-btn"
              onClick={()=>{
                setRangeStart(Math.floor(currentYear/12)*12);
                select(currentYear);
              }}
            >
              This Year ({currentYear})
            </button>
            {rawValue&&(
              <button
                type="button"
                className="year-picker-quick-clear"
                onClick={clear}
              >
                Clear
              </button>
            )}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
