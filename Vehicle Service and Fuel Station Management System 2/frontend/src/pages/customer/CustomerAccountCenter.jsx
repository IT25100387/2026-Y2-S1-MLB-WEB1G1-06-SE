import {Input} from '../../components/Validation';
import {useEffect,useState} from 'react';
import {Link,useLocation} from 'react-router-dom';
import {useAuth} from '../../context/AuthContext';
import {api,notice} from '../../lib/api';
import {Field} from '../../components/DataUI';

export default function CustomerAccountCenter() {
  const {account,reload}=useAuth(),location=useLocation();
  const tab=location.pathname.endsWith('/security')?'security':location.pathname.endsWith('/edit')?'edit':location.pathname.endsWith('/settings')?'settings':'profile';
  const [form,setForm]=useState(null),[password,setPassword]=useState({currentPassword:'',newPassword:'',confirmPassword:''}),[preferences,setPreferences]=useState({emailNotifications:true,smsNotifications:false});
  const [error,setError]=useState(''),[issues,setIssues]=useState({}),[busy,setBusy]=useState(false);
  const load=async()=>{try{const {user}=await api('/api/v1/account');setForm({fullName:user.fullName||'',email:user.email||'',phoneNumber:user.phoneNumber||'',address:user.address||'',city:user.city||''});setPreferences({emailNotifications:Boolean(user.emailNotifications),smsNotifications:Boolean(user.smsNotifications)});setError('');}catch(e){setError(e.message);}};
  useEffect(()=>{load();},[]);
  useEffect(()=>{setError('');setIssues({});setPassword({currentPassword:'',newPassword:'',confirmPassword:''});},[tab]);
  const submit=async e=>{
    e.preventDefault();setError('');const validation={};
    if(tab==='edit'){if(!form.fullName.trim())validation.fullName='Full name is required';if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))validation.email='Enter a valid email';}
    if(tab==='security'){if(!password.currentPassword)validation.currentPassword='Current password is required';if([...password.newPassword].length<15)validation.newPassword='Use at least 15 characters';if(password.newPassword!==password.confirmPassword)validation.confirmPassword='Passwords do not match';}
    setIssues(validation);if(Object.keys(validation).length)return;setBusy(true);
    try{if(tab==='edit'){await api('/api/v1/account',{method:'PUT',body:JSON.stringify(form)});await reload();notice('Profile updated successfully!');}else if(tab==='security'){await api('/api/v1/account/password',{method:'POST',body:JSON.stringify(password)});setPassword({currentPassword:'',newPassword:'',confirmPassword:''});notice('Password changed successfully!');}else{await api('/api/v1/account/settings',{method:'PATCH',body:JSON.stringify(preferences)});notice('Notification preferences saved!');}}
    catch(e){setError(e.message);}finally{setBusy(false);}
  };
  const name=account.fullName||account.username,initials=name.split(' ').map(word=>word[0]).join('').slice(0,2).toUpperCase();
  const tabs=[['profile','Your profile','/customer/profile','fa-user'],['edit','Personal details','/customer/profile/edit','fa-user-pen'],['security','Password','/customer/profile/security','fa-key'],['settings','Preferences','/customer/settings','fa-sliders']];
  return <div className="customer-account-center"><div className="customer-page-header"><p className="customer-eyebrow">Your account, your way</p><h1>Account center</h1><p>Keep your details up to date and make FuelCore feel like home.</p></div>
    <div className="customer-account-layout"><aside className="customer-account-sidebar"><div className="customer-account-identity"><span className="customer-avatar">{initials}</span><h2>{name}</h2><p>{account.email}</p><small>Customer account</small></div><nav aria-label="Account sections">{tabs.map(([key,label,path,icon])=><Link key={key} to={path} aria-current={tab===key?'page':undefined} className={tab===key?'is-active':''}><i aria-hidden="true" className={'fa-solid '+icon}/>{label}<i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>)}</nav></aside>
      <section className="customer-account-content" aria-labelledby="customer-account-title">
        <h2 id="customer-account-title">{tab==='profile'?'Good to have you here.':tab==='edit'?'Your personal details':tab==='security'?'Keep your account secure':'Choose your preferences'}</h2>
        {error&&<p role="alert" className="customer-error">{error}{!form&&<button onClick={load}>Retry</button>}</p>}
        {!form&&!error?<p className="customer-loading">Loading your account...</p>:form&&tab==='profile'?<><p className="customer-account-intro">Everything you need for your next visit, in one place.</p><dl className="customer-account-details">{[['Full name',form.fullName],['Email',form.email],['Phone number',form.phoneNumber],['City',form.city],['Address',form.address],['Account status',account.status||'Active']].map(([label,value])=><div key={label}><dt>{label}</dt><dd>{value||'Not added yet'}</dd></div>)}</dl><Link to="/customer/profile/edit" className="btn-accent customer-primary-button">Edit my details <i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link></>:form&&<form noValidate onSubmit={submit}>
          {tab==='edit'?<div className="customer-account-fields">{[['fullName','Full name',false],['email','Email address',false],['phoneNumber','Phone number',true],['city','City',true],['address','Address',true]].map(([key,label,optional])=><Field key={key} label={label} optional={optional} error={issues[key]}><Input validationKey={key} className="input-dark" type={key==='email'?'email':key==='phoneNumber'?'tel':'text'} value={form[key]} placeholder={optional?'optional':label} onChange={e=>setForm({...form,[key]:e.target.value})}/></Field>)}</div>:tab==='security'?<div className="customer-account-fields customer-password-fields">{[['currentPassword','Current password'],['newPassword','New password'],['confirmPassword','Confirm new password']].map(([key,label])=><Field key={key} label={label} error={issues[key]}><Input validationKey={key} className="input-dark" type="password" autoComplete={key==='currentPassword'?'current-password':'new-password'} value={password[key]} onChange={e=>setPassword({...password,[key]:e.target.value})}/></Field>)}</div>:<div className="customer-preferences">{[['emailNotifications','Email notifications','Your email notification preference.','fa-envelope'],['smsNotifications','SMS notifications','Your SMS notification preference.','fa-mobile-screen']].map(([key,label,description,icon])=><div key={key}><span><i aria-hidden="true" className={'fa-solid '+icon}/></span><div><h3>{label}</h3><p>{description}</p></div><button type="button" role="switch" aria-label={label} aria-checked={preferences[key]} className={`customer-switch ${preferences[key]?'is-on':''}`} onClick={()=>setPreferences({...preferences,[key]:!preferences[key]})}><span/></button></div>)}</div>}
          <button className="btn-accent customer-primary-button" disabled={busy}>{busy?'Saving...':tab==='edit'?'Save changes':tab==='security'?'Update password':'Save preferences'}<i aria-hidden="true" className="fa-solid fa-check"/></button>
        </form>}
      </section></div>
  </div>;
}
