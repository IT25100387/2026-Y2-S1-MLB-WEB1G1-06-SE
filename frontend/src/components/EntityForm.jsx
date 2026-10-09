import {Input,TextArea} from './Validation';
import {useState} from 'react';
import {Field} from './DataUI';
import ChoiceInput from './ChoiceInput';
import {normalizePayload,placeholderFor,ruleFor,validateFields} from '../lib/validation';

export default function EntityForm({fields,initial={},onSave,submitLabel='Save'}){
  const [form,setForm]=useState(initial),[errors,setErrors]=useState({}),[busy,setBusy]=useState(false);
  const resolved=fields.map(field=>({...field,options:typeof field.options==='function'?field.options(form):field.options}));
  const change=(key,value)=>{
    const reset=fields.find(field=>field.key===key)?.resets||[];
    setForm(old=>({...old,[key]:value,...Object.fromEntries(reset.map(item=>[item,'']))}));
    setErrors(old=>({...old,[key]:'',...Object.fromEntries(reset.map(item=>[item,''])),form:''}));
  };
  const save=async event=>{
    event.preventDefault();
    const payload=normalizePayload(form),issues=validateFields(resolved,payload);
    for(const field of resolved){
      if(field.type==='number'||field.type==='year'||field.key==='manufactureYear')payload[field.key]=payload[field.key]===''||payload[field.key]==null?null:Number(payload[field.key]);
      if(field.type==='boolean')payload[field.key]=payload[field.key]===true||payload[field.key]==='true';
    }
    setErrors(issues);if(Object.keys(issues).length)return;
    setBusy(true);try{await onSave(payload);}catch(error){setErrors({...error.fields,form:error.message});}finally{setBusy(false);}
  };
  return <form noValidate onSubmit={save} className="space-y-5"><fieldset disabled={busy} className="grid sm:grid-cols-2 gap-5">{resolved.map(field=>{
    const validation={...field,options:field.allowCustom?undefined:field.options};
    const hasError=Boolean(errors[field.key]);
    const attributes={validation,validationKey:field.key,'aria-invalid':hasError,className:`input-dark w-full p-3 text-sm ${hasError?'border-red-500 !border-[#ef5555]':''}`,value:form[field.key]??'',onChange:event=>change(field.key,event.target.value),disabled:typeof field.disabled==='function'?field.disabled(form):field.disabled};
    return <Field key={field.key} label={field.label} error={errors[field.key]} optional={field.optional}>
      {field.options?<ChoiceInput {...attributes} maxRows={field.maxRows} options={field.options} allowCustom={field.allowCustom} placeholder={field.choicePlaceholder||placeholderFor(ruleFor(field.key,field.label),field.placeholder)} customPlaceholder={field.customPlaceholder}/>
        :field.type==='textarea'?<TextArea {...attributes} placeholder={field.placeholder}/>
        :<Input {...attributes} placeholder={field.placeholder} readOnly={typeof field.readOnly==='function'?field.readOnly(form):field.readOnly} type={field.type||'text'} step={field.type==='number'?(field.integer?'1':'0.01'):undefined}/>}
    </Field>;
  })}</fieldset>{errors.form&&<p role="alert" className="text-brand-accent text-[10px] font-bold uppercase">{errors.form}</p>}<button disabled={busy} className="btn-accent px-6 py-3 rounded-full text-xs uppercase font-bold disabled:opacity-40">{busy?(form.paymentMethod==='CARD'?'Waiting for PayHere confirmation…':'Saving…'):submitLabel}</button></form>;
}
