import {Navigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import AdminHome from './admin/AdminHome';
import ManagerHome from './manager/ManagerHome';

export default function Dashboard(){
  const {account}=useAuth();
  if(account?.role==='ROLE_ADMIN')return <AdminHome/>;
  if(account?.role==='ROLE_MANAGER')return <ManagerHome/>;
  if(account?.role==='ROLE_CASHIER')return <Navigate to="/dashboard/pos" replace/>;
  if(account?.role==='ROLE_MECHANIC')return <Navigate to="/dashboard/mechanic-workspace" replace/>;
  return null;
}
