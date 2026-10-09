import {createContext,useContext,useEffect,useId,useLayoutEffect,useRef,useState} from 'react';
import {flushSync} from 'react-dom';
import {inferKey,normalizePhone,placeholderFor,relatedErrors,ruleFor,sanitizeInput,validateValue} from '../lib/validation';
import ThemedSelect from './ThemedSelect';
import PhoneInput from './PhoneInput';
import DeliveryScheduleInput from './DeliveryScheduleInput';
import YearPicker from './YearPicker';
import {billingCountries,withCurrent} from '../lib/fieldOptions';
import './Validation.css';

export const FieldValidationContext=createContext(null);
const Registry=createContext(null);
export function RequiredMark(){return <span className="required-mark" aria-hidden="true"> *</span>;}
export function ValidationProvider({children}){
  const controls=useRef(new Map()),lastForm=useRef(null);
  useEffect(()=>{
    const submit=event=>{
      const active=[...controls.current.values()].filter(control=>control.node?.form===event.target&&!control.node.readOnly&&(!control.node.matches(':disabled')||control.required));
      flushSync(()=>{for(const control of active)control.normalize();});
      const remaining=active.filter(control=>control.node?.form===event.target),values=Object.fromEntries(remaining.map(control=>[control.key,control.node.value])),related=relatedErrors(values);
      let first;
      for(const control of remaining)if(control.check(values,related[control.key])&&!first&&!control.node.matches(':disabled'))first=control.node;
      if(first){event.preventDefault();event.stopPropagation();(first.closest('.phone-input')?.querySelector('[data-phone-national]')||first.closest('.delivery-schedule-input')?.querySelector('button')||first.closest('.year-picker')?.querySelector('button')||first.closest('.themed-select')?.querySelector('button')||first).focus({preventScroll:false});}
      else lastForm.current=event.target;
    };
    const server=event=>{if(lastForm.current?.isConnected)for(const control of controls.current.values())if(control.node?.form===lastForm.current){const fields=event.detail.fields,message=fields[control.key]||Object.entries(fields).find(([key])=>key.endsWith('.'+control.key))?.[1];if(message)control.server(message);}};
    document.addEventListener('submit',submit,true);
    window.addEventListener('fuelcore:field-errors',server);
    return()=>{document.removeEventListener('submit',submit,true);window.removeEventListener('fuelcore:field-errors',server);};
  },[]);
  return <Registry.Provider value={controls}>{children}</Registry.Provider>;
}
function Control({tag='input',validationKey,validation={},showValidation=true,onChange,onBlur,onFocus,onKeyDown,maxRows,children,...props}){
  const field=useContext(FieldValidationContext),registry=useContext(Registry),ref=useRef(null),identity=useId(),latest=useRef(null);
  const [issue,setIssue]=useState(''),touched=useRef(false),acceptedValue=useRef('');
  const keyLabel=validationKey?.replace(/([a-z])([A-Z])/g,'$1 $2');
  const label=validation.label||field?.label||props['aria-label']||keyLabel||props.placeholder||'This field';
  const key=validationKey||props.name||inferKey(label);
  const rule=ruleFor(key,label,{...validation,...(props.type?{type:props.type}:{}),...(props.min!=null?{min:props.min}:{}),...(props.max!=null?{max:props.max}:{}),...(props.maxLength!=null?{maxLength:props.maxLength}:{}),...(props.step&&props.step!=='any'?{step:props.step}:{}),required:props.required??(props['aria-required']==='true'||props['aria-required']===true?true:field?!field.optional:false)});
  if(props.type==='password')rule.newPassword=validation.newPassword??(props.autoComplete!=='current-password'&&key!=='currentPassword');
  if(props.type==='password'&&key==='password'&&window.location.pathname==='/login')rule.newPassword=false;
  if(tag==='textarea')rule.multiline=true;
  if(tag==='select')rule.kind='choice';
  const skipped=['checkbox','radio','file','hidden'].includes(props.type);
  const normalize=()=>{
    const node=ref.current;if(!node||skipped||tag==='select'||node.matches(':disabled')||node.readOnly)return;
    const clean=sanitizeInput(node.value,rule,{final:true,previous:node.value});
    if(clean!==node.value){node.value=clean;acceptedValue.current=clean;onChange?.({target:node,currentTarget:node});}
  };
  const check=(values={},related)=>{
    const node=ref.current;if(!node||(node.matches(':disabled')&&!rule.required)||node.readOnly||skipped)return '';
    const group=node.form||node.closest('[role="group"]')||node.parentElement;
    if(registry)values={...Object.fromEntries([...registry.current.values()].filter(control=>control.node&&!control.node.matches(':disabled')&&(control.node.form||control.node.closest('[role="group"]')||control.node.parentElement)===group).map(control=>[control.key,control.node.value])),...values};
    related=related||relatedErrors(values)[key];
    const options=tag==='select'?[...node.options].filter(option=>!option.disabled).map(option=>option.value):null;
    const message=node.validity.badInput?'Enter a valid number':validateValue(node.value,{...rule,...(options?{options}:{})},values);
    const result=related||message;
    setIssue(result);node.setCustomValidity(result);touched.current=true;
    return result;
  };
  useLayoutEffect(()=>{
    acceptedValue.current=ref.current?.value??'';
    latest.current={key,required:Boolean(rule.required),check,normalize,server:message=>{setIssue(message);touched.current=true;ref.current?.setCustomValidity(message);}};
  });
  useEffect(()=>{
    if(!registry)return;
    const control={get node(){return ref.current;},get key(){return latest.current.key;},get required(){return latest.current?.required;},normalize:()=>latest.current.normalize(),check:(...args)=>latest.current.check(...args),server:message=>latest.current.server(message)};
    const registered=registry.current;
    registered.set(identity,control);return()=>registered.delete(identity);
  },[registry,identity]);
  useEffect(()=>{if(props.readOnly||(props.disabled&&!rule.required)){if(ref.current)ref.current.setCustomValidity('');}},[props.disabled,props.readOnly,rule.required]);
  const numeric=rule.kind==='number',inputType=props.type||(rule.kind==='email'?'email':rule.kind==='phone'?'tel':'text');
  const errorId='field-validation-'+identity,hintId='field-hint-'+identity,external=field?.error;
  const attributes={...props,name:props.name||key,'data-validation-key':key,'aria-invalid':!!(issue||external||props['aria-invalid']),'aria-required':!skipped&&rule.required||undefined,'aria-describedby':[props['aria-describedby'],rule.kind==='email'||rule.key==='address'?hintId:null,issue&&showValidation&&!external?errorId:null].filter(Boolean).join(' ')||undefined,
    onChange:event=>{
      const node=event.target;
      if(!skipped&&tag!=='select'&&!event.nativeEvent?.isComposing){
        const cursor=node.selectionStart,raw=node.value,previous=acceptedValue.current,clean=sanitizeInput(raw,rule,{previous});
        node.value=clean;acceptedValue.current=clean;
        if(cursor!=null&&node.setSelectionRange&&clean!==raw){
          const before=clean===previous&&raw.length>clean.length?cursor-(raw.length-clean.length):sanitizeInput(raw.slice(0,cursor),rule,{previous}).length;
          const position=Math.max(0,Math.min(clean.length,before));node.setSelectionRange(position,position);
        }
      }
      node.setCustomValidity('');setIssue('');onChange?.(event);if(touched.current)check();
    },
    onBlur:event=>{if(!skipped&&tag!=='select'){const clean=sanitizeInput(event.target.value,rule,{final:true,previous:acceptedValue.current});acceptedValue.current=clean;if(clean!==event.target.value){event.target.value=clean;onChange?.(event);}}if(!skipped)check();onBlur?.(event);},
    onFocus:event=>onFocus?.(event),
    onKeyDown:event=>{if(numeric&&(['e','E','+','-'].includes(event.key)||rule.integer&&event.key==='.')&&!(event.ctrlKey||event.metaKey)){event.preventDefault();return;}onKeyDown?.(event);},
    onCompositionEnd:event=>{if(!skipped&&tag!=='select'){event.target.value=sanitizeInput(event.target.value,rule,{previous:acceptedValue.current});acceptedValue.current=event.target.value;onChange?.(event);if(touched.current)check();}}
  };
  if(tag==='select')attributes['aria-label']=props['aria-label']||label;
  // A text control exposes the caret needed to reject email characters in place.
  // Email keyboards/autofill and the shared email validator still apply.
  if(tag==='input')attributes.type=numeric||rule.kind==='email'?'text':rule.kind==='phone'?'tel':inputType;
  if(!skipped&&tag!=='select'){
    attributes.maxLength=rule.maxLength;
    if(rule.kind!=='date')attributes.placeholder=placeholderFor(rule,props.placeholder);
    if(rule.kind==='phone'){attributes.inputMode='tel';attributes.autoComplete=props.autoComplete||'tel';if(props.value!=null)attributes.value=normalizePhone(props.value);}
    if(rule.kind==='email'){attributes.inputMode='email';attributes.autoComplete=props.autoComplete||'email';}
    if(numeric){attributes.min=rule.min;attributes.max=rule.max;attributes.step=props.step??(rule.integer?'1':'0.01');attributes.inputMode=props.inputMode??(rule.integer?'numeric':'decimal');}
    if(rule.kind==='date'){attributes.min=rule.min;attributes.max=rule.max;}
  }
  const Tag=tag;
  return <>
    {tag==='select'?<ThemedSelect ref={ref} attributes={attributes} maxRows={maxRows}>{children}</ThemedSelect>
      :rule.kind==='phone'&&!skipped?<PhoneInput ref={ref} attributes={attributes} label={label}/>
      :rule.kind==='schedule'?<DeliveryScheduleInput ref={ref} attributes={attributes}/>
      :rule.kind==='year'&&!skipped?<YearPicker ref={ref} attributes={attributes} rule={rule}/>
      :<Tag {...attributes} ref={ref}>{children}</Tag>}
    {issue&&showValidation&&!external&&<span id={errorId} className="field-validation-error" role="alert">{issue}</span>}
    {rule.kind==='email'&&<span id={hintId} className="field-validation-hint">{String(props.value??'').length} / {rule.maxLength} characters; up to 64 before @.</span>}
    {rule.key==='address'&&<span id={hintId} className="field-validation-hint">Use 5–{rule.maxLength} characters, including the street or locality.</span>}
  </>;
}
export function Input(props){
  if(props.validationKey==='country'||props.validationKey==='rating'){
    const {type:_type,min:_min,max:_max,step:_step,...selectProps}=props;
    const options=props.validationKey==='country'?withCurrent(billingCountries,props.value):[1,2,3,4,5];
    return <Control tag="select" {...selectProps}><option value="">{props.validationKey==='country'?'Choose your billing country':'Choose a score from 1 to 5'}</option>{options.map(value=><option key={value} value={value}>{value}</option>)}</Control>;
  }
  return <Control {...props}/>;
}
export function TextArea(props){return <Control tag="textarea" {...props}/>;}
export function Select(props){return <Control tag="select" {...props}/>;}
