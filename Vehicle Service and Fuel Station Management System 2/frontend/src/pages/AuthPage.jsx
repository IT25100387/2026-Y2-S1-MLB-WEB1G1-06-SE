import {Input} from '../components/Validation';
import {lazy,Suspense,useState} from 'react';
import {Link,useNavigate,useLocation,useSearchParams} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import {api} from '../lib/api';
import {validateFields} from '../lib/validation';
import {Field} from '../components/DataUI';
import './SignIn.css';

// Set VITE_ENABLE_QUICK_LOGIN=false to hide the temporary test-account popup.
// The lazy import keeps demo credentials out of production builds.
const TemporaryQuickLogin = import.meta.env.DEV && import.meta.env.VITE_ENABLE_QUICK_LOGIN !== 'false'
  ? lazy(() => import('../components/TemporaryQuickLogin'))
  : null;

function AuthForm({mode='login'}){const [form,setForm]=useState({}),[errors,setErrors]=useState({}),[busy,setBusy]=useState(false),[message,setMessage]=useState('');const {login}=useAuth();const navigate=useNavigate(),location=useLocation();const [params]=useSearchParams();const title={login:'Sign in',signup:'Create account',forgot:'Recover password',reset:'Reset password'}[mode];const fields=mode==='signup'?[['fullName','Full name'],['username','Username'],['email','Email','email'],['phoneNumber','Phone','tel',true],['password','Password','password'],['confirmPassword','Confirm password','password']]:mode==='forgot'?[['identifier','Username or email']]:mode==='reset'?[['newPassword','New password','password'],['confirmPassword','Confirm password','password']]:[['username','Username'],['password','Password','password']];const submit=async e=>{e.preventDefault();setMessage('');const issues=validateFields(fields.map(([key,label,type,optional])=>({key,label,type,optional,newPassword:type==='password'&&mode!=='login'})),form);fields.forEach(([key,label,,optional])=>{if(!optional&&!form[key]?.trim())issues[key]=`${label} is required`;});if(form.email&&!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))issues.email='Enter a valid email';if(mode==='signup'||mode==='reset'){const key=mode==='signup'?'password':'newPassword';if([...(form[key]||'')].length<15)issues[key]='Use at least 15 characters';if(form[key]!==form.confirmPassword)issues.confirmPassword='Passwords do not match';}setErrors(issues);if(Object.keys(issues).length)return;setBusy(true);try{if(mode==='login'){const role=await login(form.username,form.password);const base=role==='ROLE_CUSTOMER'?'/customer':'/dashboard';const requested=location.state?.from;navigate(role==='ROLE_CASHIER'?'/dashboard/pos':requested?.startsWith(base)?requested:base,{replace:true});}else{const path=mode==='signup'?'signup':mode==='forgot'?'forgot-password':'reset-password';const data=await api(`/api/auth/${path}`,{method:'POST',body:JSON.stringify({...form,token:params.get('token')})});setMessage(data.message);setForm({});}}catch(error){setErrors({...error.fields,form:error.message});}finally{setBusy(false);}};
const descriptions = {
  login: 'Enter your account details to continue.',
  signup: 'Create your customer account for bookings, updates and payments.',
  forgot: 'Enter your username or email to request a password reset link.',
  reset: 'Choose a new password for your FuelCore account.',
};
return (
  <main className={`fuelcore-signin auth-stage-${mode}`}>
    <div className="signin-corner-photo" aria-hidden="true"><img src="/signin-workshop.jpg" alt="" decoding="async" /></div>
    <header className="signin-header">
      <Link to="/" className="signin-brand">FUEL<span>CORE</span></Link>
      <Link to="/" className="signin-home"><i aria-hidden="true" className="fa-solid fa-arrow-left" />Back to home</Link>
    </header>
    <div className="signin-body">
      <div className="signin-stack">
      <section className="signin-card" aria-labelledby="auth-title">
        <div className="signin-form-pane">
          <p className="signin-eyebrow">Your FuelCore account</p>
          <h1 id="auth-title">{title}</h1>
          <p className="signin-description">{descriptions[mode]}</p>
          <form noValidate onSubmit={submit}>
            <div className="signin-fields">
              {fields.map(([key,label,type,optional]) => (
                <Field key={key} label={label} optional={optional} error={errors[key]}>
                  <Input validationKey={key} autoComplete={key.includes('Password')||key==='password'?(mode==='login'?'current-password':'new-password'):key==='username'?'username':undefined}
                    type={type||'text'} value={form[key]||''} onChange={e=>setForm({...form,[key]:e.target.value})}
                    placeholder={optional?'optional':label} className={`input-dark w-full text-sm ${errors[key]?'border-brand-accent':''}`} />
                </Field>
              ))}
            </div>
            {errors.form&&<p role="alert" className="signin-feedback text-brand-accent">{errors.form}</p>}
            {message&&<p role="status" className="signin-feedback text-green-400">{message}</p>}
            <button disabled={busy} className="btn-accent signin-submit disabled:opacity-40">{busy?'Please wait...':title}</button>
          </form>
        </div>
        <aside className="signin-aside">
          <div className="signin-account-copy">
          <p className="signin-register-label">{mode==='login'?'New to FuelCore?':'Already have an account?'}</p>
          <p className="signin-aside-description">{mode==='login'?'Keep your vehicles, bookings and invoices together.':'Sign in to return to your account and continue your journey.'}</p>
          </div>
          <nav className="signin-links" aria-label="Account options">
            <Link to={mode==='login'?'/signup':'/login'}>{mode==='login'?'Create account':'Sign in'}<i aria-hidden="true" className="fa-solid fa-arrow-right" /></Link>
            <Link to={mode==='forgot'?'/signup':'/forgot-password'}>{mode==='forgot'?'Create account':'Forgot password?'}</Link>
          </nav>
        </aside>
      </section>
      {/* Temporary testing controls sit separately so removal leaves the account card intact. */}
      {mode==='login'&&TemporaryQuickLogin&&<div className="signin-quick-login"><Suspense fallback={null}><TemporaryQuickLogin/></Suspense></div>}
      </div>
    </div>
  </main>
);
}

// Each stage gets fresh inputs and validation when moving between account pages.
export default function AuthPage({mode='login'}) {
  return <AuthForm key={mode} mode={mode} />;
}
