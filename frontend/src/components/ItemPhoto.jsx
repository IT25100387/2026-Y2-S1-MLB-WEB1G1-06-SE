import {Input} from './Validation';
import {normalizePayload} from '../lib/validation';
import {useEffect,useState} from 'react';
import './ItemPhoto.css';
export const defaultItemPhoto=kind=>kind==='service'?'/landing-service-repairs.jpg':'/landing-parts-essentials.jpg';
export function ItemPhoto({src,kind='part',alt='',className=''}){
  const [failed,setFailed]=useState('');
  return <img className={'item-photo '+className} src={src&&failed!==src?src:defaultItemPhoto(kind)} alt={alt} loading="lazy" onError={()=>{if(src)setFailed(src);}}/>;
}
export function PhotoEditor({kind='part',saved,file,removed,onChange,onRemove,error}){
  const [preview,setPreview]=useState('');
  useEffect(()=>{if(!file){setPreview('');return;}const url=URL.createObjectURL(file);setPreview(url);return()=>URL.revokeObjectURL(url);},[file]);
  return <section className="item-photo-editor"><div><ItemPhoto kind={kind} src={preview||(!removed?saved:'')} alt={kind==='service'?'Service photo preview':'Spare part photo preview'}/></div><div><label><span>{kind==='service'?'Service photo':'Spare part photo'} <small>(optional)</small></span><Input aria-label={kind==='service'?'Service photo':'Spare part photo'} type="file" accept="image/png,image/jpeg,image/webp" onChange={event=>{const input=event.target;onChange(input.files?.[0]||null);input.value='';}}/></label><p>PNG, JPEG or WebP. Maximum 5 MB. A default picture is used when no photo is saved.</p>{(file||saved&&!removed||error)&&<button type="button" className="manager-service-action" onClick={onRemove}>Use default picture</button>}{error&&<p role="alert" className="manager-inventory-error">{error}</p>}</div></section>;
}
export function photoError(file){return file&&(!file.size?'Select a nonempty photo':file.size>5_000_000?'Use a photo under 5 MB':!['image/png','image/jpeg','image/webp'].includes(file.type)?'Select a PNG, JPEG or WebP photo':'');}
export function photoPayload(key,record,file,removed){const body=new FormData();body.append(key,new Blob([JSON.stringify(normalizePayload(record))],{type:'application/json'}));if(file)body.append('image',file);body.append('removeImage',String(removed));return body;}
