import {Input,Select,RequiredMark} from '../../../components/Validation';
import {useId,useState} from 'react';
import {Field} from '../../../components/DataUI';
import {InventoryModal} from './InventoryUI';
import {PhotoEditor,photoError,photoPayload} from '../../../components/ItemPhoto';
import {ruleFor,validateValue} from '../../../lib/validation';
import {parseGoods,serializeGoods} from './inventoryValues';

const specifications={
  fuel:[['fuelType','Fuel grade / type','text',false,'e.g. Lanka Petrol 92'],['currentStockLitres','Current stock (litres)','number',false,'e.g. 5000.00'],['maxCapacityLitres','Max storage capacity (litres)','number',false,'e.g. 10000.00'],['minStockWarning','Min stock warning level (litres)','number',false,'e.g. 1500.00'],['supplier','Supplier','supplier',true,'optional']],
  parts:[['partName','Part name','text',false,'e.g. Brake Pads'],['category','Category','text',false,'e.g. Braking System'],['stockQuantity','Stock quantity','stock',false,'Final stock qty'],['minStockWarning','Min stock warning','integer',false,'e.g. 10'],['buyingPrice','Buying price (Rs)','number',false,'e.g. 1500.00'],['sellingPrice','Selling price (Rs)','number',false,'e.g. 2500.00'],['supplier','Supplier name','supplier',true,'optional']],
  suppliers:[['supplierName','Supplier name','text',false,'e.g. John Doe'],['company','Company','text',false,'e.g. Acme Corp'],['contactNumber','Contact number','text',false,'e.g. +94 77 123 4567'],['email','Email','email',true,'optional'],['deliverySchedule','Delivery schedule','text',true,'optional'],['status','Status','status',false,'']]
};

function GoodsPicker({selected,onChange,options}){
  const [query,setQuery]=useState(''),[other,setOther]=useState(''),[issue,setIssue]=useState('');
  const goods=[...new Set([...options,...selected])],filtered=goods.filter(item=>item.toLowerCase().includes(query.toLowerCase()));
  const update=next=>{if(serializeGoods(next).length>255){setIssue('Selected goods must fit within 255 characters');return;}setIssue('');onChange(next);};
  const toggle=item=>update(selected.includes(item)?selected.filter(value=>value!==item):[...selected,item]);
  return <section className="manager-inventory-goods"><h3>Supplied goods/types</h3><Input className="input-dark" aria-label="Search goods" placeholder="Search goods..." value={query} onChange={event=>setQuery(event.target.value)}/><div className="manager-inventory-goods-options">{filtered.map(item=><label key={item} title={item}><Input type="checkbox" checked={selected.includes(item)} onChange={()=>toggle(item)}/><span>{item}</span></label>)}{!filtered.length&&<p>No matching goods.</p>}</div>{selected.length>0&&<div className="manager-inventory-selected"><p>Selected ({selected.length})</p><div>{selected.map(item=><button type="button" key={item} aria-label={'Remove '+item} onClick={()=>toggle(item)}>{item}<span aria-hidden="true">×</span></button>)}</div></div>}<div className="manager-inventory-other"><Input validationKey="goodsItem" validation={{minLength:2,maxLength:100}} aria-label="Other supplied item" className="input-dark" placeholder="Other supplied item (optional)" value={other} onChange={event=>setOther(event.target.value)}/><button type="button" className="manager-service-action" disabled={!other.trim()} onClick={()=>{const item=other.trim(),message=validateValue(item,ruleFor('goodsItem','Supplied item',{required:true,minLength:2,maxLength:100}));if(message){setIssue(message);return;}if(!selected.includes(item))update([...selected,item]);setOther('');}}>Add item</button></div>{issue&&<p className="field-validation-error" role="alert">{issue}</p>}</section>;
}

export default function InventoryForm({kind,initial,workspace}){
  const [photo,setPhoto]=useState(null),[removed,setRemoved]=useState(false),[photoIssue,setPhotoIssue]=useState('');
  const [form,setForm]=useState(initial),[errors,setErrors]=useState({}),[busy,setBusy]=useState(false),[mode,setMode]=useState(kind==='parts'&&initial.id?'add':'final');
  const [amount,setAmount]=useState(kind==='parts'&&initial.id?0:initial.stockQuantity??''),[goods,setGoods]=useState(()=>parseGoods(initial.fuelTypesSupplied));
  const stockLabel=useId(),edit=Boolean(initial.id),title=(edit?'Edit ':'Add ')+(kind==='fuel'?'fuel tank':kind==='parts'?'spare part':'supplier');
  const set=(key,value)=>{setForm(old=>({...old,[key]:value}));setErrors(old=>({...old,[key]:'',form:''}));};
  const current=Number(initial.stockQuantity)||0,finalStock=mode==='add'?current+Number(amount):Number(amount);
  const changeMode=next=>{if(next===mode)return;setMode(next);setAmount(amount===''?'':next==='final'?current+Number(amount):Math.max(0,Number(amount)-current));};
  const save=async event=>{
    event.preventDefault();if(busy)return;
    const issues={},payload={...form};
    for(const [key,label,type,optional] of specifications[kind]){
      const value=type==='stock'?amount:form[key];
      if(!optional&&(value==null||String(value).trim()===''))issues[key]=label+' is required';
      if(['number','integer','stock'].includes(type)){
        const number=Number(value),integer=type==='integer'||type==='stock';
        if(value!==''&&value!=null&&(!Number.isFinite(number)||number<0||integer&&(!Number.isInteger(number)||number>2147483647)||key==='maxCapacityLitres'&&number<=0))issues[key]=integer?'Enter a nonnegative whole number':'Enter a valid nonnegative amount'+(key==='maxCapacityLitres'?' above zero':'');
        payload[key]=type==='stock'?finalStock:number;
        if(type==='stock'&&(!Number.isInteger(finalStock)||finalStock>2147483647))issues[key]='Final stock must be a valid whole number';
      }else if(value!=null)payload[key]=String(value).trim();
      if(type==='email'&&value&&!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value))issues[key]='Enter a valid email';
    }
    if(kind==='fuel'){
      if(payload.currentStockLitres>payload.maxCapacityLitres)issues.currentStockLitres='Stock cannot exceed storage capacity';
      if(payload.minStockWarning>payload.maxCapacityLitres)issues.minStockWarning='Warning level cannot exceed storage capacity';
    }
    if(kind==='suppliers'){payload.fuelTypesSupplied=serializeGoods(goods);if(payload.fuelTypesSupplied.length>255)issues.form='Selected goods must fit within 255 characters';}
    setErrors(issues);if(Object.keys(issues).length||photoIssue)return;
    setBusy(true);try{await workspace.save(kind==='parts'&&(photo||removed)?photoPayload('part',payload,photo,removed):payload);}catch(error){setErrors({...error.fields,form:error.message});}finally{setBusy(false);}
  };
  const suppliers=workspace.related.suppliers||[],options=[...new Set([...(workspace.related.parts||[]).map(part=>part.partName),...(workspace.related.inventories||[]).map(tank=>tank.fuelType)].filter(Boolean))];
  const reload=async()=>{setBusy(true);try{await workspace.reloadSelected();}catch(error){setErrors(old=>({...old,form:error.message}));}finally{setBusy(false);}};
  return <InventoryModal title={title} kicker={edit?'Update '+(kind==='fuel'?'infrastructure':'inventory'):kind==='fuel'?'New infrastructure':'New inventory'} variant={kind} busy={busy} onClose={()=>workspace.setSelected(null)}><form noValidate onSubmit={save}><fieldset disabled={busy}><div className="manager-inventory-fields">{specifications[kind].map(([key,label,type,optional,placeholder])=>type==='stock'?<div key={key} className="manager-inventory-stock"><div><span id={stockLabel}>Stock quantity<RequiredMark/></span>{edit&&<div role="group" aria-label="Stock update mode">{[['add','Add'],['final','Set final']].map(([value,label])=><button type="button" key={value} aria-pressed={mode===value} className={mode===value?'is-active':''} onClick={()=>changeMode(value)}>{label}</button>)}</div>}</div><Input required validationKey="stockQuantity" validation={{label:"Stock quantity"}} max={2147483647} className="input-dark" aria-labelledby={stockLabel} aria-invalid={Boolean(errors.stockQuantity)} type="number" min="0" step="1" value={amount} placeholder={mode==='add'?'Qty to add':'Final stock qty'} onChange={event=>{setAmount(event.target.value);setErrors(old=>({...old,stockQuantity:'',form:''}));}}/>{edit&&<p>Current stock: {current} <span>After update: {Number.isFinite(finalStock)?finalStock:'—'}</span></p>}{errors.stockQuantity&&<p role="alert" className="manager-inventory-error">{errors.stockQuantity}</p>}</div>:<Field key={key} label={label} optional={optional} error={errors[key]}>{type==='supplier'||type==='status'?<div className="manager-inventory-select"><Select validationKey={key} className="input-dark" aria-label={label} aria-invalid={Boolean(errors[key])} value={form[key]??''} onChange={event=>set(key,event.target.value)}>{type==='supplier'?<><option value="">optional</option>{form[key]&&!suppliers.some(supplier=>supplier.supplierName===form[key])&&<option>{form[key]}</option>}{suppliers.map(supplier=><option key={supplier.id} value={supplier.supplierName}>{supplier.supplierName}</option>)}</>:<><option>Active</option><option>Inactive</option></>}</Select><i aria-hidden="true" className="fa-solid fa-chevron-down"/></div>:<Input validationKey={key} className="input-dark" aria-invalid={Boolean(errors[key])} type={type==='integer'?'number':type} min={type==='number'||type==='integer'?0:undefined} step={type==='number'?'0.01':type==='integer'?'1':undefined} value={form[key]??''} placeholder={placeholder} onChange={event=>set(key,event.target.value)}/>}</Field>)}</div>{kind==='parts'&&<PhotoEditor saved={initial.imageUrl} file={photo} removed={removed} error={photoIssue} onChange={file=>{const issue=photoError(file);setPhotoIssue(issue||'');if(!issue){setPhoto(file);setRemoved(false);}}} onRemove={()=>{setPhoto(null);setPhotoIssue('');setRemoved(true);}}/>}{kind==='suppliers'&&<GoodsPicker selected={goods} onChange={setGoods} options={options}/>}</fieldset>{workspace.relatedError&&<p className="manager-inventory-error" role="alert">Related options are unavailable. Existing selections are retained. <button type="button" onClick={workspace.load}>Retry options</button></p>}{errors.form&&<div className="manager-inventory-error" role="alert">{errors.form}{/changed while this form was open/i.test(errors.form)&&<button type="button" disabled={busy} onClick={reload}>Reload latest record (resets form)</button>}</div>}<footer><button type="button" disabled={busy} className="manager-service-action" onClick={()=>workspace.setSelected(null)}>Cancel</button><button disabled={busy} type="submit" className="btn-accent manager-inventory-submit">{busy?'Saving…':edit?'Save changes':kind==='fuel'?'Create tank':kind==='parts'?'Create part':'Register supplier'}</button></footer></form></InventoryModal>;
}
