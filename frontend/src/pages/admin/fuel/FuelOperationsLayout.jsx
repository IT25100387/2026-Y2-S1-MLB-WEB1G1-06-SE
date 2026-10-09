import {useCallback,useEffect,useRef,useState} from 'react';
import {Outlet} from 'react-router-dom';
import {api,confirmAction,notice} from '../../../lib/api';
import {ruleFor,validateValue} from '../../../lib/validation';
import {FuelOperationsContext} from './FuelOperationsContext';
import './FuelOperations.css';

export default function FuelOperationsLayout(){
  const [data,setData]=useState(null),[forms,setForms]=useState({}),[errors,setErrors]=useState({}),[busy,setBusy]=useState(new Set());
  const dirty=useRef(new Set()),day=useRef(null),requests=useRef(0),pending=useRef(new Set());
  const load=useCallback(async()=>{
    const request=++requests.current;
    try{
      const feed=await api('/api/fuel-operations/sales');
      if(request!==requests.current)return;
      const changed=day.current!==feed.stationDay;
      if(changed){
        if(day.current)notice('A new station day started at 5 a.m.');
        day.current=feed.stationDay;dirty.current.clear();setErrors({});
      }else setErrors(old=>({...old,load:''}));
      setData(feed);
      setForms(old=>{
        const next={};
        for(const row of feed.currentFigures){
          const id=row.pump.id;
          next[id]=!changed&&dirty.current.has(id)&&!row.confirmed&&old[id]?old[id]:{
            pumpStatus:row.pumpStatus,litersSold:row.litersSold,collectedAmount:row.collectedAmount,deactivationReason:row.deactivationReason||''
          };
        }
        return next;
      });
    }catch(error){if(request===requests.current)setErrors(old=>({...old,load:error.message}));}
  },[]);
  useEffect(()=>{
    load();
    const tick=setInterval(()=>{if(!document.hidden)load();},30000);
    window.addEventListener('fuelcore:refresh',load);
    return()=>{requests.current++;clearInterval(tick);window.removeEventListener('fuelcore:refresh',load);};
  },[load]);
  const change=(id,key,value)=>{
    dirty.current.add(id);
    setForms(old=>({...old,[id]:{...old[id],[key]:value,...(key==='pumpStatus'&&value==='Inactive'?{litersSold:0,collectedAmount:0}:{})}}));
    setErrors(old=>({...old,[id]:{...old[id],[key]:'',form:''}}));
  };
  const save=async(id,confirm)=>{
    if(pending.current.has(id)||data?.confirmedPumpIds.includes(id))return;
    const form=forms[id],issue={};
    if(!form)return;
    const inactive=form.pumpStatus==='Inactive';
    const number=value=>value!==''&&value!=null&&Number.isFinite(Number(value))&&Number(value)>=0;
    if(!inactive&&!number(form.litersSold))issue.litersSold='Enter nonnegative litres';
    if(!inactive&&!number(form.collectedAmount))issue.collectedAmount='Enter a nonnegative amount';
    if(inactive){const message=validateValue(form.deactivationReason,ruleFor('deactivationReason','Reason for deactivation',{required:confirm}));if(message)issue.deactivationReason=message;}
    else for(const [key,label] of [['litersSold','Fuel litres sold'],['collectedAmount','Collected amount']]){const message=validateValue(form[key],ruleFor(key,label,{required:true}));if(message)issue[key]=message;}
    setErrors(old=>({...old,[id]:issue}));
    if(Object.keys(issue).length)return;
    pending.current.add(id);setBusy(new Set(pending.current));
    try{
      if(confirm&&!(await confirmAction(inactive?'Confirm this pump as inactive for this station day? Its reason will be retained in history.':'Confirm these daily pump figures? Confirmed figures are retained in history.')))return;
      const result=await api(`/api/fuel-operations/sales/${confirm?'add':'draft'}`,{method:'POST',body:JSON.stringify({pumpId:id,...form,litersSold:inactive?0:Number(form.litersSold),collectedAmount:inactive?0:Number(form.collectedAmount)})});
      dirty.current.delete(id);
      if(confirm)setData(old=>old?{...old,confirmedPumpIds:[...new Set([...old.confirmedPumpIds,id])],currentFigures:old.currentFigures.map(row=>row.pump.id===id?{...row,...result.sale,confirmed:true}:row)}:old);
      await load();notice(confirm?'Pump figures confirmed':'Draft saved');
    }catch(error){setErrors(old=>({...old,[id]:{...error.fields,form:error.message}}));}
    finally{pending.current.delete(id);setBusy(new Set(pending.current));}
  };
  return <FuelOperationsContext.Provider value={{data,forms,errors,busy,load,change,save}}><Outlet/></FuelOperationsContext.Provider>;
}
