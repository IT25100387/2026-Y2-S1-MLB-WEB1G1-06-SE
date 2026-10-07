import {createContext,useContext,useEffect,useId,useLayoutEffect,useRef,useState} from 'react';
import {inferKey,relatedErrors,ruleFor,validateValue} from '../lib/validation';
import './Validation.css';

export const FieldValidationContext=createContext(null);
const Registry=createContext(null);
export function RequiredMark(){return <span className="required-mark" aria-hidden="true"> *</span>;}
export function ValidationProvider({children}){
  const controls=useRef(new Map()),lastForm=useRef(null);
  useEffect(()=>{
    const submit=event=>{
      const active=[...controls.current.values()].filter(control=>control.node?.form===event.target&&!control.node.disabled&&!control.node.readOnly);
      const values=Object.fromEntries(active.map(control=>[control.key,control.node.value])),related=relatedErrors(values);
      let first;
      for(const control of active)if(control.check(values,related[control.key])&&!first)first=control.node;
      if(first){event.preventDefault();event.stopPropagation();first.focus({preventScroll:false});}
      else lastForm.current=event.target;
    };
    const server=event=>{if(lastForm.current?.isConnected)for(const control of controls.current.values())if(control.node?.form===lastForm.current){const fields=event.detail.fields,message=fields[control.key]||Object.entries(fields).find(([key])=>key.endsWith('.'+control.key))?.[1];if(message)control.server(message);}};
    document.addEventListener('submit',submit,true);
    window.addEventListener('fuelcore:field-errors',server);
    return()=>{document.removeEventListener('submit',submit,true);window.removeEventListener('fuelcore:field-errors',server);};
  },[]);
  return <Registry.Provider value={controls}>{children}</Registry.Provider>;
}
function Control({tag='input',validationKey,validation={},showValidation=true,onChange,onBlur,onKeyDown,children,...props}){
  const field=useContext(FieldValidationContext),registry=useContext(Registry),ref=useRef(null),identity=useId(),latest=useRef(null);
  const [issue,setIssue]=useState(''),touched=useRef(false);
  const keyLabel=validationKey?.replace(/([a-z])([A-Z])/g,'$1 $2');
  const label=validation.label||field?.label||props['aria-label']||keyLabel||props.placeholder||'This field';
  const key=validationKey||props.name||inferKey(label);
  const rule=ruleFor(key,label,{...validation,...(props.type?{type:props.type}:{}),...(props.min!=null?{min:props.min}:{}),...(props.max!=null?{max:props.max}:{}),...(props.maxLength!=null?{maxLength:props.maxLength}:{}),...(props.step&&props.step!=='any'?{step:props.step}:{}),required:props.required??(props['aria-required']==='true'||props['aria-required']===true?true:field?!field.optional:false)});
  if(props.type==='password')rule.newPassword=validation.newPassword??(props.autoComplete!=='current-password'&&key!=='currentPassword');
  if(props.type==='password'&&key==='password'&&window.location.pathname==='/login')rule.newPassword=false;
  if(tag==='textarea')rule.multiline=true;
  if(tag==='select')rule.kind='choice';
  const skipped=['checkbox','radio','file','hidden'].includes(props.type);
  const check=(values={},related)=>{
    const node=ref.current;if(!node||node.disabled||node.readOnly||skipped)return '';
    const group=node.form||node.closest('[role="group"]')||node.parentElement;
    if(registry)values={...Object.fromEntries([...registry.current.values()].filter(control=>control.node&&!control.node.disabled&&(control.node.form||control.node.closest('[role="group"]')||control.node.parentElement)===group).map(control=>[control.key,control.node.value])),...values};
    related=related||relatedErrors(values)[key];
    const options=tag==='select'?[...node.options].filter(option=>!option.disabled).map(option=>option.value):null;
    const message=node.validity.badInput?'Enter a valid number':validateValue(node.value,{...rule,...(options?{options}:{})},values);
    const result=related||message;
    setIssue(result);node.setCustomValidity(result);touched.current=true;
    return result;
  };
  useLayoutEffect(()=>{
    latest.current={key,check,server:message=>{setIssue(message);touched.current=true;ref.current?.setCustomValidity(message);}};
  });
  useEffect(()=>{
    if(!registry)return;
    const control={get node(){return ref.current;},get key(){return latest.current.key;},check:(...args)=>latest.current.check(...args),server:message=>latest.current.server(message)};
    const registered=registry.current;
    registered.set(identity,control);return()=>registered.delete(identity);
  },[registry,identity]);
  useEffect(()=>{if(props.disabled||props.readOnly){if(ref.current)ref.current.setCustomValidity('');}},[props.disabled,props.readOnly]);
  const numeric=rule.kind==='number',inputType=props.type||(rule.kind==='email'?'email':rule.kind==='phone'?'tel':'text');
  const errorId='field-validation-'+identity,external=field?.error;
  const attributes={...props,ref,name:props.name||key,'data-validation-key':key,'aria-invalid':!!(issue||external||props['aria-invalid']),'aria-required':!skipped&&rule.required||undefined,'aria-describedby':[props['aria-describedby'],issue&&showValidation&&!external?errorId:null].filter(Boolean).join(' ')||undefined,
    onChange:event=>{const node=event.target;node.setCustomValidity('');onChange?.(event);if(touched.current)check();},
    onBlur:event=>{if(!skipped)check();onBlur?.(event);},
    onKeyDown:event=>{if(numeric&&inputType==='number'&&['e','E','+','-'].includes(event.key)&&!(event.ctrlKey||event.metaKey)){event.preventDefault();return;}onKeyDown?.(event);}
  };
  if(tag==='input')attributes.type=inputType;
  if(!skipped&&tag!=='select'){
    attributes.maxLength=rule.maxLength;
    if(numeric){attributes.min=rule.min;attributes.max=rule.max;attributes.step=props.step??(rule.integer?'1':'0.01');attributes.inputMode=props.inputMode??(rule.integer?'numeric':'decimal');}
    if(rule.kind==='date'){attributes.min=rule.min;attributes.max=rule.max;}
  }
  const Tag=tag;
  return <><Tag {...attributes}>{children}</Tag>{issue&&showValidation&&!external&&<span id={errorId} className="field-validation-error" role="alert">{issue}</span>}</>;
}
export function Input(props){return <Control {...props}/>;}
export function TextArea(props){return <Control tag="textarea" {...props}/>;}
export function Select(props){return <Control tag="select" {...props}/>;}
