import {createContext,useContext,useEffect,useState,useCallback} from 'react';
import {Navigate,useLocation} from 'react-router-dom';
import {api,apiFetch} from '../lib/api';
const AuthContext=createContext(null);
export const useAuth=()=>useContext(AuthContext);
export function AuthProvider({children}) {
  const [account,setAccount]=useState(null),[loading,setLoading]=useState(true);
  const clear=useCallback(()=>{setAccount(null);['userRole','username','email','accountUsername'].forEach(key=>localStorage.removeItem(key));},[]);
  const reload=useCallback(async()=>{try {const data=await api('/api/auth/me'); setAccount({...data.user,role:data.role});localStorage.setItem('userRole',data.role);localStorage.setItem('username',data.user.fullName||data.user.username);localStorage.setItem('accountUsername',data.user.username);localStorage.setItem('email',data.user.email||'');return data.role;} catch(error){clear();if(error.status!==401)throw error;} finally{setLoading(false);}},[clear]);
  useEffect(()=>{reload().catch(()=>{});window.addEventListener('fuelcore:session-expired',clear);return()=>window.removeEventListener('fuelcore:session-expired',clear);},[reload,clear]);
  const login=async(username,password)=>{await apiFetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({username,password}).toString()});return reload();};
  const logout=async()=>{await api('/api/auth/logout',{method:'POST'});clear();};
  return <AuthContext.Provider value={{account,loading,login,logout,reload}}>{children}</AuthContext.Provider>;
}
export function RequireRole({roles,children}) {const {account,loading}=useAuth();const location=useLocation();if(loading)return <div className="p-12 text-center text-gray-400">Loading your account?</div>;if(!account)return <Navigate to="/login" replace state={{from:location.pathname}}/>;if(roles&&!roles.includes(account.role))return <div className="p-12 text-center"><h1 className="text-xl font-black uppercase">Access unavailable</h1><p className="my-4 text-gray-400">This page is outside your role?s workspace.</p><a className="btn-accent px-6 py-3" href={account.role==='ROLE_CUSTOMER'?'/customer':'/dashboard'}>Open my workspace</a></div>;return children;}
